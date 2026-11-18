package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.model.AssetRecord
import com.example.model.UploadedDocumentRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {
    @Query("SELECT * FROM asset_records WHERE storeBranch = :branch ORDER BY isSurplus ASC, id ASC")
    fun observeAssetsByBranch(branch: String): Flow<List<AssetRecord>>

    @Query("SELECT * FROM asset_records ORDER BY storeBranch ASC, isSurplus ASC, id ASC")
    fun observeAllAssets(): Flow<List<AssetRecord>>

    @Query("SELECT COUNT(*) FROM asset_records WHERE storeBranch = :branch")
    suspend fun getCountForBranch(branch: String): Int

    @Query("SELECT COUNT(*) FROM asset_records")
    suspend fun getTotalCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AssetRecord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(item: AssetRecord)

    @Update
    suspend fun updateAsset(item: AssetRecord)

    @Query("DELETE FROM asset_records WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM asset_records WHERE storeBranch = :branch")
    suspend fun deleteBranchAssets(branch: String)

    // Uploaded Documents Queries
    @Query("SELECT * FROM uploaded_documents WHERE storeBranch = :branch ORDER BY uploadedAt DESC")
    fun observeDocumentsByBranch(branch: String): Flow<List<UploadedDocumentRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUploadedDocuments(docs: List<UploadedDocumentRecord>)

    @Query("DELETE FROM uploaded_documents WHERE id = :id")
    suspend fun deleteUploadedDocumentById(id: String)
}

@Database(
    entities = [AssetRecord::class, UploadedDocumentRecord::class],
    version = 2,
    exportSchema = false
)
abstract class AssetDatabase : RoomDatabase() {
    abstract fun assetDao(): AssetDao

    companion object {
        @Volatile
        private var INSTANCE: AssetDatabase? = null

        fun getInstance(context: Context): AssetDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AssetDatabase::class.java,
                    "retail_asset_audit.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
