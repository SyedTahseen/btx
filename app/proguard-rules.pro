# kotlinx.serialization (app data models are serialized to JSON)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.betterx.app.**$$serializer { *; }
-keepclassmembers class com.betterx.app.** { *** Companion; }
-keepclasseswithmembers class com.betterx.app.** { kotlinx.serialization.KSerializer serializer(...); }
