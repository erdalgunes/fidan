plugins {
    alias(libs.plugins.fidan.android.library)
    alias(libs.plugins.fidan.android.hilt)
    alias(libs.plugins.room)
}

android {
    namespace = "com.erdalgunes.fidan.core.data"
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(projects.core.domain)
    
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    implementation(libs.androidx.dataStore.preferences)
    
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}