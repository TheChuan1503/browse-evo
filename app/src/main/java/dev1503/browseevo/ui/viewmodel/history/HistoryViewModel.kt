package dev1503.browseevo.ui.viewmodel.history

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import dev1503.browseevo.R
import dev1503.browseevo.ui.viewmodel.ViewModel

open class HistoryViewModel(override val activity: AppCompatActivity) : ViewModel(activity) {
    protected open val layoutResId: Int = R.layout.view_model_history_phone

    protected lateinit var btnBack: MaterialButton
    protected lateinit var tabLayout: TabLayout
    protected lateinit var pager: ViewPager

    override fun onCreate(savedInstanceState: Bundle?) {
        _view = View.inflate(activity, layoutResId, null)
        btnBack = _view.findViewById(R.id.btnBack)
        tabLayout = _view.findViewById(R.id.tabLayout)
        pager = _view.findViewById(R.id.pager)
        btnBack.setOnClickListener { activity.finish() }
    }
}
