plugins {
    alias(libs.plugins.fidan.android.feature)
}

android {
    namespace = "com.erdalgunes.fidan.feature.forest"
}

dependencies {
    // Dependencies are handled by the feature convention plugin
    // which includes core.ui and core.domain
}