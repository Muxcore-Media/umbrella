# kotlinx.serialization: keep serializers for our models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class systems.zem.muxcore.**$$serializer { *; }
-keepclassmembers class systems.zem.muxcore.** {
    *** Companion;
}
-keepclasseswithmembers class systems.zem.muxcore.** {
    kotlinx.serialization.KSerializer serializer(...);
}
