# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class dev.hinny.skrot.** {
    *** Companion;
}
-keepclasseswithmembers class dev.hinny.skrot.** {
    kotlinx.serialization.KSerializer serializer(...);
}
