package com.cabel.rutacabel

import android.app.Application
import com.cabel.rutacabel.data.local.AppDatabase

class RutaCABELApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
    }
}
