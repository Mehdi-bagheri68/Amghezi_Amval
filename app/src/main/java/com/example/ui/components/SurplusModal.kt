package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberBorder
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberSurplus
import com.example.ui.theme.AmberText
import com.example.ui.theme.CoralBorder
import com.example.ui.theme.CoralContainer
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurplusModal(
    storeBranch: String,
    onDismiss: () -> Unit,
    onSubmitSurplus: (
        description: String,
        assetTag1: String,
        assetTag2: String,
        assetTag3: String,
        assetCardCode: String,
        batch: String,
        photoResult: CompressedPhotoResult
    ) -> Unit
) {
    BackHandler { onDismiss() }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var description by remember { mutableStateOf("") }
    var assetTag1 by remember { mutableStateOf("") }
    var assetTag2 by remember { mutableStateOf("ندارد") }
    var assetTag3 by remember { mutableStateOf("ندارد") }
    var assetCardCode by remember { mutableStateOf("IT-SURPLUS") }
    var batch by remember { mutableStateOf("Surplus") }
    var capturedPhoto by remember { mutableStateOf<CompressedPhotoResult?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SlateSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.testTag("surplus_modal_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = AmberContainer,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AmberBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = AmberSurplus,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "افزودن کالای مازاد در فروشگاه",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )
                        Text(
                            text = "ثبت دارایی خارج از لیست اصلی در «$storeBranch»",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateTextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_surplus_modal_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "بستن",
                        tint = SlateTextSecondary
                    )
                }
            }

            HorizontalDivider(color = SlateBorder)

            // 1. شرح کالا (Item Description - Required text input)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "شرح کالا (اجباری)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        validationError = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("surplus_description_input"),
                    placeholder = {
                        Text(
                            text = "مثال: مانیتور ۲۴ اینچ سامسونگ یا پرینتر لیزری...",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextMuted
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberSurplus,
                        unfocusedBorderColor = SlateBorder,
                        focusedContainerColor = SlateSurface,
                        unfocusedContainerColor = SlateSurfaceVariant
                    )
                )
            }

            // 2. کد / شماره اموال (Asset Tag Number - Required text input + quick buttons for "ندارد" / "ناخوانا")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "کد / شماره اموال (اجباری)",
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
                        value = assetTag1,
                        onValueChange = {
                            assetTag1 = it
                            validationError = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("surplus_tag_input"),
                        placeholder = {
                            Text(
                                text = "شماره اموال روی پلاک...",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateTextMuted
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberSurplus,
                            unfocusedBorderColor = SlateBorder,
                            focusedContainerColor = SlateSurface,
                            unfocusedContainerColor = SlateSurfaceVariant
                        )
                    )

                    OutlinedButton(
                        onClick = {
                            assetTag1 = "ندارد"
                            validationError = null
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("surplus_quick_none"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (assetTag1 == "ندارد") AmberContainer else SlateSurfaceVariant,
                            contentColor = if (assetTag1 == "ندارد") AmberText else SlateTextPrimary
                        ),
                        border = BorderStroke(1.dp, if (assetTag1 == "ندارد") AmberSurplus else SlateBorder),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text(
                            text = "ندارد",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            assetTag1 = "ناخوانا"
                            validationError = null
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("surplus_quick_illegible"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (assetTag1 == "ناخوانا") AmberContainer else SlateSurfaceVariant,
                            contentColor = if (assetTag1 == "ناخوانا") AmberText else SlateTextPrimary
                        ),
                        border = BorderStroke(1.dp, if (assetTag1 == "ناخوانا") AmberSurplus else SlateBorder),
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

            // Optional Card Code & Batch row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "کارت دارایی",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateTextSecondary
                    )
                    OutlinedTextField(
                        value = assetCardCode,
                        onValueChange = { assetCardCode = it },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "بچ (دسته)",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateTextSecondary
                    )
                    OutlinedTextField(
                        value = batch,
                        onValueChange = { batch = it },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // 3. عکس کالا (Asset Photo - Required camera/file upload with preview)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "عکس کالا (الزامی)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary
                )

                PhotoCaptureBox(
                    assetId = "surplus_modal_new",
                    assetDescription = description.ifBlank { "کالای مازاد فروشگاهی" },
                    assetTag = assetTag1.ifBlank { "SURPLUS" },
                    assetCardCode = assetCardCode,
                    storeBranch = storeBranch,
                    photoUrl = capturedPhoto?.filePath,
                    photoFileName = capturedPhoto?.fileName,
                    photoSizeKb = capturedPhoto?.sizeKb ?: 0,
                    isHighlighted = capturedPhoto == null,
                    accentColor = AmberSurplus,
                    onPhotoCompressed = { result ->
                        capturedPhoto = result
                        validationError = null
                    },
                    onPhotoDeleted = {
                        capturedPhoto = null
                    }
                )
            }

            if (!validationError.isNullOrBlank()) {
                Surface(
                    color = CoralContainer,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CoralBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = CoralText,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (description.isBlank()) {
                            validationError = "وارد کردن «شرح کالا» برای کالای مازاد الزامی است."
                            return@Button
                        }
                        if (assetTag1.isBlank()) {
                            validationError = "وارد کردن «کد / شماره اموال» (یا انتخاب ندارد/ناخوانا) الزامی است."
                            return@Button
                        }
                        val photo = capturedPhoto
                        if (photo == null) {
                            validationError = "ثبت یا بارگذاری «عکس کالا» برای کالای مازاد اجباری است."
                            return@Button
                        }
                        onSubmitSurplus(
                            description,
                            assetTag1,
                            assetTag2,
                            assetTag3,
                            assetCardCode,
                            batch,
                            photo
                        )
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(50.dp)
                        .testTag("submit_surplus_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberSurplus,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ثبت و افزودن کالای مازاد",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "انصراف",
                        style = MaterialTheme.typography.labelLarge,
                        color = SlateTextSecondary
                    )
                }
            }
        }
    }
}
