package net.clagon.aozora

import android.app.Application
import androidx.room.Room
import kotlinx.coroutines.MainScope
import net.clagon.aozora.catalog.Catalog
import net.clagon.aozora.catalog.CatalogDatabase

class AozoraApp : Application() {
    private val scope = MainScope()

    val catalog by lazy {
        val db = Room.databaseBuilder(this, CatalogDatabase::class.java, "catalog.db").build()
        Catalog(db.catalogDao(), scope)
    }

    override fun onCreate() {
        super.onCreate()
        catalog.updateIfEmpty()
    }
}
