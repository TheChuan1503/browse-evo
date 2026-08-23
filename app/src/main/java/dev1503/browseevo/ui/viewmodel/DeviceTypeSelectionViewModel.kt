package dev1503.browseevo.ui.viewmodel

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import com.google.android.material.button.MaterialButton
import dev1503.browseevo.MainActivity
import dev1503.browseevo.R
import androidx.core.content.edit

class DeviceTypeSelectionViewModel(override val activity: MainActivity): ViewModel(activity) {
    companion object {
        const val PREF_KEY_DEVICE_TYPE = "device_type"
        const val DEVICE_TYPE_PHONE = "phone"
        const val DEVICE_TYPE_WATCH_SQUARE = "watch_square"
    }

    var onDeviceTypeSelected: (() -> Unit)? = null

    private val deviceTypes = listOf(DEVICE_TYPE_PHONE, DEVICE_TYPE_WATCH_SQUARE)
    private val typeIcons = listOf(R.drawable.mobile_24px, R.drawable.fitness_tracker_24px)
    private val typeLabels = listOf("Phone", "Watch(Square)")

    private lateinit var btnLeft: MaterialButton
    private lateinit var btnRight: MaterialButton
    private lateinit var btnDone: MaterialButton
    private lateinit var imageView: ImageView
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        _view = View.inflate(activity, R.layout.view_model_device_type_selection, null)
        btnLeft = _view.findViewById(R.id.btnLeft)
        btnRight = _view.findViewById(R.id.btnRight)
        btnDone = _view.findViewById(R.id.btnDone)
        imageView = _view.findViewById(R.id.imageView)

        btnDone.setOnClickListener { confirmSelection() }

        btnLeft.setOnClickListener { cycleSelection(-1) }
        btnRight.setOnClickListener { cycleSelection(1) }

        setupKeyInterception()

        applyPreviewSize()
        updatePreview()
    }

    private fun setupKeyInterception() {
        _view.isFocusable = true
        _view.isFocusableInTouchMode = true
        _view.requestFocus()
        _view.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> cycleSelection(-1)
                    KeyEvent.KEYCODE_DPAD_RIGHT -> cycleSelection(1)
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_NUMPAD_ENTER,
                    KeyEvent.KEYCODE_DPAD_CENTER -> btnDone.performClick()
                }
            }
            true
        }
    }

    private fun cycleSelection(direction: Int) {
        currentIndex = (currentIndex + direction + deviceTypes.size) % deviceTypes.size
        updatePreview()
    }

    private fun applyPreviewSize() {
        val metrics = activity.resources.displayMetrics
        val size = minOf(metrics.widthPixels / 3, metrics.heightPixels / 3)
        imageView.layoutParams = imageView.layoutParams.apply {
            width = size
            height = size
        }
    }

    private fun confirmSelection() {
        android.preference.PreferenceManager.getDefaultSharedPreferences(activity)
            .edit {
                putString(PREF_KEY_DEVICE_TYPE, deviceTypes[currentIndex])
            }
        onDeviceTypeSelected?.invoke()
    }

    private fun updatePreview() {
        imageView.setImageResource(typeIcons[currentIndex])
        btnDone.text = typeLabels[currentIndex]
    }
}
