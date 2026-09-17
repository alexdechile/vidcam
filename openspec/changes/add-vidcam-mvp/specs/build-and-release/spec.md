## Purpose

Define cómo se compila y verifica la aplicación de forma automática y reproducible mediante GitHub Actions, incluyendo la generación de un release firmado.

## ADDED Requirements

### Requirement: Verificación continua en GitHub Actions
El repositorio DEBE (MUST) compilar y verificar la aplicación automáticamente en cada push y pull request mediante un workflow de GitHub Actions que ejecute lint, pruebas unitarias, la compilación de los tests instrumentados y los ensamblados de depuración y release.

#### Scenario: Pull request verificado
- **WHEN** se abre o actualiza un pull request hacia la rama principal
- **THEN** GitHub Actions ejecuta lint, pruebas unitarias, la compilación de los tests instrumentados y los ensamblados de depuración y release, y el resultado queda reportado en el pull request

#### Scenario: Build falla por lint o pruebas
- **WHEN** el lint o una prueba unitaria falla
- **THEN** el workflow termina en error y no publica ningún artefacto

### Requirement: Artefacto APK de depuración
El workflow de verificación DEBE (MUST) publicar el APK de depuración como artefacto descargable cuando la compilación es exitosa.

#### Scenario: Descargar el APK de depuración
- **WHEN** el workflow de verificación termina correctamente
- **THEN** el APK de depuración queda disponible como artefacto descargable asociado a esa ejecución

### Requirement: APK de release minificado
El build de release DEBE (MUST) usar R8 y reducción de recursos para producir un APK sustancialmente más pequeño que el de depuración sin minificar, y el workflow de verificación DEBE (MUST) compilar el release en cada push para validar la ofuscación y publicar ese APK como artefacto.

#### Scenario: Release minificado disponible
- **WHEN** el workflow de verificación termina correctamente
- **THEN** el APK de release queda ofuscado, con recursos reducidos, y disponible como artefacto descargable

#### Scenario: Fallo de ofuscación
- **WHEN** R8 falla por una regla de keep faltante
- **THEN** el workflow de verificación termina en error y no publica el artefacto de release

### Requirement: Release firmado con secrets
El repositorio DEBE (MUST) generar un APK o AAB firmado al crear un tag de versión o al ejecutar manualmente el workflow de release, usando un keystore provisto exclusivamente mediante secrets del repositorio.

#### Scenario: Tag de versión genera release
- **WHEN** se crea un tag que empieza con la letra v, por ejemplo v1.0.0
- **THEN** GitHub Actions compila la versión de release, la firma con el keystore de los secrets y la publica como artefacto y GitHub Release

#### Scenario: Secrets ausentes
- **WHEN** el workflow de release se ejecuta sin los secrets del keystore
- **THEN** el workflow falla con un mensaje claro y no publica un artefacto sin firmar

### Requirement: Build reproducible sin secretos locales
El proyecto DEBE (MUST) compilar localmente sin requerir el keystore de release, y la configuración de firma DEBE (MUST) tomarse de variables de entorno o propiedades inyectadas por integración continua.

#### Scenario: Compilación local de depuración
- **WHEN** un colaborador clona el repositorio y ejecuta el build de depuración sin configurar secretos
- **THEN** el build compila y produce un APK de depuración firmado con la clave de depuración
