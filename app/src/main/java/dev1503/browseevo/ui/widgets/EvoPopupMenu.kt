package dev1503.browseevo.ui.widgets

import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.PopupMenu
import dev1503.browseevo.R

open class EvoPopupMenu(
    anchor: View,
    gravity: Int = Gravity.NO_GRAVITY,
) : PopupMenu(
    ContextThemeWrapper(anchor.context, R.style.EvoPopupMenuOverlay),
    anchor,
    gravity
)
