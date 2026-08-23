package dev1503.browseevo.ui.viewmodel.settings

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import dev1503.browseevo.R
import dev1503.browseevo.ui.viewmodel.ViewModel

abstract class SettingsViewModel(override val activity: AppCompatActivity) : ViewModel(activity) {
    protected abstract val layoutResId: Int

    private lateinit var btnBack: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        _view = View.inflate(activity, layoutResId, null)
        btnBack = _view.findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            val fragmentManager = activity.supportFragmentManager
            if (fragmentManager.backStackEntryCount > 0) {
                fragmentManager.popBackStack()
            } else {
                activity.finish()
            }
        }
    }
}
