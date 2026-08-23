package dev1503.browseevo.ui.widgets

import android.app.Activity
import android.graphics.Typeface
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog

class BottomSheetDialogBuilder(private val activity: Activity) {
    private var titleText: String? = null
    private var contentView: View? = null

    fun title(title: String): BottomSheetDialogBuilder {
        titleText = title
        return this
    }

    fun view(view: View): BottomSheetDialogBuilder {
        contentView = view
        return this
    }

    fun show(): BottomSheetDialog {
        val dialog = BottomSheetDialog(activity)
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(0, pad, 0, pad)
        }
        titleText?.let { title ->
            container.addView(TextView(activity).apply {
                text = title
                textSize = 18f
                setTypeface(typeface, Typeface.BOLD)
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (12 * activity.resources.displayMetrics.density).toInt()
                leftMargin = (16 * activity.resources.displayMetrics.density).toInt()
            })
        }
        contentView?.let {
            container.addView(it, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        }
        dialog.setContentView(container)
        dialog.show()
        return dialog
    }
}
