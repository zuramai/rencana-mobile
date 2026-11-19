# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.zuramai.rencana.**$$serializer { *; }
-keepclassmembers class com.zuramai.rencana.** { *** Companion; }
-keepclasseswithmembers class com.zuramai.rencana.** { kotlinx.serialization.KSerializer serializer(...); }
-keepattributes SourceFile,LineNumberTable
# Ktor / OkHttp optional dependencies
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
