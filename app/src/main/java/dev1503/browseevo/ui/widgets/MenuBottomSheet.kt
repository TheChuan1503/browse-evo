package dev1503.browseevo.ui.widgets

import android.app.Activity
import androidx.viewpager.widget.ViewPager
import com.google.android.material.button.MaterialButton
import dev1503.browseevo.R
import dev1503.browseevo.Utils

class MenuBottomSheet(
    private val activity: Activity,
    private val onAddBookmarkClick: () -> Unit,
    private val onShareClick: (() -> Unit)? = null,
    private val onViewSourceClick: (() -> Unit)? = null,
) {
    fun show() {
        val contentView = activity.layoutInflater.inflate(R.layout.bottom_sheet_menu, null)
        val viewPager = contentView.findViewById<ViewPager>(R.id.viewPager)
        val adapter = MenuPagerAdapter()
        viewPager.adapter = adapter
        val dialog = BottomSheetDialogBuilder(activity)
            .title("Menu")
            .view(contentView)
            .show()
        viewPager.post {
            adapter.page1View?.findViewById<MaterialButton>(R.id.btnTools)?.setOnClickListener {
                viewPager.setCurrentItem(1, true)
            }
            adapter.page1View?.let { Utils.bindMenuPageButtons(it, onAddBookmarkClick, onShareClick) }
            adapter.page2View?.findViewById<MaterialButton>(R.id.btnViewSource)?.setOnClickListener {
                dialog.dismiss()
                onViewSourceClick?.invoke()
            }
        }
    }
}
