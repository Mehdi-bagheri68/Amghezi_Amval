package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import com.example.model.AppScreen
import com.example.model.AuditFilterTab
import com.example.model.AuditSummaryStats
import com.example.model.PersianTextUtils
import com.example.ui.AuditViewModel
import com.example.ui.components.AssetRowCard
import com.example.ui.components.AuditReportDashboardSection
import com.example.ui.components.CompactAuditTopBar
import com.example.ui.components.ExcelExportReadyDialog
import com.example.ui.components.SurplusModal
import com.example.ui.components.ValidationBlockDialog
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.AmberSurplus
import com.example.ui.theme.EmeraldConfirm
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.IndigoOnContainer
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.TealVibrant

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
    val currentScreen by auditViewModel.currentScreen.collectAsStateWithLifecycle()
    val isLoggingIn by auditViewModel.isLoggingIn.collectAsStateWithLifecycle()
    val loginError by auditViewModel.loginError.collectAsStateWithLifecycle()
    val selectedBranch by auditViewModel.selectedBranch.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screenNavigation"
    ) { screen ->
        when (screen) {
            AppScreen.LOGIN -> {
                LoginScreen(
                    initialBranch = selectedBranch,
                    isLoggingIn = isLoggingIn,
                    errorMessage = loginError,
                    onClearError = auditViewModel::clearLoginError,
                    onLoginSubmit = { username, password, branch ->
                        auditViewModel.performLogin(username, password, branch)
                    }
                )
            }

            AppScreen.ASSET_INVENTORY -> {
                AssetInventoryScreen(auditViewModel = auditViewModel)
            }
        }
    }
}

@Composable
fun AssetInventoryScreen(
    auditViewModel: AuditViewModel
) {
    val context = LocalContext.current
    val userSession by auditViewModel.userSession.collectAsStateWithLifecycle()
    val selectedBranch by auditViewModel.selectedBranch.collectAsStateWithLifecycle()
    val searchQuery by auditViewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by auditViewModel.selectedFilter.collectAsStateWithLifecycle()
    val filteredAssets by auditViewModel.filteredAssets.collectAsStateWithLifecycle()
    val stats by auditViewModel.summaryStats.collectAsStateWithLifecycle()
    val activeDiscrepancyId by auditViewModel.activeDiscrepancyAssetId.collectAsStateWithLifecycle()
    val expandedPhotoId by auditViewModel.expandedPhotoAssetId.collectAsStateWithLifecycle()
    val showSurplusModal by auditViewModel.showSurplusModal.collectAsStateWithLifecycle()
    val validationAlert by auditViewModel.validationBlockAlert.collectAsStateWithLifecycle()
    val exportPayload by auditViewModel.exportPayload.collectAsStateWithLifecycle()
    val statusBanner by auditViewModel.statusBanner.collectAsStateWithLifecycle()
    val isBusy by auditViewModel.isBusy.collectAsStateWithLifecycle()

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
                CompactAuditTopBar(
                    userSession = userSession,
                    selectedBranch = selectedBranch,
                    onBranchSelected = auditViewModel::selectBranch,
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
                    onResetBranchClicked = auditViewModel::resetCurrentBranchSampleData,
                    onLogoutClicked = auditViewModel::logout
                )
                if (isBusy) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = TealVibrant
                    )
                }
            }
        },
        bottomBar = {
            CompactBottomStickyBar(
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

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1100.dp)
                    .align(Alignment.TopCenter)
                    .testTag("asset_audit_list"),
                contentPadding = PaddingValues(
                    start = 10.dp,
                    end = 10.dp,
                    top = 8.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Scrollable High-Contrast Live Report Dashboard & Compact Search Bar
                item(key = "report_dashboard_header") {
                    AuditReportDashboardSection(
                        searchQuery = searchQuery,
                        selectedFilter = selectedFilter,
                        stats = stats,
                        onSearchQueryChanged = auditViewModel::updateSearchQuery,
                        onFilterSelected = auditViewModel::selectFilter
                    )
                }

                // Dismissible Status Notification Banner
                if (!statusBanner.isNullOrBlank()) {
                    item(key = "status_banner") {
                        Surface(
                            color = IndigoContainer,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = IndigoAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = statusBanner.orEmpty(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = IndigoOnContainer
                                    )
                                }

                                IconButton(
                                    onClick = auditViewModel::dismissBanner,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "بستن پیام",
                                        tint = IndigoOnContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Desktop Table Header Row
                if (isDesktopTableMode && filteredAssets.isNotEmpty()) {
                    item(key = "desktop_table_header") {
                        DesktopTableHeaderBanner()
                    }
                }

                if (filteredAssets.isEmpty()) {
                    item(key = "empty_placeholder") {
                        EmptyAuditListPlaceholder(
                            selectedFilter = selectedFilter,
                            searchQuery = searchQuery,
                            onClearFilters = {
                                auditViewModel.updateSearchQuery("")
                                auditViewModel.selectFilter(AuditFilterTab.ALL)
                            }
                        )
                    }
                } else {
                    itemsIndexed(
                        items = filteredAssets,
                        key = { _, item -> item.id }
                    ) { idx, asset ->
                        AssetRowCard(
                            index = idx,
                            asset = asset,
                            isDiscrepancyAccordionOpen = activeDiscrepancyId == asset.id,
                            isPhotoBoxExpanded = expandedPhotoId == asset.id,
                            isDesktopTableMode = isDesktopTableMode,
                            onConfirmClicked = { auditViewModel.onConfirmClicked(asset) },
                            onDiscrepancyClicked = { auditViewModel.onDiscrepancyClicked(asset) },
                            onMarkMissingClicked = { auditViewModel.onMarkMissingClicked(asset) },
                            onTogglePhotoBox = { auditViewModel.togglePhotoBoxExpanded(asset.id) },
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

    validationAlert?.let { alert ->
        ValidationBlockDialog(
            alert = alert,
            onDismiss = auditViewModel::dismissValidationAlert,
            onFilterPendingClicked = {
                auditViewModel.selectFilter(AuditFilterTab.PENDING)
            }
        )
    }

    exportPayload?.let { payload ->
        ExcelExportReadyDialog(
            payload = payload,
            stats = stats,
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
        modifier = Modifier.fillMaxWidth(),
        color = NavyDeep,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "شرح کارت دارایی و وضعیت",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(2.2f)
            )
            Text(
                text = "شماره اموال ۱ / ۲ / ۳",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1.7f)
            )
            Text(
                text = "کارت و بچ",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "عملیات ممیزی",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(2.1f)
            )
        }
    }
}

/**
 * Ultra-slim single-row bottom action bar (~64dp) with unmistakable semantic colors:
 * - Warm Amber (#D97706) for "+ کالای مازاد"
 * - Royal Indigo (#4338CA) / Emerald (#059669) for "گزارش نهایی و اکسل"
 */
@Composable
private fun CompactBottomStickyBar(
    stats: AuditSummaryStats,
    onAddSurplusClicked: () -> Unit,
    onFinalizeExportClicked: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SlateSurface,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Distinct Amber Button for Surplus Asset
            Button(
                onClick = onAddSurplusClicked,
                modifier = Modifier
                    .weight(0.95f)
                    .height(48.dp)
                    .testTag("add_surplus_sticky_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberSurplus,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "کالای مازاد",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Distinct Royal Indigo / Emerald Button for Final Report & Excel Export
            Button(
                onClick = onFinalizeExportClicked,
                modifier = Modifier
                    .weight(1.35f)
                    .height(48.dp)
                    .testTag("finalize_export_sticky_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (stats.isReadyForFinalExport) EmeraldConfirm else IndigoAccent,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Icon(
                    imageVector = if (stats.isReadyForFinalExport) Icons.Default.CheckCircle else Icons.Default.Assessment,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (stats.isReadyForFinalExport) {
                        "گزارش نهایی و اکسل ✓"
                    } else {
                        "گزارش و خروجی اکسل (${PersianTextUtils.toPersianDigits(stats.completedCount)}/${PersianTextUtils.toPersianDigits(stats.totalCount)})"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
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
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
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
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = if (searchQuery.isNotBlank()) {
                    "هیچ دارایی با عبارت «$searchQuery» یافت نشد."
                } else {
                    "هیچ ردیفی در بخش «${selectedFilter.persianLabel}» وجود ندارد."
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary,
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
