# R8 rules for Women Risk Map. Library consumer rules (Ktor, Koin, MapLibre, Supabase, kotlinx) are applied
# automatically; only app-specific or missing rules belong here.

# Keep line numbers for readable Crashlytics stack traces, but hide original source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# kotlinx.serialization: keep generated serializers of @Serializable classes (DTOs, routes, cached state).
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,InnerClasses,Signature,*Annotation*
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor / OkHttp optional dependencies referenced but not shipped.
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
-dontwarn io.ktor.util.debug.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
