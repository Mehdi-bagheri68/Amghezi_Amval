package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AssetDatabase
import com.example.data.AssetRepository
import com.example.model.AppScreen
import com.example.model.AssetRecord
import com.example.model.AssetStatus
import com.example.model.AuditFilterTab
import com.example.model.AuditSummaryStats
import com.example.model.DocumentCategory
import com.example.model.PersianTextUtils
import com.example.model.StagedDocument
import com.example.model.StoreBranches
import com.example.model.UploadedDocumentRecord
import com.example.model.UserSession
import com.example.util.CompressedPhotoResult
import com.example.util.ExcelExportPayload
import com.example.util.ExcelHelper
import com.example.util.ImageCompressor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    // Navigation & Authentication Session State
    private val _userSession = MutableStateFlow<UserSession?>(loadSavedSession())
    val userSession: StateFlow<UserSession?> = _userSession.asStateFlow()

    private val _currentScreen = MutableStateFlow(
        if (_userSession.value != null) AppScreen.HOME_HUB else AppScreen.LOGIN
    )
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // Asset Inventory Filter & Search State
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

    // Document Upload Screen State
    private val _selectedDocCategory = MutableStateFlow(DocumentCategory.MINUTES)
    val selectedDocCategory: StateFlow<DocumentCategory> = _selectedDocCategory.asStateFlow()

    private val _stagedDocuments = MutableStateFlow<List<StagedDocument>>(emptyList())
    val stagedDocuments: StateFlow<List<StagedDocument>> = _stagedDocuments.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()

    val branchAssets: StateFlow<List<AssetRecord>>
    val branchUploadedDocuments: StateFlow<List<UploadedDocumentRecord>>
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

        branchUploadedDocuments = _selectedBranch
            .flatMapLatest { branch -> repository.observeDocumentsForBranch(branch) }
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

    private fun loadSavedSession(): UserSession? {
        val isLogged = prefs.getBoolean("is_logged_in", false)
        if (!isLogged) return null
        val branch = prefs.getString("selected_branch", StoreBranches.ALL_BRANCHES.first())
            ?: StoreBranches.ALL_BRANCHES.first()
        return UserSession(
            fullName = prefs.getString("user_full_name", "مهندس علی باقری") ?: "مهندس علی باقری",
            personnelCode = prefs.getString("user_personnel_code", "۹۸۴۱۲") ?: "۹۸۴۱۲",
            roleTitle = prefs.getString("user_role", "ممیز ارشد اموال شعب") ?: "ممیز ارشد اموال شعب",
            branchName = branch,
            branchCode = StoreBranches.codeForBranch(branch)
        )
    }

    fun clearLoginError() {
        _loginError.value = null
    }

    fun performLogin(
        usernameOrCode: String,
        password: String,
        selectedBranch: String
    ) {
        val cleanUser = usernameOrCode.trim()
        val cleanPass = password.trim()
        if (cleanUser.isBlank()) {
            _loginError.value = "لطفاً نام کاربری یا کد پرسنلی ممیز را وارد نمایید."
            return
        }
        if (cleanPass.length < 4) {
            _loginError.value = "رمز عبور باید حداقل ۴ کاراکتر باشد."
            return
        }

        viewModelScope.launch {
            _loginError.value = null
            _isLoggingIn.value = true
            delay(550) // Smooth UX transition for login spinner

            val displayName = when {
                cleanUser.contains("admin", ignoreCase = true) || cleanUser == "101" || cleanUser == "۱۰۱" ->
                    "مهندس علی باقری"
                cleanUser.all { it.isDigit() || it in '۰'..'۹' } ->
                    "ممیز ارشد (کد ${PersianTextUtils.toPersianDigits(cleanUser)})"
                else -> cleanUser
            }
            val pCode = if (cleanUser.any { it.isDigit() || it in '۰'..'۹' }) {
                PersianTextUtils.toPersianDigits(cleanUser)
            } else {
                "۹۸۴۱۲"
            }

            val session = UserSession(
                fullName = displayName,
                personnelCode = pCode,
                roleTitle = "ممیز ارشد اموال و دارایی‌ها",
                branchName = selectedBranch,
                branchCode = StoreBranches.codeForBranch(selectedBranch)
            )
            _selectedBranch.value = selectedBranch
            _userSession.value = session
            prefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("user_full_name", session.fullName)
                .putString("user_personnel_code", session.personnelCode)
                .putString("user_role", session.roleTitle)
                .putString("selected_branch", selectedBranch)
                .apply()

            _isLoggingIn.value = false
            _currentScreen.value = AppScreen.HOME_HUB
            _statusBanner.value = "خوش آمدید، ${session.fullName} • آماده ممیزی «$selectedBranch»"
        }
    }

    fun logout() {
        prefs.edit().putBoolean("is_logged_in", false).apply()
        _userSession.value = null
        _activeDiscrepancyAssetId.value = null
        _highlightedPhotoAssetId.value = null
        _currentScreen.value = AppScreen.LOGIN
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun navigateBackToHub() {
        _activeDiscrepancyAssetId.value = null
        _highlightedPhotoAssetId.value = null
        _currentScreen.value = AppScreen.HOME_HUB
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
        _userSession.value = _userSession.value?.copy(
            branchName = branch,
            branchCode = StoreBranches.codeForBranch(branch)
        )
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

    // Document Upload Actions
    fun selectDocumentCategory(category: DocumentCategory) {
        _selectedDocCategory.value = category
    }

    fun addStagedDocument(
        title: String,
        category: DocumentCategory,
        compressed: CompressedPhotoResult
    ) {
        val docTitle = title.trim().ifBlank {
            "${category.persianTitle} - ${_selectedBranch.value}"
        }
        val item = StagedDocument(
            id = UUID.randomUUID().toString(),
            title = docTitle,
            category = category,
            photoPath = compressed.filePath,
            fileName = compressed.fileName,
            sizeKb = compressed.sizeKb
        )
        _stagedDocuments.value = _stagedDocuments.value + item
        _statusBanner.value = "مدرک «${item.title}» آماده ارسال شد."
    }

    fun removeStagedDocument(id: String) {
        val target = _stagedDocuments.value.firstOrNull { it.id == id }
        if (target != null) {
            ImageCompressor.deletePhotoFileIfExists(target.photoPath)
            _stagedDocuments.value = _stagedDocuments.value.filterNot { it.id == id }
        }
    }

    fun uploadAllStagedDocuments() {
        val staged = _stagedDocuments.value
        if (staged.isEmpty()) {
            _statusBanner.value = "لطفاً ابتدا حداقل یک تصویر مدرک (صورتجلسه، فاکتور یا تصویر کالا) اضافه کنید."
            return
        }
        if (_uploadProgress.value != null) return

        viewModelScope.launch {
            val branch = _selectedBranch.value
            val uploader = _userSession.value?.fullName ?: "ممیز ارشد"
            for (step in 1..10) {
                _uploadProgress.value = step / 10f
                delay(90)
            }
            val records = staged.map { item ->
                UploadedDocumentRecord(
                    id = item.id,
                    storeBranch = branch,
                    categoryCode = item.category.code,
                    categoryTitle = item.category.persianTitle,
                    title = item.title,
                    photoPath = item.photoPath,
                    fileName = item.fileName,
                    sizeKb = item.sizeKb,
                    uploadedBy = uploader
                )
            }
            repository.insertUploadedDocuments(records)
            _stagedDocuments.value = emptyList()
            _uploadProgress.value = null
            _statusBanner.value =
                "${PersianTextUtils.toPersianDigits(records.size)} مستند با موفقیت در پرونده «$branch» بارگذاری و ثبت شد."
        }
    }

    fun deleteUploadedDocumentRecord(doc: UploadedDocumentRecord) {
        viewModelScope.launch {
            ImageCompressor.deletePhotoFileIfExists(doc.photoPath)
            repository.deleteUploadedDocument(doc.id)
            _statusBanner.value = "مستند «${doc.title}» حذف شد."
        }
    }

    // Asset Inventory Actions
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

    fun resetCurrentBranchSampleData() {
        viewModelScope.launch {
            val branch = _selectedBranch.value
            repository.resetBranchToSampleData(branch)
            _activeDiscrepancyAssetId.value = null
            _highlightedPhotoAssetId.value = null
            _statusBanner.value = "اطلاعات شعبه «$branch» به ۵ ردیف پیش‌فرض بازنشانی شد."
        }
    }

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
