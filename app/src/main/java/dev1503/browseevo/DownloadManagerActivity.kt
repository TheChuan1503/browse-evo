package dev1503.browseevo

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.preference.PreferenceManager
import dev1503.browseevo.ui.viewmodel.DeviceTypeSelectionViewModel
import dev1503.browseevo.ui.viewmodel.ViewModel
import dev1503.browseevo.ui.viewmodel.download.PhoneDownloadManagerViewModel
import dev1503.browseevo.ui.viewmodel.download.WatchSquareDownloadManagerViewModel

class DownloadManagerActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_OPEN_TIMESTAMP = "open_timestamp"
    }

    private lateinit var viewModel: ViewModel

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Utils.applyNightModeOverride(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val deviceType = PreferenceManager.getDefaultSharedPreferences(this)
            .getString(DeviceTypeSelectionViewModel.PREF_KEY_DEVICE_TYPE, null)
        viewModel = if (deviceType == DeviceTypeSelectionViewModel.DEVICE_TYPE_WATCH_SQUARE) {
            WatchSquareDownloadManagerViewModel(this)
        } else {
            PhoneDownloadManagerViewModel(this)
        }
        viewModel.onCreate(savedInstanceState)
        setContentView(viewModel.getView())
        ViewCompat.setOnApplyWindowInsetsListener(viewModel.getView()) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        getDelegate().applyDayNight()
        enableEdgeToEdge()
    }
}
