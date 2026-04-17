import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
}

fun Project.isUsablePython3(path: String): Boolean {
  if (path.isBlank() || !file(path).canExecute()) {
    return false
  }

  return try {
    val process = ProcessBuilder(
      path,
      "-c",
      "import sys; print('{}.{}'.format(*sys.version_info[:2]))",
    ).start()
    val stdout = process.inputStream.bufferedReader().use { it.readText().trim() }
    process.errorStream.close()
    process.waitFor() == 0 && run {
      val match = Regex("""^3\.(\d+)$""").matchEntire(stdout)
      match != null && match.groupValues[1].toInt() >= 6
    }
  } catch (_: Exception) {
    false
  }
}

val hostPython3 = sequenceOf(
  System.getenv("PYTHON3"),
  "/opt/homebrew/bin/python3",
  "/usr/local/bin/python3",
  "/Library/Developer/CommandLineTools/usr/bin/python3",
  "/Applications/Xcode.app/Contents/Developer/usr/bin/python3",
  "/usr/bin/python3",
).filterNotNull().firstOrNull { isUsablePython3(it) }

val keystorePropertiesFile = rootProject.file("key.properties")
val keystoreProperties = Properties()
val hasKeystoreProperties = keystorePropertiesFile.exists()

if (hasKeystoreProperties) {
  keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

val hasReleaseKeystore = hasKeystoreProperties &&
  listOf("storeFile", "storePassword", "keyAlias", "keyPassword").all {
    !keystoreProperties.getProperty(it).isNullOrBlank()
  }

android {
  namespace = "com.izzy2lost.x1box"
  compileSdk = 36
  buildToolsVersion = "36.1.0"
  ndkVersion = "29.0.14206865"

  defaultConfig {
    applicationId = "com.izzy2lost.x1box"
    minSdk = 26
    targetSdk = 36

    versionCode = 25
    versionName = "1.2.4"

    ndk {
      abiFilters += listOf("arm64-v8a")
    }

    externalNativeBuild {
      cmake {
        arguments += listOf(
          "-DXEMU_ANDROID_BUILD_ID=3",
          "-DXEMU_ENABLE_XISO_CONVERTER=ON",
          "-DCMAKE_C_FLAGS_DEBUG=-O2 -g0",
          "-DCMAKE_CXX_FLAGS_DEBUG=-O2 -g0",
          "-DCMAKE_C_FLAGS_RELWITHDEBINFO=-O2 -g0 -fvisibility=hidden",
          "-DCMAKE_CXX_FLAGS_RELWITHDEBINFO=-O2 -g0 -fvisibility=hidden",
          "-DCMAKE_C_FLAGS_RELEASE=-O2 -g0 -fvisibility=hidden",
          "-DCMAKE_CXX_FLAGS_RELEASE=-O2 -g0 -fvisibility=hidden",
        ) + listOfNotNull(
          hostPython3?.let { "-DPython3_EXECUTABLE=$it" },
          hostPython3?.let { "-DPYTHON_EXECUTABLE=$it" },
        )
        cppFlags += listOf("-std=c++17", "-fexceptions", "-frtti")
      }
    }
  }

  signingConfigs {
    if (hasReleaseKeystore) {
      create("release") {
        storeFile = file(keystoreProperties.getProperty("storeFile"))
        storePassword = keystoreProperties.getProperty("storePassword")
        keyAlias = keystoreProperties.getProperty("keyAlias")
        keyPassword = keystoreProperties.getProperty("keyPassword")
      }
    }
  }

  buildTypes {
    debug {
      ndk {
        debugSymbolLevel = "NONE"
      }
    }
    release {
      externalNativeBuild {
        cmake {
          arguments += listOf("-DXEMU_ENABLE_LTO=ON")
        }
      }
      isMinifyEnabled = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
      if (hasReleaseKeystore) {
        signingConfig = signingConfigs.getByName("release")
      }
    }
  }

  externalNativeBuild {
    cmake {
      path = file("src/main/cpp/CMakeLists.txt")
      version = "3.22.1"
    }
  }

  packaging {
    resources.excludes += setOf(
      "**/*.md",
      "META-INF/LICENSE*",
      "META-INF/NOTICE*"
    )
    /* Extract .so to disk (nativeLibraryDir); required for adrenotools hooks / custom GPU drivers. */
    jniLibs.useLegacyPackaging = true
    jniLibs.keepDebugSymbols += setOf("**/*.so")
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }

}

dependencies {
  implementation("androidx.core:core-ktx:1.15.0")
  implementation("androidx.appcompat:appcompat:1.7.0")
  implementation("androidx.constraintlayout:constraintlayout:2.1.4")
  implementation("androidx.documentfile:documentfile:1.0.1")
  implementation("io.coil-kt:coil:2.7.0")
  implementation("com.google.android.material:material:1.12.0")
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_21)
  }
}

tasks.register<Exec>("runDebug") {
  group = "install"
  description = "Installs and launches the debug APK on the connected device."
  dependsOn("installDebug")
  val adbPath = android.sdkDirectory.resolve("platform-tools/adb").absolutePath
  val pkg = android.defaultConfig.applicationId
  commandLine(adbPath, "shell", "am", "start", "-n", "$pkg/.LauncherActivity")
}
