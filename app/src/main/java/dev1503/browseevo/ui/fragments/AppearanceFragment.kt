package dev1503.browseevo.ui.fragments

import android.os.Bundle
import androidx.preference.DropDownPreference
import androidx.preference.PreferenceFragmentCompat
import dev1503.browseevo.EvoDataStore
import dev1503.browseevo.R
import dev1503.browseevo.Utils


class AppearanceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val dataStore = EvoDataStore(requireActivity())
        preferenceManager.preferenceDataStore = dataStore
        setPreferencesFromResource(R.xml.preferences_appearance, rootKey)

        findPreference<DropDownPreference>(Utils.KEY_DARK_MODE)?.let { pref ->
            val v = (Utils.neoSettings?.getInt(Utils.KEY_DARK_MODE, 0) ?: 0).toString()
            pref.setValue(v)
            pref.setOnPreferenceChangeListener { _, newValue ->
                val target = (newValue as? String)?.toIntOrNull() ?: return@setOnPreferenceChangeListener false
                Utils.neoSettings?.putInt(Utils.KEY_DARK_MODE, target)
                Utils.applySavedNightMode()
                true
            }
        }
    }
}