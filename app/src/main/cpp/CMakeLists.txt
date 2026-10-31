# Puente JNI y RNNoise (aislación de entorno para la voz en off).
#
# RNNoise 0.1.1 (Xiph.Org, BSD-3-Clause) está vendorizado en `rnnoise/` para no
# depender de descargas en tiempo de compilación. Solo se compilan los archivos
# de la librería: las herramientas de entrenamiento (`training/`) y el lector de
# modelos desde archivo quedan fuera.
cmake_minimum_required(VERSION 3.22.1)

project(vidcam_audio C)

set(CMAKE_C_STANDARD 11)

add_library(
    rnnoise_jni
    SHARED
    rnnoise_jni.c
    rnnoise/src/denoise.c
    rnnoise/src/pitch.c
    rnnoise/src/kiss_fft.c
    rnnoise/src/celt_lpc.c
    rnnoise/src/rnn.c
    rnnoise/src/rnn_data.c
)

target_include_directories(
    rnnoise_jni
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/rnnoise/include
    ${CMAKE_CURRENT_SOURCE_DIR}/rnnoise/src
)

# `-O3` y vectorización automática: RNNoise procesa 30 s de audio en menos de un
# segundo en un teléfono de gama media. Los avisos de RNNoise son del código
# original (asserts sin efecto en release) y no interesan en el log de Gradle.
target_compile_options(
    rnnoise_jni
    PRIVATE
    -O3
    -Wno-unused-function
    -Wno-unused-variable
    -Wno-unused-but-set-variable
    -Wno-null-dereference
    -Wno-unused-parameter
)

target_link_libraries(rnnoise_jni PRIVATE log m)
