package dev1503.browseevo

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import android.preference.PreferenceManager
import dev1503.browseevo.ui.viewmodel.DeviceTypeSelectionViewModel
import dev1503.browseevo.ui.viewmodel.browsermain.PhoneBrowserMainViewModel
import dev1503.browseevo.ui.viewmodel.ViewModel
import dev1503.browseevo.ui.viewmodel.browsermain.WatchSquareBrowserMainViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: ViewModel

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Utils.applyNightModeOverride(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val deviceType = PreferenceManager.getDefaultSharedPreferences(this)
            .getString(DeviceTypeSelectionViewModel.PREF_KEY_DEVICE_TYPE, null)
        viewModel = when {
            deviceType.isNullOrEmpty() -> DeviceTypeSelectionViewModel(this).apply {
                onDeviceTypeSelected = { recreate() }
            }
            deviceType == DeviceTypeSelectionViewModel.DEVICE_TYPE_WATCH_SQUARE ->
                WatchSquareBrowserMainViewModel(this)
            else -> PhoneBrowserMainViewModel(this)
        }
        viewModel.onCreate(savedInstanceState)
        setContentView(viewModel.getView())
        ViewCompat.setOnApplyWindowInsetsListener(viewModel.getView()) { v, insets ->
            val systemBars = insets.getInsets(systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!viewModel.handleBackPressed()) {
                    finish()
                }
            }
        })
        requestNotificationPermission()
        handleViewIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleViewIntent(intent)
        if (::viewModel.isInitialized) viewModel.onResume()
    }

    private fun handleViewIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val url = intent.data?.toString() ?: return
        PendingNavigation.url = url
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            androidx.core.app.ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1002
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) viewModel.onResume()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN &&
            event.isAltPressed &&
            event.keyCode == KeyEvent.KEYCODE_R
        ) {
            PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .remove(DeviceTypeSelectionViewModel.PREF_KEY_DEVICE_TYPE)
                .apply()
            recreate()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        getDelegate().applyDayNight()
        enableEdgeToEdge()
    }
}
