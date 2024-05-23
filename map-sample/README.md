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

## Installation

-   Open this sample app in Android Studio.

-   This demo app is compatible with a range of supported Navigation SDK
    versions, as indicated by the name of the containing .zip file in Google
    Drive.

-   A default compatible version number has been supplied in the app-level
    `build.gradle` file under the variable named `navSdkVersion`. Make sure to
    update that variable's value to the version of NavSDK you'd like to test.

-   Update the YOUR_API_KEY value in local.defaults.properties to your own API
    key that has been authorized to use the Google Navigation SDK. Visit
    https://developers.google.com/maps/documentation/android-sdk/start#get-key
    for instructions on how to get your own key.

-   Build and run the sample application.
