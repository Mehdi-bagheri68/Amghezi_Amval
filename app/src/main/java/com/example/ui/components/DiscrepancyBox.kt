package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AssetRecord
import com.example.model.PersianTextUtils
import com.example.ui.theme.CoralBorder
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralHeaderTint
import com.example.ui.theme.CoralReject
import com.example.ui.theme.CoralText
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.util.CompressedPhotoResult

@Composable
fun DiscrepancyBox(
    asset: AssetRecord,
    onPhotoCompressed: (CompressedPhotoResult) -> Unit,
    onPhotoDeleted: () -> Unit,
    onSaveDiscrepancy: (correctedTag1: String, correctedTag2: String, correctedTag3: String) -> Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tag1Input by rememberSaveable(asset.id) {
        mutableStateOf(asset.correctedAssetTag1)
    }
    var tag2Input by rememberSaveable(asset.id) {
        mutableStateOf(asset.correctedAssetTag2)
    }
    var tag3Input by rememberSaveable(asset.id) {
        mutableStateOf(asset.correctedAssetTag3)
    }
    var showExtraTags by rememberSaveable(asset.id) {
        mutableStateOf(asset.correctedAssetTag2.isNotBlank() || asset.correctedAssetTag3.isNotBlank())
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("discrepancy_box_${asset.id}"),
        shape = RoundedCornerShape(14.dp),
        color = SlateSurface,
        border = BorderStroke(1.5.dp, CoralBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Coral Header Banner matching the attached design screenshot ("مغایرت - ...")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CoralHeaderTint)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = CoralReject,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "ثبت مغایرت - ${asset.description}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CoralText
                    )
                }

                Text(
                    text = "کد فعلی: ${PersianTextUtils.toPersianDigits(asset.assetTag1)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CoralText
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Corrected Asset Tag 1 Input + Quick-fill helper buttons ("ندارد" and "ناخوانا")
                CorrectedTagInputRow(
                    label = "شماره اموال ۱ واقعی / اصلاحی (الزامی)",
                    placeholder = "ثبت کد اموال صحیح...",
                    value = tag1Input,
                    onValueChange = {
                        tag1Input = it
                        validationError = null
                    },
                    testTagPrefix = "discrepancy_tag1_${asset.id}"
                )

                // Toggle for Tag 2 & Tag 3 corrected inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { showExtraTags = !showExtraTags },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (showExtraTags) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showExtraTags) {
                                "بستن شماره اموال ۲ و ۳ اصلاحی"
                            } else {
                                "افزودن شماره اموال ۲ و ۳ اصلاحی (اختیاری)"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = IndigoAccent
                        )
                    }
                }

                if (showExtraTags) {
                    CorrectedTagInputRow(
                        label = "شماره اموال ۲ واقعی / اصلاحی",
                        placeholder = "شماره اموال ۲ اصلاحی...",
                        value = tag2Input,
                        onValueChange = {
                            tag2Input = it
                            validationError = null
                        },
                        testTagPrefix = "discrepancy_tag2_${asset.id}"
                    )

                    CorrectedTagInputRow(
                        label = "شماره اموال ۳ واقعی / اصلاحی",
                        placeholder = "شماره اموال ۳ اصلاحی...",
                        value = tag3Input,
                        onValueChange = {
                            tag3Input = it
                            validationError = null
                        },
                        testTagPrefix = "discrepancy_tag3_${asset.id}"
                    )
                }

                // Mandatory Photo Capture / Upload Box for asset and its tag
                PhotoCaptureBox(
                    assetId = "${asset.id}_disc",
                    assetDescription = asset.description,
                    assetTag = tag1Input.ifBlank { asset.assetTag1 },
                    assetCardCode = asset.assetCardCode,
                    storeBranch = asset.storeBranch,
                    photoUrl = asset.photoUrl,
                    photoFileName = asset.photoFileName,
                    photoSizeKb = asset.photoSizeKb,
                    isHighlighted = asset.photoUrl.isNullOrBlank(),
                    accentColor = CoralReject,
                    onPhotoCompressed = { compressed ->
                        validationError = null
                        onPhotoCompressed(compressed)
                    },
                    onPhotoDeleted = onPhotoDeleted
                )

                if (!validationError.isNullOrBlank()) {
                    Surface(
                        color = CoralContainer,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CoralBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = validationError!!,
                            style = MaterialTheme.typography.labelMedium,
                            color = CoralText,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }

                HorizontalDivider(color = SlateBorder)

                // Bottom Save Discrepancy ("ثبت مغایرت") & Cancel ("انصراف") actions matching screenshot
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (tag1Input.isBlank() && tag2Input.isBlank() && tag3Input.isBlank()) {
                                validationError = "لطفاً شماره اموال واقعی / اصلاحی را وارد کنید یا یکی از گزینه‌های «ندارد» / «ناخوانا» را انتخاب نمایید."
                                return@Button
                            }
                            if (asset.photoUrl.isNullOrBlank()) {
                                validationError = "پیوست عکس دارایی و پلاک اموال برای ثبت مغایرت الزامی است."
                                return@Button
                            }
                            onSaveDiscrepancy(tag1Input, tag2Input, tag3Input)
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                            .testTag("save_discrepancy_btn_${asset.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralReject,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ثبت مغایرت",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("cancel_discrepancy_btn_${asset.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SlateTextSecondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "انصراف",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CorrectedTagInputRow(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    testTagPrefix: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = SlateTextPrimary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("${testTagPrefix}_input"),
                placeholder = {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateTextMuted
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SlateSurface,
                    unfocusedContainerColor = SlateSurfaceVariant,
                    focusedBorderColor = CoralReject,
                    unfocusedBorderColor = SlateBorder
                ),
                textStyle = MaterialTheme.typography.bodyMedium
            )

            // Quick-fill helper button: "ندارد"
            OutlinedButton(
                onClick = { onValueChange("ندارد") },
                modifier = Modifier
                    .height(48.dp)
                    .testTag("${testTagPrefix}_quick_none"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (value == "ندارد") CoralContainer else SlateSurfaceVariant,
                    contentColor = if (value == "ندارد") CoralText else SlateTextPrimary
                ),
                border = BorderStroke(
                    1.dp,
                    if (value == "ندارد") CoralReject else SlateBorder
                ),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Text(
                    text = "ندارد",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Quick-fill helper button: "ناخوانا"
            OutlinedButton(
                onClick = { onValueChange("ناخوانا") },
                modifier = Modifier
                    .height(48.dp)
                    .testTag("${testTagPrefix}_quick_illegible"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (value == "ناخوانا") CoralContainer else SlateSurfaceVariant,
                    contentColor = if (value == "ناخوانا") CoralText else SlateTextPrimary
                ),
                border = BorderStroke(
                    1.dp,
                    if (value == "ناخوانا") CoralReject else SlateBorder
                ),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Text(
                    text = "ناخوانا",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
