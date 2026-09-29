# 3D Model Viewer — Android

Single-Activity app that loads multiple `.glb` models on screen at once, each
in a draggable, resizable container with rotate/zoom and live part labels.

## Stack
Kotlin · Jetpack Compose · Hilt · **Filament** (Google's rendering engine)

**Why Filament (not SceneView):** needed 5+ independent models with their own
camera/gesture state at once. One shared `Engine` + `SurfaceView`, each model
is its own `Scene`/`View`/`Camera` confined to a viewport rect — avoids
creating multiple GL contexts, which SceneView isn't built for.

## Architecture
MVVM + `StateFlow`, single Activity. No full Clean Architecture layering —
kept simple for the task's scope.
```
ui/     → Compose overlays (drag/resize/buttons), label canvas
render/ → Filament wrapper, per-model scene/camera, render loop
data/   → GLB catalog, label parser (extras.prop)
```
Rotation/zoom values are kept as plain fields in the render layer (not
`StateFlow`) to avoid recomposition on every touch frame.

## Labels
Parsed from each GLB's JSON chunk (`nodes[].extras.prop`). Each labelled
node's world position is projected through the camera every frame and drawn
on a `Canvas` overlay with a connector line — stays glued through rotate/zoom.

## Performance Optimizations
- Single shared `Engine`, per-model viewport rects (not per-model SurfaceView)
- Textures resized 1024→512px → **Graphics memory: 330 MB → 181 MB** (5 models, via `adb dumpsys meminfo`)
- MSAA, bloom, SSAO, dithering disabled; tonemapping kept (fully disabling made scenes too dark)
- Single directional light + flat indirect light (no HDR environment)
- Dynamic resolution scaling (min 0.5x) for small containers
- GLB byte-buffer caching to avoid re-reading disk on repeat add
- Explicit teardown on close (entities → components → view → scene); shared `Engine` destroyed only on app exit

## Trade-offs
- **Continuous rendering, not strict render-on-demand.** A dirty-flag
  on-demand system was built but caused stuttery rotation under
  high-frequency gestures; reverted for reliability given time constraints.
  Biggest remaining perf lever.
- **No KTX2/Basis texture compression** — needed external `KTX-Software`
  tool, not available in time. Resolution reduction (1024→512) applied instead.
- **No GPU instancing** for duplicate models — each add creates a full new
  Filament asset, not a shared-geometry instance.
- Square containers only (simplifies aspect-ratio handling).
- No explicit "tap to bring to front" for overlapping containers.

## What I'd Improve With More Time
Fix render-on-demand properly · add KTX2 compression · GPU instancing ·
tap-to-front · automated leak-check script.

## Testing
- Functional: add/drag/resize/rotate/zoom/label/close verified on all 5
  models individually and with 5 loaded together.
- Memory: `adb shell dumpsys meminfo` snapshots (empty → 5 loaded → closed),
  repeated across rounds — Native Heap/Graphics return to baseline after
  close, no leak across repeat add/close cycles.
- **Tested on**: `[aapna device model + Android version yahan likho]`

## Build
Open in Android Studio (min SDK 24), sync, run. Signed release APK provided
separately.

## Demo

https://github.com/user-attachments/assets/9b8baa12-b06f-494b-86c9-16183f4d48fe

## 🚀 Getting Started

1. Clone the repository
   ```bash
   git clone https://github.com/nitinpatil2000/Model3dViewer.git
   ```
2. Open in **Android Studio**
3. Sync Gradle and run on an emulator/device

## 📄 License

This project is open source and available for learning purposes.


