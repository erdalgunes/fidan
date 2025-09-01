plugins {
    alias(libs.plugins.fidan.android.feature)
}

android {
    namespace = "com.erdalgunes.fidan.feature.timer"
}

dependencies {
    implementation(projects.core.data)
}