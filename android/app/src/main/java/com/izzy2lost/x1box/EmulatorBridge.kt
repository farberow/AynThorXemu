package com.izzy2lost.x1box

/**
 * Implemented by [MainActivity] so the bottom-screen dashboard can drive the
 * running emulator. The bridge is non-null only while MainActivity is resumed.
 */
interface EmulatorBridge {
  fun saveSnapshot(slot: Int)
  fun loadSnapshot(slot: Int)
  fun pauseEmulation()
  fun resumeEmulation()
  fun rebootSystem()
  fun setFpsOverlayVisible(visible: Boolean)
}
