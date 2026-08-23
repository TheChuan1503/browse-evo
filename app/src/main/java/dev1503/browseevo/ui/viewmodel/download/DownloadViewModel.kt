package dev1503.browseevo.ui.viewmodel.download

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import dev1503.browseevo.R
import dev1503.browseevo.ui.viewmodel.ViewModel

abstract class DownloadViewModel(override val activity: AppCompatActivity) : ViewModel(activity) {
    protected abstract val layoutResId: Int

    private lateinit var btnBack: MaterialButton
    private lateinit var listViewModel: DownloadListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        _view = View.inflate(activity, layoutResId, null)
        btnBack = _view.findViewById(R.id.btnBack)
        btnBack.setOnClickListener { activity.finish() }
        listViewModel = DownloadListViewModel(activity)
        listViewModel.onCreate(null)
        _view.findViewById<ViewGroup>(R.id.contentContainer).addView(listViewModel.getView())
    }

    override fun onResume() {
        super.onResume()
        listViewModel.refresh()
    }

    override fun onDestroy() {
        super.onDestroy()
        listViewModel.onDestroy()
    }
}
