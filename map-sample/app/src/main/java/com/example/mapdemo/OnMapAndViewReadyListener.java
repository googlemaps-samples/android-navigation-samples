/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.mapdemo;

import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.libraries.navigation.SupportNavigationFragment;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

/**
 * Helper class that will delay triggering the OnMapReady callback until both the GoogleMap and the
 * View having completed initialization. This is only necessary if a Developer wishes to immediately
 * invoke any method on the GoogleMap that also requires the View to have finished layout (ie.
 * anything that needs to know the View's true size like snapshotting).
 */
public class OnMapAndViewReadyListener implements OnGlobalLayoutListener, OnMapReadyCallback {

  /** A listener that needs to wait for both the GoogleMap and the View to be initialized. */
  public static interface OnGlobalLayoutAndMapReadyListener {
    public void onMapReady(GoogleMap googleMap);
  }

  // One of these two fragments is null by design.
  private final SupportNavigationFragment navigationFragment;
  private final SupportMapFragment mapFragment;

  private final View view;
  private final OnGlobalLayoutAndMapReadyListener devCallback;

  private boolean isViewReady;
  private boolean isMapReady;
  private GoogleMap navigationMap;

  @CanIgnoreReturnValue // Calling the constructor registers the listeners as desired.
  public OnMapAndViewReadyListener(
      SupportNavigationFragment navigationFragment, OnGlobalLayoutAndMapReadyListener devCallback) {
    this.navigationFragment = navigationFragment;
    view = navigationFragment.getView();
    this.mapFragment = null;
    this.devCallback = devCallback;
    isViewReady = false;
    isMapReady = false;
    navigationMap = null;

    registerListeners();
  }

  @CanIgnoreReturnValue // Calling the constructor registers the listeners as desired.
  public OnMapAndViewReadyListener(
      SupportNavigationFragment navigationFragment, final OnMapReadyCallback devCallback) {
    this(
        navigationFragment,
        new OnGlobalLayoutAndMapReadyListener() {
          @Override
          public void onMapReady(GoogleMap map) {
            devCallback.onMapReady(map);
          }
        });
  }

  @CanIgnoreReturnValue // Calling the constructor registers the listeners as desired.
  public OnMapAndViewReadyListener(
      SupportMapFragment mapFragment, OnGlobalLayoutAndMapReadyListener devCallback) {
    this.mapFragment = mapFragment;
    view = mapFragment.getView();
    this.navigationFragment = null;
    this.devCallback = devCallback;
    isViewReady = false;
    isMapReady = false;
    navigationMap = null;

    registerListeners();
  }

  @CanIgnoreReturnValue // Calling the constructor registers the listeners as desired.
  public OnMapAndViewReadyListener(
      SupportMapFragment mapFragment, final OnMapReadyCallback devCallback) {
    this(
        mapFragment,
        new OnGlobalLayoutAndMapReadyListener() {
          @Override
          public void onMapReady(GoogleMap map) {
            devCallback.onMapReady(map);
          }
        });
  }

  private void registerListeners() {
    // View layout.
    if ((view.getWidth() != 0) && (view.getHeight() != 0)) {
      // View has already completed layout.
      isViewReady = true;
    } else {
      // Map has not undergone layout, register a View observer.
      view.getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    if (navigationFragment != null) {
      // GoogleMap. Note if the GoogleMap is already ready it will still fire the callback later.
      navigationFragment.getMapAsync(this);
    } else {
      mapFragment.getMapAsync(this);
    }
  }

  @Override
  public void onMapReady(GoogleMap googleMap) {
    // NOTE: The GoogleMap API specifies the listener is removed just prior to invocation.
    this.navigationMap = googleMap;
    isMapReady = true;
    fireCallbackIfReady();
  }

  @SuppressWarnings("deprecation") // We use the new method when supported
  @Override
  public void onGlobalLayout() {
    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
    isViewReady = true;
    fireCallbackIfReady();
  }

  private void fireCallbackIfReady() {
    if (isViewReady && isMapReady) {
      devCallback.onMapReady(navigationMap);
    }
  }
}
