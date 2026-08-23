package dev1503.browseevo.ui.viewmodel.browsermain

import dev1503.browseevo.MainActivity
import dev1503.browseevo.R

open class PhoneBrowserMainViewModel(activity: MainActivity): CommonBrowserMainViewModel(activity) {
    override val layoutResId: Int = R.layout.view_model_browser_main_phone
}
