# RNNoise vendorizado

Copia de **RNNoise 0.1.1** (Xiph.Org), licencia BSD-3-Clause (`COPYING`), usada
para la aislación de entorno de la voz en off.

- Origen: https://github.com/xiph/rnnoise (tag `v0.1.1`), tarball de la release.
- Se copiaron `include/rnnoise.h` y los archivos de `src/` que forman la
  librería (`denoise.c`, `pitch.c`, `kiss_fft.c`, `celt_lpc.c`, `rnn.c`,
  `rnn_data.c`) con sus cabeceras. Quedan fuera las herramientas de
  entrenamiento y `rnn_reader.c` (carga de modelos desde archivo), que no se usan.
- El modelo de red (`rnn_data.c`) es el que trae la release: va compilado dentro
  del `.so`, sin descargas en tiempo de compilación.
- Se elige 0.1.1 en vez de 0.2 a propósito: el modelo de 0.2 pesa ~29 MB de
  fuente y deja el APK varias veces más grande, mientras que 0.1.1 aporta
  ~130 KB por ABI y rinde de sobra para tomas de voz de hasta 30 s.

Para actualizar: reemplazar estos archivos por la versión nueva, ajustar la lista
de fuentes en `../CMakeLists.txt` y volver a firmar la APK (el `.so` cambia).
