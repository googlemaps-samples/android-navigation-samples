# Google Navigation SDK Sample Project

This sample project provides an example of using the Google Navigation SDK in an
Android Studio project.

## Installation

-   Open this sample app in Android Studio.

-   This demo app is compatible with a range of supported Navigation SDK
    versions, as indicated by the name of the containing .zip file in Google
    Drive.

-   A default compatible version number has been supplied in the app-level
    `build.gradle` file under the variable named `navSdkVersion`. Make sure to
    update that variable's value to the version of NavSDK you'd like to test.

-   Update the YOUR_API_KEY value in the local.defaults.properties to your own
    API key from a project that has been authorized to use the Google Navigation
    SDK. This API key must also have access to the Google Places API enabled in
    order to be able to search for places in the sample application.
    See [instructions](https://developers.google.com/maps/documentation/android-sdk/start#get-key)
    for how to get your own key and learn more about
    [Secrets Gradle plugin](https://developers.google.com/maps/documentation/android-sdk/secrets-gradle-plugin)
    to keep the key out of version control systems.

-   In the **Gradle Scripts folder**, open the `gradle.properties` file and add
    the following if not already present: `android.useAndroidX=true`

-   Build and run the sample application.
