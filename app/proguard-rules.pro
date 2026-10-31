-dontwarn androidx.media3.**

# Puente JNI de RNNoise: los nombres de la clase y de los métodos nativos están
# fijados en `app/src/main/cpp/rnnoise_jni.c` (Java_com_vidcam_app_audio_...),
# así que R8 no puede renombrarlos.
-keep class com.vidcam.app.audio.Rnnoise { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
