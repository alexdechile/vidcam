# Reglas de ProGuard/R8 para VidCam.
# Media3 y CameraX ya publican sus propias reglas de consumer.
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
