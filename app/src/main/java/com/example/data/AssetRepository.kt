package com.example.data

import com.example.model.AssetRecord
import com.example.model.AssetStatus
import com.example.model.StoreBranches
import com.example.model.UploadedDocumentRecord
import kotlinx.coroutines.flow.Flow

class AssetRepository(private val assetDao: AssetDao) {

    fun observeAllAssets(): Flow<List<AssetRecord>> = assetDao.observeAllAssets()

    fun observeAssetsForBranch(branch: String): Flow<List<AssetRecord>> =
        assetDao.observeAssetsByBranch(branch)

    fun observeDocumentsForBranch(branch: String): Flow<List<UploadedDocumentRecord>> =
        assetDao.observeDocumentsByBranch(branch)

    suspend fun ensureSeededForBranches() {
        for ((index, branch) in StoreBranches.ALL_BRANCHES.withIndex()) {
            val count = assetDao.getCountForBranch(branch)
            if (count == 0) {
                assetDao.insertAll(buildSampleAssetsForBranch(branch, branchIndex = index + 1))
            }
        }
    }

    suspend fun resetBranchToSampleData(branch: String) {
        val branchIndex = StoreBranches.ALL_BRANCHES.indexOf(branch).coerceAtLeast(0) + 1
        assetDao.deleteBranchAssets(branch)
        assetDao.insertAll(buildSampleAssetsForBranch(branch, branchIndex))
    }

    suspend fun replaceOrAppendImportedAssets(
        branch: String,
        importedRows: List<AssetRecord>,
        replaceExisting: Boolean
    ) {
        if (replaceExisting) {
            assetDao.deleteBranchAssets(branch)
        }
        assetDao.insertAll(importedRows)
    }

    suspend fun upsertAsset(asset: AssetRecord) {
        assetDao.insertAsset(asset.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteAsset(id: String) {
        assetDao.deleteById(id)
    }

    suspend fun insertUploadedDocuments(docs: List<UploadedDocumentRecord>) {
        assetDao.insertUploadedDocuments(docs)
    }

    suspend fun deleteUploadedDocument(id: String) {
        assetDao.deleteUploadedDocumentById(id)
    }

    companion object {
        fun buildSampleAssetsForBranch(branch: String, branchIndex: Int): List<AssetRecord> {
            val prefix = (branchIndex * 1000)
            return listOf(
                AssetRecord(
                    id = "${branch}_1",
                    storeBranch = branch,
                    description = "لپتاپ Dell Latitude 5420 - واحد مدیریت فروشگاه",
                    assetTag1 = "${prefix + 10928}",
                    assetTag2 = "${prefix + 29873}",
                    assetTag3 = "ندارد",
                    assetCardCode = "IT-LAPTOP",
                    batch = "Laptop",
                    status = AssetStatus.PENDING.code
                ),
                AssetRecord(
                    id = "${branch}_2",
                    storeBranch = branch,
                    description = "پایانه فروشگاهی (POS) لمسی Verifone V200c صندوق ۱",
                    assetTag1 = "${prefix + 44512}",
                    assetTag2 = "${prefix + 88104}",
                    assetTag3 = "ناخوانا",
                    assetCardCode = "IT-POS-TERM",
                    batch = "POS",
                    status = AssetStatus.PENDING.code
                ),
                AssetRecord(
                    id = "${branch}_3",
                    storeBranch = branch,
                    description = "بارکدخوان دوبعدی بی‌سیم Honeywell Xenon 1952g",
                    assetTag1 = "${prefix + 33219}",
                    assetTag2 = "ندارد",
                    assetTag3 = "ندارد",
                    assetCardCode = "IT-SCANNER",
                    batch = "Scanner",
                    status = AssetStatus.PENDING.code
                ),
                AssetRecord(
                    id = "${branch}_4",
                    storeBranch = branch,
                    description = "چاپگر حرارتی صدور فیش Bixolon SRP-350III صندوق ۲",
                    assetTag1 = "${prefix + 55890}",
                    assetTag2 = "${prefix + 55891}",
                    assetTag3 = "ندارد",
                    assetCardCode = "IT-PRINTER",
                    batch = "Printer",
                    status = AssetStatus.PENDING.code
                ),
                AssetRecord(
                    id = "${branch}_5",
                    storeBranch = branch,
                    description = "سوییچ شبکه ۲۴ پورت مدیریتی Cisco Catalyst 2960X رک مرکزی",
                    assetTag1 = "${prefix + 77401}",
                    assetTag2 = "${prefix + 77402}",
                    assetTag3 = "${prefix + 77403}",
                    assetCardCode = "IT-SWITCH",
                    batch = "Network",
                    status = AssetStatus.PENDING.code
                )
            )
        }
    }
}
