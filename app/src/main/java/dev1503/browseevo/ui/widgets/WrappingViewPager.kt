package dev1503.browseevo.ui.widgets

import android.content.Context
import android.util.AttributeSet
import androidx.viewpager.widget.ViewPager

class WrappingViewPager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ViewPager(context, attrs) {

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post { requestLayout() }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY) {
            var maxHeight = 0
            val childWidthSpec = MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getMode(widthMeasureSpec)
            )
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                child.measure(childWidthSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
                maxHeight = maxOf(maxHeight, child.measuredHeight)
            }
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.EXACTLY))
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}
