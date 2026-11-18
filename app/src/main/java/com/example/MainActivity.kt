package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AuditFilterTab
import com.example.model.AuditSummaryStats
import com.example.model.PersianTextUtils
import com.example.ui.AuditViewModel
import com.example.ui.components.AssetRowCard
import com.example.ui.components.ExcelExportReadyDialog
import com.example.ui.components.HeaderStats
import com.example.ui.components.SurplusModal
import com.example.ui.components.ValidationBlockDialog
import com.example.ui.theme.EmeraldConfirm
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.IndigoOnContainer
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                RetailAssetAuditApp()
            }
        }
    }
}

@Composable
fun RetailAssetAuditApp(
    auditViewModel: AuditViewModel = viewModel()
) {
    val context = LocalContext.current
    val selectedBranch by auditViewModel.selectedBranch.collectAsStateWithLifecycle()
    val searchQuery by auditViewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by auditViewModel.selectedFilter.collectAsStateWithLifecycle()
    val filteredAssets by auditViewModel.filteredAssets.collectAsStateWithLifecycle()
    val stats by auditViewModel.summaryStats.collectAsStateWithLifecycle()
    val activeDiscrepancyId by auditViewModel.activeDiscrepancyAssetId.collectAsStateWithLifecycle()
    val highlightedPhotoId by auditViewModel.highlightedPhotoAssetId.collectAsStateWithLifecycle()
    val showSurplusModal by auditViewModel.showSurplusModal.collectAsStateWithLifecycle()
    val validationAlert by auditViewModel.validationBlockAlert.collectAsStateWithLifecycle()
    val exportPayload by auditViewModel.exportPayload.collectAsStateWithLifecycle()
    val statusBanner by auditViewModel.statusBanner.collectAsStateWithLifecycle()
    val isBusy by auditViewModel.isBusy.collectAsStateWithLifecycle()

    // Launcher for importing Excel (.xlsx) or CSV files
    val excelImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            auditViewModel.importExcelFromUri(
                context = context,
                uri = uri,
                replaceCurrentList = false
            )
        }
    }

    // Launcher for saving the final exported .xlsx report to user-selected device storage
    val saveExportXlsxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { targetUri ->
        val payload = exportPayload
        if (targetUri != null && payload != null) {
            auditViewModel.copyExportFileToUri(context, payload, targetUri)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SlateBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                HeaderStats(
                    selectedBranch = selectedBranch,
                    searchQuery = searchQuery,
                    selectedFilter = selectedFilter,
                    stats = stats,
                    onBranchSelected = auditViewModel::selectBranch,
                    onSearchQueryChanged = auditViewModel::updateSearchQuery,
                    onFilterSelected = auditViewModel::selectFilter,
                    onImportExcelClicked = {
                        excelImportLauncher.launch(
                            arrayOf(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/vnd.ms-excel",
                                "text/csv",
                                "text/comma-separated-values",
                                "*/*"
                            )
                        )
                    },
                    onLoadSampleExcelClicked = {
                        auditViewModel.importSampleExcelTemplateDirectly(context)
                    },
                    onResetBranchClicked = auditViewModel::resetCurrentBranchSampleData
                )
                if (isBusy) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = IndigoAccent
                    )
                }
            }
        },
        bottomBar = {
            BottomStickyAuditBar(
                stats = stats,
                onAddSurplusClicked = { auditViewModel.setSurplusModalVisible(true) },
                onFinalizeExportClicked = { auditViewModel.validateAndFinalizeExport(context) }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isDesktopTableMode = maxWidth >= 760.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1100.dp)
                    .align(Alignment.TopCenter)
            ) {
                // Interactive Status Banner
                AnimatedVisibility(visible = !statusBanner.isNullOrBlank()) {
                    Surface(
                        color = IndigoContainer,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = IndigoAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = statusBanner.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = IndigoOnContainer
                                )
                            }

                            IconButton(
                                onClick = auditViewModel::dismissBanner,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "بستن پیام",
                                    tint = IndigoOnContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Desktop Table Header when in wide screen mode
                if (isDesktopTableMode && filteredAssets.isNotEmpty()) {
                    DesktopTableHeaderBanner()
                }

                if (filteredAssets.isEmpty()) {
                    EmptyAuditListPlaceholder(
                        selectedFilter = selectedFilter,
                        searchQuery = searchQuery,
                        onClearFilters = {
                            auditViewModel.updateSearchQuery("")
                            auditViewModel.selectFilter(AuditFilterTab.ALL)
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("asset_audit_list"),
                        contentPadding = PaddingValues(
                            start = 14.dp,
                            end = 14.dp,
                            top = 10.dp,
                            bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(
                            items = filteredAssets,
                            key = { _, item -> item.id }
                        ) { idx, asset ->
                            AssetRowCard(
                                index = idx,
                                asset = asset,
                                isDiscrepancyAccordionOpen = activeDiscrepancyId == asset.id,
                                isPhotoBoxHighlighted = highlightedPhotoId == asset.id,
                                isDesktopTableMode = isDesktopTableMode,
                                onConfirmClicked = { auditViewModel.onConfirmClicked(asset) },
                                onDiscrepancyClicked = { auditViewModel.onDiscrepancyClicked(asset) },
                                onMarkMissingClicked = { auditViewModel.onMarkMissingClicked(asset) },
                                onSaveDiscrepancy = { tag1, tag2, tag3 ->
                                    auditViewModel.saveDiscrepancyRecord(asset, tag1, tag2, tag3)
                                },
                                onCloseDiscrepancy = auditViewModel::closeDiscrepancyAccordion,
                                onPhotoCompressed = { result ->
                                    auditViewModel.attachCompressedPhoto(asset, result)
                                },
                                onPhotoDeleted = {
                                    auditViewModel.deleteAssetPhoto(asset)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Surplus Asset Modal
    if (showSurplusModal) {
        SurplusModal(
            storeBranch = selectedBranch,
            onDismiss = { auditViewModel.setSurplusModalVisible(false) },
            onSubmitSurplus = { desc, tag1, tag2, tag3, cardCode, batch, photoResult ->
                auditViewModel.addSurplusAsset(
                    description = desc,
                    assetTag1 = tag1,
                    assetTag2 = tag2,
                    assetTag3 = tag3,
                    assetCardCode = cardCode,
                    batch = batch,
                    photoResult = photoResult
                )
            }
        )
    }

    // Strict Validation Block Alert Dialog
    validationAlert?.let { alert ->
        ValidationBlockDialog(
            alert = alert,
            onDismiss = auditViewModel::dismissValidationAlert,
            onFilterPendingClicked = {
                auditViewModel.selectFilter(AuditFilterTab.PENDING)
            }
        )
    }

    // Excel Export Ready Preview & Download Dialog
    exportPayload?.let { payload ->
        ExcelExportReadyDialog(
            payload = payload,
            onSaveToDeviceClicked = {
                saveExportXlsxLauncher.launch(payload.fileName)
            },
            onDismiss = auditViewModel::dismissExportPayload
        )
    }
}

@Composable
private fun DesktopTableHeaderBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        color = IndigoPrimary,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "شرح کارت دارایی و وضعیت",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(2.2f)
            )
            Text(
                text = "شماره اموال ۱ / ۲ / ۳",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1.8f)
            )
            Text(
                text = "کارت دارایی و بچ",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1.1f)
            )
            Text(
                text = "عملیات ممیزی",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(2.1f)
            )
        }
    }
}

@Composable
private fun BottomStickyAuditBar(
    stats: AuditSummaryStats,
    onAddSurplusClicked: () -> Unit,
    onFinalizeExportClicked: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SlateSurface,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Progress indicator line
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "پیشرفت ممیزی شعبه: ${PersianTextUtils.toPersianDigits(stats.completedCount)} از ${PersianTextUtils.toPersianDigits(stats.totalCount)} قلم تکمیل شده",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateTextSecondary
                )

                Text(
                    text = if (stats.isReadyForFinalExport) {
                        "آماده خروجی اکسل ✓"
                    } else {
                        "${PersianTextUtils.toPersianDigits(stats.pendingCount + stats.missingPhotoCount)} مورد نیازمند تکمیل"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (stats.isReadyForFinalExport) EmeraldConfirm else IndigoAccent
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prominent "+ افزودن کالای مازاد در فروشگاه" button (matching screenshot)
                Button(
                    onClick = onAddSurplusClicked,
                    modifier = Modifier
                        .weight(1.1f)
                        .height(50.dp)
                        .testTag("add_surplus_sticky_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IndigoPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "افزودن کالای مازاد",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // "تایید نهایی و خروجی اکسل" button
                Button(
                    onClick = onFinalizeExportClicked,
                    modifier = Modifier
                        .weight(1.25f)
                        .height(50.dp)
                        .testTag("finalize_export_sticky_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (stats.isReadyForFinalExport) EmeraldConfirm else IndigoAccent,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (stats.isReadyForFinalExport) Icons.Default.CheckCircle else Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تایید نهایی و خروجی اکسل",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyAuditListPlaceholder(
    selectedFilter: AuditFilterTab,
    searchQuery: String,
    onClearFilters: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Inventory,
                contentDescription = null,
                tint = SlateTextMuted,
                modifier = Modifier.size(54.dp)
            )
            Text(
                text = if (searchQuery.isNotBlank()) {
                    "هیچ دارایی با عبارت «$searchQuery» یافت نشد."
                } else {
                    "هیچ ردیفی در تب «${selectedFilter.persianLabel}» وجود ندارد."
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "می‌توانید فیلترها را پاک کنید یا فایل اکسل جدیدی وارد نمایید.",
                style = MaterialTheme.typography.bodySmall,
                color = SlateTextSecondary,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onClearFilters,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent)
            ) {
                Text(
                    text = "نمایش همه دارایی‌ها",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
