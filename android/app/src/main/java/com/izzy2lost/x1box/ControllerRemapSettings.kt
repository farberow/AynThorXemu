package com.izzy2lost.x1box

import android.content.Context
import android.content.SharedPreferences
import android.view.KeyEvent
import androidx.annotation.StringRes

class ControllerRemapSettings(context: Context) {
  data class Binding(
    val prefKey: String,
    val targetKeyCode: Int,
    @param:StringRes val labelRes: Int,
  )

  companion object {
    private const val PREFS_NAME = "controller_remap_settings"
    private const val SOURCE_NONE = -1

    val supportedBindings = listOf(
      Binding("bind_a", KeyEvent.KEYCODE_BUTTON_A, R.string.controller_binding_a),
      Binding("bind_b", KeyEvent.KEYCODE_BUTTON_B, R.string.controller_binding_b),
      Binding("bind_x", KeyEvent.KEYCODE_BUTTON_X, R.string.controller_binding_x),
      Binding("bind_y", KeyEvent.KEYCODE_BUTTON_Y, R.string.controller_binding_y),
      Binding("bind_dpad_up", KeyEvent.KEYCODE_DPAD_UP, R.string.controller_binding_dpad_up),
      Binding("bind_dpad_down", KeyEvent.KEYCODE_DPAD_DOWN, R.string.controller_binding_dpad_down),
      Binding("bind_dpad_left", KeyEvent.KEYCODE_DPAD_LEFT, R.string.controller_binding_dpad_left),
      Binding("bind_dpad_right", KeyEvent.KEYCODE_DPAD_RIGHT, R.string.controller_binding_dpad_right),
      Binding("bind_white", KeyEvent.KEYCODE_BUTTON_L1, R.string.controller_binding_white),
      Binding("bind_black", KeyEvent.KEYCODE_BUTTON_R1, R.string.controller_binding_black),
      Binding("bind_start", KeyEvent.KEYCODE_BUTTON_START, R.string.controller_binding_start),
      Binding("bind_back", KeyEvent.KEYCODE_BUTTON_SELECT, R.string.controller_binding_back),
      Binding("bind_left_stick_click", KeyEvent.KEYCODE_BUTTON_THUMBL, R.string.controller_binding_left_stick_click),
      Binding("bind_right_stick_click", KeyEvent.KEYCODE_BUTTON_THUMBR, R.string.controller_binding_right_stick_click),
    )
  }

  private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun getCustomSourceKeyCode(binding: Binding): Int? {
    if (!prefs.contains(binding.prefKey)) {
      return null
    }
    return prefs.getInt(binding.prefKey, binding.targetKeyCode)
  }

  fun getEffectiveSourceKeyCode(binding: Binding): Int? {
    return when (val customSource = getCustomSourceKeyCode(binding)) {
      null -> binding.targetKeyCode
      SOURCE_NONE -> null
      else -> customSource
    }
  }

  fun isCustomized(binding: Binding): Boolean = prefs.contains(binding.prefKey)

  fun countCustomizations(): Int {
    return supportedBindings.count(::isCustomized)
  }

  fun clearAll() {
    prefs.edit().clear().apply()
  }

  fun handlesKeyCode(inputKeyCode: Int): Boolean {
    if (supportedBindings.any { it.targetKeyCode == inputKeyCode }) {
      return true
    }
    return supportedBindings.any { getEffectiveSourceKeyCode(it) == inputKeyCode }
  }

  fun mapInputKeyCode(inputKeyCode: Int): Int? {
    for (binding in supportedBindings) {
      if (getEffectiveSourceKeyCode(binding) == inputKeyCode) {
        return binding.targetKeyCode
      }
    }

    if (supportedBindings.any { it.targetKeyCode == inputKeyCode }) {
      return null
    }

    return inputKeyCode
  }

  fun assignSourceKeyCode(binding: Binding, sourceKeyCode: Int) {
    val edit = prefs.edit()

    for (other in supportedBindings) {
      if (other == binding) {
        continue
      }
      if (getEffectiveSourceKeyCode(other) == sourceKeyCode) {
        edit.putInt(other.prefKey, SOURCE_NONE)
      }
    }

    if (sourceKeyCode == binding.targetKeyCode) {
      edit.remove(binding.prefKey)
    } else {
      edit.putInt(binding.prefKey, sourceKeyCode)
    }
    edit.apply()
  }

  fun describeSourceKeyCode(context: Context, binding: Binding): String {
    val sourceKeyCode = getEffectiveSourceKeyCode(binding) ?: return context.getString(R.string.controller_binding_unassigned)
    return keyCodeLabel(context, sourceKeyCode)
  }

  fun keyCodeLabel(context: Context, keyCode: Int): String {
    return when (keyCode) {
      KeyEvent.KEYCODE_BUTTON_A -> context.getString(R.string.controller_source_button_a)
      KeyEvent.KEYCODE_BUTTON_B -> context.getString(R.string.controller_source_button_b)
      KeyEvent.KEYCODE_BUTTON_X -> context.getString(R.string.controller_source_button_x)
      KeyEvent.KEYCODE_BUTTON_Y -> context.getString(R.string.controller_source_button_y)
      KeyEvent.KEYCODE_DPAD_UP -> context.getString(R.string.controller_source_dpad_up)
      KeyEvent.KEYCODE_DPAD_DOWN -> context.getString(R.string.controller_source_dpad_down)
      KeyEvent.KEYCODE_DPAD_LEFT -> context.getString(R.string.controller_source_dpad_left)
      KeyEvent.KEYCODE_DPAD_RIGHT -> context.getString(R.string.controller_source_dpad_right)
      KeyEvent.KEYCODE_BUTTON_L1 -> context.getString(R.string.controller_source_button_l1)
      KeyEvent.KEYCODE_BUTTON_R1 -> context.getString(R.string.controller_source_button_r1)
      KeyEvent.KEYCODE_BUTTON_START -> context.getString(R.string.controller_source_button_start)
      KeyEvent.KEYCODE_BUTTON_SELECT -> context.getString(R.string.controller_source_button_select)
      KeyEvent.KEYCODE_BUTTON_THUMBL -> context.getString(R.string.controller_source_button_thumbl)
      KeyEvent.KEYCODE_BUTTON_THUMBR -> context.getString(R.string.controller_source_button_thumbr)
      else -> KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_").replace('_', ' ')
    }
  }
}
