# AGENTS.md

## Repository Overview

`android-camera-mds` is an Android **Mock Device Service (MDS)** that
implements the [MOSIP SBI (Secure Biometric Interface) specification](https://docs.mosip.io/1.1.5/biometrics/mosip-device-service-specification#android-sbi-specification).
It runs as a local Android app that exposes the SBI Intents on-device, so it
can stand in for a real biometric device (finger/face/iris) during MOSIP
registration/authentication testing.

Three Gradle modules; only one is active:

- `app/` — **[DEPRECATED]**. Application ID `nprime.reg.sbi.face`, the
  original nPrime-contributed implementation.
- `SBITestClient/` — **[DEPRECATED]**. Companion Android test-client app.
- `MockAndroidSBI/` — **the active module.** Application ID
  `io.mosip.mock.sbi`. This is the module CI builds — do new work here.

The code was forked/adapted from a third-party (nPrime) implementation —
treat `MockAndroidSBI` as MOSIP's fork/evolution of it, not an original
MOSIP implementation. Some naming still carries the original vendor
(`NprUtils.jar`, `nprime.*` packages in the deprecated `app/` module).

A top-level `package.json` declares Expo/React Native scripts (`expo
start`, `expo run:android`), but no matching RN source (`App.js`,
`app.json`, RN `src/`) exists in this branch's tracked tree — leftover
from an abandoned approach. Don't assume `npm`/`expo` commands work
without verifying first.

## Technology Stack

- **Language:** Java (Android, `sourceCompatibility`/`targetCompatibility`
  1.8) across all three Gradle modules.
- **Build system:** Gradle, Android Gradle Plugin `4.2.1`
  (`com.android.tools.build:gradle:4.2.1`), plugin `com.android.application`.
- **Android SDK:** `compileSdkVersion 30`, `buildToolsVersion "30.0.3"`,
  `minSdkVersion 26`, `targetSdkVersion 30`.
- **Key third-party libraries** (from `MockAndroidSBI/app/build.gradle`):
  `androidx.appcompat`, Google Material, `play-services-vision` (barcode /
  face detection), Glide, `metadata-extractor`, Bouncy Castle
  (`org.bouncycastle:bcprov-jdk15on`), `jose4j` (JOSE/JWT), plus vendored
  jars in `MockAndroidSBI/app/libs/` (`NprUtils.jar`, Jackson jars,
  OkHttp/Okio, `commons-codec`).
- **License:** Mozilla Public License 2.0 (see `LICENSE`; also declared as
  `"license": "MPL 2.0"` in `package.json`).

## Build & Test Commands

The active module is `MockAndroidSBI/` — it has its own Gradle wrapper,
`settings.gradle` (`rootProject.name = "MockAndroidSBI"`), and
`build.gradle`, separate from the repo-root `build.gradle`/`settings.gradle`
(which belong to the deprecated `app/` module, `rootProject.name =
"NprimeSBI"`). Running `./gradlew` from the repo root builds `app/`, not
the active module — always `cd MockAndroidSBI` first.

```shell
cd MockAndroidSBI
./gradlew assembleDebug   # debug APK (gradlew.bat on Windows)
./gradlew test            # JUnit unit tests
```

CI (`.github/workflows/android-custom-build.yml`) calls the shared
`mosip/kattu` workflow with `SERVICE_LOCATION: 'MockAndroidSBI'`,
`ANDROID_LOCATION: '../MockAndroidSBI'`, `GRADLEW_ARGS: 'assembleDebug'` —
confirming `MockAndroidSBI` is the module actually built in automation.
Triggers: releases, pull requests, manual dispatch, and pushes to
`master`, `release*`, `1.*`, `develop*`, `MOSIP*`. There is no CI job for
`app/`/`SBITestClient/`.

`.github/workflows/tag.yaml` wraps `mosip/kattu`'s `tag.yml` for manual
release tagging (`workflow_dispatch`).

Prebuilt APKs are checked in for reference/manual testing:
`APKs/FaceRegSBI-v0.9.5.2.apk`, `APKs/FaceRegSBITest-v1.1.apk`,
`Release/FaceMockRegSBI-v0.9.5.apk`, `Release/FaceMockRegSBIClient-v1.0.apk`.
Don't regenerate/overwrite these as a side effect of an unrelated change.

## Configuration

- `MockAndroidSBI/README.md` documents the in-app settings needed to run
  the mock service: Device Usage (Registration/Auth), Device Key
  (cert + keyalias/password, both modes), FTM Key (Auth only), MOSIP IDA
  Config (AppId/ClientId/SecretId/Auth+IDA server URLs, Auth only), and
  Modality Config (capture score/device status/response delay).
- `local.properties` (repo root and each module's own) holds a
  machine-specific `sdk.dir` path. Gitignored (`**/local.properties`) —
  regenerate locally (Android Studio does this on first sync), never
  commit a personal `sdk.dir` value in a PR.
- No `.env`/secrets files exist in the tracked tree, but the app isn't
  credential-free: Device Key / FTM Key / IDA credentials above are
  entered via `MockAndroidSBI`'s Settings UI at runtime and then
  persisted **unencrypted in the app's default `SharedPreferences`**
  (`device_key_store_password`, `ftm_key_store_password`,
  `mosip_auth_appid`, `mosip_auth_clientid`, `mosip_auth_secretkey`,
  `mosip_ida_server_url`) — plaintext on-device, though not repo-tracked.

## Project Structure Notes

```text
android-camera-mds/
├── app/                 # [DEPRECATED] original nPrime-contributed module
├── SBITestClient/       # [DEPRECATED] companion SBI test-client app
├── MockAndroidSBI/      # ACTIVE module — mock SBI camera/face device service
│   └── app/src/main/java/io/mosip/mock/sbi/
│       ├── constants/
│       ├── device/
│       ├── dto/
│       ├── faceCaptureApi/
│       ├── mds/
│       ├── scanner/ResponseGenerator/
│       ├── secureLib/
│       └── utility/
├── APKs/, Release/      # prebuilt reference APKs
├── build.gradle, settings.gradle, gradle.properties  # belong to deprecated app/
└── package.json         # Expo/RN scripts; no matching RN source in this tree
```

Biometric sample data used for capture responses lives under
`MockAndroidSBI/app/src/main/assets` (per `MockAndroidSBI/README.md`).

## Development Workflow

1. Fork and clone; branch from `develop` (the actively developed
   integration branch — the GitHub-configured default, `main`, holds only
   `README.md`, `LICENSE`, `.github/`, so don't branch from it).
2. Do new work inside `MockAndroidSBI/` unless fixing a deprecated module.
3. Build and test locally with the Gradle wrapper commands above before
   opening a PR.
4. Sign off commits (`git commit -s`) per MOSIP norms.

## Pull Request Guidelines

- Target `develop`, not `main`. Reference the tracking issue.
- Keep `app/`/`SBITestClient/` untouched unless the PR is about them;
  prefer changes in `MockAndroidSBI/`.
- CI only builds `MockAndroidSBI` — a PR touching only `app/` or
  `SBITestClient/` gets no automated build validation; call this out in
  the PR description so reviewers know to build manually.

## Agent rules

### Do

1. Work inside `MockAndroidSBI/` for any new feature, bug fix, or
   dependency change, and run its Gradle wrapper from within that
   directory.
2. Branch from and target PRs against `develop`, not the GitHub-default
   `main` (placeholder files only).
3. Call out explicitly in a PR description when a change touches the
   deprecated `app/` or `SBITestClient/` modules, since CI does not build
   them.
4. Verify any claim about build commands, dependency versions, or file
   paths against the actual files in the module you're changing — this
   repo has three separate Gradle projects with diverging configs.
5. Treat `MockAndroidSBI` as a fork/evolution of the original nPrime
   contribution when describing its provenance.

### Do not

1. Do not run `./gradlew` from the repository root expecting it to build
   the active mock service — that builds the deprecated `app/` module.
2. Do not commit a personal `sdk.dir` value in `local.properties`, and do
   not copy the value already committed at the repo root as a template.
3. Do not assume the `package.json` Expo/React Native scripts are
   runnable without first confirming a matching RN source tree exists on
   the branch you're working on.
4. Do not present this project as an original MOSIP implementation
   without noting its nPrime-contributed origin.
5. Do not overwrite or regenerate the committed APKs in `APKs/`/`Release/`
   as an incidental part of an unrelated change.
