package com.izzy2lost.x1box

import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ControllerMappingActivity : AppCompatActivity() {
  private val remapSettings by lazy { ControllerRemapSettings(this) }

  private lateinit var bindingContainer: LinearLayout
  private lateinit var resetButton: MaterialButton
  private lateinit var summaryText: TextView

  private var captureDialog: AlertDialog? = null
  private var waitingForBinding: ControllerRemapSettings.Binding? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    OrientationLocker(this).enable()
    setContentView(R.layout.activity_controller_mapping)
    EdgeToEdgeHelper.enable(this)
    EdgeToEdgeHelper.applySystemBarPadding(findViewById(R.id.controller_mapping_scroll))

    bindingContainer = findViewById(R.id.controller_binding_container)
    resetButton = findViewById(R.id.btn_controller_mapping_reset)
    summaryText = findViewById(R.id.tv_controller_mapping_summary)

    resetButton.setOnClickListener { confirmResetMappings() }

    renderBindings()
  }

  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    val binding = waitingForBinding
    if (binding != null && isGamepadButtonEvent(event)) {
      if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
        remapSettings.assignSourceKeyCode(binding, event.keyCode)
        waitingForBinding = null
        captureDialog?.dismiss()
        captureDialog = null
        renderBindings()
        Toast.makeText(
          this,
          getString(
            R.string.controller_mapping_saved,
            getString(binding.labelRes),
            remapSettings.keyCodeLabel(this, event.keyCode),
          ),
          Toast.LENGTH_SHORT,
        ).show()
      }
      return true
    }
    return super.dispatchKeyEvent(event)
  }

  private fun renderBindings() {
    summaryText.text = getString(
      R.string.controller_mapping_summary,
      remapSettings.countCustomizations(),
    )
    bindingContainer.removeAllViews()

    val rowPadding = (12 * resources.displayMetrics.density).toInt()
    val rowSpacing = (10 * resources.displayMetrics.density).toInt()

    for (binding in ControllerRemapSettings.supportedBindings) {
      val row = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(rowPadding, rowPadding, rowPadding, rowPadding)
        background = getDrawable(R.drawable.section_header_background)
        layoutParams = LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT,
          LinearLayout.LayoutParams.WRAP_CONTENT,
        ).also { it.bottomMargin = rowSpacing }
      }

      val title = TextView(this).apply {
        text = getString(binding.labelRes)
        setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
      }
      val source = TextView(this).apply {
        text = getString(
          if (remapSettings.isCustomized(binding)) {
            R.string.controller_mapping_source_custom
          } else {
            R.string.controller_mapping_source_default
          },
          remapSettings.describeSourceKeyCode(this@ControllerMappingActivity, binding),
        )
        setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
      }
      val button = MaterialButton(
        this,
        null,
        com.google.android.material.R.attr.materialButtonOutlinedStyle,
      ).apply {
        text = getString(R.string.controller_mapping_change_action)
        setOnClickListener { beginCapture(binding) }
      }

      row.addView(title)
      row.addView(source)
      row.addView(button)
      bindingContainer.addView(row)
    }
  }

  private fun beginCapture(binding: ControllerRemapSettings.Binding) {
    waitingForBinding = binding
    captureDialog?.dismiss()
    captureDialog = MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Xemu_RoundedDialog)
      .setTitle(getString(R.string.controller_mapping_capture_title, getString(binding.labelRes)))
      .setMessage(R.string.controller_mapping_capture_message)
      .setNegativeButton(android.R.string.cancel) { _, _ ->
        waitingForBinding = null
      }
      .create()
    captureDialog?.setOnDismissListener {
      if (waitingForBinding == binding) {
        waitingForBinding = null
      }
      captureDialog = null
    }
    captureDialog?.show()
  }

  private fun confirmResetMappings() {
    MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Xemu_RoundedDialog)
      .setTitle(R.string.controller_mapping_reset_title)
      .setMessage(R.string.controller_mapping_reset_message)
      .setPositiveButton(R.string.controller_mapping_reset_action) { _, _ ->
        remapSettings.clearAll()
        renderBindings()
        Toast.makeText(this, R.string.controller_mapping_reset_done, Toast.LENGTH_SHORT).show()
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun isGamepadButtonEvent(event: KeyEvent): Boolean {
    val source = event.source
    return ((source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) ||
      ((source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)
  }
}
