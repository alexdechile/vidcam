-dontwarn androidx.media3.**

# Mantener los componentes de Media3 Transformer y la referencia a LogSessionId
# para que R8 (minificación en release) no rompa la carga de clips o imágenes.
-keep class androidx.media3.transformer.** { *; }
-keep class androidx.media3.effect.** { *; }
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.exoplayer.** { *; }
-dontwarn android.media.metrics.LogSessionId
-keep class android.media.metrics.LogSessionId { *; }

# Puente JNI de RNNoise: los nombres de la clase y de los métodos nativos están
# fijados en `app/src/main/cpp/rnnoise_jni.c` (Java_com_vidcam_app_audio_...),
# así que R8 no puede renombrarlos.
-keep class com.vidcam.app.audio.Rnnoise { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
