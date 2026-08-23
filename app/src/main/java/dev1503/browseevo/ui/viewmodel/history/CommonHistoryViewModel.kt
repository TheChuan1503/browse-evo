package dev1503.browseevo.ui.viewmodel.history

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import dev1503.browseevo.HistoryActivity
import dev1503.browseevo.R

abstract class CommonHistoryViewModel(activity: AppCompatActivity) : HistoryViewModel(activity) {
    override val layoutResId: Int = R.layout.view_model_history_phone

    private val bookmarkListViewModel = BookmarkListViewModel(activity)
    private val historyListViewModel = HistoryListViewModel(activity)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        historyListViewModel.onCreate(null)
        bookmarkListViewModel.onCreate(null)
        pager.adapter = object : PagerAdapter() {
            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                val pageViewModel = if (position == 0) bookmarkListViewModel else historyListViewModel
                val view = pageViewModel.getView()
                (container as ViewPager).addView(view)
                return view
            }

            override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
                container.removeView(`object` as View)
            }

            override fun getCount(): Int = 2

            override fun isViewFromObject(view: View, `object`: Any): Boolean = view === `object`

            override fun getPageTitle(position: Int): CharSequence? =
                activity.getString(if (position == 0) R.string.tab_bookmark else R.string.tab_history)
        }
        tabLayout.setupWithViewPager(pager)
        pager.currentItem = when (activity.intent?.getStringExtra(HistoryActivity.EXTRA_SELECTED_TAB)) {
            HistoryActivity.TAB_BOOKMARK -> 0
            else -> 1
        }
    }
}
