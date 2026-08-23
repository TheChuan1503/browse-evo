package dev1503.browseevo

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import dev1503.browseevo.ui.fragments.SettingsFragment
import dev1503.browseevo.ui.viewmodel.DeviceTypeSelectionViewModel
import dev1503.browseevo.ui.viewmodel.ViewModel
import dev1503.browseevo.ui.viewmodel.history.PhoneHistoryViewModel
import dev1503.browseevo.ui.viewmodel.history.WatchSquareHistoryViewModel
import dev1503.browseevo.ui.viewmodel.settings.PhoneSettingsViewModel
import dev1503.browseevo.ui.viewmodel.settings.WatchSquareSettingsViewModel


class SettingsActivity : AppCompatActivity() {
    private lateinit var viewModel: ViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val deviceType = PreferenceManager.getDefaultSharedPreferences(this)
            .getString(DeviceTypeSelectionViewModel.PREF_KEY_DEVICE_TYPE, null)
        viewModel = if (deviceType == DeviceTypeSelectionViewModel.DEVICE_TYPE_WATCH_SQUARE) {
            WatchSquareSettingsViewModel(this)
        } else {
            PhoneSettingsViewModel(this)
        }
        viewModel.onCreate(savedInstanceState)
        setContentView(viewModel.getView())
        ViewCompat.setOnApplyWindowInsetsListener(viewModel.getView()) { v, insets ->
            val systemBars = insets.getInsets(systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, SettingsFragment())
            .commit()
    }
}