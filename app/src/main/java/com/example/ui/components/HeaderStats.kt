package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AuditFilterTab
import com.example.model.AuditSummaryStats
import com.example.model.PersianTextUtils
import com.example.model.StoreBranches
import com.example.ui.theme.AmberBorder
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberLight
import com.example.ui.theme.AmberText
import com.example.ui.theme.CoralBorder
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralReject
import com.example.ui.theme.CoralText
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldConfirm
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldText
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.IndigoOnContainer
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealVibrant

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeaderStats(
    selectedBranch: String,
    searchQuery: String,
    selectedFilter: AuditFilterTab,
    stats: AuditSummaryStats,
    onBackToHubClicked: () -> Unit,
    onBranchSelected: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onFilterSelected: (AuditFilterTab) -> Unit,
    onImportExcelClicked: () -> Unit,
    onLoadSampleExcelClicked: () -> Unit,
    onResetBranchClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var branchMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SlateSurface,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Corporate Deep Navy Top Bar with Back to Hub Button + Excel Import
            Surface(
                color = NavyDeep,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBackToHubClicked,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .testTag("inventory_back_to_hub_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "بازگشت به پیشخوان",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "اموال‌گردانی و ممیزی دارایی‌ها",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "$selectedBranch (کد ${StoreBranches.codeForBranch(selectedBranch)})",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onImportExcelClicked,
                            modifier = Modifier.testTag("import_excel_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = TealVibrant,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = "ورود اکسل",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ورود اکسل (.xlsx)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onLoadSampleExcelClicked,
                            modifier = Modifier.testTag("load_sample_template_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "افزودن قالب اکسل نمونه",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "نمونه اکسل",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        IconButton(
                            onClick = onResetBranchClicked,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("reset_branch_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "بازنشانی داده‌های شعبه",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Store / Branch Selector Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { branchMenuExpanded = true }
                            .testTag("branch_selector_dropdown"),
                        color = SlateSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SlateBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "انتخاب فروشگاه: $selectedBranch (کد ${StoreBranches.codeForBranch(selectedBranch)})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "تغییر شعبه",
                                tint = SlateTextSecondary
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
                                        color = if (branch == selectedBranch) TealDark else SlateTextPrimary
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

                // Live Summary Counters Row (Total, Confirmed, Discrepancy, Missing Photo, Surplus)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatCounterChip(
                        label = "کل اقلام",
                        count = stats.totalCount,
                        dotColor = TealVibrant,
                        bgColor = TealContainer,
                        borderColor = TealDark.copy(alpha = 0.3f),
                        textColor = TealDark
                    )
                    StatCounterChip(
                        label = "تأیید شده",
                        count = stats.confirmedCount,
                        dotColor = EmeraldLight,
                        bgColor = EmeraldContainer,
                        borderColor = EmeraldBorder,
                        textColor = EmeraldText
                    )
                    StatCounterChip(
                        label = "مغایرت",
                        count = stats.discrepancyCount,
                        dotColor = CoralReject,
                        bgColor = CoralContainer,
                        borderColor = CoralBorder,
                        textColor = CoralText
                    )
                    StatCounterChip(
                        label = "مازاد",
                        count = stats.surplusCount,
                        dotColor = AmberLight,
                        bgColor = AmberContainer,
                        borderColor = AmberBorder,
                        textColor = AmberText
                    )
                    StatCounterChip(
                        label = "بدون عکس",
                        count = stats.missingPhotoCount,
                        dotColor = if (stats.missingPhotoCount > 0) CoralReject else EmeraldConfirm,
                        bgColor = if (stats.missingPhotoCount > 0) CoralContainer else SlateSurfaceVariant,
                        borderColor = if (stats.missingPhotoCount > 0) CoralBorder else SlateBorder,
                        textColor = if (stats.missingPhotoCount > 0) CoralText else SlateTextSecondary
                    )
                    StatCounterChip(
                        label = "در انتظار",
                        count = stats.pendingCount,
                        dotColor = SlateTextMuted,
                        bgColor = SlateSurfaceVariant,
                        borderColor = SlateBorder,
                        textColor = SlateTextSecondary
                    )
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input"),
                    placeholder = {
                        Text(
                            text = "جستجوی شرح کالا، شماره اموال ۱/۲/۳، کارت دارایی یا بچ...",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "جستجو",
                            tint = SlateTextSecondary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "پاک کردن جستجو",
                                    tint = SlateTextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SlateSurface,
                        unfocusedContainerColor = SlateSurfaceVariant,
                        focusedBorderColor = TealVibrant,
                        unfocusedBorderColor = SlateBorder
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                // Filter Tabs Scrollable Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuditFilterTab.entries.forEach { tab ->
                        val countForTab = when (tab) {
                            AuditFilterTab.ALL -> stats.totalCount
                            AuditFilterTab.PENDING -> stats.pendingCount
                            AuditFilterTab.CONFIRMED -> stats.confirmedCount
                            AuditFilterTab.DISCREPANCY -> stats.discrepancyCount
                            AuditFilterTab.SURPLUS -> stats.surplusCount
                            AuditFilterTab.MISSING -> stats.missingItemCount
                        }
                        val isSelected = selectedFilter == tab
                        FilterChip(
                            selected = isSelected,
                            onClick = { onFilterSelected(tab) },
                            modifier = Modifier.testTag("filter_tab_${tab.code.lowercase()}"),
                            label = {
                                Text(
                                    text = "${tab.persianLabel} (${PersianTextUtils.toPersianDigits(countForTab)})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TealVibrant,
                                selectedLabelColor = Color.White,
                                containerColor = SlateSurfaceVariant,
                                labelColor = SlateTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = SlateBorder,
                                selectedBorderColor = TealVibrant
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCounterChip(
    label: String,
    count: Int,
    dotColor: Color,
    bgColor: Color,
    borderColor: Color,
    textColor: Color
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = "$label: ${PersianTextUtils.toPersianDigits(count)}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}
