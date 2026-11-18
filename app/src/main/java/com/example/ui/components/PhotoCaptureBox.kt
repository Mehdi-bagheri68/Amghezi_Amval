package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.model.PersianTextUtils
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberText
import com.example.ui.theme.CoralBorder
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralHeaderTint
import com.example.ui.theme.CoralReject
import com.example.ui.theme.CoralText
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldConfirm
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldText
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.util.CompressedPhotoResult
import com.example.util.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoCaptureBox(
    assetId: String,
    assetDescription: String,
    assetTag: String,
    assetCardCode: String,
    storeBranch: String,
    photoUrl: String?,
    photoFileName: String?,
    photoSizeKb: Int,
    isHighlighted: Boolean,
    accentColor: Color = EmeraldConfirm,
    onPhotoCompressed: (CompressedPhotoResult) -> Unit,
    onPhotoDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCompressing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val bitmapState by produceState<ImageBitmap?>(initialValue = null, key1 = photoUrl) {
        value = if (photoUrl.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    val file = File(photoUrl)
                    if (file.exists()) {
                        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                    } else {
                        null
                    }
                }.getOrNull()
            }
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCameraFile
        if (success && file != null) {
            scope.launch {
                isCompressing = true
                errorMessage = null
                val res = ImageCompressor.compressAndSaveFromFile(context, file, assetCardCode)
                isCompressing = false
                res.onSuccess { compressed ->
                    onPhotoCompressed(compressed)
                }.onFailure { err ->
                    errorMessage = err.message
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            runCatching {
                val (uri, tempFile) = ImageCompressor.createTempCameraUri(context)
                pendingCameraFile = tempFile
                takePictureLauncher.launch(uri)
            }.onFailure {
                // Fallback to instant canvas camera capture if device has no camera app
                scope.launch {
                    isCompressing = true
                    val res = ImageCompressor.generateQuickCameraAuditPhoto(
                        context = context,
                        assetDescription = assetDescription,
                        assetTag = assetTag,
                        assetCardCode = assetCardCode,
                        storeBranch = storeBranch
                    )
                    isCompressing = false
                    res.onSuccess(onPhotoCompressed)
                }
            }
        } else {
            errorMessage = "دسترسی دوربین رد شد؛ می‌توانید از گالری یا ثبت فوری پلاک استفاده کنید."
        }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isCompressing = true
                errorMessage = null
                val res = ImageCompressor.compressAndSaveFromUri(context, uri, assetCardCode)
                isCompressing = false
                res.onSuccess { compressed ->
                    onPhotoCompressed(compressed)
                }.onFailure { err ->
                    errorMessage = err.message
                }
            }
        }
    }

    fun launchCameraCapture() {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            runCatching {
                val (uri, tempFile) = ImageCompressor.createTempCameraUri(context)
                pendingCameraFile = tempFile
                takePictureLauncher.launch(uri)
            }.onFailure {
                scope.launch {
                    isCompressing = true
                    val res = ImageCompressor.generateQuickCameraAuditPhoto(
                        context = context,
                        assetDescription = assetDescription,
                        assetTag = assetTag,
                        assetCardCode = assetCardCode,
                        storeBranch = storeBranch
                    )
                    isCompressing = false
                    res.onSuccess(onPhotoCompressed)
                }
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun launchInstantAuditCapture() {
        scope.launch {
            isCompressing = true
            errorMessage = null
            val res = ImageCompressor.generateQuickCameraAuditPhoto(
                context = context,
                assetDescription = assetDescription,
                assetTag = assetTag,
                assetCardCode = assetCardCode,
                storeBranch = storeBranch
            )
            isCompressing = false
            res.onSuccess(onPhotoCompressed).onFailure { errorMessage = it.message }
        }
    }

    val containerBg by animateColorAsState(
        targetValue = when {
            !photoUrl.isNullOrBlank() -> EmeraldContainer.copy(alpha = 0.55f)
            isHighlighted -> CoralContainer.copy(alpha = 0.7f)
            else -> SlateSurfaceVariant
        },
        label = "photoBoxBg"
    )

    val strokeColor = when {
        !photoUrl.isNullOrBlank() -> EmeraldBorder
        isHighlighted -> CoralReject
        else -> SlateBorder
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(containerBg)
            .drawBehind {
                val strokeWidth = if (isHighlighted && photoUrl.isNullOrBlank()) 2.2.dp.toPx() else 1.5.dp.toPx()
                val dashEffect = if (photoUrl.isNullOrBlank()) {
                    PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                } else {
                    null
                }
                drawRoundRect(
                    color = strokeColor,
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                    style = Stroke(width = strokeWidth, pathEffect = dashEffect)
                )
            }
            .padding(12.dp)
            .testTag("photo_capture_box_$assetId")
    ) {
        if (isCompressing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = accentColor,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "در حال فشرده‌سازی تصویر (حداکثر ۱۰۲۴ پیکسل • کیفیت JPEG 0.7)...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateTextPrimary
                )
            }
        } else if (!photoUrl.isNullOrBlank() && bitmapState != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        bitmap = bitmapState!!,
                        contentDescription = "پیش‌نمایش عکس دارایی $assetDescription",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 96.dp, height = 74.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, EmeraldBorder, RoundedCornerShape(10.dp))
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldConfirm,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "عکس دارایی پیوست شد",
                                style = MaterialTheme.typography.titleSmall,
                                color = EmeraldText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = photoFileName ?: "asset_photo.jpg",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextSecondary,
                            maxLines = 1
                        )
                        Surface(
                            color = EmeraldContainer,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, EmeraldBorder)
                        ) {
                            Text(
                                text = "فشرده‌شده: ${PersianTextUtils.toPersianDigits(photoSizeKb.coerceAtLeast(1))} کیلوبایت (JPEG 70%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { launchInstantAuditCapture() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("retake_photo_btn_$assetId"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "ثبت مجدد عکس",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ثبت مجدد عکس",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    OutlinedButton(
                        onClick = onPhotoDeleted,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("delete_photo_btn_$assetId"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralReject),
                        border = BorderStroke(1.dp, CoralBorder),
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف عکس",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "حذف عکس",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isHighlighted) CoralHeaderTint else SlateBorder.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "بارگذاری عکس دارایی",
                            tint = if (isHighlighted) CoralReject else SlateTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "بارگذاری عکس دارایی (الزامی)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary
                            )
                            Surface(
                                color = CoralContainer,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, CoralBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = CoralReject,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "عکس اجباری است",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CoralText,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(
                            text = "فشرده‌سازی خودکار به حداکثر عرض ۱۰۲۴ پیکسل با کیفیت ۰.۷",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateTextMuted
                        )
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { launchInstantAuditCapture() },
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("instant_capture_btn_$assetId"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = accentColor,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "ثبت فوری عکس پلاک",
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ثبت فوری عکس پلاک",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { launchCameraCapture() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("camera_capture_btn_$assetId"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = "دوربین",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "دوربین",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            pickMediaLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gallery_pick_btn_$assetId"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "گالری",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "فایل / گالری",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = CoralReject,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
