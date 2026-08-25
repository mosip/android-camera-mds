# AGENTS.md

## Repository Overview

`android-camera-mds` is an Android **Mock Device Service (MDS)** that
implements the [MOSIP SBI (Secure Biometric Interface) specification](https://docs.mosip.io/1.1.5/biometrics/mosip-device-service-specification#android-sbi-specification).
It runs as a local Android app that exposes the SBI Intents on-device, so it
can stand in for a real biometric device (finger/face/iris) during MOSIP
registration/authentication testing.

The repository root `README.md` marks two of the three Gradle modules as
deprecated:

- `app/` — **[DEPRECATED]**. Application ID `nprime.reg.sbi.face`. This is
  the original implementation, contributed by nPrime (per
  `MockAndroidSBI/README.md`: "The first version of mock MDS was contributed
  by nPrime").
- `SBITestClient/` — **[DEPRECATED]**. A companion Android test-client app
  for exercising the SBI service.
- `MockAndroidSBI/` — **the active module.** Application ID
  `io.mosip.mock.sbi`, package root `io.mosip.mock.sbi`. This is the module
  CI actually builds (see Build & Test Commands below) and is where new
  work should go.

Because the code was forked/adapted from a third-party (nPrime)
implementation, do not describe it as written from scratch by MOSIP —
treat `MockAndroidSBI` as a MOSIP-maintained fork/evolution of that
original contribution, and expect some naming (e.g. `NprUtils.jar`,
`nprime.*` package names in the deprecated `app/` module) to still carry
the original vendor's naming.

The repo also carries a top-level `package.json` with Expo/React Native
dependencies and scripts (`expo start`, `expo run:android`, etc.), but no
matching React Native source (no `App.js`/`app.json`/RN `src` tree) exists
anywhere in the tracked tree on this branch. Treat `package.json` as a
leftover from an earlier/abandoned approach — do not assume `npm`/`expo`
commands are runnable, and verify before relying on them.

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
"NprimeSBI"`).

Build the debug APK from within `MockAndroidSBI/`:

```shell
cd MockAndroidSBI
./gradlew assembleDebug
```

On Windows use `gradlew.bat` instead of `./gradlew`.

Run unit tests (JUnit) for the module:

```shell
cd MockAndroidSBI
./gradlew test
```

CI (`.github/workflows/android-custom-build.yml`) calls the shared
`mosip/kattu` workflow with `SERVICE_LOCATION: 'MockAndroidSBI'`,
`ANDROID_LOCATION: '../MockAndroidSBI'`, and `GRADLEW_ARGS:
'assembleDebug'` — confirming `MockAndroidSBI` is the module that is
actually built in automation. This workflow triggers on releases, pull
requests, manual dispatch, and pushes to `master`, `release*`, `1.*`,
`develop*`, and `MOSIP*` branches.

A separate workflow, `.github/workflows/tag.yaml`, wraps
`mosip/kattu/.github/workflows/tag.yml` for manually tagging/publishing a
release via `workflow_dispatch`.

There is no CI job for the deprecated `app/` or `SBITestClient/` modules —
don't assume changes there are validated by CI.

Prebuilt APKs are checked into the repo for reference/manual testing:
`APKs/FaceRegSBI-v0.9.5.2.apk`, `APKs/FaceRegSBITest-v1.1.apk`,
`Release/FaceMockRegSBI-v0.9.5.apk`, `Release/FaceMockRegSBIClient-v1.0.apk`.

## Configuration

- `MockAndroidSBI/README.md` documents the in-app settings needed to run
  the mock service:
  1. **Device Usage** — select Registration or Auth.
  2. **Device Key** — device certificate + keyalias/password (required for
     both Auth and Registration).
  3. **FTM Key** — FTM key + keyalias/password (required for Auth).
  4. **MOSIP IDA Config** — AppId, ClientId, SecretId, Auth Server URL, IDA
     Server URL (required for Auth).
  5. **Modality Config** — capture score, device status, and response
     delay per modality.
- `local.properties` (repo root) and each module's own `local.properties`
  hold a machine-specific `sdk.dir` path. This is now gitignored
  (`**/local.properties`) — regenerate it locally (Android Studio does
  this automatically on first sync) and never commit a personal
  `sdk.dir` value in a PR.
- No `.env`/secrets files were found in the tracked tree, but the app
  itself is not credential-free: the "Device Key" / "FTM Key" / IDA
  credentials above are entered through `MockAndroidSBI`'s Settings UI
  at runtime and then persisted **unencrypted in the app's default
  `SharedPreferences`** (`device_key_store_password`,
  `ftm_key_store_password`, `mosip_auth_appid`, `mosip_auth_clientid`,
  `mosip_auth_secretkey`, `mosip_ida_server_url`) — they are not
  ephemeral or repo-tracked, but they are stored in plaintext on-device.

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

1. Fork the repo and clone your fork.
2. Branch from `develop` — that is the actively developed integration
   branch (the GitHub-configured default branch, `main`, contains only
   `README.md`, `LICENSE`, and `.github/`, so do not branch from `main`).
3. Do new work inside `MockAndroidSBI/` unless you are specifically fixing
   something in the deprecated modules.
4. Build and test locally with the Gradle wrapper commands above before
   opening a PR.
5. Keep commits signed off (`git commit -s`) per MOSIP contribution norms.

## Pull Request Guidelines

- Target the `develop` branch, not `main`.
- Reference the tracking issue in the PR description.
- Keep the deprecated `app/`/`SBITestClient/` modules untouched unless the
  PR is explicitly about them; prefer changes in `MockAndroidSBI/`.
- Because CI only builds `MockAndroidSBI`, a PR touching only `app/` or
  `SBITestClient/` will not get automated build validation — call this out
  explicitly in the PR description so reviewers know to build manually.

## Repository-Specific Considerations

- This is a **thin/forked** implementation: the original mock MDS was
  contributed by nPrime, and MOSIP's active fork lives in `MockAndroidSBI/`.
  Do not present code here as purely MOSIP-original — some vendored jars
  and naming still trace back to the nPrime contribution.
- Three Gradle projects coexist in one repo root (`NprimeSBI` at root,
  `MockAndroidSBI`, and a third inside `SBITestClient/`), each with its own
  wrapper and `settings.gradle`. Always `cd` into the specific module
  directory before running Gradle commands — running `./gradlew` from the
  repo root builds the deprecated `app/` module, not the active one.
- The repo mixes committed build artifacts (APKs) with source — do not
  regenerate/overwrite the `APKs/` or `Release/` APKs as a side effect of
  an unrelated change.
- `package.json`/Expo tooling at the root has no corresponding RN app
  source in this branch; don't build guidance around it without first
  verifying an RN source tree actually exists.

## Agent rules

### Do

1. Do work inside `MockAndroidSBI/` for any new feature, bug fix, or
   dependency change, and run its Gradle wrapper from within that
   directory.
2. Branch from and target PRs against `develop`, not the GitHub-default
   `main` branch (which only holds placeholder files).
3. Call out explicitly in a PR description when a change touches the
   deprecated `app/` or `SBITestClient/` modules, since CI does not build
   them.
4. Verify any claim about build commands, dependency versions, or file
   paths against the actual files in the module you are changing before
   writing it down — this repo has three separate Gradle projects with
   diverging configurations.
5. Treat `MockAndroidSBI` as a fork/evolution of the original nPrime
   contribution when describing its provenance.

### Do not

1. Do not run `./gradlew` from the repository root expecting it to build
   the active mock service — that builds the deprecated `app/` module
   instead.
2. Do not commit a personal `sdk.dir` value in `local.properties`, and do
   not copy the value already committed at the repo root as if it were a
   valid template.
3. Do not assume the `package.json` Expo/React Native scripts are
   runnable without first confirming a matching RN source tree exists on
   the branch you're working on.
4. Do not present this project as an original MOSIP implementation
   without noting its nPrime-contributed origin.
5. Do not overwrite or regenerate the committed APKs in `APKs/`/`Release/`
   as an incidental part of an unrelated change.
