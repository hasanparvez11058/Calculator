# Calcora — Android Calculator

A privacy-first, offline native Android calculator built with Kotlin, Jetpack Compose and Material 3, based on the supplied product requirements document.

## Features

- Basic arithmetic, brackets, operator precedence, percent, sign toggle, clear and backspace
- Live expression preview with decimal-safe arithmetic for basic operations
- Scientific functions, degree/radian mode, powers, roots, logarithms and factorial
- Emerald Material 3-inspired UI, light/dark system theme
- Local calculation history and reusable results
- No internet permission, ads, analytics, accounts or tracking
- Android 8.0 (API 26) and newer

## Build

1. Open this repository in the latest stable Android Studio.
2. Allow Gradle to sync and download Android build dependencies.
3. Run the `app` configuration on an emulator or device (API 26+).
4. Run tests with `./gradlew test` after adding/using a local Gradle wrapper, or run the unit tests from Android Studio.

The project uses the Android Gradle Plugin and Kotlin version catalog. The Gradle wrapper JAR is not checked in; Android Studio can generate it, or run `gradle wrapper` with a compatible Gradle installation.

## Project structure

- `app/` — Compose app, application manifest, resources and unit tests
- `core/engine/` — pure Kotlin expression evaluator and tests
- `.github/workflows/android.yml` — Android build CI

## Scope note

This is the first implementation slice of the PRD: the core calculator, scientific functions, history and a basic appearance setting are included. Advanced Room-backed history management, DataStore settings, all four palettes, adaptive tablet layouts, haptics, finance helpers and unit conversion remain follow-up work before claiming full PRD acceptance.

## Privacy

The app does not request network access and performs calculations locally.
