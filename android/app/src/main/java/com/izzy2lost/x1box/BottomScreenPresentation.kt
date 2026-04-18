package com.izzy2lost.x1box

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import java.io.File

class BottomScreenPresentation(
  val ownerContext: Context,
  display: Display
) : Presentation(ownerContext, display, R.style.Theme_Xemu) {

  private val handler = Handler(Looper.getMainLooper())
  private val prefs = ownerContext.getSharedPreferences("x1box_prefs", Context.MODE_PRIVATE)

  private var brandX: ImageView? = null
  private var fpsValue: TextView? = null
  private var cpuValue: TextView? = null
  private var gpuValue: TextView? = null
  private var statusLabel: TextView? = null
  private var buildBadge: TextView? = null
  private var slotLabel: TextView? = null
  private var slotPrev: MaterialButton? = null
  private var slotNext: MaterialButton? = null
  private var saveBtn: MaterialButton? = null
  private var loadBtn: MaterialButton? = null
  private var pauseBtn: MaterialButton? = null
  private var rebootBtn: MaterialButton? = null
  private var fpsSwitch: MaterialSwitch? = null

  private var bridge: EmulatorBridge? = null
  private var slot: Int = 1
  private var paused: Boolean = false

  private val thermalInterval = 2000L
  private val thermalRunnable = object : Runnable {
    override fun run() {
      cpuValue?.text = formatTemp(maxTemp(cpuZones))
      gpuValue?.text = formatTemp(maxTemp(gpuZones))
      handler.postDelayed(this, thermalInterval)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // The dashboard is always-on furniture, not a dialog — never let Back,
    // gamepad B, or stray touches dismiss it.
    setCancelable(false)
    setCanceledOnTouchOutside(false)
    window?.apply {
      setFlags(
        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
      )
      // Dialogs dim their host window by default. On a dual-display setup,
      // attach glitches can briefly paint that dim on the top screen.
      clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
      setDimAmount(0f)
    }
    setContentView(R.layout.bottom_dashboard)

    brandX = findViewById(R.id.dashboard_brand_wedge)
    brandX?.startAnimation(AnimationUtils.loadAnimation(context, R.anim.x_mark_idle_pulse))

    fpsValue = findViewById(R.id.dashboard_fps_value)
    cpuValue = findViewById(R.id.dashboard_cpu_value)
    gpuValue = findViewById(R.id.dashboard_gpu_value)
    statusLabel = findViewById(R.id.dashboard_status)
    buildBadge = findViewById(R.id.dashboard_build_number)
    buildBadge?.text = formatBuildBadge()
    slotLabel = findViewById(R.id.dashboard_slot_label)
    slotPrev = findViewById(R.id.dashboard_slot_prev)
    slotNext = findViewById(R.id.dashboard_slot_next)
    saveBtn = findViewById(R.id.dashboard_save)
    loadBtn = findViewById(R.id.dashboard_load)
    pauseBtn = findViewById(R.id.dashboard_pause)
    rebootBtn = findViewById(R.id.dashboard_reboot)
    fpsSwitch = findViewById(R.id.dashboard_fps_switch)

    slot = prefs.getInt(PREF_SLOT, 1).coerceIn(1, TOTAL_SLOTS)
    renderSlot()

    slotPrev?.setOnClickListener {
      slot = if (slot <= 1) TOTAL_SLOTS else slot - 1
      persistSlot()
      renderSlot()
      flashSlot()
    }
    slotNext?.setOnClickListener {
      slot = if (slot >= TOTAL_SLOTS) 1 else slot + 1
      persistSlot()
      renderSlot()
      flashSlot()
    }

    saveBtn?.setOnClickListener { bridge?.saveSnapshot(slot) }
    loadBtn?.setOnClickListener { bridge?.loadSnapshot(slot) }

    pauseBtn?.setOnClickListener {
      val b = bridge ?: return@setOnClickListener
      if (paused) {
        b.resumeEmulation()
        paused = false
      } else {
        b.pauseEmulation()
        paused = true
      }
      renderPause()
    }

    rebootBtn?.setOnClickListener { bridge?.rebootSystem() }

    fpsSwitch?.isChecked = prefs.getBoolean(PREF_SHOW_FPS, false)
    fpsSwitch?.setOnCheckedChangeListener { _, checked ->
      prefs.edit().putBoolean(PREF_SHOW_FPS, checked).apply()
      bridge?.setFpsOverlayVisible(checked)
    }

    applyBridgeEnabled(bridge != null)
  }

  override fun onStart() {
    super.onStart()
    handler.removeCallbacks(thermalRunnable)
    handler.post(thermalRunnable)
  }

  override fun onStop() {
    handler.removeCallbacks(thermalRunnable)
    super.onStop()
  }

  fun setFps(fps: Int?) {
    fpsValue?.text = fps?.toString() ?: "—"
  }

  fun setEmulatorBridge(b: EmulatorBridge?) {
    bridge = b
    paused = false
    renderPause()
    applyBridgeEnabled(b != null)
  }

  private fun applyBridgeEnabled(enabled: Boolean) {
    val views = listOf(saveBtn, loadBtn, pauseBtn, rebootBtn)
    for (v in views) {
      v?.isEnabled = enabled
      v?.alpha = if (enabled) 1f else 0.4f
    }
    statusLabel?.text = if (enabled) "in-game" else "idle"
    statusLabel?.setTextColor(
      resources.getColor(
        if (enabled) R.color.xemu_green else R.color.xemu_text_muted, null
      )
    )
    if (enabled) {
      statusLabel?.startAnimation(AnimationUtils.loadAnimation(context, R.anim.status_active_pulse))
    } else {
      statusLabel?.clearAnimation()
    }
  }

  private fun renderSlot() {
    slotLabel?.text = "SLOT $slot"
  }

  private fun flashSlot() {
    slotLabel?.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slot_change_flash))
  }

  private fun renderPause() {
    pauseBtn?.text = if (paused) "RESUME" else "PAUSE"
  }

  private fun persistSlot() {
    prefs.edit().putInt(PREF_SLOT, slot).apply()
  }

  /** "BUILD v1.2.4 • thor-2026-04-18.01" */
  private fun formatBuildBadge(): String {
    val version = BuildConfig.VERSION_NAME.trim()
    val manualLabel = BuildConfig.BOTTOM_SCREEN_BUILD_LABEL.trim()
    if (version.isEmpty()) {
      return manualLabel.ifEmpty { "BUILD unknown" }
    }
    return if (manualLabel.isEmpty()) {
      "BUILD v$version"
    } else {
      "BUILD v$version • $manualLabel"
    }
  }

  companion object {
    private const val PREF_SLOT = "dashboard_slot"
    private const val PREF_SHOW_FPS = "show_fps"
    private const val TOTAL_SLOTS = 10

    private val cpuZones by lazy { discoverZones { it.startsWith("cpu") } }
    private val gpuZones by lazy { discoverZones { it.startsWith("gpu") } }

    private fun discoverZones(match: (String) -> Boolean): List<File> {
      val base = File("/sys/class/thermal")
      val zones = base.listFiles { f -> f.name.startsWith("thermal_zone") } ?: return emptyList()
      return zones.mapNotNull { zone ->
        val typeFile = File(zone, "type")
        val tempFile = File(zone, "temp")
        val type = runCatching { typeFile.readText().trim() }.getOrNull() ?: return@mapNotNull null
        if (!match(type) || !tempFile.canRead()) null else tempFile
      }
    }

    private fun maxTemp(files: List<File>): Int? {
      var best: Int? = null
      for (f in files) {
        val raw = runCatching { f.readText().trim().toIntOrNull() }.getOrNull() ?: continue
        if (best == null || raw > best) best = raw
      }
      return best
    }

    private fun formatTemp(milliC: Int?): String {
      val c = milliC ?: return "—"
      return "${c / 1000}°"
    }
  }
}
