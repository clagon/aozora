package net.clagon.aozora

import android.app.Application
import androidx.room.Room
import kotlinx.coroutines.MainScope
import net.clagon.aozora.catalog.Catalog
import net.clagon.aozora.catalog.CatalogDatabase
import net.clagon.aozora.library.Library
import net.clagon.aozora.library.LibraryDatabase
import net.clagon.aozora.library.MIGRATION_1_2
import net.clagon.aozora.reader.Images
import net.clagon.aozora.reader.Texts
import java.io.File

/**
 * アプリ全体で 1 つだけ持つ依存の置き場。DI ライブラリは使わず、画面からは
 * `LocalContext.current.applicationContext as AozoraApp` で取り出す（ui/App.kt）。
 */
class AozoraApp : Application() {
    /** カタログの取り込みを走らせるスコープ。画面ではなくプロセスに紐づけ、画面を離れても取り込みを続ける。 */
    private val scope = MainScope()

    /** カタログの取り込みと参照の入口。初回アクセス時に catalog.db を開く。 */
    val catalog by lazy {
        val db = Room.databaseBuilder(this, CatalogDatabase::class.java, "catalog.db")
            // 作り直しても、起動時の updateIfEmpty が取り込み直す
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
        Catalog(db.catalogDao(), scope)
    }

    /** 本棚・読書履歴・読書位置。 */
    val library by lazy {
        val db = Room.databaseBuilder(this, LibraryDatabase::class.java, "library.db")
            .addMigrations(MIGRATION_1_2)
            .build()
        Library(db.libraryDao(), catalog.dao)
    }

    /** 作品の本文の置き場。読んだ本文は files/texts に残る。 */
    val texts by lazy { Texts(File(filesDir, "texts")) }

    /** 挿絵と外字の画像の置き場。リーダーで読み込んだ画像は files/images に残る。 */
    val images by lazy { Images(File(filesDir, "images")) }

    override fun onCreate() {
        super.onCreate()
        catalog.updateIfEmpty()
    }
}
