package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AssetDatabase
import com.example.data.AssetRepository
import com.example.model.AssetRecord
import com.example.model.AssetStatus
import com.example.model.AuditFilterTab
import com.example.model.AuditSummaryStats
import com.example.model.PersianTextUtils
import com.example.model.StoreBranches
import com.example.util.CompressedPhotoResult
import com.example.util.ExcelExportPayload
import com.example.util.ExcelHelper
import com.example.util.ImageCompressor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class ValidationBlockAlert(
    val pendingCount: Int,
    val missingPhotoCount: Int,
    val incompleteDiscrepancyCount: Int,
    val problematicDescriptions: List<String>
)

@OptIn(ExperimentalCoroutinesApi::class)
class AuditViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("audit_prefs", Context.MODE_PRIVATE)
    private val repository: AssetRepository

    private val _selectedBranch = MutableStateFlow(
        prefs.getString("selected_branch", StoreBranches.ALL_BRANCHES.first())
            ?: StoreBranches.ALL_BRANCHES.first()
    )
    val selectedBranch: StateFlow<String> = _selectedBranch.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(AuditFilterTab.ALL)
    val selectedFilter: StateFlow<AuditFilterTab> = _selectedFilter.asStateFlow()

    private val _activeDiscrepancyAssetId = MutableStateFlow<String?>(null)
    val activeDiscrepancyAssetId: StateFlow<String?> = _activeDiscrepancyAssetId.asStateFlow()

    private val _highlightedPhotoAssetId = MutableStateFlow<String?>(null)
    val highlightedPhotoAssetId: StateFlow<String?> = _highlightedPhotoAssetId.asStateFlow()

    private val _showSurplusModal = MutableStateFlow(false)
    val showSurplusModal: StateFlow<Boolean> = _showSurplusModal.asStateFlow()

    private val _validationBlockAlert = MutableStateFlow<ValidationBlockAlert?>(null)
    val validationBlockAlert: StateFlow<ValidationBlockAlert?> = _validationBlockAlert.asStateFlow()

    private val _exportPayload = MutableStateFlow<ExcelExportPayload?>(null)
    val exportPayload: StateFlow<ExcelExportPayload?> = _exportPayload.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    val branchAssets: StateFlow<List<AssetRecord>>

    val summaryStats: StateFlow<AuditSummaryStats>

    val filteredAssets: StateFlow<List<AssetRecord>>

    init {
        val db = AssetDatabase.getInstance(application)
        repository = AssetRepository(db.assetDao())

        viewModelScope.launch {
            repository.ensureSeededForBranches()
        }

        branchAssets = _selectedBranch
            .flatMapLatest { branch -> repository.observeAssetsForBranch(branch) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        summaryStats = branchAssets
            .combine(_selectedBranch) { assets, _ ->
                computeStats(assets)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = AuditSummaryStats()
            )

        filteredAssets = combine(
            branchAssets,
            _searchQuery,
            _selectedFilter
        ) { assets, query, filter ->
            val normQuery = PersianTextUtils.normalizeForSearch(query)
            assets.filter { item ->
                val matchesTab = when (filter) {
                    AuditFilterTab.ALL -> true
                    AuditFilterTab.PENDING -> item.status == AssetStatus.PENDING.code
                    AuditFilterTab.CONFIRMED -> item.status == AssetStatus.CONFIRMED.code
                    AuditFilterTab.DISCREPANCY -> item.status == AssetStatus.DISCREPANCY.code
                    AuditFilterTab.SURPLUS -> item.isSurplus || item.status == AssetStatus.SURPLUS.code
                    AuditFilterTab.MISSING -> item.status == AssetStatus.MISSING.code
                }
                val matchesSearch = if (normQuery.isBlank()) {
                    true
                } else {
                    val searchable = PersianTextUtils.normalizeForSearch(
                        "${item.description} ${item.assetTag1} ${item.assetTag2} ${item.assetTag3} " +
                            "${item.correctedAssetTag1} ${item.correctedAssetTag2} ${item.correctedAssetTag3} " +
                            "${item.assetCardCode} ${item.batch}"
                    )
                    searchable.contains(normQuery)
                }
                matchesTab && matchesSearch
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    private fun computeStats(assets: List<AssetRecord>): AuditSummaryStats {
        var pending = 0
        var confirmed = 0
        var discrepancy = 0
        var missingPhoto = 0
        var surplus = 0
        var missingItem = 0
        var incompleteDiscrepancy = 0

        for (item in assets) {
            when (item.status) {
                AssetStatus.PENDING.code -> pending++
                AssetStatus.CONFIRMED.code -> confirmed++
                AssetStatus.DISCREPANCY.code -> discrepancy++
                AssetStatus.SURPLUS.code -> surplus++
                AssetStatus.MISSING.code -> missingItem++
            }
            if (item.isSurplus && item.status != AssetStatus.SURPLUS.code) {
                surplus++
            }
            if (item.isMissingMandatoryPhoto) {
                missingPhoto++
            }
            if (item.status == AssetStatus.DISCREPANCY.code && !item.hasCorrectedTagForDiscrepancy) {
                incompleteDiscrepancy++
            }
        }

        return AuditSummaryStats(
            totalCount = assets.size,
            pendingCount = pending,
            confirmedCount = confirmed,
            discrepancyCount = discrepancy,
            missingPhotoCount = missingPhoto,
            surplusCount = surplus,
            missingItemCount = missingItem,
            incompleteDiscrepancyCount = incompleteDiscrepancy
        )
    }

    fun selectBranch(branch: String) {
        _selectedBranch.value = branch
        prefs.edit().putString("selected_branch", branch).apply()
        _activeDiscrepancyAssetId.value = null
        _highlightedPhotoAssetId.value = null
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectFilter(tab: AuditFilterTab) {
        _selectedFilter.value = tab
    }

    fun dismissBanner() {
        _statusBanner.value = null
    }

    fun setSurplusModalVisible(visible: Boolean) {
        _showSurplusModal.value = visible
    }

    fun dismissValidationAlert() {
        _validationBlockAlert.value = null
    }

    fun dismissExportPayload() {
        _exportPayload.value = null
    }

    /**
     * Button A: "تایید" (Confirm - Green):
     * Selects CONFIRMED and immediately opens/highlights the Mandatory Photo Capture/Upload box.
     */
    fun onConfirmClicked(asset: AssetRecord) {
        viewModelScope.launch {
            val updated = asset.copy(
                status = if (asset.isSurplus) AssetStatus.SURPLUS.code else AssetStatus.CONFIRMED.code
            )
            repository.upsertAsset(updated)
            _activeDiscrepancyAssetId.value = null
            if (!updated.hasPhoto) {
                _highlightedPhotoAssetId.value = asset.id
                _statusBanner.value = "وضعیت «تایید» انتخاب شد. لطفاً عکس دارایی را جهت تکمیل ردیف ثبت کنید."
            } else {
                _highlightedPhotoAssetId.value = null
                _statusBanner.value = "دارایی «${asset.description}» با موفقیت تایید شد."
            }
        }
    }

    /**
     * Button B: "رد / مغایرت" (Reject / Discrepancy - Red):
     * Selects DISCREPANCY and smoothly expands the inline accordion box.
     */
    fun onDiscrepancyClicked(asset: AssetRecord) {
        viewModelScope.launch {
            if (asset.status != AssetStatus.DISCREPANCY.code) {
                repository.upsertAsset(
                    asset.copy(
                        status = AssetStatus.DISCREPANCY.code,
                        correctedAssetTag1 = asset.correctedAssetTag1.ifBlank { asset.assetTag1 }
                    )
                )
            }
            _highlightedPhotoAssetId.value = null
            _activeDiscrepancyAssetId.value = asset.id
        }
    }

    fun closeDiscrepancyAccordion() {
        _activeDiscrepancyAssetId.value = null
    }

    /**
     * Saves the Discrepancy details after validating that both corrected tag input and photo exist.
     */
    fun saveDiscrepancyRecord(
        asset: AssetRecord,
        correctedTag1: String,
        correctedTag2: String,
        correctedTag3: String
    ): Boolean {
        val trim1 = correctedTag1.trim()
        val trim2 = correctedTag2.trim()
        val trim3 = correctedTag3.trim()
        if (trim1.isBlank() && trim2.isBlank() && trim3.isBlank()) {
            _statusBanner.value = "خطا: وارد کردن حداقل یک شماره اموال واقعی / اصلاحی (یا انتخاب ندارد/ناخوانا) الزامی است."
            return false
        }
        if (asset.photoUrl.isNullOrBlank()) {
            _statusBanner.value = "خطا: پیوست عکس دارایی و پلاک اموال برای ثبت مغایرت اجباری است."
            return false
        }
        viewModelScope.launch {
            repository.upsertAsset(
                asset.copy(
                    status = AssetStatus.DISCREPANCY.code,
                    correctedAssetTag1 = trim1.ifBlank { "ندارد" },
                    correctedAssetTag2 = trim2,
                    correctedAssetTag3 = trim3
                )
            )
            _activeDiscrepancyAssetId.value = null
            _statusBanner.value = "مغایرت برای «${asset.description}» با موفقیت ثبت شد."
        }
        return true
    }

    /**
     * Secondary Action: "موجود نیست" (Not Found / Missing)
     */
    fun onMarkMissingClicked(asset: AssetRecord) {
        viewModelScope.launch {
            repository.upsertAsset(
                asset.copy(
                    status = AssetStatus.MISSING.code
                )
            )
            _activeDiscrepancyAssetId.value = null
            _highlightedPhotoAssetId.value = null
            _statusBanner.value = "ردیف «${asset.description}» به عنوان «موجود نیست» ثبت شد."
        }
    }

    fun attachCompressedPhoto(asset: AssetRecord, result: CompressedPhotoResult) {
        viewModelScope.launch {
            ImageCompressor.deletePhotoFileIfExists(asset.photoUrl)
            val newStatus = when {
                asset.isSurplus -> AssetStatus.SURPLUS.code
                asset.status == AssetStatus.PENDING.code -> AssetStatus.CONFIRMED.code
                else -> asset.status
            }
            repository.upsertAsset(
                asset.copy(
                    status = newStatus,
                    photoUrl = result.filePath,
                    photoFileName = result.fileName,
                    photoSizeKb = result.sizeKb
                )
            )
            if (_highlightedPhotoAssetId.value == asset.id) {
                _highlightedPhotoAssetId.value = null
            }
            _statusBanner.value =
                "تصویر با موفقیت فشرده‌سازی (${PersianTextUtils.toPersianDigits(result.sizeKb)} کیلوبایت) و ذخیره شد."
        }
    }

    fun deleteAssetPhoto(asset: AssetRecord) {
        viewModelScope.launch {
            ImageCompressor.deletePhotoFileIfExists(asset.photoUrl)
            repository.upsertAsset(
                asset.copy(
                    photoUrl = null,
                    photoFileName = null,
                    photoSizeKb = 0
                )
            )
            _highlightedPhotoAssetId.value = asset.id
            _statusBanner.value = "تصویر حذف شد. برای تکمیل این ردیف، ثبت مجدد عکس الزامی است."
        }
    }

    fun addSurplusAsset(
        description: String,
        assetTag1: String,
        assetTag2: String,
        assetTag3: String,
        assetCardCode: String,
        batch: String,
        photoResult: CompressedPhotoResult
    ) {
        viewModelScope.launch {
            val branch = _selectedBranch.value
            val newRecord = AssetRecord(
                id = "${branch}_surplus_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
                storeBranch = branch,
                description = description.trim(),
                assetTag1 = assetTag1.trim().ifBlank { "ندارد" },
                assetTag2 = assetTag2.trim().ifBlank { "ندارد" },
                assetTag3 = assetTag3.trim().ifBlank { "ندارد" },
                assetCardCode = assetCardCode.trim().ifBlank { "SURPLUS-ITEM" },
                batch = batch.trim().ifBlank { "Surplus" },
                status = AssetStatus.SURPLUS.code,
                photoUrl = photoResult.filePath,
                photoFileName = photoResult.fileName,
                photoSizeKb = photoResult.sizeKb,
                isSurplus = true
            )
            repository.upsertAsset(newRecord)
            _showSurplusModal.value = false
            _statusBanner.value = "کالای مازاد «${newRecord.description}» با موفقیت به لیست فروشگاه اضافه شد."
        }
    }

    fun importExcelFromUri(context: Context, uri: Uri, replaceCurrentList: Boolean) {
        viewModelScope.launch {
            _isBusy.value = true
            val branch = _selectedBranch.value
            val result = ExcelHelper.importAssetsFromUri(context, uri, branch)
            _isBusy.value = false
            result.onSuccess { importedRows ->
                repository.replaceOrAppendImportedAssets(branch, importedRows, replaceCurrentList)
                _statusBanner.value =
                    "${PersianTextUtils.toPersianDigits(importedRows.size)} ردیف دارایی از فایل اکسل با موفقیت وارد شعبه «$branch» شد."
            }.onFailure { err ->
                _statusBanner.value = "خطا در خواندن فایل اکسل: ${err.message ?: "فرمت نامعتبر"}"
            }
        }
    }

    fun importSampleExcelTemplateDirectly(context: Context) {
        viewModelScope.launch {
            _isBusy.value = true
            val branch = _selectedBranch.value
            val sampleFileResult = ExcelHelper.generateSampleTemplateXlsx(context, branch)
            sampleFileResult.onSuccess { file ->
                val bytes = file.readBytes()
                val rawRows = ExcelHelper.parseXlsxBytes(bytes)
                val now = System.currentTimeMillis()
                val imported = rawRows.drop(1).mapIndexed { index, row ->
                    AssetRecord(
                        id = "${branch}_tpl_${now}_$index",
                        storeBranch = branch,
                        description = row.getOrNull(0).orEmpty(),
                        assetTag1 = row.getOrNull(1).orEmpty().ifBlank { "ندارد" },
                        assetTag2 = row.getOrNull(2).orEmpty().ifBlank { "ندارد" },
                        assetTag3 = row.getOrNull(3).orEmpty().ifBlank { "ندارد" },
                        assetCardCode = row.getOrNull(4).orEmpty().ifBlank { "IT-ASSET" },
                        batch = row.getOrNull(5).orEmpty().ifBlank { "General" },
                        status = AssetStatus.PENDING.code
                    )
                }
                repository.replaceOrAppendImportedAssets(branch, imported, replaceExisting = false)
                _statusBanner.value =
                    "${PersianTextUtils.toPersianDigits(imported.size)} ردیف از قالب استاندارد اکسل (.xlsx) به لیست «$branch» افزوده شد."
            }.onFailure { err ->
                _statusBanner.value = "خطا در بارگذاری قالب اکسل: ${err.message}"
            }
            _isBusy.value = false
        }
    }

    fun saveSampleTemplateToUri(context: Context, targetUri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            val branch = _selectedBranch.value
            ExcelHelper.generateSampleTemplateXlsx(context, branch).onSuccess { sampleFile ->
                runCatching {
                    context.contentResolver.openOutputStream(targetUri)?.use { out ->
                        sampleFile.inputStream().use { input -> input.copyTo(out) }
                    }
                }.onSuccess {
                    _statusBanner.value = "فایل قالب نمونه اکسل (.xlsx) با موفقیت ذخیره شد."
                }.onFailure {
                    _statusBanner.value = "خطا در ذخیره فایل قالب: ${it.message}"
                }
            }
            _isBusy.value = false
        }
    }

    fun resetCurrentBranchSampleData() {
        viewModelScope.launch {
            val branch = _selectedBranch.value
            repository.resetBranchToSampleData(branch)
            _activeDiscrepancyAssetId.value = null
            _highlightedPhotoAssetId.value = null
            _statusBanner.value = "اطلاعات شعبه «$branch» به ۵ ردیف پیش‌فرض بازنشانی شد."
        }
    }

    /**
     * Final Submission & Excel Export with strict validation:
     * If any row is still PENDING, or if any CONFIRMED / DISCREPANCY / SURPLUS row lacks a photo,
     * block final submission and show a clear Persian alert listing how many items still need attention.
     */
    fun validateAndFinalizeExport(context: Context) {
        val currentAssets = branchAssets.value
        val stats = summaryStats.value

        if (currentAssets.isEmpty()) {
            _statusBanner.value = "هیچ دارایی در لیست فعلی برای خروجی اکسل وجود ندارد."
            return
        }

        if (!stats.isReadyForFinalExport) {
            val problematic = currentAssets.filter { !it.isRowComplete }.map { item ->
                val reason = when {
                    item.status == AssetStatus.PENDING.code -> "تعیین وضعیت نشده (در انتظار بررسی)"
                    item.isMissingMandatoryPhoto -> "فاقد عکس اجباری (${item.statusEnum.persianTitle})"
                    !item.hasCorrectedTagForDiscrepancy -> "فاقد کد اموال اصلاحی در بخش مغایرت"
                    else -> "نیازمند بررسی"
                }
                "${item.description} ← $reason"
            }

            _validationBlockAlert.value = ValidationBlockAlert(
                pendingCount = stats.pendingCount,
                missingPhotoCount = stats.missingPhotoCount,
                incompleteDiscrepancyCount = stats.incompleteDiscrepancyCount,
                problematicDescriptions = problematic
            )
            return
        }

        viewModelScope.launch {
            _isBusy.value = true
            val branch = _selectedBranch.value
            val result = ExcelHelper.generateAuditExportXlsx(
                context = context,
                storeBranch = branch,
                assets = currentAssets,
                stats = stats
            )
            _isBusy.value = false
            result.onSuccess { payload ->
                _exportPayload.value = payload
                _statusBanner.value =
                    "فایل اکسل «${payload.fileName}» شامل شیت‌های «گزارش نهایی» و «خلاصه وضعیت» آماده دانلود است."
            }.onFailure { err ->
                _statusBanner.value = "خطا در تولید فایل اکسل: ${err.message}"
            }
        }
    }

    fun copyExportFileToUri(context: Context, payload: ExcelExportPayload, targetUri: Uri) {
        viewModelScope.launch {
            runCatching {
                context.contentResolver.openOutputStream(targetUri)?.use { out ->
                    payload.file.inputStream().use { input -> input.copyTo(out) }
                }
            }.onSuccess {
                _statusBanner.value = "گزارش نهایی اکسل (.xlsx) با موفقیت در حافظه دستگاه ذخیره شد."
            }.onFailure { err ->
                _statusBanner.value = "خطا در ذخیره فایل اکسل: ${err.message}"
            }
        }
    }
}
