# Dynamic Material Island

> A floating **Material 3 capsule** that lives around your front camera —
> quick media controls, shortcuts and expressive motion. Inspired by the
> Dynamic Island, but **genuinely Android**: Material You colors, native
> services, 100% on-device.

[![Build APK](https://github.com/HOWCKs/PortMobileTerraria-Tlaucher/actions/workflows/build-apk.yml/badge.svg)](https://github.com/HOWCKs/PortMobileTerraria-Tlaucher/actions/workflows/build-apk.yml)

## Features (v0.1.0 — MVP)

- **Living capsule**: tap to expand with a spring, drag anywhere on screen,
  compact pill shows clock + battery (and the current track when media plays)
- **Media controls**: play/pause, previous/next and seek over any app that
  exposes a `MediaSession` (Spotify, YouTube Music, Podcasts…)
- **Quick shortcuts**: Camera, Phone, Email, Settings and Hide — one tap from
  any screen
- **Material You**: colors derived from your wallpaper (Android 12+)
- **Zero data collection**: no account, no analytics, no network at all

## Roadmap

| Milestone | Content | Status |
|---|---|---|
| M0/M1 | Project, CI/CD pipeline, living capsule (drag, spring expand, Material You), shortcuts, media controls | ✅ v0.1.0 |
| M2 | Flashlight with smooth brightness (duty-cycle pulse), polish | ⏳ |
| M3 | Accessibility gestures (swipe-to-top), system events, download progress, barcodes | ⏳ |
| M4 | Settings screen (theme, chips, sensitivity), release signing | ⏳ |

## Install the APK (from your phone)

1. Push (or run) the **Build APK** workflow — it runs on every push.
   Wait for the green check (≈ 4–8 min).
2. Open the repo → **Actions** tab → click the run.
3. Download the **`DynamicMaterialIsland-apk`** artifact (unzip it).
4. Tap the `.apk` → **Install** (allow "Install unknown apps" for your browser
   if prompted).
5. Open **Dynamic Material Island** → *Grant* the "Draw over other apps"
   permission → **Start**.

## Build locally

Requirements: JDK 17 + Android SDK (platform 36).

```bash
./gradlew :app:assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

## Permissions (and why)

| Permission | Why |
|---|---|
| `SYSTEM_ALERT_WINDOW` | The capsule floats above other apps (the whole point). |
| `FOREGROUND_SERVICE` + `specialUse` | Keeps the overlay alive in the background (Android 14+ rule). |
| `POST_NOTIFICATIONS` | The required foreground-service notification that shows "Exit". |

## Honest platform notes

- **Flashlight** (M2): the public Android API only exposes torch on/off;
  "smooth brightness" is simulated with a fast duty-cycle pulse — the eye
  perceives the tone, it is not hardware dimming.
- **Downloads** (M3): there is no public "global download manager"; progress
  is read from notifications via `NotificationListenerService`, which works
  for Play, Telegram, browsers… but depends on each app reporting progress.
- **Media**: only apps that expose a `MediaSession` are controllable — that is
  a system design choice, not an app limitation.
- **Distribution**: overlay apps using `AccessibilityService` are restricted on
  Google Play; this project is built for **personal sideload use** via CI
  artifacts.

## Architecture

```
com.howck.dmi
├── DmiApp               → notification channel bootstrap
├── MainActivity         → onboarding + status (Compose, Material You)
├── service/
│   └── CapsuleService   → FGS (specialUse) that owns the overlay window,
│                          the MediaController and shortcut intents
├── model/
│   └── MediaState       → volatile snapshot of the active session
└── ui/
    ├── CapsuleOverlay   → the floating pill (tap/expand, drag, media, chips)
    ├── DmiScreen        → main settings/onboarding screen
    └── Theme            → dynamic (Material You) color schemes
```

**Stack**: Kotlin · Jetpack Compose · Material 3 · Gradle (KTS, version
catalog) · GitHub Actions — `minSdk 31`, `targetSdk 36`, debug signing.
