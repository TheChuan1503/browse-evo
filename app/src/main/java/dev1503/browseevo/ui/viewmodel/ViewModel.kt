package dev1503.browseevo.ui.viewmodel

import android.app.Activity
import android.os.Bundle
import android.view.View

open class ViewModel(open val activity: Activity) {
    open lateinit var _view: View

    open fun onCreate(savedInstanceState: Bundle?) {

    }
    open fun onDestroy() {

    }
    open fun onResume() {

    }
    open fun onHostStopped() {

    }
    open fun handleBackPressed(): Boolean {
        return false
    }
    open fun getView(): View {
        return _view
    }
}