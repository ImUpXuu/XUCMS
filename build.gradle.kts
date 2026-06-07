tasks.register("listFiles2") {
    doLast {
        fileTree(".").forEach { file ->
            if (file.name.endsWith(".svg") || file.name.endsWith(".ico") || file.name.endsWith(".gif") || file.name.endsWith(".jpeg") || file.name.endsWith(".bmp")) {
                println("FOUND_IMAGE2: " + file.absolutePath)
            }
        }
    }
}
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
}
