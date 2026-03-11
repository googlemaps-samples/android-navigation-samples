# Google Navigation SDK Sample Project

This sample project provides runnable examples demonstrating how to integrate the **Google Navigation SDK for Android** and the **Google Places SDK for Android (New)**.

## Prerequisites

Before building and running the sample app, you need a Google Cloud Project with the correct APIs enabled and an API key with billing configured.

### 1. Enable Required APIs
Go to the [Google Cloud Console](https://console.cloud.google.com/) and enable the following APIs for your project:
*   **Navigation SDK for Android**
*   **Places API (New)** (Required for the location search features)
*   **Maps SDK for Android** (Often required alongside Navigation)

### 2. Configure Your API Key
1. Generate an API Key in the Google Cloud Console (APIs & Services > Credentials).
2. For security, restrict this API key to your Android app's signing certificate and package name (`com.example.navigationapidemo`).
3. Create a file named `secrets.properties` in the root directory of this project (the folder containing `build.gradle.kts` and `settings.gradle.kts`).
4. Add your API key to `secrets.properties` using the following format:
   ```properties
   MAPS_API_KEY=YOUR_API_KEY
   ```
   *Note: This file is ignored by Git, ensuring your API key is not accidentally committed to version control. The app includes a `local.defaults.properties` fallback if the secrets file is missing.*

## Building and Running

You can build and run this sample project using either Android Studio or the command line.

### Using Android Studio
1. **Open the project in Android Studio.** Let Gradle sync the dependencies downloaded from the `gradle/libs.versions.toml` version catalog.
2. Select an emulator or connected physical device.
3. Click **Run** to build and launch the application.

### Using the Command Line
Ensure you have a device connected or an emulator running (verify with `adb devices`). Run the following command from the root of the project to cleanly build, install, and launch the app in one step:

```bash
./gradlew clean :app:installAndLaunch
```

Alternatively, to only build the APK without installing it:
```bash
./gradlew assembleDebug
```

## Modifying Dependencies
This project uses Gradle Version Catalogs. To update the Navigation SDK or Places SDK to a newer version, modify the version strings declared in `gradle/libs.versions.toml`.
