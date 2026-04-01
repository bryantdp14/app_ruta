-keep class com.cabel.rutacabel.** { *; }
-keepclassmembers class com.cabel.rutacabel.data.local.entities.** { *; }

-keepattributes Signature
-keepattributes *Annotation*

-dontwarn okhttp3.**
-dontwarn retrofit2.**
