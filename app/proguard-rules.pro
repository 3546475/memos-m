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
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# --- R8 rules for the shrunk release build (test branch) ---

# Gson reads model fields through @SerializedName; keep the annotation and annotated members.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep,allowobfuscation @interface com.google.gson.annotations.SerializedName
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# MapLibre's Java layer is called from its native library by name.
-keep class org.maplibre.android.** { *; }
-dontwarn org.maplibre.**

# The app serialises its own models with Gson by declared field name (accounts, cache
# snapshots, sync ops, editor sessions, backups). Keep every field name so the stored JSON
# stays readable in both directions; classes and methods can still be shrunk and renamed.
-keepclassmembers class org.example.moxia.** { <fields>; }

# Reflect-driven API layer: Gson parses these DTOs by field name and Retrofit resolves the
# service interfaces at runtime. Keep them whole so R8 does not merge or reshape them.
-keep class org.example.moxia.api.** { *; }
-keep class org.example.moxia.model.** { *; }
