package dev1503.browseevo

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.preference.PreferenceManager
import dev1503.browseevo.ui.viewmodel.DeviceTypeSelectionViewModel
import dev1503.browseevo.ui.viewmodel.history.PhoneHistoryViewModel
import dev1503.browseevo.ui.viewmodel.history.WatchSquareHistoryViewModel
import dev1503.browseevo.ui.viewmodel.ViewModel

class HistoryActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_SELECTED_TAB = "selected_tab"
        const val TAB_BOOKMARK = "bookmark"
        const val TAB_HISTORY = "history"
    }

    private lateinit var viewModel: ViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val deviceType = PreferenceManager.getDefaultSharedPreferences(this)
            .getString(DeviceTypeSelectionViewModel.PREF_KEY_DEVICE_TYPE, null)
        viewModel = if (deviceType == DeviceTypeSelectionViewModel.DEVICE_TYPE_WATCH_SQUARE) {
            WatchSquareHistoryViewModel(this)
        } else {
            PhoneHistoryViewModel(this)
        }
        viewModel.onCreate(savedInstanceState)
        setContentView(viewModel.getView())
        ViewCompat.setOnApplyWindowInsetsListener(viewModel.getView()) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        getDelegate().applyDayNight()
        enableEdgeToEdge()
    }
}
