package dev1503.browseevo

import android.app.Activity
import androidx.core.content.edit
import androidx.preference.PreferenceDataStore
import com.google.android.material.snackbar.Snackbar
import dev1503.browseevo.ui.viewmodel.DeviceTypeSelectionViewModel.Companion.PREF_KEY_DEVICE_TYPE

class EvoDataStore(val context: Activity): PreferenceDataStore() {
    override fun putString(key: String?, value: String?) {
        if (key == "device_type") {
            android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .edit {
                    putString(PREF_KEY_DEVICE_TYPE, value)
                }
            requestRestart()
            return
        }
        Utils.neoSettings?.putString(key!!, value)
    }

    override fun getString(key: String?, defValue: String?): String? {
        if (key == "device_type") {
            return android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_KEY_DEVICE_TYPE, defValue)
        }
        return Utils.neoSettings?.getString(key!!, defValue)
    }

    fun requestRestart() {
        val snackbar = Snackbar.make(context.window.decorView, "重启以应用更改", Snackbar.LENGTH_SHORT)
//        snackbar.setAction("重启") {
//
//        }
        snackbar.show()
    }
}