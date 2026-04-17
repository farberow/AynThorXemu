package com.izzy2lost.x1box

import android.app.Activity
import android.app.Application
import android.os.Bundle

class XemuApp : Application() {

  lateinit var bottomScreen: BottomScreenController
    private set

  override fun onCreate() {
    super.onCreate()
    bottomScreen = BottomScreenController(this)
    registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
      override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
      override fun onActivityStarted(activity: Activity) {}
      override fun onActivityResumed(activity: Activity) {
        bottomScreen.onActivityResumed(activity)
      }
      override fun onActivityPaused(activity: Activity) {
        bottomScreen.onActivityPaused(activity)
      }
      override fun onActivityStopped(activity: Activity) {}
      override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
      override fun onActivityDestroyed(activity: Activity) {
        bottomScreen.onActivityDestroyed(activity)
      }
    })
  }
}
