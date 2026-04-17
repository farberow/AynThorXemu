package com.izzy2lost.x1box

import android.content.Context
import java.io.File

object ControllerMappingFiles {
  private const val DIRECTORY_NAME = "controller"
  private const val FILE_NAME = "gamecontrollerdb.txt"

  fun resolveFile(context: Context): File {
    return File(File(context.filesDir, DIRECTORY_NAME), FILE_NAME)
  }

  fun ensureParentDirectory(context: Context): File {
    val parent = resolveFile(context).parentFile
      ?: throw IllegalStateException("Controller mapping file has no parent directory.")
    if (!parent.exists() && !parent.mkdirs()) {
      throw IllegalStateException("Failed to prepare controller mapping directory.")
    }
    return parent
  }
}
