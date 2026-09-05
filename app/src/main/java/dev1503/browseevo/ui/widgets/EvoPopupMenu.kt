package dev1503.browseevo.ui.widgets

import android.view.View
import dev1503.materialpopups.widgets.menuitem.MenuItem
import dev1503.materialpopups.widgets.popup.MenuPopup

class EvoPopupMenu(private val anchor: View) {

    class MenuEntry(val title: CharSequence)

    private val popup = MenuPopup(anchor.context)
    private var clickListener: ((MenuEntry) -> Boolean)? = null

    val menu = MenuBuilder()

    inner class MenuBuilder {
        fun add(title: CharSequence, icResId: Int? = null) {
            val onClick = View.OnClickListener {
                val result = clickListener?.invoke(MenuEntry(title)) ?: true
                popup.setAutoDismiss(result)
            }
            popup.addMenuItem(MenuItem(title, onClick).apply {
                icResId?.let {
                    setIcon(it)
                }
            })
        }
        fun addDivider() {
            popup.addDivider()
        }
    }

    fun setOnMenuItemClickListener(listener: (MenuEntry) -> Boolean) {
        clickListener = listener
    }

    fun show() {
        popup.build()
        popup.showAsDropDown(anchor)
    }
}
