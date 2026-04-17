package com.izzy2lost.x1box

import android.app.Activity
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display

/**
 * Owns the Presentation shown on the Ayn Thor's bottom screen.
 *
 * The bottom display is discovered via DISPLAY_CATEGORY_PRESENTATION (FLAG_PRESENTATION),
 * not hardcoded by id. When any Activity resumes, we (re)create the Presentation using
 * that Activity as the UI context. When the last Activity pauses or the bottom display
 * unplugs, we dismiss it.
 */
class BottomScreenController(private val app: Context) {

  private val dm: DisplayManager =
    app.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
  private val mainHandler = Handler(Looper.getMainLooper())

  private var resumedActivity: Activity? = null
  private var presentation: BottomScreenPresentation? = null
  private var lastFps: Int? = null

  fun postFps(fps: Int?) {
    lastFps = fps
    presentation?.setFps(fps)
  }

  private val displayListener = object : DisplayManager.DisplayListener {
    override fun onDisplayAdded(displayId: Int) {
      resumedActivity?.let { showOn(it) }
    }
    override fun onDisplayRemoved(displayId: Int) {
      presentation?.takeIf { it.display.displayId == displayId }?.let {
        it.dismiss()
        presentation = null
      }
    }
    override fun onDisplayChanged(displayId: Int) {}
  }

  init {
    dm.registerDisplayListener(displayListener, mainHandler)
  }

  fun onActivityResumed(activity: Activity) {
    resumedActivity = activity
    showOn(activity)
  }

  fun onActivityPaused(activity: Activity) {
    if (resumedActivity === activity) {
      resumedActivity = null
    }
  }

  fun onActivityDestroyed(activity: Activity) {
    val p = presentation ?: return
    if (p.ownerContext === activity) {
      p.dismiss()
      presentation = null
    }
  }

  private fun showOn(activity: Activity) {
    val target = findBottomDisplay() ?: return
    val existing = presentation
    if (existing != null) {
      val sameDisplay = existing.display.displayId == target.displayId
      val sameOwner = existing.ownerContext === activity
      if (sameDisplay && sameOwner && existing.isShowing) return
      existing.dismiss()
    }
    val p = BottomScreenPresentation(activity, target)
    presentation = p
    runCatching {
      p.show()
      p.setFps(lastFps)
    }.onFailure { presentation = null }
  }

  private fun findBottomDisplay(): Display? {
    val displays = dm.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
    return displays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
  }
}
