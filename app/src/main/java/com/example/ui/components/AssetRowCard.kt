package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AssetRecord
import com.example.model.AssetStatus
import com.example.model.PersianTextUtils
import com.example.ui.theme.AmberBorder
import com.example.ui.theme.AmberCardTint
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberSurplus
import com.example.ui.theme.AmberText
import com.example.ui.theme.CoralBorder
import com.example.ui.theme.CoralCardTint
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralReject
import com.example.ui.theme.CoralText
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldCardTint
import com.example.ui.theme.EmeraldConfirm
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldText
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.IndigoOnContainer
import com.example.ui.theme.MissingBorder
import com.example.ui.theme.MissingCardTint
import com.example.ui.theme.MissingContainer
import com.example.ui.theme.MissingPurple
import com.example.ui.theme.PendingAccent
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.util.CompressedPhotoResult

/**
 * High-density, color-coded Asset Audit Card (~110dp collapsed) so 4-5 items fit simultaneously on screen,
 * while maintaining 48dp accessible touch targets and unmistakable status colors.
 */
@Composable
fun AssetRowCard(
    index: Int,
    asset: AssetRecord,
    isDiscrepancyAccordionOpen: Boolean,
    isPhotoBoxExpanded: Boolean,
    isDesktopTableMode: Boolean,
    onConfirmClicked: () -> Unit,
    onDiscrepancyClicked: () -> Unit,
    onMarkMissingClicked: () -> Unit,
    onTogglePhotoBox: () -> Unit,
    onSaveDiscrepancy: (String, String, String) -> Boolean,
    onCloseDiscrepancy: () -> Unit,
    onPhotoCompressed: (CompressedPhotoResult) -> Unit,
    onPhotoDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Distinct Color Coding per Asset Status
    val (stripColor, cardBg, borderColor) = when {
        isDiscrepancyAccordionOpen || asset.status == AssetStatus.DISCREPANCY.code -> Triple(
            CoralReject,
            CoralCardTint,
            CoralBorder
        )
        asset.isMissingMandatoryPhoto -> Triple(
            CoralReject,
            CoralCardTint.copy(alpha = 0.6f),
            CoralReject
        )
        asset.isSurplus || asset.status == AssetStatus.SURPLUS.code -> Triple(
            AmberSurplus,
            AmberCardTint,
            AmberBorder
        )
        asset.status == AssetStatus.CONFIRMED.code -> Triple(
            EmeraldConfirm,
            EmeraldCardTint,
            EmeraldBorder
        )
        asset.status == AssetStatus.MISSING.code -> Triple(
            MissingPurple,
            MissingCardTint,
            MissingBorder
        )
        else -> Triple(
            PendingAccent.copy(alpha = 0.55f),
            SlateSurface,
            SlateBorder
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("asset_row_card_${asset.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = BorderStroke(
            width = if (asset.status != AssetStatus.PENDING.code || isDiscrepancyAccordionOpen) 1.4.dp else 1.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // RTL Start (Right Edge) 5dp Bold Vertical Status Accent Strip
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(stripColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                if (isDesktopTableMode) {
                    DesktopCompactRow(
                        index = index,
                        asset = asset,
                        onConfirmClicked = onConfirmClicked,
                        onDiscrepancyClicked = onDiscrepancyClicked,
                        onMarkMissingClicked = onMarkMissingClicked,
                        onTogglePhotoBox = onTogglePhotoBox
                    )
                } else {
                    MobileCompactCardBody(
                        index = index,
                        asset = asset,
                        onConfirmClicked = onConfirmClicked,
                        onDiscrepancyClicked = onDiscrepancyClicked,
                        onMarkMissingClicked = onMarkMissingClicked,
                        onTogglePhotoBox = onTogglePhotoBox
                    )
                }

                // Compact inline corrected-tag notice when DISCREPANCY is saved & accordion is collapsed
                if (asset.status == AssetStatus.DISCREPANCY.code && !isDiscrepancyAccordionOpen) {
                    Surface(
                        color = CoralContainer,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CoralBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onDiscrepancyClicked)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "اموال اصلاحی: ${PersianTextUtils.toPersianDigits(asset.correctedAssetTag1.ifBlank { "-" })}" +
                                    if (asset.correctedAssetTag2.isNotBlank()) " | ${PersianTextUtils.toPersianDigits(asset.correctedAssetTag2)}" else "",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CoralText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "ویرایش مغایرت",
                                    tint = CoralReject,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "ویرایش",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CoralReject
                                )
                            }
                        }
                    }
                }

                // Inline Discrepancy Accordion Box
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

                // Mandatory Photo Capture/Upload Box (only expands when photo is missing OR user taps the photo chip)
                val showStandalonePhotoBox = !isDiscrepancyAccordionOpen && (
                    asset.isMissingMandatoryPhoto || isPhotoBoxExpanded
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
                        isHighlighted = asset.isMissingMandatoryPhoto,
                        accentColor = if (asset.status == AssetStatus.DISCREPANCY.code) CoralReject else EmeraldConfirm,
                        onPhotoCompressed = onPhotoCompressed,
                        onPhotoDeleted = onPhotoDeleted
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileCompactCardBody(
    index: Int,
    asset: AssetRecord,
    onConfirmClicked: () -> Unit,
    onDiscrepancyClicked: () -> Unit,
    onMarkMissingClicked: () -> Unit,
    onTogglePhotoBox: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Line 1: Index + Bold Asset Title + Compact Status/Photo Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = SlateSurfaceVariant,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, SlateBorder)
            ) {
                Text(
                    text = PersianTextUtils.toPersianDigits(index + 1),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextSecondary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Text(
                text = asset.description,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            CompactStatusBadge(asset = asset, onTogglePhotoBox = onTogglePhotoBox)
        }

        // Line 2: Compact Single-Row Metadata (Tag1, Tag2, Tag3/CardCode, Batch)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            MiniMetaChip(
                text = "اموال ۱: ${PersianTextUtils.toPersianDigits(asset.assetTag1)}",
                highlightWarn = asset.assetTag1 == "ندارد" || asset.assetTag1 == "ناخوانا",
                bold = true
            )
            if (asset.assetTag2 != "ندارد" && asset.assetTag2.isNotBlank()) {
                MiniMetaChip(
                    text = "اموال ۲: ${PersianTextUtils.toPersianDigits(asset.assetTag2)}",
                    highlightWarn = asset.assetTag2 == "ناخوانا",
                    bold = false
                )
            }
            Surface(
                color = IndigoContainer,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = asset.assetCardCode,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = IndigoOnContainer,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Text(
                text = "• ${asset.batch}",
                style = MaterialTheme.typography.labelSmall,
                color = SlateTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Line 3: Single Compact Action Row (Confirm + Discrepancy + Missing on ONE row!)
        CompactActionButtonsRow(
            asset = asset,
            onConfirmClicked = onConfirmClicked,
            onDiscrepancyClicked = onDiscrepancyClicked,
            onMarkMissingClicked = onMarkMissingClicked
        )
    }
}

@Composable
private fun DesktopCompactRow(
    index: Int,
    asset: AssetRecord,
    onConfirmClicked: () -> Unit,
    onDiscrepancyClicked: () -> Unit,
    onMarkMissingClicked: () -> Unit,
    onTogglePhotoBox: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            modifier = Modifier.weight(2.2f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "#${PersianTextUtils.toPersianDigits(index + 1)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextMuted
                )
                Text(
                    text = asset.description,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            CompactStatusBadge(asset = asset, onTogglePhotoBox = onTogglePhotoBox)
        }

        Row(
            modifier = Modifier.weight(1.7f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MiniMetaChip("۱: ${PersianTextUtils.toPersianDigits(asset.assetTag1)}", asset.assetTag1 == "ناخوانا", true)
            MiniMetaChip("۲: ${PersianTextUtils.toPersianDigits(asset.assetTag2)}", asset.assetTag2 == "ناخوانا", false)
            MiniMetaChip("۳: ${PersianTextUtils.toPersianDigits(asset.assetTag3)}", asset.assetTag3 == "ناخوانا", false)
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = asset.assetCardCode,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = IndigoOnContainer
            )
            Text(
                text = "بچ: ${asset.batch}",
                style = MaterialTheme.typography.labelSmall,
                color = SlateTextSecondary
            )
        }

        Box(modifier = Modifier.weight(2.1f)) {
            CompactActionButtonsRow(
                asset = asset,
                onConfirmClicked = onConfirmClicked,
                onDiscrepancyClicked = onDiscrepancyClicked,
                onMarkMissingClicked = onMarkMissingClicked
            )
        }
    }
}

@Composable
private fun CompactStatusBadge(
    asset: AssetRecord,
    onTogglePhotoBox: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (asset.isMissingMandatoryPhoto) {
            Surface(
                color = CoralReject,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onTogglePhotoBox)
                    .testTag("mandatory_photo_badge_${asset.id}")
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
                        text = "عکس اجباری",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        } else if (asset.hasPhoto) {
            Surface(
                color = EmeraldContainer,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, EmeraldBorder),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onTogglePhotoBox)
                    .testTag("photo_attached_chip_${asset.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "مشاهده یا تغییر عکس",
                        tint = EmeraldConfirm,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "عکس ✓",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldText
                    )
                }
            }
        }

        val (bg, textCol, label) = when {
            asset.isSurplus || asset.status == AssetStatus.SURPLUS.code -> Triple(
                AmberSurplus,
                Color.White,
                "مازاد"
            )
            asset.status == AssetStatus.CONFIRMED.code -> Triple(
                EmeraldConfirm,
                Color.White,
                "تأیید شده"
            )
            asset.status == AssetStatus.DISCREPANCY.code -> Triple(
                CoralReject,
                Color.White,
                "مغایرت"
            )
            asset.status == AssetStatus.MISSING.code -> Triple(
                MissingPurple,
                Color.White,
                "ناموجود"
            )
            else -> Triple(
                SlateSurfaceVariant,
                SlateTextSecondary,
                "در انتظار"
            )
        }

        Surface(
            color = bg,
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textCol,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun MiniMetaChip(
    text: String,
    highlightWarn: Boolean,
    bold: Boolean
) {
    Surface(
        color = if (highlightWarn) AmberContainer else SlateSurfaceVariant,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, if (highlightWarn) AmberBorder else SlateBorder.copy(alpha = 0.7f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = if (highlightWarn) AmberText else SlateTextPrimary,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun CompactActionButtonsRow(
    asset: AssetRecord,
    onConfirmClicked: () -> Unit,
    onDiscrepancyClicked: () -> Unit,
    onMarkMissingClicked: () -> Unit
) {
    val isConfirmed = asset.status == AssetStatus.CONFIRMED.code || asset.status == AssetStatus.SURPLUS.code
    val isDiscrepancy = asset.status == AssetStatus.DISCREPANCY.code
    val isMissing = asset.status == AssetStatus.MISSING.code

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Button A: Confirm (Emerald Green) — 48dp accessible height
        Button(
            onClick = onConfirmClicked,
            modifier = Modifier
                .weight(1.1f)
                .height(48.dp)
                .testTag("confirm_btn_${asset.id}"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isConfirmed) EmeraldConfirm else EmeraldConfirm.copy(alpha = 0.88f),
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = if (isConfirmed && asset.hasPhoto) Icons.Default.CheckCircle else Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "تایید",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Button B: Discrepancy (Bold Crimson Red) — 48dp accessible height
        Button(
            onClick = onDiscrepancyClicked,
            modifier = Modifier
                .weight(1.15f)
                .height(48.dp)
                .testTag("discrepancy_btn_${asset.id}"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDiscrepancy) CoralReject else CoralReject.copy(alpha = 0.88f),
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "رد / مغایرت",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Button C: Missing / Absent (Distinct Purple) — on the SAME row!
        OutlinedButton(
            onClick = onMarkMissingClicked,
            modifier = Modifier
                .weight(0.85f)
                .height(48.dp)
                .testTag("missing_btn_${asset.id}"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isMissing) MissingPurple else MissingContainer.copy(alpha = 0.6f),
                contentColor = if (isMissing) Color.White else MissingPurple
            ),
            border = BorderStroke(1.dp, if (isMissing) MissingPurple else MissingBorder),
            contentPadding = PaddingValues(horizontal = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.HideSource,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "ناموجود",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
