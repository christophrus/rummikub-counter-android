# Rummikub Counter Android

Native Android-App (Kotlin + Jetpack Compose), die Rummikub-Steine per Kamera oder Galerie-Bild erkennt und die Punktzahl berechnet.
Die Erkennung läuft lokal auf dem Gerät mit einem vortrainierten YOLO26n-Modell (ONNX Runtime).

## Voraussetzungen

- Android Studio Ladybug oder neuer
- Min SDK 26 (Android 8.0)
- ONNX-Modell `rummikub_yolo.onnx` in `app/src/main/assets/` ablegen

## Build

```bash
./gradlew assembleDebug
```
