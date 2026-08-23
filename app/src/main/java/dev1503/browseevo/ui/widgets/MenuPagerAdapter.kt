package dev1503.browseevo.ui.widgets

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import dev1503.browseevo.R

open class MenuPagerAdapter(
    private val firstPageLayout: Int = R.layout.layout_bottom_sheet_menu_1,
    private val secondPageLayout: Int = R.layout.layout_bottom_sheet_menu_2,
) : PagerAdapter() {
    var page1View: View? = null
        private set
    var page2View: View? = null
        private set

    override fun getCount(): Int = 2

    override fun isViewFromObject(view: View, `object`: Any): Boolean = view === `object`

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val layoutId = if (position == 0) firstPageLayout else secondPageLayout
        val page = LayoutInflater.from(container.context).inflate(layoutId, container, false)
        if (position == 0) {
            page1View = page
        } else {
            page2View = page
        }
        container.addView(page)
        return page
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }
}
