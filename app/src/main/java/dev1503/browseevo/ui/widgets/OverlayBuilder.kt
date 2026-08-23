package dev1503.browseevo.ui.widgets

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import dev1503.browseevo.R

class OverlayBuilder(private val activity: Activity) {
    private var titleText: String = ""
    private var contentView: View? = null
    private var fillContent = false

    fun title(title: String): OverlayBuilder {
        titleText = title
        return this
    }

    fun view(view: View): OverlayBuilder {
        contentView = view
        return this
    }

    fun fillContent(value: Boolean): OverlayBuilder {
        fillContent = value
        return this
    }

    fun show(onDismiss: () -> Unit): View {
        val density = activity.resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).toInt()

        val root = FrameLayout(activity).apply {
            setBackgroundColor(SCRIM_COLOR)
            isClickable = true
            isFocusable = true
            setOnClickListener { onDismiss() }
        }

        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(resolveBackgroundColor())
            isClickable = true
            isFocusable = true
        }

        card.addView(buildHeader(onDismiss))

        contentView?.let { content ->
            val lp = if (fillContent) {
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            } else {
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            card.addView(content, lp)
            content.post { content.requestLayout() }
        }

        root.addView(card, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            if (fillContent) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER_VERTICAL
        ).apply { setMargins(0, dp(16), 0, dp(16)) })

        return root
    }

    private fun buildHeader(onDismiss: () -> Unit): View {
        val header = LayoutInflater.from(activity).inflate(R.layout.overlay_header, null)
        header.findViewById<TextView>(R.id.textOverlayTitle).text = titleText
        header.findViewById<TextView>(R.id.textOverlayGhostTitle).text = titleText
        header.findViewById<View>(R.id.btnCloseOverlay).setOnClickListener { onDismiss() }
        return header
    }

    private fun resolveBackgroundColor(): Int {
        val typedValue = android.util.TypedValue()
        if (activity.theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)) {
            val type = typedValue.type
            if (type >= android.util.TypedValue.TYPE_FIRST_COLOR_INT &&
                type <= android.util.TypedValue.TYPE_LAST_COLOR_INT
            ) {
                return typedValue.data or 0xFF000000.toInt()
            }
        }
        return Color.WHITE
    }

    companion object {
        private const val SCRIM_COLOR = 0x67000000.toInt()
    }
}
