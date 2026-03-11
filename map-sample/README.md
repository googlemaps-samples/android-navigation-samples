# Google Navigation SDK: GoogleMap Demo

This sample project provides examples for how the Navigation SDK supports map
styling and features through the "GoogleMap" entry point.

## Description

The source code will look familiar if you've used the
[Google Maps API demos](https://github.com/googlemaps/android-samples/) before.

The toggle switch at the top of the app will allow you to select what kind of
view is stressed as part of each demo. If you choose the "MapView" toggle, the
demo will use a MapView or SupportMapFragment as part of the UI. If you choose
the "NavView" toggle, the demo will use a NavigationView or
SupportNavigationFragment as part of the UI.

This is to showcase that NavigationView supports the behaviors you may have come
to expect from your previous usage of the public Maps APIs.

## Installation & Setup

### Prerequisites
Before running this app, ensure you have enabled the following **three** APIs in your Google Cloud Console for your project:
1. **Google Navigation SDK**
2. **Maps SDK for Android**
3. **Places API (New)**

### 1. API Key Configuration
This project uses the [Secrets Gradle Plugin for Android](https://github.com/google/secrets-gradle-plugin) to safely inject your API key.

1. Create a `secrets.properties` file in the `map-sample` directory (this file is gitignored).
2. Add your authorized Google Maps API key to this file:
   ```properties
   MAPS_API_KEY=AIzaSyYourKeyHere...
   ```

### 2. Dependency Management
This app uses a modern Gradle Version Catalog (`gradle/libs.versions.toml`). To modify the Navigation SDK version or other library versions, update the corresponding `version` property within this file.

### 3. Build and Run
You can open this project in **Android Studio** and click "Run", or use the command line directly:

To compile the app from the terminal:
```bash
./gradlew clean assembleDebug
```

To automatically compile, install, and launch the demo app on a connected emulator or physical device, run:
```bash
./gradlew clean :app:installAndLaunch
```
