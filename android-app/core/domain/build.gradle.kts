plugins {
    alias(libs.plugins.fidan.android.library)
}

android {
    namespace = "com.erdalgunes.fidan.core.domain"
}

dependencies {
    implementation("javax.inject:javax.inject:1")
    
    testImplementation("junit:junit:4.13.2")
    testImplementation(libs.kotlinx.coroutines.test)
}