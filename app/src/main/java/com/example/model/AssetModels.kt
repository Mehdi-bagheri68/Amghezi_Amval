package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AssetStatus(val code: String, val persianTitle: String) {
    PENDING("PENDING", "در انتظار بررسی"),
    CONFIRMED("CONFIRMED", "تایید شده"),
    DISCREPANCY("DISCREPANCY", "مغایرت"),
    MISSING("MISSING", "موجود نیست"),
    SURPLUS("SURPLUS", "کالای مازاد");

    companion object {
        fun fromCode(code: String): AssetStatus {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: PENDING
        }
    }
}

enum class AuditFilterTab(val code: String, val persianLabel: String) {
    ALL("ALL", "همه"),
    PENDING("PENDING", "در انتظار بررسی"),
    CONFIRMED("CONFIRMED", "تایید شده"),
    DISCREPANCY("DISCREPANCY", "مغایرت"),
    SURPLUS("SURPLUS", "مازاد"),
    MISSING("MISSING", "موجود نیست")
}

@Entity(tableName = "asset_records")
data class AssetRecord(
    @PrimaryKey
    val id: String,
    val storeBranch: String,
    val description: String,
    val assetTag1: String,
    val assetTag2: String,
    val assetTag3: String,
    val assetCardCode: String,
    val batch: String,
    val status: String = AssetStatus.PENDING.code,
    val correctedAssetTag1: String = "",
    val correctedAssetTag2: String = "",
    val correctedAssetTag3: String = "",
    val photoUrl: String? = null,
    val photoFileName: String? = null,
    val photoSizeKb: Int = 0,
    val isSurplus: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val statusEnum: AssetStatus
        get() = AssetStatus.fromCode(status)

    val requiresPhoto: Boolean
        get() = status == AssetStatus.CONFIRMED.code ||
            status == AssetStatus.DISCREPANCY.code ||
            status == AssetStatus.SURPLUS.code ||
            isSurplus

    val hasPhoto: Boolean
        get() = !photoUrl.isNullOrBlank()

    val isMissingMandatoryPhoto: Boolean
        get() = requiresPhoto && !hasPhoto

    val hasCorrectedTagForDiscrepancy: Boolean
        get() = status != AssetStatus.DISCREPANCY.code ||
            correctedAssetTag1.isNotBlank() ||
            correctedAssetTag2.isNotBlank() ||
            correctedAssetTag3.isNotBlank()

    val isRowComplete: Boolean
        get() = status != AssetStatus.PENDING.code &&
            !isMissingMandatoryPhoto &&
            hasCorrectedTagForDiscrepancy
}

data class AuditSummaryStats(
    val totalCount: Int = 0,
    val pendingCount: Int = 0,
    val confirmedCount: Int = 0,
    val discrepancyCount: Int = 0,
    val missingPhotoCount: Int = 0,
    val surplusCount: Int = 0,
    val missingItemCount: Int = 0,
    val incompleteDiscrepancyCount: Int = 0
) {
    val completedCount: Int
        get() = (totalCount - pendingCount - missingPhotoCount - incompleteDiscrepancyCount).coerceAtLeast(0)

    val isReadyForFinalExport: Boolean
        get() = totalCount > 0 &&
            pendingCount == 0 &&
            missingPhotoCount == 0 &&
            incompleteDiscrepancyCount == 0
}

object StoreBranches {
    val ALL_BRANCHES = listOf(
        "شعبه مرکزی",
        "شعبه ونک",
        "شعبه سعادت‌آباد",
        "شعبه تجریش",
        "شعبه پالادیوم"
    )
}

object PersianTextUtils {
    private val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    private val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    fun toPersianDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val idx = englishDigits.indexOf(ch)
            if (idx >= 0) {
                sb.append(persianDigits[idx])
            } else {
                val arIdx = arabicDigits.indexOf(ch)
                if (arIdx >= 0) sb.append(persianDigits[arIdx]) else sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toPersianDigits(number: Int): String = toPersianDigits(number.toString())

    fun normalizeForSearch(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input.trim().lowercase()) {
            val pIdx = persianDigits.indexOf(ch)
            val aIdx = arabicDigits.indexOf(ch)
            when {
                pIdx >= 0 -> sb.append(englishDigits[pIdx])
                aIdx >= 0 -> sb.append(englishDigits[aIdx])
                ch == 'ي' -> sb.append('ی')
                ch == 'ك' -> sb.append('ک')
                ch == '\u200C' -> sb.append(' ')
                else -> sb.append(ch)
            }
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }
}
