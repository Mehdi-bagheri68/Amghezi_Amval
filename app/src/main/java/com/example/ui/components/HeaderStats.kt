package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AuditFilterTab
import com.example.model.AuditSummaryStats
import com.example.model.PersianTextUtils
import com.example.model.StoreBranches
import com.example.model.UserSession
import com.example.ui.theme.AmberBorder
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberSurplus
import com.example.ui.theme.AmberText
import com.example.ui.theme.CoralBorder
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralReject
import com.example.ui.theme.CoralText
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldConfirm
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldText
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.MissingBorder
import com.example.ui.theme.MissingContainer
import com.example.ui.theme.MissingPurple
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyLightContainer
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySurface
import com.example.ui.theme.SkyImport
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.TealBorder
import com.example.ui.theme.TealVibrant

/**
 * Slim single-row sticky Top Bar (~54dp) so maximum screen real estate stays dedicated to the asset cards.
 */
@Composable
fun CompactAuditTopBar(
    userSession: UserSession?,
    selectedBranch: String,
    onBranchSelected: (String) -> Unit,
    onImportExcelClicked: () -> Unit,
    onLoadSampleExcelClicked: () -> Unit,
    onResetBranchClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var branchMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = NavyDeep,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Compact Branch Dropdown Pill + Auditor subtitle
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NavySurface,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { branchMenuExpanded = true }
                        .testTag("branch_selector_dropdown")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = TealBorder,
                            modifier = Modifier.size(16.dp)
                        )
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "$selectedBranch (${StoreBranches.codeForBranch(selectedBranch)})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = userSession?.fullName ?: "اموال‌گردانی فروشگاه",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "تغییر شعبه",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = branchMenuExpanded,
                    onDismissRequest = { branchMenuExpanded = false }
                ) {
                    StoreBranches.ALL_BRANCHES.forEach { branch ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "$branch (کد ${StoreBranches.codeForBranch(branch)})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (branch == selectedBranch) FontWeight.Bold else FontWeight.Normal,
                                    color = if (branch == selectedBranch) NavyPrimary else SlateTextPrimary
                                )
                            },
                            onClick = {
                                branchMenuExpanded = false
                                onBranchSelected(branch)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Distinct Sky-Blue Excel Import + Compact Action Icons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilledTonalButton(
                    onClick = onImportExcelClicked,
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("import_excel_button"),
                    shape = RoundedCornerShape(9.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = SkyImport,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = "ورود اکسل",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "اکسل",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onLoadSampleExcelClicked,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .testTag("load_sample_template_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "افزودن نمونه اکسل",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }

                IconButton(
                    onClick = onResetBranchClicked,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .testTag("reset_branch_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "بازنشانی لیست",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }

                IconButton(
                    onClick = onLogoutClicked,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(CoralReject.copy(alpha = 0.25f))
                        .testTag("logout_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "خروج از حساب",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

/**
 * Scrollable Live Report Dashboard & Compact Search/Filter Bar placed at top of LazyColumn.
 * Visually separates the color-coded live reports from the asset items below, and scrolls out
 * of the way so 4-6 asset cards can be seen at once!
 */
@Composable
fun AuditReportDashboardSection(
    searchQuery: String,
    selectedFilter: AuditFilterTab,
    stats: AuditSummaryStats,
    onSearchQueryChanged: (String) -> Unit,
    onFilterSelected: (AuditFilterTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Distinct Color-Coded Live Report Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("live_report_dashboard_card"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, SlateBorder)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Dark Report Header Strip to visually separate Report Summary from Asset Cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavySurface)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = TealBorder,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "گزارش لحظه‌ای وضعیت اموال",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (stats.missingPhotoCount > 0) {
                            Surface(
                                color = CoralReject,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "${PersianTextUtils.toPersianDigits(stats.missingPhotoCount)} بدون عکس",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Surface(
                            color = if (stats.isReadyForFinalExport) EmeraldConfirm else TealVibrant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${PersianTextUtils.toPersianDigits((stats.completionRatio * 100).toInt())}٪ تکمیل",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Slim Progress Bar
                LinearProgressIndicator(
                    progress = { stats.completionRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = if (stats.isReadyForFinalExport) EmeraldConfirm else TealVibrant,
                    trackColor = SlateSurfaceVariant
                )

                // Single Compact Row of High-Contrast Color-Coded KPI Report Tiles (tap to filter!)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReportKpiTile(
                        label = "کل",
                        count = stats.totalCount,
                        isSelected = selectedFilter == AuditFilterTab.ALL,
                        bgColor = NavyLightContainer,
                        accentColor = NavyPrimary,
                        onClick = { onFilterSelected(AuditFilterTab.ALL) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("filter_tab_all")
                    )
                    ReportKpiTile(
                        label = "تأیید",
                        count = stats.confirmedCount,
                        isSelected = selectedFilter == AuditFilterTab.CONFIRMED,
                        bgColor = EmeraldContainer,
                        accentColor = EmeraldConfirm,
                        onClick = { onFilterSelected(AuditFilterTab.CONFIRMED) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("filter_tab_confirmed")
                    )
                    ReportKpiTile(
                        label = "مغایرت",
                        count = stats.discrepancyCount,
                        isSelected = selectedFilter == AuditFilterTab.DISCREPANCY,
                        bgColor = CoralContainer,
                        accentColor = CoralReject,
                        onClick = { onFilterSelected(AuditFilterTab.DISCREPANCY) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("filter_tab_discrepancy")
                    )
                    ReportKpiTile(
                        label = "مازاد",
                        count = stats.surplusCount,
                        isSelected = selectedFilter == AuditFilterTab.SURPLUS,
                        bgColor = AmberContainer,
                        accentColor = AmberSurplus,
                        onClick = { onFilterSelected(AuditFilterTab.SURPLUS) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("filter_tab_surplus")
                    )
                    ReportKpiTile(
                        label = "ناموجود",
                        count = stats.missingItemCount,
                        isSelected = selectedFilter == AuditFilterTab.MISSING,
                        bgColor = MissingContainer,
                        accentColor = MissingPurple,
                        onClick = { onFilterSelected(AuditFilterTab.MISSING) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("filter_tab_missing")
                    )
                    ReportKpiTile(
                        label = "انتظار",
                        count = stats.pendingCount,
                        isSelected = selectedFilter == AuditFilterTab.PENDING,
                        bgColor = SlateSurfaceVariant,
                        accentColor = SlateTextSecondary,
                        onClick = { onFilterSelected(AuditFilterTab.PENDING) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("filter_tab_pending")
                    )
                }
            }
        }

        // Compact Inline Search Box (42dp height)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(12.dp),
            color = SlateSurface,
            border = BorderStroke(1.dp, SlateBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "جستجو",
                    tint = SlateTextMuted,
                    modifier = Modifier.size(18.dp)
                )

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "جستجوی شرح کالا، شماره اموال، کارت دارایی یا بچ...",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = SlateTextPrimary),
                        cursorBrush = SolidColor(IndigoAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input")
                    )
                }

                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChanged("") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "پاک کردن جستجو",
                            tint = SlateTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportKpiTile(
    label: String,
    count: Int,
    isSelected: Boolean,
    bgColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) accentColor else bgColor,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) accentColor else accentColor.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = PersianTextUtils.toPersianDigits(count),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else accentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) Color.White.copy(alpha = 0.95f) else accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
