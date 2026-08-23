package dev1503.browseevo.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dev1503.browseevo.EvoDataStore
import dev1503.browseevo.PendingNavigation
import dev1503.browseevo.R
import dev1503.browseevo.Utils


class AboutFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_about, rootKey)
        val dataStore = EvoDataStore(requireActivity())
        preferenceManager.preferenceDataStore = dataStore

        findPreference<Preference>("app")?.summary = "v0.1.0"
        findPreference<Preference>("github")?.onPreferenceClickListener = this
        findPreference<Preference>("oss")?.onPreferenceClickListener = this
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        if (preference.key == "github") {
            PendingNavigation.url = preference.summary.toString()
            activity?.finish()
        } else if (preference.key == "oss") {
            enter(OssFragment())
        }
        return false
    }

    @SuppressLint("CommitTransaction")
    fun enter(fragment: Fragment) {
        val fragmentManager: FragmentManager = activity?.supportFragmentManager!!
        val transaction: FragmentTransaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}