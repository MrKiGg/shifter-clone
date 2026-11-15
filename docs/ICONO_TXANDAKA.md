# Icono Txandaka

Fuente del logo: calendario TXANDAKA (`Egutegi-2.jpg`).

## Generar con Android Studio (recomendado)

1. Clic derecho en `app` → **New** → **Image Asset**
2. **Launcher Icons (Adaptive and Legacy)**
3. **Foreground Layer** → Image → selecciona `Egutegi-2.jpg`
4. **Background Layer** → Color → `#1B1464`
5. Ajusta el scale (~90–100 %) para que el calendario quede centrado
6. Next → Finish

Android Studio genera solo todos los `mipmap-*` y el adaptive icon.

## Alternativa: pack generado

En el chat del proyecto hay un ZIP `txandaka_app_icon.zip` con:

- `mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png` (+ round)
- `drawable-{densities}/ic_launcher_foreground.png`
- `mipmap-anydpi-v26/ic_launcher.xml`
- fondo sólido `#1B1464`

Copia su contenido en `app/src/main/res/` y elimina el viejo `drawable/ic_launcher_foreground.xml` (vector) si sigue presente.

## Manifest

```xml
android:icon="@mipmap/ic_launcher"
android:roundIcon="@mipmap/ic_launcher_round"
```
