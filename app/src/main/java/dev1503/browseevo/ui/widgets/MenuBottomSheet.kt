package dev1503.browseevo.ui.widgets

import android.app.Activity
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import dev1503.browseevo.R
import dev1503.browseevo.Utils

class MenuBottomSheet(
    private val activity: Activity,
    private val onAddBookmarkClick: () -> Unit,
    private val onShareClick: (() -> Unit)? = null,
    private val onViewSourceClick: (() -> Unit)? = null,
    private val onDarkModeToggled: (() -> Unit)? = null,
    private val onPcModeToggled: (() -> Unit)? = null,
) {
    private var dialog: BottomSheetDialog? = null

    fun show() {
        val contentView = activity.layoutInflater.inflate(R.layout.bottom_sheet_menu, null)
        val viewPager = contentView.findViewById<ViewPager>(R.id.viewPager)
        dialog = BottomSheetDialogBuilder(activity)
            .title("Menu")
            .view(contentView)
            .show()
        setupPages(viewPager)
    }

    /** 实例化并绑定两页菜单。 */
    private fun setupPages(viewPager: ViewPager) {
        val adapter = MenuPagerAdapter()
        viewPager.adapter = adapter
        viewPager.post {
            adapter.page1View?.findViewById<MaterialButton>(R.id.btnTools)?.setOnClickListener {
                viewPager.setCurrentItem(1, true)
            }
            adapter.page1View?.let {
                Utils.bindMenuPageButtons(
                    it,
                    onAddBookmarkClick,
                    onShareClick,
                    onDarkModeToggleClick = {
                        Utils.cycleDarkMode()
                        onDarkModeToggled?.invoke()
                        dialog?.dismiss()
                    }
                )
            }
            adapter.page2View?.findViewById<MaterialButton>(R.id.btnViewSource)?.setOnClickListener {
                dialog?.dismiss()
                onViewSourceClick?.invoke()
            }
            adapter.page2View?.findViewById<MaterialButton>(R.id.btnPcMode)?.let { button ->
                button.isChecked = Utils.isPcMode()
                button.setOnClickListener {
                    Utils.setPcMode(!Utils.isPcMode())
                    button.isChecked = Utils.isPcMode()
                    onPcModeToggled?.invoke()
                    dialog?.dismiss()
                }
            }
        }
    }
}
