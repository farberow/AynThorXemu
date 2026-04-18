package com.izzy2lost.x1box

import android.app.Activity
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
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
  private var emulatorBridge: EmulatorBridge? = null

  fun postFps(fps: Int?) {
    lastFps = fps
    presentation?.setFps(fps)
  }

  fun setEmulatorBridge(bridge: EmulatorBridge?) {
    emulatorBridge = bridge
    presentation?.setEmulatorBridge(bridge)
  }

  fun currentEmulatorBridge(): EmulatorBridge? = emulatorBridge

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
      // Don't leave the bottom screen dark — if another activity is currently
      // resumed, re-host the dashboard on it.
      val next = resumedActivity
      if (next != null && next !== activity) {
        showOn(next)
      }
    }
  }

  private fun showOn(activity: Activity) {
    val target = findBottomDisplay()
    if (target == null) {
      Log.i(TAG, "no bottom display found")
      return
    }
    val existing = presentation
    if (existing != null) {
      val sameDisplay = existing.display.displayId == target.displayId
      val healthy = existing.isShowing && !isOwnerDead(existing)
      if (sameDisplay && healthy) {
        // Reuse across activity transitions — re-creating the Presentation on
        // every resume causes a visible flash on the bottom screen and briefly
        // steals focus on the top screen.
        existing.setFps(lastFps)
        existing.setEmulatorBridge(emulatorBridge)
        return
      }
      Log.i(TAG, "replacing presentation sameDisplay=$sameDisplay healthy=$healthy")
      existing.dismiss()
    }
    Log.i(TAG, "showOn activity=${activity::class.java.simpleName} display=${target.displayId}")
    val p = BottomScreenPresentation(activity, target)
    presentation = p
    try {
      p.show()
      p.setFps(lastFps)
      p.setEmulatorBridge(emulatorBridge)
      Log.i(TAG, "presentation shown")
    } catch (t: Throwable) {
      Log.e(TAG, "presentation show failed", t)
      presentation = null
    }
  }

  private fun isOwnerDead(p: BottomScreenPresentation): Boolean {
    val owner = p.ownerContext as? Activity ?: return false
    if (owner.isFinishing) return true
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && owner.isDestroyed
  }

  companion object {
    private const val TAG = "BottomScreen"
  }

  private fun findBottomDisplay(): Display? {
    val displays = dm.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
    return displays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
  }
}
