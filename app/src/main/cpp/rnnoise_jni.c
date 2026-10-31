/*
 * Puente JNI mínimo sobre RNNoise para la voz en off de VidCam.
 *
 * El lado Kotlin (com.vidcam.app.audio.Rnnoise) crea un DenoiseState por toma y
 * le pasa fotogramas de 480 muestras (10 ms a 48 kHz) en PCM 16 bits. RNNoise
 * trabaja en float con la escala de int16 y devuelve, además del audio limpio,
 * la probabilidad de voz del fotograma.
 */
#include <jni.h>
#include <stdint.h>

#include "rnnoise.h"

/** Tamaño máximo de fotograma que se acepta (RNNoise usa 480). */
#define MAX_FRAME 1024

JNIEXPORT jint JNICALL
Java_com_vidcam_app_audio_Rnnoise_nativeFrameSize(JNIEnv *env, jobject thiz) {
    (void) env;
    (void) thiz;
    return rnnoise_get_frame_size();
}

JNIEXPORT jlong JNICALL
Java_com_vidcam_app_audio_Rnnoise_nativeCreate(JNIEnv *env, jobject thiz) {
    (void) env;
    (void) thiz;
    DenoiseState *state = rnnoise_create(NULL);
    return (jlong) (intptr_t) state;
}

JNIEXPORT void JNICALL
Java_com_vidcam_app_audio_Rnnoise_nativeDestroy(JNIEnv *env, jobject thiz, jlong handle) {
    (void) env;
    (void) thiz;
    if (handle != 0) {
        rnnoise_destroy((DenoiseState *) (intptr_t) handle);
    }
}

/*
 * Procesa un fotograma de `count` muestras: `input` es PCM 16 bits y `output`
 * recibe el mismo número de muestras en float, con la escala de int16
 * (-32768..32767), que es la que espera RNNoise. Devuelve la probabilidad de
 * voz del fotograma (0..1).
 */
JNIEXPORT jfloat JNICALL
Java_com_vidcam_app_audio_Rnnoise_nativeProcess(
        JNIEnv *env,
        jobject thiz,
        jlong handle,
        jshortArray input,
        jfloatArray output,
        jint count) {
    (void) thiz;
    if (handle == 0 || input == NULL || output == NULL) {
        return 0.0f;
    }
    if (count <= 0 || count > MAX_FRAME) {
        return 0.0f;
    }
    jsize inputLength = (*env)->GetArrayLength(env, input);
    jsize outputLength = (*env)->GetArrayLength(env, output);
    if (inputLength < count || outputLength < count) {
        return 0.0f;
    }

    float frameIn[MAX_FRAME];
    float frameOut[MAX_FRAME];
    jshort *samples = (*env)->GetShortArrayElements(env, input, NULL);
    if (samples == NULL) {
        return 0.0f;
    }
    for (int i = 0; i < count; i++) {
        frameIn[i] = (float) samples[i];
    }
    (*env)->ReleaseShortArrayElements(env, input, samples, JNI_ABORT);

    float vad = rnnoise_process_frame(
            (DenoiseState *) (intptr_t) handle,
            frameOut,
            frameIn);

    (*env)->SetFloatArrayRegion(env, output, 0, count, frameOut);
    return vad;
}
