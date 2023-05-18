# MockAndroidSBI

## Overview

This module contains Mock Android Device service implementation as per defined [SBI specification](https://docs.mosip.io/1.1.5/biometrics/mosip-device-service-specification#android-sbi-specification). Biometric data for capture is available at `app\src\main\assets`

## Prerequisites

### System Requirements

- 64-bit Microsoft Windows 10 OS.
- x86_64 CPU architecture; 2nd generation Intel Core or higher/AMD CPU with support for a Windows Hypervisor.
- 8 GB RAM or more.
- 8 GB of available disk space minimum (IDE + Android SDK + Android Emulator)

### Application Requirements

To run the application the following are required:

- Intellij version 2020.3.+ (or) Android Studio version 2020.3.+
- Android SDK 26.0
- JDK 1\.8 or higher

### Application Configuration

Following are the configurations in Settings.

1. Device Usage: Select Registration or Auth based on the require response.
2. Device Key: Upload device certificate here with keyalias and password. Required for both Auth and Registration purpose.
3. FTM Key: Upload device FTM key here with keyalias and password. Required for Auth purpose.
4. MOSIP IDA Config: Add AppId, ClientId, SecertId, Auth Server URL and IDA Server URL here. Required for Auth purpose
5. Modality Config: Add capture score, device status and response delay for each individual modality here.
