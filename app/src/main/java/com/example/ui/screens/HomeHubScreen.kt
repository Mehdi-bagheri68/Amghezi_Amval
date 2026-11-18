package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.IndigoOnContainer
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.TealBorder
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealVibrant

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeHubScreen(
    userSession: UserSession?,
    selectedBranch: String,
    stats: AuditSummaryStats,
    uploadedDocsCount: Int,
    onBranchSelected: (String) -> Unit,
    onNavigateToAssetInventory: () -> Unit,
    onNavigateToDocumentUpload: () -> Unit,
    onLogoutClicked: () -> Unit
) {
    var branchMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SlateBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            // Compact Corporate Top Header displaying user name/role, branch code, and logout button
            Surface(
                color = NavyDeep,
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = TealVibrant,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "پروفایل ممیز",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = userSession?.fullName ?: "مهندس علی باقری",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${userSession?.roleTitle ?: "ممیز ارشد اموال"} • کد پرسنلی: ${userSession?.personnelCode ?: "۹۸۴۱۲"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Branch Pill & Logout Icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { branchMenuExpanded = true }
                                    .testTag("hub_branch_selector")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = TealBorder,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "$selectedBranch (${StoreBranches.codeForBranch(selectedBranch)})",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "تغییر شعبه",
                                        tint = Color.White,
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
                                                fontWeight = if (branch == selectedBranch) FontWeight.Bold else FontWeight.Normal
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

                        IconButton(
                            onClick = onLogoutClicked,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .testTag("hub_logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "خروج از حساب کاربری",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Quick Status Summary Widget at the top
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hub_status_summary_widget"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    border = BorderStroke(1.dp, SlateBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "وضعیت لحظه‌ای ممیزی $selectedBranch",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateTextPrimary
                                )
                                Text(
                                    text = "کد شعبه: ${StoreBranches.codeForBranch(selectedBranch)} • ذخیره‌سازی خودکار فعال",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateTextSecondary
                                )
                            }

                            Surface(
                                color = if (stats.isReadyForFinalExport) EmeraldContainer else IndigoContainer,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (stats.isReadyForFinalExport) EmeraldBorder else IndigoAccent.copy(alpha = 0.3f)
                                )
                            ) {
                                Text(
                                    text = "${PersianTextUtils.toPersianDigits((stats.completionRatio * 100).toInt())}٪ تکمیل‌شده",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (stats.isReadyForFinalExport) EmeraldText else IndigoOnContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { stats.completionRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(9.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = TealVibrant,
                            trackColor = SlateSurfaceVariant
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HubSummaryBadge(
                                title = "کل دارایی‌ها",
                                count = stats.totalCount,
                                bgColor = SlateSurfaceVariant,
                                borderColor = SlateBorder,
                                textColor = SlateTextPrimary
                            )
                            HubSummaryBadge(
                                title = "تأیید شده",
                                count = stats.confirmedCount,
                                bgColor = EmeraldContainer,
                                borderColor = EmeraldBorder,
                                textColor = EmeraldText
                            )
                            HubSummaryBadge(
                                title = "مغایرت",
                                count = stats.discrepancyCount,
                                bgColor = CoralContainer,
                                borderColor = CoralBorder,
                                textColor = CoralText
                            )
                            HubSummaryBadge(
                                title = "کالای مازاد",
                                count = stats.surplusCount,
                                bgColor = AmberContainer,
                                borderColor = AmberBorder,
                                textColor = AmberText
                            )
                            HubSummaryBadge(
                                title = "در انتظار بررسی",
                                count = stats.pendingCount,
                                bgColor = IndigoContainer,
                                borderColor = IndigoAccent.copy(alpha = 0.3f),
                                textColor = IndigoOnContainer
                            )
                            HubSummaryBadge(
                                title = "مستندات ارسالی",
                                count = uploadedDocsCount,
                                bgColor = TealContainer,
                                borderColor = TealBorder,
                                textColor = TealDark
                            )
                        }
                    }
                }

                Text(
                    text = "انتخاب عملیات اصلی ممیزی",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary
                )

                // PRIMARY ACTION CARD 1: "اموال‌گردانی" (Asset Inventory / Stocktaking)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 175.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(onClick = onNavigateToAssetInventory)
                        .testTag("hub_card_asset_inventory"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                    border = BorderStroke(1.5.dp, TealBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = TealVibrant,
                                shadowElevation = 4.dp,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "اموال‌گردانی",
                                        tint = Color.White,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "اموال‌گردانی",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SlateTextPrimary
                                    )

                                    Surface(
                                        color = TealContainer,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, TealBorder)
                                    ) {
                                        Text(
                                            text = "${PersianTextUtils.toPersianDigits(stats.totalCount)} قلم در لیست",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TealDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "ثبت، اسکن و مغایرت‌گیری اموال شعبه به همراه پیوست تصاویر پلاک، ثبت کالای مازاد و خروجی اکسل (.xlsx)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SlateTextSecondary
                                )
                            }
                        }

                        // Thumb-friendly 64dp Action Button inside Primary Card 1
                        Button(
                            onClick = onNavigateToAssetInventory,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .testTag("hub_btn_asset_inventory"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TealVibrant,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FactCheck,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "ورود به بخش اموال‌گردانی و شمارش دارایی‌ها",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // PRIMARY ACTION CARD 2: "بارگذاری مستندات" (Upload Documentation)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 175.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(onClick = onNavigateToDocumentUpload)
                        .testTag("hub_card_document_upload"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                    border = BorderStroke(1.5.dp, IndigoAccent.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = IndigoAccent,
                                shadowElevation = 4.dp,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "بارگذاری مستندات",
                                        tint = Color.White,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "بارگذاری مستندات",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SlateTextPrimary
                                    )

                                    Surface(
                                        color = IndigoContainer,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = "${PersianTextUtils.toPersianDigits(uploadedDocsCount)} سند ثبت‌شده",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = IndigoOnContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "ارسال تصاویر صورتجلسات، فاکتورها و مستندات اموال شعبه با دسته‌بندی هوشمند و فشرده‌سازی خودکار",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SlateTextSecondary
                                )
                            }
                        }

                        // Thumb-friendly 64dp Action Button inside Primary Card 2
                        Button(
                            onClick = onNavigateToDocumentUpload,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .testTag("hub_btn_document_upload"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IndigoAccent,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "ورود به بخش بارگذاری صورتجلسات و فاکتورها",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HubSummaryBadge(
    title: String,
    count: Int,
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "$title:",
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )
            Text(
                text = PersianTextUtils.toPersianDigits(count),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}
