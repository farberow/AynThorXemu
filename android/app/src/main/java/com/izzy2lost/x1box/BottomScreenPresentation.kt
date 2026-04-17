package com.izzy2lost.x1box

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.WindowManager
import android.widget.TextView
import java.io.File

class BottomScreenPresentation(
  val ownerContext: Context,
  display: Display
) : Presentation(ownerContext, display) {

  private val handler = Handler(Looper.getMainLooper())

  private var fpsValue: TextView? = null
  private var cpuValue: TextView? = null
  private var gpuValue: TextView? = null

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
    window?.setFlags(
      WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
      WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
    )
    setContentView(R.layout.bottom_dashboard)
    fpsValue = findViewById(R.id.dashboard_fps_value)
    cpuValue = findViewById(R.id.dashboard_cpu_value)
    gpuValue = findViewById(R.id.dashboard_gpu_value)
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

  companion object {
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
