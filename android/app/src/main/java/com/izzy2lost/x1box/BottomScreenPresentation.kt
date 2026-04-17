package com.izzy2lost.x1box

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.view.WindowManager

class BottomScreenPresentation(
  val ownerContext: Context,
  display: Display
) : Presentation(ownerContext, display) {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window?.setFlags(
      WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
      WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
    )
    setContentView(R.layout.bottom_dashboard)
  }
}
