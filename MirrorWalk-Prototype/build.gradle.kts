plugins {
    id("com.android.application") version "9.3.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0" apply false
    // KSP 2.3.10 is the current processor plugin compatible with Kotlin 2.4.x.
    id("com.google.devtools.ksp") version "2.3.10" apply false
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false
}
