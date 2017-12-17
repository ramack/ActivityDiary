# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/r/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

-keepattributes InnerClasses,EnclosingMethod

-keepattributes Signature

-keep public class * extends java.lang.Exception

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ACRA needs "annotations" so add this...
# Note: This may already be defined in the default "proguard-android-optimize.txt"
# file in the SDK. If it is, then you don't need to duplicate it. See your
# "project.properties" file to get the path to the default "proguard-android-optimize.txt".
-keepattributes *Annotation*

# Keep all the ACRA classes
-keep class org.acra.** { *; }

# Don't warn about removed methods from AppCompat
-keep android.support.v4.app.NotificationCompat*

-keep public class android.support.v7.widget.GridLayoutManager {
    public protected *;
}

#we do not want to obfuscate at all, just the optimization of proguard is of interest
-dontobfuscate