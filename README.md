# DavidLG IPTV Player 5.11.0 — Premium Redesign

Aplicación Android nativa con Jetpack Compose, preparada para Android TV, teléfonos y tablets. Esta versión parte del código fuente 5.10.0 Classic Premium y conserva la arquitectura, listas, perfiles, EPG y motores de reproducción existentes.

## Cambios de esta versión

### Inicio clásico
- El cuadro principal muestra únicamente el arte del programa actual obtenido de EPG y el logo del canal superpuesto.
- Se elimina la información textual del interior del cuadro principal; los detalles de canal y programación permanecen fuera de ese cuadro donde ya existían.
- La imagen de programa se limita a Inicio clásico; la guía EPG no muestra imágenes de programas.

### Guía EPG clásica
- Se refuerza la jerarquía de la parrilla amplia: columna de canal más generosa, filas más altas, logos mayores y bloques de programas más legibles.
- Se mantienen los horarios basados en la preferencia 12/24 horas y los datos EPG reales.
- La vista compacta mantiene tarjetas táctiles para móvil y tablet, sin artwork de programa.

### Reproductor
- Se integra el OSD basado en la referencia compartida: logo, número y nombre del canal, calidad, FPS, bitrate, programa actual y siguiente, reloj con segundos, tiempo restante y progreso.
- La lista abierta con flecha izquierda se reorganiza en dos columnas: categorías y canales. Cada canal muestra logo, nombre, número, programa actual y progreso EPG.
- La guía abierta con flecha derecha incorpora logo de canal, programación y selección de canal, sin imágenes de programa.
- Arriba/abajo y Channel Up/Down siguen ejecutando zapping inmediato; al abrir paneles, las flechas se dejan a sus controles internos para navegación con D-pad.
- OK prolongado abre opciones de calidad baja, media/adaptativa, alta o automática, audio, subtítulos y ajuste de pantalla (ajustar, estirar o zoom).
- ExoPlayer aplica la selección de bitrate y el ajuste de pantalla; idioma y subtítulos se aplican mediante las preferencias de pistas disponibles. En Shaka, las pistas dependen de lo expuesto por el manifiesto y el WebView; el ajuste de pantalla de ExoPlayer no afecta al WebView.

### Ajustes y perfiles
- Se agregan los niveles de calidad media y el ajuste de pantalla a los ajustes del reproductor.
- La opción de imagen EPG se describe como exclusiva de Inicio clásico; la guía queda expresamente sin imágenes de programas.
- Se conserva el formulario de perfil centrado, desplazable y con acciones fijas Guardar/Cancelar.
- Se conservan las categorías de ajustes existentes y sus conexiones con las preferencias actuales.

### OTT Nuevo
- Se aumenta de forma visible la altura de la barra superior, la tipografía del título, la navegación, el perfil, el reloj y el botón de ajustes.
- Se conserva el resto de las pantallas OTT.

## Construcción

Ejecutar desde la raíz del proyecto:

```bash
./gradlew assembleDebug --no-daemon
```

## Estado de verificación

- ZIP de origen extraído y estructura del proyecto inspeccionada.
- Se ejecutó una comprobación de análisis sintáctico Kotlin sobre los archivos modificados; no aparecieron diagnósticos de sintaxis (`expecting`, `unexpected tokens`).
- **No se pudo completar `assembleDebug`**: Gradle Wrapper intentó descargar Gradle 8.14.3 y el entorno no pudo resolver `services.gradle.org` (`UnknownHostException`). Por ello, esta entrega no se declara compilada ni probada en dispositivo. Se requiere ejecutar Gradle en un entorno con acceso a la distribución y dependencias.
