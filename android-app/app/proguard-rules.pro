# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Circuit - Keep screens and their parcelize implementations
-keep class * implements com.slack.circuit.runtime.screen.Screen
-keep class * implements com.slack.circuit.runtime.presenter.Presenter
-keep class * implements com.slack.circuit.runtime.ui.Ui
-keep class * implements com.slack.circuit.runtime.CircuitUiState

# Keep @Parcelize classes
-keep @kotlinx.parcelize.Parcelize class * { *; }
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Hilt - Rules are added automatically by the Hilt plugin

# Compose - Rules are handled by R8 and the Compose compiler plugin

# Keep data classes used with Circuit
-keep class com.erdalgunes.fidan.screens.** { *; }
-keep class com.erdalgunes.fidan.data.** { *; }