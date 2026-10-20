# Calcora — Android Calculator

A privacy-first, offline native Android calculator built with Kotlin, Jetpack Compose and Material 3, based on the supplied Product Requirements Document.

## Current implementation

- Standard arithmetic, brackets, operator precedence, backspace, sign toggle, percentage, live result preview and grouped numbers.
- Decimal-safe `BigDecimal` arithmetic for basic operations (for example, `0.1 + 0.2 = 0.3`).
- Scientific functions: trigonometry and inverse trigonometry, DEG/RAD/GRAD, powers, roots, logarithms, factorial, constants, hyperbolic functions, reciprocal and random.
- Local calculation history with search, pinning, deletion and clear-unpinned; history, expression, result, angle mode and memory persist on the device.
- MC, MR, M+, M− memory keys.
- Dark and AMOLED appearance plus Emerald, Sunset, Violet and Rose palette options.
- Offline unit converter for length, weight, temperature, area, volume, speed, time and data.
- Finance helpers for tip/bill splitting, discount and tax calculations.
- Help sheet explaining the calculator's percentage rules.
- No internet permission, ads, analytics or tracking SDK.

## Requirements

- Android Studio with JDK 17
- Android SDK Platform 35
- Android 8.0 (API 26) or newer

## Build locally

1. Open this repository in Android Studio.
2. Let Gradle sync and install any requested Android SDK components.
3. Run the `app` configuration on an emulator or Android device.
4. Run unit tests with `gradle testDebugUnitTest` from a terminal with Gradle installed, or run tests from Android Studio.

## Download an APK

Every push to `main` or `master` starts the Android CI workflow. When the run succeeds:

1. Open [GitHub Actions](https://github.com/hasanparvez11058/Calculator/actions).
2. Open the successful **Android CI** run.
3. Download the `Calcora-debug-apk` artifact.
4. Extract the ZIP to find `app-debug.apk`, then transfer it to your Android phone and install it.

The CI artifact is a debug APK intended for testing, not a Play Store-signed release.

## Project structure

- `app/` — Compose UI, manifest, resources and unit tests
- `app/src/main/java/com/calcora/app/engine/` — expression evaluator
- `.github/workflows/android.yml` — Android build, test and APK artifact upload

## Technical notes

- Calculation history and preferences currently use Android SharedPreferences and JSON serialization. Moving to Room and DataStore, as specified in the PRD architecture, remains follow-up work.
- The current engine uses exact decimal arithmetic for basic operations and JVM math functions for scientific operations.
- This repository has not been independently verified against every PRD acceptance criterion or a physical-device matrix. Check the GitHub Actions run for actual build/test results before distributing an APK.

## Privacy

The app performs calculations locally and does not request network access.
