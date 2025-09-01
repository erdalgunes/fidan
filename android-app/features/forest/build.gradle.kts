plugins {
    alias(libs.plugins.fidan.android.feature)
}

android {
    namespace = "com.erdalgunes.fidan.feature.forest"
}

dependencies {
    implementation(projects.core.data)
}