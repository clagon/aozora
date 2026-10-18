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
data class WorkItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val firstAppearance: String,
    val ndc: String,
    val kanaType: String,
    val authors: String?,
)

/** 作家一覧の 1 行。workCount は役割を問わず関わった公開作品の数。lastNameSort は見出しの頭文字に使う。 */
data class PersonItem(val id: Int, val lastName: String, val firstName: String, val lastNameSort: String, val workCount: Int) {
    val name get() = "$lastName $firstName".trim()
}

// 読みの範囲の条件。:from が空なら平仮名で始まらない読み（YomiRange を参照）
private const val WORK_IN_YOMI = "((:from != '' AND w.sortYomi >= :from AND w.sortYomi < :to) OR (:from = '' AND (w.sortYomi < 'ぁ' OR w.sortYomi >= 'ゟ')))"
private const val PERSON_IN_YOMI = "((:from != '' AND p.lastNameSort >= :from AND p.lastNameSort < :to) OR (:from = '' AND (p.lastNameSort < 'ぁ' OR p.lastNameSort >= 'ゟ')))"

// 分野の条件。児童書だけのときは K つきの分類だけを見る
private const val IN_NDC = "((:kidsOnly = 0 AND ' ' || w.ndc LIKE '% ' || :code || '%') OR ' ' || w.ndc LIKE '% K' || :code || '%')"

private const val WORK_ITEM = """
    SELECT w.id, w.title, w.subtitle, w.firstAppearance, w.ndc, w.kanaType,
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

    /**
     * code は NDC の上 1〜3 桁（例: "9"、"91"）。ndc は "NDC 913" や "NDC 756 914" の形。
     * 分野別では児童書（K つき）も含め、児童書では K つきだけを見る。
     */
    @Query("$WORK_ITEM WHERE $IN_NDC ORDER BY w.sortYomi")
    abstract fun worksByNdc(code: String, kidsOnly: Boolean): Flow<List<WorkItem>>

    /** worksByNdc と同じ条件で件数を数える。 */
    @Query("SELECT COUNT(*) FROM works w WHERE $IN_NDC")
    abstract fun ndcCount(code: String, kidsOnly: Boolean): Flow<Int>

    @Query("$WORK_ITEM WHERE $WORK_IN_YOMI ORDER BY w.sortYomi")
    abstract fun worksByYomi(from: String, to: String): Flow<List<WorkItem>>

    @Query("$WORK_ITEM WHERE w.id IN (SELECT workId FROM credits WHERE personId = :personId) ORDER BY w.sortYomi")
    abstract fun worksByPerson(personId: Int): Flow<List<WorkItem>>

    @Query("""
        SELECT p.id, p.lastName, p.firstName, p.lastNameSort,
            (SELECT COUNT(DISTINCT c.workId) FROM credits c WHERE c.personId = p.id) AS workCount
        FROM persons p WHERE $PERSON_IN_YOMI ORDER BY p.lastNameSort, p.firstNameSort""")
    abstract fun personsByYomi(from: String, to: String): Flow<List<PersonItem>>

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
