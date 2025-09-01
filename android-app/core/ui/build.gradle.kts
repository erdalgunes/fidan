plugins {
    alias(libs.plugins.fidan.android.library)
    alias(libs.plugins.fidan.android.library.compose)
}

android {
    namespace = "com.erdalgunes.fidan.core.ui"
}

dependencies {
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.iconsExtended)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.ui.tooling.preview)
    
    debugImplementation(libs.androidx.compose.ui.tooling)
}