import com.android.build.gradle.LibraryExtension
import com.erdalgunes.fidan.configureAndroidCompose
import com.erdalgunes.fidan.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("fidan.android.library")
                apply("fidan.android.hilt")
                apply("org.jetbrains.kotlin.plugin.parcelize")
            }

            extensions.configure<LibraryExtension> {
                configureAndroidCompose(this)
            }

            dependencies {
                add("implementation", project(":core:ui"))
                add("implementation", project(":core:domain"))

                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-ktx").get())
                add("implementation", libs.findLibrary("circuit-foundation").get())
                add("implementation", libs.findLibrary("circuit-overlay").get())
                add("implementation", libs.findLibrary("circuit-retained").get())
                add("implementation", libs.findLibrary("circuit-codegen-annotations").get())
                add("kapt", libs.findLibrary("circuit-codegen").get())
            }
        }
    }
}