package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.model.DocumentCategory
import com.example.model.PersianTextUtils
import com.example.model.StagedDocument
import com.example.model.StoreBranches
import com.example.model.UploadedDocumentRecord
import com.example.ui.theme.AmberBorder
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberSurplus
import com.example.ui.theme.AmberText
import com.example.ui.theme.CoralBorder
import com.example.ui.theme.CoralReject
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldConfirm
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldText
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.IndigoOnContainer
import com.example.ui.theme.NavyDeep
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
import com.example.util.CompressedPhotoResult
import com.example.util.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DocumentUploadScreen(
    storeBranch: String,
    auditorName: String,
    selectedCategory: DocumentCategory,
    stagedDocuments: List<StagedDocument>,
    uploadedDocuments: List<UploadedDocumentRecord>,
    uploadProgress: Float?,
    onSelectCategory: (DocumentCategory) -> Unit,
    onAddStagedDocument: (title: String, category: DocumentCategory, compressed: CompressedPhotoResult) -> Unit,
    onRemoveStagedDocument: (String) -> Unit,
    onUploadAllStaged: () -> Unit,
    onDeleteUploadedRecord: (UploadedDocumentRecord) -> Unit,
    onBackToHub: () -> Unit
) {
    BackHandler { onBackToHub() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var documentTitle by rememberSaveable { mutableStateOf("") }
    var isCompressing by remember { mutableStateOf(false) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCameraFile
        if (success && file != null) {
            scope.launch {
                isCompressing = true
                val res = ImageCompressor.compressAndSaveFromFile(context, file, selectedCategory.code)
                isCompressing = false
                res.onSuccess { compressed ->
                    onAddStagedDocument(documentTitle, selectedCategory, compressed)
                    documentTitle = ""
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
                scope.launch {
                    isCompressing = true
                    val res = ImageCompressor.generateQuickDocumentScanPhoto(
                        context = context,
                        categoryTitle = selectedCategory.persianTitle,
                        documentTitle = documentTitle,
                        storeBranch = storeBranch,
                        auditorName = auditorName
                    )
                    isCompressing = false
                    res.onSuccess { compressed ->
                        onAddStagedDocument(documentTitle, selectedCategory, compressed)
                        documentTitle = ""
                    }
                }
            }
        }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isCompressing = true
                val res = ImageCompressor.compressAndSaveFromUri(context, uri, selectedCategory.code)
                isCompressing = false
                res.onSuccess { compressed ->
                    onAddStagedDocument(documentTitle, selectedCategory, compressed)
                    documentTitle = ""
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
                    val res = ImageCompressor.generateQuickDocumentScanPhoto(
                        context = context,
                        categoryTitle = selectedCategory.persianTitle,
                        documentTitle = documentTitle,
                        storeBranch = storeBranch,
                        auditorName = auditorName
                    )
                    isCompressing = false
                    res.onSuccess { compressed ->
                        onAddStagedDocument(documentTitle, selectedCategory, compressed)
                        documentTitle = ""
                    }
                }
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun launchQuickSimulatedDocumentScan() {
        scope.launch {
            isCompressing = true
            val res = ImageCompressor.generateQuickDocumentScanPhoto(
                context = context,
                categoryTitle = selectedCategory.persianTitle,
                documentTitle = documentTitle,
                storeBranch = storeBranch,
                auditorName = auditorName
            )
            isCompressing = false
            res.onSuccess { compressed ->
                onAddStagedDocument(documentTitle, selectedCategory, compressed)
                documentTitle = ""
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SlateBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Surface(
                color = NavyDeep,
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = onBackToHub,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .testTag("doc_upload_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "بازگشت به خانه",
                                tint = Color.White
                            )
                        }

                        Column {
                            Text(
                                text = "بارگذاری مستندات اموال",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "$storeBranch (کد ${StoreBranches.codeForBranch(storeBranch)}) • ارسال صورتجلسات و فاکتورها",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Surface(
                        color = IndigoAccent,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${PersianTextUtils.toPersianDigits(uploadedDocuments.size)} ثبت‌شده",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = SlateSurface,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AnimatedVisibility(visible = uploadProgress != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "در حال بارگذاری و ثبت مستندات در پرونده شعبه...",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = IndigoAccent
                                )
                                Text(
                                    text = "${PersianTextUtils.toPersianDigits(((uploadProgress ?: 0f) * 100).toInt())}٪",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = IndigoAccent
                                )
                            }
                            LinearProgressIndicator(
                                progress = { uploadProgress ?: 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = IndigoAccent,
                                trackColor = IndigoContainer
                            )
                        }
                    }

                    Button(
                        onClick = onUploadAllStaged,
                        enabled = stagedDocuments.isNotEmpty() && uploadProgress == null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .testTag("upload_staged_docs_btn"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IndigoAccent,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (stagedDocuments.isEmpty()) {
                                "ابتدا تصویر صورتجلسه یا فاکتور را اضافه کنید"
                            } else {
                                "بارگذاری نهایی مستندات (${PersianTextUtils.toPersianDigits(stagedDocuments.size)} مدرک آماده)"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Step 1: Select Document Category (صورتجلسه، فاکتور، تصویر کالا)
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, SlateBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "۱. انتخاب دسته‌بندی مستندات",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                DocumentCategory.entries.forEach { category ->
                                    val isSelected = category == selectedCategory
                                    val icon = when (category) {
                                        DocumentCategory.MINUTES -> Icons.Default.Description
                                        DocumentCategory.INVOICE -> Icons.Default.ReceiptLong
                                        DocumentCategory.ASSET_PHOTO -> Icons.Default.Image
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { onSelectCategory(category) }
                                            .testTag("doc_category_${category.code.lowercase()}"),
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isSelected) IndigoContainer else SlateSurfaceVariant,
                                        border = BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) IndigoAccent else SlateBorder
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = category.persianTitle,
                                                tint = if (isSelected) IndigoAccent else SlateTextSecondary,
                                                modifier = Modifier.size(26.dp)
                                            )
                                            Text(
                                                text = category.persianTitle,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) IndigoOnContainer else SlateTextPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = selectedCategory.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateTextSecondary
                            )

                            OutlinedTextField(
                                value = documentTitle,
                                onValueChange = { documentTitle = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("doc_title_input"),
                                label = {
                                    Text("عنوان یا توضیحات مدرک (اختیاری)")
                                },
                                placeholder = {
                                    Text("مثال: صورتجلسه تحویل صندوق‌های فروشگاهی یا فاکتور شماره ۴۱۲...")
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IndigoAccent,
                                    unfocusedBorderColor = SlateBorder,
                                    focusedContainerColor = SlateSurface,
                                    unfocusedContainerColor = SlateSurfaceVariant
                                )
                            )
                        }
                    }
                }

                // Step 2: Capture via Camera or Pick from Gallery
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, SlateBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "۲. ثبت تصویر یا انتخاب فایل مدرک",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary
                            )

                            if (isCompressing) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = IndigoAccent
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "در حال بهینه‌سازی و آماده‌سازی تصویر مدرک...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SlateTextPrimary
                                    )
                                }
                            } else {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = { launchQuickSimulatedDocumentScan() },
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .height(54.dp)
                                            .testTag("doc_instant_scan_btn"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = TealVibrant,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "ثبت فوری اسکن ${selectedCategory.persianTitle}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { launchCameraCapture() },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(54.dp)
                                            .testTag("doc_camera_btn"),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddAPhoto,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "دوربین",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
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
                                            .height(54.dp)
                                            .testTag("doc_gallery_btn"),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoLibrary,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "گالری",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Step 3: Staged Thumbnails Preview with Delete Option
                if (stagedDocuments.isNotEmpty()) {
                    item {
                        Text(
                            text = "پیش‌نمایش مدارک آماده ارسال (${PersianTextUtils.toPersianDigits(stagedDocuments.size)} مورد)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = IndigoOnContainer
                        )
                    }

                    items(
                        items = stagedDocuments,
                        key = { it.id }
                    ) { staged ->
                        StagedDocumentPreviewCard(
                            item = staged,
                            onDelete = { onRemoveStagedDocument(staged.id) }
                        )
                    }
                }

                // Step 4: Uploaded Documents Archive for Current Branch
                item {
                    HorizontalDivider(color = SlateBorder)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "مستندات بارگذاری‌شده در پرونده $storeBranch",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )
                        Text(
                            text = "${PersianTextUtils.toPersianDigits(uploadedDocuments.size)} سند",
                            style = MaterialTheme.typography.labelMedium,
                            color = SlateTextSecondary
                        )
                    }
                }

                if (uploadedDocuments.isEmpty()) {
                    item {
                        Surface(
                            color = SlateSurface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, SlateBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = SlateTextMuted,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "هنوز مستندی برای این شعبه بارگذاری نشده است.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SlateTextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = uploadedDocuments,
                        key = { it.id }
                    ) { record ->
                        UploadedDocumentCard(
                            record = record,
                            onDelete = { onDeleteUploadedRecord(record) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StagedDocumentPreviewCard(
    item: StagedDocument,
    onDelete: () -> Unit
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = item.photoPath) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val file = File(item.photoPath)
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
            }.getOrNull()
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.5.dp, IndigoAccent.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("staged_doc_card_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 88.dp, height = 68.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(width = 88.dp, height = 68.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SlateSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = SlateTextMuted
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    color = AmberContainer,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, AmberBorder)
                ) {
                    Text(
                        text = "آماده بارگذاری • ${item.category.persianTitle}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AmberText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary
                )
                Text(
                    text = "${item.fileName} • ${PersianTextUtils.toPersianDigits(item.sizeKb)} کیلوبایت",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateTextSecondary
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_staged_doc_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "حذف پیش‌نمایش",
                    tint = CoralReject
                )
            }
        }
    }
}

@Composable
private fun UploadedDocumentCard(
    record: UploadedDocumentRecord,
    onDelete: () -> Unit
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = record.photoPath) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val file = File(record.photoPath)
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
            }.getOrNull()
        }
    }

    val formattedTime = remember(record.uploadedAt) {
        PersianTextUtils.toPersianDigits(
            SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(record.uploadedAt))
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, EmeraldBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("uploaded_doc_card_${record.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = record.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 84.dp, height = 64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, EmeraldBorder, RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(width = 84.dp, height = 64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EmeraldContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldConfirm
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = EmeraldContainer,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, EmeraldBorder)
                    ) {
                        Text(
                            text = "${record.categoryTitle} ✓",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateTextMuted
                    )
                }

                Text(
                    text = record.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary
                )

                Text(
                    text = "ثبت توسط: ${record.uploadedBy} • ${PersianTextUtils.toPersianDigits(record.sizeKb)} کیلوبایت",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateTextSecondary
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "حذف مستند",
                    tint = CoralReject
                )
            }
        }
    }
}
