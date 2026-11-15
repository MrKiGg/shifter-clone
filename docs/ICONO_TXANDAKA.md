# Icono Txandaka

Fuente del logo: calendario TXANDAKA (`Egutegi-2.jpg`).

## Estado en el repo

- `drawable/ic_launcher_background.xml` — fondo `#1B1464`
- `drawable/ic_launcher_foreground.xml` — vector calendario Txandaka (color)
- `drawable/ic_launcher_monochrome.xml` — silueta blanca para **iconos temáticos** (Android 13+)
- `mipmap-anydpi-v26/ic_launcher.xml` y `ic_launcher_round.xml` — adaptive con background + foreground + **monochrome**

Con esto ya tienes icono adaptive y monochrome en GitHub **sin PNG**.

## Icono fotorealista (tu imagen Egutegi-2.jpg)

El conector de GitHub no sube bien binarios PNG. Usa una de estas opciones:

### A) Android Studio Image Asset (recomendado)

1. Clic derecho en `app` → **New** → **Image Asset**
2. Launcher Icons (Adaptive and Legacy)
3. Foreground → Image → `Egutegi-2.jpg`
4. Background → Color → `#1B1464`
5. Finish

### B) Pack generado en el proyecto Grok

Archivo: `txandaka_app_icon.zip` (mipmaps + foreground PNG + monochrome PNG).

```bash
# Desde la raíz del repo, tras descomprimir el zip en /tmp/app_icon:
cp -r /tmp/app_icon/mipmap-* app/src/main/res/
cp -r /tmp/app_icon/drawable-* app/src/main/res/
# Opcional: quitar el vector si prefieres solo PNG
# rm app/src/main/res/drawable/ic_launcher_foreground.xml
git add app/src/main/res
git commit -m "feat(icon): PNGs launcher Txandaka desde Egutegi-2"
git push
```

## Manifest

```xml
android:icon="@mipmap/ic_launcher"
android:roundIcon="@mipmap/ic_launcher_round"
```
