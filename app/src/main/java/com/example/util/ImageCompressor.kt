package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CompressedPhotoResult(
    val filePath: String,
    val fileName: String,
    val width: Int,
    val height: Int,
    val sizeKb: Int
)

object ImageCompressor {
    private const val MAX_WIDTH = 1024
    private const val JPEG_QUALITY = 70 // 0.7 quality as specified

    fun createTempCameraUri(context: Context): Pair<Uri, File> {
        val cacheDir = File(context.cacheDir, "camera_cache").apply { mkdirs() }
        val tempFile = File(cacheDir, "capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        return uri to tempFile
    }

    suspend fun compressAndSaveFromUri(
        context: Context,
        sourceUri: Uri,
        assetCodePrefix: String
    ): Result<CompressedPhotoResult> = withContext(Dispatchers.IO) {
        runCatching {
            val rawBytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
                ?: throw IllegalArgumentException("امکان خواندن فایل تصویر وجود ندارد.")

            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOptions)

            var sampleSize = 1
            while (boundsOptions.outWidth / sampleSize > MAX_WIDTH * 2) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decodedBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
                ?: throw IllegalArgumentException("فرمت فایل تصویر معتبر نیست.")

            compressAndPersistBitmap(context, decodedBitmap, assetCodePrefix)
        }
    }

    suspend fun compressAndSaveFromFile(
        context: Context,
        sourceFile: File,
        assetCodePrefix: String
    ): Result<CompressedPhotoResult> = withContext(Dispatchers.IO) {
        runCatching {
            if (!sourceFile.exists() || sourceFile.length() == 0L) {
                throw IllegalArgumentException("فایل تصویر دوربین یافت نشد.")
            }
            val decodedBitmap = BitmapFactory.decodeFile(sourceFile.absolutePath)
                ?: throw IllegalArgumentException("امکان پردازش تصویر ثبت شده وجود ندارد.")
            compressAndPersistBitmap(context, decodedBitmap, assetCodePrefix)
        }
    }

    /**
     * Generates a realistic 1024x720 asset & barcode tag audit photo on HTML5-equivalent Android Canvas
     * and runs it through the exact same max-width 1024px, JPEG 0.7 compression pipeline.
     * Ideal for instant testing in browser emulators or quick field verification.
     */
    suspend fun generateQuickCameraAuditPhoto(
        context: Context,
        assetDescription: String,
        assetTag: String,
        assetCardCode: String,
        storeBranch: String
    ): Result<CompressedPhotoResult> = withContext(Dispatchers.IO) {
        runCatching {
            val width = 1024
            val height = 720
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Studio metallic/slate background
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E293B")
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Subtle grid lines
            val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#334155")
                strokeWidth = 2f
            }
            for (x in 0..width step 64) {
                canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), gridPaint)
            }
            for (y in 0..height step 64) {
                canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)
            }

            // Outer metallic asset plaque card
            val cardRect = RectF(90f, 90f, width - 90f, height - 90f)
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F8FAFC")
            }
            canvas.drawRoundRect(cardRect, 28f, 28f, cardPaint)

            // Top emerald/indigo header band inside plaque
            val headerRect = RectF(90f, 90f, width - 90f, 195f)
            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
            }
            canvas.drawRoundRect(headerRect, 28f, 28f, headerPaint)
            canvas.drawRect(90f, 150f, width - 90f, 195f, headerPaint)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 32f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                "پلاک اموال فروشگاه‌های زنجیره‌ای • $storeBranch",
                width / 2f,
                155f,
                titlePaint
            )

            // Asset description & card code
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 30f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val trimmedDesc = if (assetDescription.length > 42) {
                assetDescription.take(42) + "..."
            } else {
                assetDescription
            }
            canvas.drawText(trimmedDesc, width / 2f, 265f, bodyPaint)

            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#475569")
                textSize = 24f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                "کارت دارایی: ${assetCardCode.ifBlank { "IT-ASSET" }}",
                width / 2f,
                315f,
                subPaint
            )

            // Barcode stripes simulation
            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
            }
            val startX = 250f
            val endX = width - 250f
            var curX = startX
            var idx = 0
            while (curX < endX) {
                val barW = when ((idx + assetTag.hashCode()) % 4) {
                    0 -> 4f
                    1 -> 8f
                    2 -> 12f
                    else -> 6f
                }
                if (idx % 2 == 0) {
                    canvas.drawRect(curX, 355f, (curX + barW).coerceAtMost(endX), 475f, barPaint)
                }
                curX += barW + 4f
                idx++
            }

            // Asset Tag number box
            val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#059669")
                textSize = 36f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val displayTag = assetTag.ifBlank { "بدون کد" }
            canvas.drawText("ASSET TAG: $displayTag", width / 2f, 530f, tagPaint)

            // Timestamp footer
            val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#64748B")
                textSize = 20f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Verified Audit Capture • $timeStr • Max 1024px JPEG 0.7", width / 2f, 595f, footerPaint)

            compressAndPersistBitmap(context, bitmap, assetCardCode.ifBlank { "ASSET" })
        }
    }

    private fun compressAndPersistBitmap(
        context: Context,
        sourceBitmap: Bitmap,
        assetCodePrefix: String
    ): CompressedPhotoResult {
        // Scale down using Canvas if width > 1024px
        val targetWidth: Int
        val targetHeight: Int
        if (sourceBitmap.width > MAX_WIDTH) {
            val ratio = MAX_WIDTH.toFloat() / sourceBitmap.width.toFloat()
            targetWidth = MAX_WIDTH
            targetHeight = (sourceBitmap.height * ratio).toInt().coerceAtLeast(1)
        } else {
            targetWidth = sourceBitmap.width
            targetHeight = sourceBitmap.height
        }

        val scaledBitmap = if (targetWidth != sourceBitmap.width || targetHeight != sourceBitmap.height) {
            val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(
                sourceBitmap,
                null,
                RectF(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat()),
                paint
            )
            output
        } else {
            sourceBitmap
        }

        val baos = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos)
        val compressedBytes = baos.toByteArray()

        val sanitizedPrefix = assetCodePrefix
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .take(18)
            .ifBlank { "asset" }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "${sanitizedPrefix}_${timestamp}.jpg"

        val photosDir = File(context.filesDir, "audit_photos").apply { mkdirs() }
        val destFile = File(photosDir, fileName)
        FileOutputStream(destFile).use { fos ->
            fos.write(compressedBytes)
            fos.flush()
        }

        val sizeKb = (compressedBytes.size / 1024).coerceAtLeast(1)
        return CompressedPhotoResult(
            filePath = destFile.absolutePath,
            fileName = fileName,
            width = targetWidth,
            height = targetHeight,
            sizeKb = sizeKb
        )
    }

    fun deletePhotoFileIfExists(filePath: String?) {
        if (filePath.isNullOrBlank()) return
        runCatching {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        }
    }
}
