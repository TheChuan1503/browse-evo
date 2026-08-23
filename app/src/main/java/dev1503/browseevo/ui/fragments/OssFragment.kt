package dev1503.browseevo.ui.fragments

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dev1503.browseevo.EvoDataStore
import dev1503.browseevo.PendingNavigation
import dev1503.browseevo.R
import dev1503.browseevo.Utils


class OssFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_oss, rootKey)
    }
}