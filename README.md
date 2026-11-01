# ZJ Head Unit

Launcher nativo (Kotlin + Jetpack Compose) para usar una tablet Android 11 como radio del Grand Cherokee ZJ.

## Cómo compilar
1. Abre esta carpeta en Android Studio (Koala o más nuevo) y deja que sincronice Gradle.
2. Conecta la tablet (depuración USB) y pulsa Run, o genera el APK con Build > Build APK.
3. En la tablet: Ajustes dentro de la app > "Elegir pantalla de inicio" > ZJ Head Unit.

## Qué incluye
- Mapa de OpenStreetMap con MapLibre (sin Google): GPS, búsqueda (Nominatim), ruta (OSRM), indicaciones paso a paso con voz.
- Reproductor propio (Media3): carpetas elegidas con el selector del sistema, sigue sonando en segundo plano.
- Spotify y cualquier otra app se abren desde el launcher.
- Bluetooth: lista de vinculados y acceso a los ajustes del sistema.
- Logo: rejilla de siete ranuras de la ZJ (res/drawable/ic_logo_fg.xml).

## Limitaciones actuales
- Los servidores públicos de Nominatim, OSRM y las teselas CARTO son para pruebas. Para uso diario hay que alojar los propios o pasar a mapas sin internet (siguiente fase).
- Sin internet no hay mapa ni rutas.
- Conectar o desconectar dispositivos Bluetooth lo hace el sistema; la app solo los lista.

## Compilar sin computadora (desde el teléfono)
1. Crea una cuenta gratis en github.com y un repositorio nuevo (privado).
2. Sube el contenido de esta carpeta (incluida la carpeta oculta .github).
3. Entra a la pestaña Actions > Build APK > Run workflow.
4. Cuando termine (unos 5 a 10 min), descarga el artefacto ZJHeadUnit-apk, descomprímelo e instala el APK.
