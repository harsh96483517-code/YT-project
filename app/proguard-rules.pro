
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**
-dontwarn javax.script.**
-dontwarn java.beans.**
-dontwarn org.ietf.jgss.**
-dontwarn javax.annotation.**


-dontwarn jdk.dynalink.**



-allowaccessmodification
-repackageclasses
-optimizationpasses 5






-keep class org.schabi.newpipe.extractor.** { *; }
-keep class com.grack.nanojson.** { *; }
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

-dontwarn androidx.media3.**

# Release logs are not user-visible; dropping the calls also drops every format string.
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}


# Phase 1 security: remove debug/verbose/info logging from release builds so nothing
# sensitive can leak through logcat.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# yt-dlp (youtubedl-android) and its dependencies
-keep class com.yausername.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keep class org.apache.commons.compress.** { *; }
-keep class org.apache.commons.io.** { *; }
-dontwarn com.fasterxml.jackson.**
-dontwarn org.apache.commons.compress.**
-dontwarn org.apache.commons.io.**
-dontwarn java.lang.management.**
