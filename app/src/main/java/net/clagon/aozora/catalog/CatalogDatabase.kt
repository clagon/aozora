package net.clagon.aozora.catalog

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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
    /** 並べ替えと五十音での絞り込みに使う読み。濁点・小書きを除いた平仮名（例: 「ぎ」→「き」）。 */
    val sortYomi: String,
    val subtitle: String,
    val subtitleYomi: String,
    val originalTitle: String,
    val firstAppearance: String,
    /** 例: `NDC 913`、`NDC K913`、`NDC 756 914`（複数の分類を持つこともある）。空のこともある */
    val ndc: String,
    /** 文字遣い種別（例: 新字新仮名、旧字旧仮名）。 */
    val kanaType: String,
    /** 作品に著作権が残っているか（CSV の作品著作権フラグが「あり」）。 */
    val copyrighted: Boolean,
    /** 青空文庫での公開日。`yyyy-MM-dd` の文字列なので、文字列の比較で新しい順に並べられる。 */
    val publishedOn: String,
    val updatedOn: String,
    /** 図書カード（作品ごとの紹介ページ）の URL。 */
    val cardUrl: String,
    /** ルビつきテキストの zip の URL。 */
    val textUrl: String,
    /** XHTML 版がない作品は空 */
    val xhtmlUrl: String,
    val inputter: String,
    val proofreader: String,
    /** 底本。複数の底本から入力した作品だけ 2 冊目が入る。底本名が空なら null。 */
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
    /** 並べ替えと五十音での絞り込みに使う姓の読み。[Work.sortYomi] と同じく濁点・小書きを除いた平仮名。 */
    val lastNameSort: String,
    val firstNameSort: String,
    val lastNameRoman: String,
    val firstNameRoman: String,
    /** 生年月日・没年月日。CSV の値をそのまま持つ。`yyyy-MM-dd` が多いが、年だけ・年月だけのこともあり、不明や存命なら空。 */
    val born: String,
    val died: String,
    val copyrighted: Boolean,
)

/** 作品と作家の組。role は CSV の役割フラグ（著者・翻訳者・校訂者・編者・その他）そのまま。 */
@Entity(tableName = "credits", primaryKeys = ["workId", "personId", "role"], indices = [Index("personId")])
data class Credit(val workId: Int, val personId: Int, val role: String)

/**
 * 図書カードから取り出した「作品について」。CSV にないので、作品詳細を開いたときに取りに行って残す。
 * カタログを取り直しても消さない。図書カードに書かれていない作品は空文字で残し、取りに行き直さない。
 */
@Entity(tableName = "work_notes")
data class WorkNote(@PrimaryKey val workId: Int, val about: String)

/** 作家ページから取り出した「人物について」。[WorkNote] と同じ扱い。 */
@Entity(tableName = "person_notes")
data class PersonNote(@PrimaryKey val personId: Int, val about: String)

/** 作品詳細に並べる作家の 1 人。role は [Credit.role] と同じ。 */
data class CreditItem(val personId: Int, val lastName: String, val firstName: String, val role: String) {
    /** 表示用の名前。[PersonItem.name] と同じ形。 */
    val name get() = "$lastName $firstName".trim()
}

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
data class PersonItem(
    val id: Int,
    val lastName: String,
    val firstName: String,
    val lastNameSort: String,
    val workCount: Int,
) {
    /** 表示用の名前。名が空の作家（一語の筆名や団体など）は姓の欄の名前だけになる。 */
    val name get() = "$lastName $firstName".trim()
}

// 読みの範囲の条件。:from が空なら平仮名で始まらない読み（YomiRange を参照）
private const val WORK_IN_YOMI = "((:from != '' AND w.sortYomi >= :from AND w.sortYomi < :to) OR " +
    "(:from = '' AND (w.sortYomi < 'ぁ' OR w.sortYomi >= 'ゟ')))"
private const val PERSON_IN_YOMI = "((:from != '' AND p.lastNameSort >= :from AND p.lastNameSort < :to) OR " +
    "(:from = '' AND (p.lastNameSort < 'ぁ' OR p.lastNameSort >= 'ゟ')))"

// 分野の条件。児童書だけのときは K つきの分類だけを見る
private const val IN_NDC = "((:kidsOnly = 0 AND ' ' || w.ndc LIKE '% ' || :code || '%') OR " +
    "' ' || w.ndc LIKE '% K' || :code || '%')"

/** [WorkItem] を返すクエリの共通部分。後ろに WHERE や ORDER BY を足して使う。 */
private const val WORK_ITEM = """
    SELECT w.id, w.title, w.subtitle, w.firstAppearance, w.ndc, w.kanaType,
        (SELECT GROUP_CONCAT(TRIM(p.lastName || ' ' || p.firstName), '、')
         FROM credits c JOIN persons p ON p.id = c.personId
         WHERE c.workId = w.id AND c.role = '著者') AS authors
    FROM works w"""

/**
 * カタログの読み書き。画面は ViewModel を挟まずにこの DAO の Flow を直接購読する。
 * Flow はテーブルが書き換わると流れ直すので、カタログを取り直すと開いている一覧も更新される。
 */
@Dao
abstract class CatalogDao {
    /** 作品数。カタログが空かどうか（初回取り込みが要るか）の判定と、件数の表示に使う。 */
    @Query("SELECT COUNT(*) FROM works")
    abstract fun workCount(): Flow<Int>

    /** 新着作品。ホームは数件だけ、新着の一覧は全件（[Int.MAX_VALUE]）を取る。 */
    @Query("$WORK_ITEM ORDER BY w.publishedOn DESC, w.id DESC LIMIT :limit")
    abstract fun newWorks(limit: Int): Flow<List<WorkItem>>

    /**
     * code は NDC の上 1〜3 桁（例: "9"、"91"）。ndc は "NDC 913" や "NDC 756 914" の形。
     * 分野別では児童書（K つき）も含め、児童書では K つきだけを見る。
     */
    @Query("$WORK_ITEM WHERE $IN_NDC ORDER BY w.sortYomi")
    abstract fun worksByNdc(code: String, kidsOnly: Boolean): Flow<List<WorkItem>>

    /** 全作品の分類番号。分野ごとの件数は [countNdc] でまとめて数える。 */
    @Query("SELECT ndc FROM works")
    abstract fun allNdc(): Flow<List<String>>

    /** 作品別で選んだ文字の範囲（[kanaRange]）に読みが入る作品。from が空なら平仮名で始まらない作品。 */
    @Query("$WORK_ITEM WHERE $WORK_IN_YOMI ORDER BY w.sortYomi")
    abstract fun worksByYomi(from: String, to: String): Flow<List<WorkItem>>

    /** 作品詳細に出す作品の全項目。 */
    @Query("SELECT * FROM works WHERE id = :id")
    abstract fun work(id: Int): Flow<Work?>

    /** 作品詳細の表紙と見出しに使う、一覧と同じ形の 1 件。 */
    @Query("$WORK_ITEM WHERE w.id = :id")
    abstract fun workItem(id: Int): Flow<WorkItem?>

    /** 作品に関わった作家。CSV の行の順（著者が先）に並べる。 */
    @Query(
        "SELECT p.id AS personId, p.lastName, p.firstName, c.role FROM credits c JOIN persons p ON p.id = c.personId WHERE c.workId = :workId ORDER BY c.rowid",
    )
    abstract fun creditsOf(workId: Int): Flow<List<CreditItem>>

    /** 残してある「作品について」。まだ取りに行っていなければ null。 */
    @Query("SELECT about FROM work_notes WHERE workId = :workId")
    abstract suspend fun about(workId: Int): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun saveNote(note: WorkNote)

    /** 作家詳細に出す作家の全項目。 */
    @Query("SELECT * FROM persons WHERE id = :id")
    abstract fun person(id: Int): Flow<Person?>

    /** 残してある「人物について」。まだ取りに行っていなければ null。 */
    @Query("SELECT about FROM person_notes WHERE personId = :personId")
    abstract suspend fun personAbout(personId: Int): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun savePersonNote(note: PersonNote)

    /** 作家が役割を問わず関わった作品。 */
    @Query("$WORK_ITEM WHERE w.id IN (SELECT workId FROM credits WHERE personId = :personId) ORDER BY w.sortYomi")
    abstract fun worksByPerson(personId: Int): Flow<List<WorkItem>>

    /** 作家別で選んだ行（[KanaRow]）に姓の読みが入る作家と、それぞれの作品数。 */
    @Query(
        """
        SELECT p.id, p.lastName, p.firstName, p.lastNameSort,
            (SELECT COUNT(DISTINCT c.workId) FROM credits c WHERE c.personId = p.id) AS workCount
        FROM persons p WHERE $PERSON_IN_YOMI ORDER BY p.lastNameSort, p.firstNameSort""",
    )
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

    @Query("DELETE FROM works")
    protected abstract suspend fun deleteWorks()

    @Query("DELETE FROM persons")
    protected abstract suspend fun deletePersons()

    @Query("DELETE FROM credits")
    protected abstract suspend fun deleteCredits()

    @Insert protected abstract suspend fun insertWorks(works: List<Work>)

    @Insert protected abstract suspend fun insertPersons(persons: List<Person>)

    @Insert protected abstract suspend fun insertCredits(credits: List<Credit>)
}

/**
 * カタログ専用の DB（`catalog.db`）。いつでも公式 CSV から作り直せるので、
 * 自動バックアップの対象から外している（res/xml/data_extraction_rules.xml）。
 * 本棚などの利用者データは別の DB に置く。版を上げるときは移行を書かずに作り直す（AozoraApp）。
 */
@Database(
    entities = [Work::class, Person::class, Credit::class, WorkNote::class, PersonNote::class],
    version = 3,
    exportSchema = false,
)
abstract class CatalogDatabase : RoomDatabase() {
    /** Room が実装を生成する。 */
    abstract fun catalogDao(): CatalogDao
}
