package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Xml
import androidx.core.content.FileProvider
import com.example.model.AssetRecord
import com.example.model.AssetStatus
import com.example.model.AuditSummaryStats
import com.example.model.PersianTextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ExcelExportPayload(
    val file: File,
    val shareUri: Uri,
    val fileName: String,
    val finalReportRows: List<List<String>>,
    val summarySheetRows: List<List<String>>
)

object ExcelHelper {

    val FINAL_REPORT_HEADERS = listOf(
        "ردیف",
        "شعبه",
        "شرح کارت دارایی",
        "شماره اموال ۱",
        "شماره اموال ۲",
        "شماره اموال ۳",
        "کارت دارایی",
        "بچ",
        "وضعیت بررسی",
        "شماره اموال ۱ اصلاحی",
        "شماره اموال ۲ اصلاحی",
        "شماره اموال ۳ اصلاحی",
        "نوع ردیف",
        "نام فایل عکس"
    )

    val TEMPLATE_HEADERS = listOf(
        "شرح کارت دارایی",
        "شماره اموال ۱",
        "شماره اموال ۲",
        "شماره اموال ۳",
        "کارت دارایی",
        "بچ"
    )

    /**
     * Parses an Excel (.xlsx) or CSV/TSV file stream and extracts AssetRecord rows
     * matching columns:
     * شرح کارت دارایی, شماره اموال ۱, شماره اموال ۲, شماره اموال ۳, کارت دارایی, بچ
     */
    suspend fun importAssetsFromUri(
        context: Context,
        uri: Uri,
        storeBranch: String
    ): Result<List<AssetRecord>> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalArgumentException("فایل انتخابی قابل خواندن نیست.")

            val rawRows = if (isZipArchive(bytes)) {
                parseXlsxBytes(bytes)
            } else {
                parseCsvBytes(bytes)
            }

            if (rawRows.isEmpty()) {
                throw IllegalArgumentException("هیچ ردیف داده‌ای در فایل اکسل یافت نشد.")
            }

            convertRawRowsToAssetRecords(rawRows, storeBranch)
        }
    }

    fun parseXlsxBytes(bytes: ByteArray): List<List<String>> {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val out = ByteArrayOutputStream()
                    val buffer = ByteArray(4096)
                    var len: Int
                    while (zis.read(buffer).also { len = it } > 0) {
                        out.write(buffer, 0, len)
                    }
                    entries[entry.name] = out.toByteArray()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val sharedStringsBytes = entries["xl/sharedStrings.xml"]
        val sharedStrings = if (sharedStringsBytes != null) {
            parseSharedStringsXml(ByteArrayInputStream(sharedStringsBytes))
        } else {
            emptyList()
        }

        val sheetEntry = entries["xl/worksheets/sheet1.xml"]
            ?: entries.entries.firstOrNull { it.key.startsWith("xl/worksheets/sheet") && it.key.endsWith(".xml") }?.value
            ?: throw IllegalArgumentException("شیت داده در فایل اکسل (.xlsx) یافت نشد.")

        return parseWorksheetXml(ByteArrayInputStream(sheetEntry), sharedStrings)
    }

    private fun parseSharedStringsXml(inputStream: InputStream): List<String> {
        val result = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        var eventType = parser.eventType
        var insideSi = false
        val currentSiText = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "si") {
                        insideSi = true
                        currentSiText.clear()
                    } else if (insideSi && parser.name == "t") {
                        currentSiText.append(parser.nextText())
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "si" && insideSi) {
                        result.add(currentSiText.toString())
                        insideSi = false
                    }
                }
            }
            eventType = parser.next()
        }
        return result
    }

    private fun parseWorksheetXml(
        inputStream: InputStream,
        sharedStrings: List<String>
    ): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        var eventType = parser.eventType
        var currentRowMap = sortedMapOf<Int, String>()
        var currentCellCol = 0
        var currentCellType: String? = null
        var insideCell = false
        var insideIs = false
        val cellText = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            currentRowMap = sortedMapOf()
                        }
                        "c" -> {
                            insideCell = true
                            cellText.clear()
                            val ref = parser.getAttributeValue(null, "r") ?: ""
                            currentCellCol = columnRefToIndex(ref, fallback = currentRowMap.size)
                            currentCellType = parser.getAttributeValue(null, "t")
                        }
                        "is" -> {
                            if (insideCell) insideIs = true
                        }
                        "v" -> {
                            if (insideCell) {
                                val rawVal = parser.nextText()
                                if (currentCellType == "s") {
                                    val idx = rawVal.trim().toIntOrNull()
                                    if (idx != null && idx in sharedStrings.indices) {
                                        cellText.append(sharedStrings[idx])
                                    } else {
                                        cellText.append(rawVal)
                                    }
                                } else {
                                    cellText.append(rawVal)
                                }
                            }
                        }
                        "t" -> {
                            if (insideCell && (currentCellType == "inlineStr" || insideIs || currentCellType == "str")) {
                                cellText.append(parser.nextText())
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "is" -> insideIs = false
                        "c" -> {
                            if (insideCell) {
                                currentRowMap[currentCellCol] = cellText.toString().trim()
                                insideCell = false
                            }
                        }
                        "row" -> {
                            if (currentRowMap.isNotEmpty()) {
                                val maxCol = (currentRowMap.keys.maxOrNull() ?: 0).coerceAtMost(25)
                                val rowList = (0..maxCol).map { col -> currentRowMap[col] ?: "" }
                                if (rowList.any { it.isNotBlank() }) {
                                    rows.add(rowList)
                                }
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return rows
    }

    private fun columnRefToIndex(cellRef: String, fallback: Int): Int {
        val letters = cellRef.takeWhile { it.isLetter() }.uppercase(Locale.US)
        if (letters.isEmpty()) return fallback
        var col = 0
        for (ch in letters) {
            col = col * 26 + (ch - 'A' + 1)
        }
        return (col - 1).coerceAtLeast(0)
    }

    private fun parseCsvBytes(bytes: ByteArray): List<List<String>> {
        val content = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
        val delimiter = if (content.lines().firstOrNull()?.contains('\t') == true) '\t' else ','
        return content.lines()
            .map { line -> line.split(delimiter).map { it.trim().removeSurrounding("\"") } }
            .filter { row -> row.any { it.isNotBlank() } }
    }

    private fun convertRawRowsToAssetRecords(
        rawRows: List<List<String>>,
        storeBranch: String
    ): List<AssetRecord> {
        val firstRow = rawRows.first()
        val normalizedHeaders = firstRow.map { PersianTextUtils.normalizeForSearch(it) }

        val hasHeaderRow = normalizedHeaders.any {
            it.contains("شرح") || it.contains("اموال") || it.contains("دارایی") || it.contains("بچ") || it.contains("description")
        }

        val dataRows = if (hasHeaderRow) rawRows.drop(1) else rawRows
        if (dataRows.isEmpty()) {
            throw IllegalArgumentException("فایل اکسل فقط شامل سرستون است و ردیف کالایی ندارد.")
        }

        fun findColumnIndex(vararg candidates: String, defaultIdx: Int): Int {
            if (!hasHeaderRow) return defaultIdx
            val idx = normalizedHeaders.indexOfFirst { header ->
                candidates.any { candidate -> header == candidate || header.contains(candidate) }
            }
            return if (idx >= 0) idx else defaultIdx
        }

        val descCol = findColumnIndex("شرح کارت دارایی", "شرح کالا", "شرح", "description", defaultIdx = 0)
        val tag1Col = findColumnIndex("شماره اموال 1", "شماره اموال", "اموال 1", "assettag1", defaultIdx = 1)
        val tag2Col = findColumnIndex("شماره اموال 2", "اموال 2", "assettag2", defaultIdx = 2)
        val tag3Col = findColumnIndex("شماره اموال 3", "اموال 3", "assettag3", defaultIdx = 3)
        val cardCodeCol = findColumnIndex("کارت دارایی", "کد دارایی", "assetcardcode", defaultIdx = 4)
        val batchCol = findColumnIndex("بچ", "دسته", "batch", defaultIdx = 5)

        val now = System.currentTimeMillis()
        return dataRows.mapIndexedNotNull { index, row ->
            val description = row.getOrNull(descCol)?.trim().orEmpty()
            if (description.isBlank()) return@mapIndexedNotNull null
            val tag1 = row.getOrNull(tag1Col)?.trim().orEmpty().ifBlank { "ندارد" }
            val tag2 = row.getOrNull(tag2Col)?.trim().orEmpty().ifBlank { "ندارد" }
            val tag3 = row.getOrNull(tag3Col)?.trim().orEmpty().ifBlank { "ندارد" }
            val cardCode = row.getOrNull(cardCodeCol)?.trim().orEmpty().ifBlank { "IT-ASSET" }
            val batch = row.getOrNull(batchCol)?.trim().orEmpty().ifBlank { "General" }

            AssetRecord(
                id = "${storeBranch}_imp_${now}_${index + 1}_${UUID.randomUUID().toString().take(4)}",
                storeBranch = storeBranch,
                description = description,
                assetTag1 = tag1,
                assetTag2 = tag2,
                assetTag3 = tag3,
                assetCardCode = cardCode,
                batch = batch,
                status = AssetStatus.PENDING.code
            )
        }
    }

    /**
     * Generates a formatted 2-sheet OpenXML (.xlsx) file containing:
     * Sheet 1: گزارش نهایی (all rows with original tags, corrected tags, status in Persian, and photo filename)
     * Sheet 2: خلاصه وضعیت (summary metrics)
     */
    suspend fun generateAuditExportXlsx(
        context: Context,
        storeBranch: String,
        assets: List<AssetRecord>,
        stats: AuditSummaryStats
    ): Result<ExcelExportPayload> = withContext(Dispatchers.IO) {
        runCatching {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
            val fileStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

            val sheet1Rows = mutableListOf<List<String>>()
            sheet1Rows.add(FINAL_REPORT_HEADERS)

            assets.forEachIndexed { index, item ->
                sheet1Rows.add(
                    listOf(
                        (index + 1).toString(),
                        item.storeBranch,
                        item.description,
                        item.assetTag1,
                        item.assetTag2,
                        item.assetTag3,
                        item.assetCardCode,
                        item.batch,
                        item.statusEnum.persianTitle,
                        item.correctedAssetTag1.ifBlank { "-" },
                        item.correctedAssetTag2.ifBlank { "-" },
                        item.correctedAssetTag3.ifBlank { "-" },
                        if (item.isSurplus || item.status == AssetStatus.SURPLUS.code) "کالای مازاد" else "کالای لیست اصلی",
                        item.photoFileName ?: "بدون عکس"
                    )
                )
            }

            val sheet2Rows = listOf(
                listOf("شاخص ممیزی اموال", "مقدار ثبت شده"),
                listOf("نام فروشگاه / شعبه", storeBranch),
                listOf("تاریخ و ساعت تایید نهایی", timestamp),
                listOf("تعداد کل اقلام بررسی شده", stats.totalCount.toString()),
                listOf("تایید شده (معتبر به همراه عکس)", stats.confirmedCount.toString()),
                listOf("دارای مغایرت (به همراه کد اصلاحی و عکس)", stats.discrepancyCount.toString()),
                listOf("کالای مازاد کشف شده در فروشگاه", stats.surplusCount.toString()),
                listOf("موجود نیست / مفقودی", stats.missingItemCount.toString()),
                listOf("در انتظار بررسی", stats.pendingCount.toString()),
                listOf("اقلام فاقد عکس الزامی", stats.missingPhotoCount.toString()),
                listOf("وضعیت نهایی ممیزی", if (stats.isReadyForFinalExport) "تکمیل و تایید نهایی شده" else "ناقص")
            )

            val xlsxBytes = createTwoSheetXlsxArchive(
                sheet1Name = "گزارش نهایی",
                sheet1Rows = sheet1Rows,
                sheet2Name = "خلاصه وضعیت",
                sheet2Rows = sheet2Rows
            )

            val exportsDir = File(context.filesDir, "excel_exports").apply { mkdirs() }
            val fileName = "Audit_Report_${fileStamp}.xlsx"
            val outFile = File(exportsDir, fileName)
            FileOutputStream(outFile).use { it.write(xlsxBytes) }

            val shareUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )

            ExcelExportPayload(
                file = outFile,
                shareUri = shareUri,
                fileName = fileName,
                finalReportRows = sheet1Rows,
                summarySheetRows = sheet2Rows
            )
        }
    }

    /**
     * Generates a downloadable sample Excel (.xlsx) template with the 6 required Persian columns
     * so auditors can also test importing a fresh Excel file on the spot.
     */
    suspend fun generateSampleTemplateXlsx(
        context: Context,
        storeBranch: String
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val rows = listOf(
                TEMPLATE_HEADERS,
                listOf("مانیتور ۲۴ اینچ صنعتی Dell P2422H", "10928", "29873", "ندارد", "IT-MONITOR", "B-04"),
                listOf("کیس کامپیوتر اداری HP ProDesk 400 G7", "44512", "ندارد", "ندارد", "IT-PC-CASE", "PC"),
                listOf("پرینتر لیزری شبکه HP LaserJet Pro M404dn", "78110", "78111", "ناخوانا", "IT-LASER-PRN", "Printer"),
                listOf("ترازوی دیجیتال بارکدزن آزمایشگاهی و فروشگاهی Digi SM-5100", "62301", "62302", "ندارد", "IT-SCALE", "Scale"),
                listOf("یوپی‌اس ۲۰۰۰ ولت‌آمپر آنلاین Faratel", "91045", "ندارد", "ندارد", "IT-UPS", "Power")
            )

            val summaryRows = listOf(
                listOf("راهنمای قالب ورود اکسل", "توضیحات"),
                listOf("شعبه هدف", storeBranch),
                listOf("ستون ۱", "شرح کارت دارایی"),
                listOf("ستون ۲ تا ۴", "شماره اموال ۱، ۲ و ۳ (عدد، ندارد یا ناخوانا)"),
                listOf("ستون ۵ و ۶", "کارت دارایی و بچ")
            )

            val xlsxBytes = createTwoSheetXlsxArchive(
                sheet1Name = "قالب اموال",
                sheet1Rows = rows,
                sheet2Name = "راهنما",
                sheet2Rows = summaryRows
            )

            val exportsDir = File(context.filesDir, "excel_exports").apply { mkdirs() }
            val outFile = File(exportsDir, "Sample_Asset_Template.xlsx")
            FileOutputStream(outFile).use { it.write(xlsxBytes) }
            outFile
        }
    }

    private fun createTwoSheetXlsxArchive(
        sheet1Name: String,
        sheet1Rows: List<List<String>>,
        sheet2Name: String,
        sheet2Rows: List<List<String>>
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            fun putXmlEntry(path: String, xmlContent: String) {
                zos.putNextEntry(ZipEntry(path))
                zos.write(xmlContent.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            putXmlEntry(
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""
            )

            putXmlEntry(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
            )

            putXmlEntry(
                "xl/workbook.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="${escapeXml(sheet1Name)}" sheetId="1" r:id="rId1"/>
    <sheet name="${escapeXml(sheet2Name)}" sheetId="2" r:id="rId2"/>
  </sheets>
</workbook>"""
            )

            putXmlEntry(
                "xl/_rels/workbook.xml.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
            )

            putXmlEntry(
                "xl/styles.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><name val="Tahoma"/></font>
    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Tahoma"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF1E293B"/><bgColor indexed="64"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FFCBD5E1"/></left>
      <right style="thin"><color rgb="FFCBD5E1"/></right>
      <top style="thin"><color rgb="FFCBD5E1"/></top>
      <bottom style="thin"><color rgb="FFCBD5E1"/></bottom>
      <diagonal/>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="3">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center" readingOrder="2"/>
    </xf>
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center" readingOrder="2"/>
    </xf>
  </cellXfs>
</styleSheet>"""
            )

            putXmlEntry("xl/worksheets/sheet1.xml", buildSheetXml(sheet1Rows))
            putXmlEntry("xl/worksheets/sheet2.xml", buildSheetXml(sheet2Rows))
        }
        return baos.toByteArray()
    }

    private fun buildSheetXml(rows: List<List<String>>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("\n")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("""<sheetViews><sheetView rightToLeft="1" workbookViewId="0"/></sheetViews>""")
        sb.append("<sheetData>")

        rows.forEachIndexed { rowIdx, row ->
            val rNum = rowIdx + 1
            val styleIndex = if (rowIdx == 0) 1 else 2
            sb.append("""<row r="$rNum">""")
            row.forEachIndexed { colIdx, cellValue ->
                val colRef = indexToColumnRef(colIdx) + rNum
                sb.append("""<c r="$colRef" s="$styleIndex" t="inlineStr"><is><t>${escapeXml(cellValue)}</t></is></c>""")
            }
            sb.append("</row>")
        }

        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun indexToColumnRef(index: Int): String {
        var n = index
        val sb = StringBuilder()
        while (n >= 0) {
            sb.insert(0, ('A' + (n % 26)))
            n = (n / 26) - 1
        }
        return sb.toString()
    }

    private fun escapeXml(value: String): String {
        return buildString(value.length) {
            for (ch in value) {
                when (ch) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&apos;")
                    else -> if (ch.code >= 0x20 || ch == '\n' || ch == '\r' || ch == '\t') append(ch)
                }
            }
        }
    }

    private fun isZipArchive(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() &&
            bytes[1] == 0x4B.toByte() &&
            bytes[2] == 0x03.toByte() &&
            bytes[3] == 0x04.toByte()
    }
}
