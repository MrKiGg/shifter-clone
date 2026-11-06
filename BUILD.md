# Cómo generar el APK

## Opción 1 — GitHub Actions (recomendada, online)

1. En GitHub: pestaña **Actions** → workflow **Build APK** → **Run workflow**.
2. Cuando termine: **Artifacts** → descarga `shifter-clone-debug-apk`.
3. Instala el `.apk` en el móvil (orígenes desconocidos si hace falta).

APK **debug** para pruebas. Para Play Store hace falta firma release.

## Opción 2 — Android Studio

```bash
./gradlew assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

Requisitos: JDK 17, Android SDK API 35, minSdk 26.
