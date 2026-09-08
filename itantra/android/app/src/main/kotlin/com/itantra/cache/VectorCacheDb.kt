package com.itantra.cache

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "cached_phrases")
data class CachedPhrase(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "label") val label: String?,
    @ColumnInfo(name = "embedding", typeAffinity = ColumnInfo.BLOB) val embedding: ByteArray,
    @ColumnInfo(name = "audio_path") val audioPath: String,
    @ColumnInfo(name = "lang_code") val langCode: String,
    @ColumnInfo(name = "hit_count") val hitCount: Int = 0
)

@Dao
interface CachedPhraseDao {
    @Query("SELECT * FROM cached_phrases WHERE lang_code = :langCode")
    suspend fun getPhrasesForLang(langCode: String): List<CachedPhrase>

    @Query("UPDATE cached_phrases SET hit_count = hit_count + 1 WHERE id = :id")
    suspend fun incrementHitCount(id: Int)

    @Insert
    suspend fun insertPhrase(phrase: CachedPhrase)
}

@Database(entities = [CachedPhrase::class], version = 1, exportSchema = false)
abstract class VectorCacheDb : RoomDatabase() {
    abstract fun cachedPhraseDao(): CachedPhraseDao
}
