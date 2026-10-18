package net.clagon.aozora.catalog

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** 作品。カタログ CSV の作品側の列と、作品詳細に出す底本・工作員の情報を持つ。 */
@Entity(tableName = "works")
data class Work(
    @PrimaryKey val id: Int,
    val title: String,
    val titleYomi: String,
    val sortYomi: String,
    val subtitle: String,
    val subtitleYomi: String,
    val originalTitle: String,
    val firstAppearance: String,
    /** 例: `NDC 913`、`NDC K913`、`NDC 756 914`（複数の分類を持つこともある）。空のこともある */
    val ndc: String,
    val kanaType: String,
    val copyrighted: Boolean,
    val publishedOn: String,
    val updatedOn: String,
    val cardUrl: String,
    val textUrl: String,
    /** XHTML 版がない作品は空 */
    val xhtmlUrl: String,
    val inputter: String,
    val proofreader: String,
    @Embedded(prefix = "source1_") val source1: SourceBook?,
    @Embedded(prefix = "source2_") val source2: SourceBook?,
)

/** 底本。 */
data class SourceBook(
    val name: String,
    val publisher: String,
    val firstEdition: String,
    val inputEdition: String,
    val proofEdition: String,
    val parentName: String,
    val parentPublisher: String,
    val parentFirstEdition: String,
)

/** 作家。 */
@Entity(tableName = "persons")
data class Person(
    @PrimaryKey val id: Int,
    val lastName: String,
    val firstName: String,
    val lastNameYomi: String,
    val firstNameYomi: String,
    val lastNameSort: String,
    val firstNameSort: String,
    val lastNameRoman: String,
    val firstNameRoman: String,
    val born: String,
    val died: String,
    val copyrighted: Boolean,
)

/** 作品と作家の組。role は CSV の役割フラグ（著者・翻訳者・校訂者・編者・その他）そのまま。 */
@Entity(tableName = "credits", primaryKeys = ["workId", "personId", "role"], indices = [Index("personId")])
data class Credit(val workId: Int, val personId: Int, val role: String)

/** 一覧の 1 行。authors は役割が著者の作家を「、」でつないだもの。 */
data class WorkItem(val id: Int, val title: String, val subtitle: String, val authors: String?)

private const val WORK_ITEM = """
    SELECT w.id, w.title, w.subtitle,
        (SELECT GROUP_CONCAT(TRIM(p.lastName || ' ' || p.firstName), '、')
         FROM credits c JOIN persons p ON p.id = c.personId
         WHERE c.workId = w.id AND c.role = '著者') AS authors
    FROM works w"""

@Dao
abstract class CatalogDao {
    @Query("SELECT COUNT(*) FROM works")
    abstract fun workCount(): Flow<Int>

    @Query("$WORK_ITEM ORDER BY w.publishedOn DESC, w.id DESC LIMIT :limit")
    abstract fun newWorks(limit: Int): Flow<List<WorkItem>>

    /** code は NDC の上 2 桁（例: "91"）。児童書（K つき）も含める。ndc は "NDC 913" や "NDC 756 914" の形。 */
    @Query("$WORK_ITEM WHERE ' ' || w.ndc LIKE '% ' || :code || '%' OR ' ' || w.ndc LIKE '% K' || :code || '%' ORDER BY w.sortYomi")
    abstract fun worksByNdc(code: String): Flow<List<WorkItem>>

    @Query("$WORK_ITEM WHERE w.sortYomi >= :from AND w.sortYomi < :to AND (:kidsOnly = 0 OR w.ndc LIKE '%K%') ORDER BY w.sortYomi")
    abstract fun worksByYomi(from: String, to: String, kidsOnly: Boolean): Flow<List<WorkItem>>

    @Query("$WORK_ITEM WHERE w.id IN (SELECT workId FROM credits WHERE personId = :personId) ORDER BY w.sortYomi")
    abstract fun worksByPerson(personId: Int): Flow<List<WorkItem>>

    @Query("SELECT * FROM persons WHERE lastNameSort >= :from AND lastNameSort < :to ORDER BY lastNameSort, firstNameSort")
    abstract fun personsByYomi(from: String, to: String): Flow<List<Person>>

    /** カタログは丸ごと取り直すので、差分更新はせず全件を入れ替える。 */
    @Transaction
    open suspend fun replaceAll(catalog: ParsedCatalog) {
        deleteCredits()
        deleteWorks()
        deletePersons()
        insertWorks(catalog.works)
        insertPersons(catalog.persons)
        insertCredits(catalog.credits)
    }

    @Query("DELETE FROM works") protected abstract suspend fun deleteWorks()
    @Query("DELETE FROM persons") protected abstract suspend fun deletePersons()
    @Query("DELETE FROM credits") protected abstract suspend fun deleteCredits()
    @Insert protected abstract suspend fun insertWorks(works: List<Work>)
    @Insert protected abstract suspend fun insertPersons(persons: List<Person>)
    @Insert protected abstract suspend fun insertCredits(credits: List<Credit>)
}

/**
 * カタログ専用の DB（`catalog.db`）。いつでも公式 CSV から作り直せるので、
 * 自動バックアップの対象から外している（res/xml/data_extraction_rules.xml）。
 * 本棚などの利用者データは別の DB に置く。
 */
@Database(entities = [Work::class, Person::class, Credit::class], version = 1, exportSchema = false)
abstract class CatalogDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao
}
