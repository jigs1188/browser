# Add project specific ProGuard rules here.

# Keep kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class dev.mer.**$$serializer { *; }
-keepclassmembers class dev.mer.** {
    *** Companion;
}
-keepclasseswithmembers class dev.mer.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep JavaScript interface methods
-keepclassmembers class dev.mer.runtime.bridge.RuntimeBridge {
    @android.webkit.JavascriptInterface <methods>;
}
