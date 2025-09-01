plugins {
    alias(libs.plugins.fidan.android.feature)
}

android {
    namespace = "com.erdalgunes.fidan.feature.settings"
}

dependencies {
    // Dependencies are handled by the feature convention plugin
    // which includes core.ui and core.domain
}