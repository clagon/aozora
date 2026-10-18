package net.clagon.aozora

import android.app.Application
import androidx.room.Room
import kotlinx.coroutines.MainScope
import net.clagon.aozora.catalog.Catalog
import net.clagon.aozora.catalog.CatalogDatabase

/**
 * アプリ全体で 1 つだけ持つ依存の置き場。DI ライブラリは使わず、画面からは
 * `LocalContext.current.applicationContext as AozoraApp` で取り出す（ui/App.kt）。
 */
class AozoraApp : Application() {
    /** カタログの取り込みを走らせるスコープ。画面ではなくプロセスに紐づけ、画面を離れても取り込みを続ける。 */
    private val scope = MainScope()

    /** カタログの取り込みと参照の入口。初回アクセス時に catalog.db を開く。 */
    val catalog by lazy {
        val db = Room.databaseBuilder(this, CatalogDatabase::class.java, "catalog.db").build()
        Catalog(db.catalogDao(), scope)
    }

    override fun onCreate() {
        super.onCreate()
        catalog.updateIfEmpty()
    }
}
