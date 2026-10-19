package net.clagon.aozora.library

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.Flow

/** 本棚に入れた作品。addedAt は追加した日時（エポックミリ秒）。 */
@Entity(tableName = "shelf")
data class ShelfEntry(@PrimaryKey val workId: Int, val addedAt: Long)

/**
 * 開いたことのある作品の読書位置（読了率 0〜1）と、最後に開いた日時（エポックミリ秒）。
 * 読書履歴から外しても読書位置は残したいので、行は消さずに inHistory を落とす。
 */
@Entity(tableName = "readings")
data class Reading(@PrimaryKey val workId: Int, val progress: Float, val openedAt: Long, val inHistory: Boolean)

/** お気に入り作家。addedAt は印をつけた日時（エポックミリ秒）。 */
@Entity(tableName = "favorite_persons")
data class FavoritePerson(@PrimaryKey val personId: Int, val addedAt: Long)

/** 本棚の 1 冊と、開いたことがあればその読書位置。 */
data class ShelfRow(val workId: Int, val addedAt: Long, val progress: Float?, val openedAt: Long?)

/** 本棚・読書履歴・読書位置の読み書き。 */
@Dao
abstract class LibraryDao {
    @Query(
        "SELECT s.workId, s.addedAt, r.progress, r.openedAt FROM shelf s LEFT JOIN readings r ON r.workId = s.workId",
    )
    abstract fun shelf(): Flow<List<ShelfRow>>

    /** 読書履歴。最後に開いた順。 */
    @Query("SELECT * FROM readings WHERE inHistory ORDER BY openedAt DESC")
    abstract fun history(): Flow<List<Reading>>

    @Query("SELECT EXISTS(SELECT 1 FROM shelf WHERE workId = :workId)")
    abstract fun onShelf(workId: Int): Flow<Boolean>

    @Query("INSERT OR IGNORE INTO shelf (workId, addedAt) VALUES (:workId, :now)")
    abstract suspend fun addToShelf(workId: Int, now: Long = System.currentTimeMillis())

    /** 本棚から外す。読書位置と保存済みの本文は残す。 */
    @Query("DELETE FROM shelf WHERE workId = :workId")
    abstract suspend fun removeFromShelf(workId: Int)

    /** 残してある読了率。開いたことがなければ null。 */
    @Query("SELECT progress FROM readings WHERE workId = :workId")
    abstract suspend fun progress(workId: Int): Float?

    /** 作品を開いたことを残す。読書履歴の先頭に入り、外していたら戻る。 */
    @Transaction
    open suspend fun open(workId: Int, now: Long = System.currentTimeMillis()) {
        insertReading(workId, now)
        touch(workId, now)
    }

    @Query("UPDATE readings SET progress = :progress WHERE workId = :workId")
    abstract suspend fun saveProgress(workId: Int, progress: Float)

    @Query("UPDATE readings SET inHistory = 0 WHERE workId = :workId")
    abstract suspend fun removeFromHistory(workId: Int)

    /** お気に入り作家の ID。 */
    @Query("SELECT personId FROM favorite_persons")
    abstract fun favoritePersons(): Flow<List<Int>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_persons WHERE personId = :personId)")
    abstract fun isFavorite(personId: Int): Flow<Boolean>

    @Query("INSERT OR IGNORE INTO favorite_persons (personId, addedAt) VALUES (:personId, :now)")
    abstract suspend fun addFavorite(personId: Int, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM favorite_persons WHERE personId = :personId")
    abstract suspend fun removeFavorite(personId: Int)

    @Query(
        "INSERT OR IGNORE INTO readings (workId, progress, openedAt, inHistory) VALUES (:workId, 0, :now, 1)",
    )
    protected abstract suspend fun insertReading(workId: Int, now: Long)

    @Query("UPDATE readings SET openedAt = :now, inHistory = 1 WHERE workId = :workId")
    protected abstract suspend fun touch(workId: Int, now: Long)
}

/**
 * 利用者のデータの DB（`library.db`）。カタログと違って作り直せないので、自動バックアップに含め、
 * 版を上げるときは移行を書く。
 */
@Database(entities = [ShelfEntry::class, Reading::class, FavoritePerson::class], version = 2, exportSchema = false)
abstract class LibraryDatabase : RoomDatabase() {
    /** Room が実装を生成する。 */
    abstract fun libraryDao(): LibraryDao
}

/** 版 2 でお気に入り作家の表を足した。 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS favorite_persons (personId INTEGER NOT NULL, addedAt INTEGER NOT NULL, " +
                "PRIMARY KEY(personId))",
        )
    }
}
