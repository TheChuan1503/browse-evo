package dev1503.browseevo.ui.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import dev1503.browseevo.EvoDataStore
import dev1503.browseevo.R


class AppearanceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_appearance, rootKey)
        val dataStore = EvoDataStore(requireActivity())
        preferenceManager.preferenceDataStore = dataStore
    }
}