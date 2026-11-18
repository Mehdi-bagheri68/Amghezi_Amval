package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AssetRecord
import com.example.model.AssetStatus
import com.example.model.PersianTextUtils
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
import com.example.ui.theme.MissingBorder
import com.example.ui.theme.MissingContainer
import com.example.ui.theme.MissingGray
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.util.CompressedPhotoResult

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssetRowCard(
    index: Int,
    asset: AssetRecord,
    isDiscrepancyAccordionOpen: Boolean,
    isPhotoBoxHighlighted: Boolean,
    isDesktopTableMode: Boolean,
    onConfirmClicked: () -> Unit,
    onDiscrepancyClicked: () -> Unit,
    onMarkMissingClicked: () -> Unit,
    onSaveDiscrepancy: (String, String, String) -> Boolean,
    onCloseDiscrepancy: () -> Unit,
    onPhotoCompressed: (CompressedPhotoResult) -> Unit,
    onPhotoDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBorderColor = when {
        isDiscrepancyAccordionOpen || asset.status == AssetStatus.DISCREPANCY.code -> CoralBorder
        asset.isMissingMandatoryPhoto -> CoralReject
        asset.isSurplus || asset.status == AssetStatus.SURPLUS.code -> AmberBorder
        asset.status == AssetStatus.CONFIRMED.code -> EmeraldBorder
        else -> SlateBorder
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("asset_row_card_${asset.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            width = if (asset.status != AssetStatus.PENDING.code || isDiscrepancyAccordionOpen) 1.5.dp else 1.dp,
            color = cardBorderColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isDesktopTableMode) {
                DesktopTableStructureRow(
                    index = index,
                    asset = asset,
                    onConfirmClicked = onConfirmClicked,
                    onDiscrepancyClicked = onDiscrepancyClicked,
                    onMarkMissingClicked = onMarkMissingClicked
                )
            } else {
                MobileAuditCardHeader(
                    index = index,
                    asset = asset,
                    onConfirmClicked = onConfirmClicked,
                    onDiscrepancyClicked = onDiscrepancyClicked,
                    onMarkMissingClicked = onMarkMissingClicked
                )
            }

            // Show saved discrepancy summary when status is DISCREPANCY and accordion is closed
            if (asset.status == AssetStatus.DISCREPANCY.code && !isDiscrepancyAccordionOpen) {
                Surface(
                    color = CoralContainer,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CoralBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "کد اموال واقعی / اصلاحی ثبت‌شده:",
                                style = MaterialTheme.typography.labelSmall,
                                color = CoralText
                            )
                            Text(
                                text = buildString {
                                    append("اموال ۱: ${PersianTextUtils.toPersianDigits(asset.correctedAssetTag1.ifBlank { "-" })}")
                                    if (asset.correctedAssetTag2.isNotBlank()) {
                                        append("  |  اموال ۲: ${PersianTextUtils.toPersianDigits(asset.correctedAssetTag2)}")
                                    }
                                    if (asset.correctedAssetTag3.isNotBlank()) {
                                        append("  |  اموال ۳: ${PersianTextUtils.toPersianDigits(asset.correctedAssetTag3)}")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = CoralText
                            )
                        }

                        TextButton(onClick = onDiscrepancyClicked) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "ویرایش مغایرت",
                                modifier = Modifier.size(16.dp),
                                tint = CoralReject
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ویرایش مغایرت",
                                style = MaterialTheme.typography.labelMedium,
                                color = CoralReject,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Inline Discrepancy Accordion Box when DISCREPANCY is active
            AnimatedVisibility(
                visible = isDiscrepancyAccordionOpen,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                DiscrepancyBox(
                    asset = asset,
                    onPhotoCompressed = onPhotoCompressed,
                    onPhotoDeleted = onPhotoDeleted,
                    onSaveDiscrepancy = onSaveDiscrepancy,
                    onCancel = onCloseDiscrepancy
                )
            }

            // Mandatory Photo Capture/Upload Box when CONFIRMED or SURPLUS (or when photo already attached and accordion is closed)
            val showStandalonePhotoBox = !isDiscrepancyAccordionOpen && (
                asset.status == AssetStatus.CONFIRMED.code ||
                    asset.status == AssetStatus.SURPLUS.code ||
                    asset.isSurplus ||
                    isPhotoBoxHighlighted ||
                    (asset.status == AssetStatus.DISCREPANCY.code && (asset.hasPhoto || asset.isMissingMandatoryPhoto))
                )

            AnimatedVisibility(
                visible = showStandalonePhotoBox,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                PhotoCaptureBox(
                    assetId = asset.id,
                    assetDescription = asset.description,
                    assetTag = asset.correctedAssetTag1.ifBlank { asset.assetTag1 },
                    assetCardCode = asset.assetCardCode,
                    storeBranch = asset.storeBranch,
                    photoUrl = asset.photoUrl,
                    photoFileName = asset.photoFileName,
                    photoSizeKb = asset.photoSizeKb,
                    isHighlighted = isPhotoBoxHighlighted || asset.isMissingMandatoryPhoto,
                    accentColor = if (asset.status == AssetStatus.DISCREPANCY.code) CoralReject else EmeraldConfirm,
                    onPhotoCompressed = onPhotoCompressed,
                    onPhotoDeleted = onPhotoDeleted
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MobileAuditCardHeader(
    index: Int,
    asset: AssetRecord,
    onConfirmClicked: () -> Unit,
    onDiscrepancyClicked: () -> Unit,
    onMarkMissingClicked: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: Title + Status / Surplus / Mandatory Photo Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = SlateSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, SlateBorder)
                ) {
                    Text(
                        text = PersianTextUtils.toPersianDigits(index + 1),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = asset.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            StatusBadgesCluster(asset = asset)
        }

        // Row 2: Asset Tags 1/2/3 + Asset Card Code + Batch pills (matching screenshot style)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AssetTagChip(label = "تگ ۱", value = asset.assetTag1, isPrimary = true)
            AssetTagChip(label = "تگ ۲", value = asset.assetTag2, isPrimary = false)
            AssetTagChip(label = "تگ ۳", value = asset.assetTag3, isPrimary = false)

            Surface(
                color = IndigoContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "کارت دارایی: ${asset.assetCardCode}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = IndigoOnContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Surface(
                color = SlateSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SlateBorder)
            ) {
                Text(
                    text = "بچ: ${asset.batch}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateTextSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Row 3: Two prominent action buttons ("تایید" Green & "رد / مغایرت" Red) + subtle "موجود نیست"
        AuditActionButtonsRow(
            asset = asset,
            onConfirmClicked = onConfirmClicked,
            onDiscrepancyClicked = onDiscrepancyClicked,
            onMarkMissingClicked = onMarkMissingClicked
        )
    }
}

@Composable
private fun DesktopTableStructureRow(
    index: Int,
    asset: AssetRecord,
    onConfirmClicked: () -> Unit,
    onDiscrepancyClicked: () -> Unit,
    onMarkMissingClicked: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Col 1: Index + Description + Badges (weight 2.2f)
        Column(
            modifier = Modifier.weight(2.2f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "#${PersianTextUtils.toPersianDigits(index + 1)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextMuted
                )
                Text(
                    text = asset.description,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary
                )
            }
            StatusBadgesCluster(asset = asset)
        }

        // Col 2: Asset Tags 1, 2, 3 (weight 1.8f)
        Row(
            modifier = Modifier.weight(1.8f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssetTagChip(label = "اموال ۱", value = asset.assetTag1, isPrimary = true)
            AssetTagChip(label = "اموال ۲", value = asset.assetTag2, isPrimary = false)
            AssetTagChip(label = "اموال ۳", value = asset.assetTag3, isPrimary = false)
        }

        // Col 3: Card Code & Batch (weight 1.1f)
        Column(
            modifier = Modifier.weight(1.1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "کارت: ${asset.assetCardCode}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = IndigoOnContainer
            )
            Text(
                text = "بچ: ${asset.batch}",
                style = MaterialTheme.typography.labelSmall,
                color = SlateTextSecondary
            )
        }

        // Col 4: Action Buttons (weight 2.1f)
        Box(modifier = Modifier.weight(2.1f)) {
            AuditActionButtonsRow(
                asset = asset,
                onConfirmClicked = onConfirmClicked,
                onDiscrepancyClicked = onDiscrepancyClicked,
                onMarkMissingClicked = onMarkMissingClicked
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusBadgesCluster(asset: AssetRecord) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (asset.isSurplus || asset.status == AssetStatus.SURPLUS.code) {
            Surface(
                color = AmberContainer,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, AmberBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = AmberSurplus,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "کالای مازاد",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AmberText
                    )
                }
            }
        }

        if (asset.isMissingMandatoryPhoto) {
            Surface(
                color = CoralContainer,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, CoralBorder),
                modifier = Modifier.testTag("mandatory_photo_badge_${asset.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = CoralReject,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "عکس اجباری است",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CoralText
                    )
                }
            }
        }

        val (statusBg, statusBorder, statusText, statusLabel) = when (asset.status) {
            AssetStatus.CONFIRMED.code -> Quadruple(
                EmeraldContainer,
                EmeraldBorder,
                EmeraldText,
                if (asset.hasPhoto) "تایید شده ✓" else "تایید (در انتظار عکس)"
            )
            AssetStatus.DISCREPANCY.code -> Quadruple(
                CoralContainer,
                CoralBorder,
                CoralText,
                if (asset.isRowComplete) "مغایرت ثبت‌شده" else "مغایرت (ناقص)"
            )
            AssetStatus.MISSING.code -> Quadruple(
                MissingContainer,
                MissingBorder,
                MissingGray,
                "موجود نیست"
            )
            AssetStatus.SURPLUS.code -> Quadruple(
                AmberContainer,
                AmberBorder,
                AmberText,
                if (asset.hasPhoto) "ثبت مازاد ✓" else "مازاد (بدون عکس)"
            )
            else -> Quadruple(
                SlateSurfaceVariant,
                SlateBorder,
                SlateTextMuted,
                "در انتظار بررسی"
            )
        }

        Surface(
            color = statusBg,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, statusBorder)
        ) {
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = statusText,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun AssetTagChip(
    label: String,
    value: String,
    isPrimary: Boolean
) {
    val isMissingOrIllegible = value == "ندارد" || value == "ناخوانا"
    val bgColor = when {
        isMissingOrIllegible -> AmberContainer.copy(alpha = 0.7f)
        isPrimary -> SlateSurfaceVariant
        else -> SlateSurfaceVariant.copy(alpha = 0.7f)
    }
    val borderColor = when {
        isMissingOrIllegible -> AmberBorder
        else -> SlateBorder
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = "$label: ${PersianTextUtils.toPersianDigits(value)}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Medium,
            color = if (isMissingOrIllegible) AmberText else SlateTextPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun AuditActionButtonsRow(
    asset: AssetRecord,
    onConfirmClicked: () -> Unit,
    onDiscrepancyClicked: () -> Unit,
    onMarkMissingClicked: () -> Unit
) {
    val isConfirmed = asset.status == AssetStatus.CONFIRMED.code || asset.status == AssetStatus.SURPLUS.code
    val isDiscrepancy = asset.status == AssetStatus.DISCREPANCY.code
    val isMissing = asset.status == AssetStatus.MISSING.code

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Button A: "تایید" (Confirm - Emerald Green)
            Button(
                onClick = onConfirmClicked,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("confirm_btn_${asset.id}"),
                shape = RoundedCornerShape(11.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConfirmed) EmeraldConfirm else EmeraldConfirm.copy(alpha = 0.9f),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = if (isConfirmed) 3.dp else 0.dp
                )
            ) {
                Icon(
                    imageVector = if (isConfirmed && asset.hasPhoto) Icons.Default.CheckCircle else Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "تایید",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Button B: "رد / مغایرت" (Reject / Discrepancy - Coral Red)
            Button(
                onClick = onDiscrepancyClicked,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("discrepancy_btn_${asset.id}"),
                shape = RoundedCornerShape(11.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDiscrepancy) CoralReject else CoralReject.copy(alpha = 0.9f),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = if (isDiscrepancy) 3.dp else 0.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "رد / مغایرت",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Secondary Action: Subtle "موجود نیست" (Not Found / Missing) option + photo status hint
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = onMarkMissingClicked,
                modifier = Modifier.testTag("missing_btn_${asset.id}"),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isMissing) CoralText else SlateTextSecondary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.HideSource,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isMissing) "وضعیت فعلی: موجود نیست (ثبت‌شده)" else "موجود نیست (مفقودی در فروشگاه)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isMissing) FontWeight.Bold else FontWeight.Normal
                )
            }

            if (asset.hasPhoto) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = EmeraldConfirm,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "دارای تصویر پیوست",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldText,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
