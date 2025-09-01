plugins {
    alias(libs.plugins.fidan.android.feature)
}

android {
    namespace = "com.erdalgunes.fidan.feature.settings"
}

dependencies {
    implementation(projects.core.data)
}