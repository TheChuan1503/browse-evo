package dev1503.browseevo

import android.app.Application
import dev1503.browseevo.data.NeoSettings
import java.io.File

class MainApplication : Application() {

    var neoSettings: NeoSettings? = null

    override fun onCreate() {
        super.onCreate()
        neoSettings = NeoSettings(this, File(filesDir, "neo_settings.json").absolutePath)
        Utils.neoSettings = neoSettings
    }
}
