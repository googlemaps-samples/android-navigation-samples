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

import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.StreetViewPanoramaCamera;
import com.google.android.gms.maps.model.StreetViewPanoramaLink;
import com.google.android.gms.maps.model.StreetViewPanoramaLocation;
import com.google.android.gms.maps.model.StreetViewSource;

/**
 * This shows how to create an activity with access to all the options in Panorama which can be
 * adjusted dynamically
 */
public class StreetViewPanoramaNavigationDemoActivity extends AppCompatActivity {

  // Pitt St, Sydney
  private static final LatLng SYDNEY = new LatLng(-33.8682624, 151.2083773);

  // Cole St, San Fran
  private static final LatLng SAN_FRAN = new LatLng(37.7692657, -122.4507992);

  // LatLng with no panorama
  private static final LatLng INVALID = new LatLng(-45.125783, 151.276417);

  /** The amount in degrees by which to scroll the camera */
  private static final int PAN_BY_DEG = 30;

  private static final float ZOOM_BY = 0.5f;

  private StreetViewPanorama streetViewPanorama;

  private SeekBar customDurationBar;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.street_view_panorama_navigation_demo);

    SupportStreetViewPanoramaFragment streetViewPanoramaFragment =
        (SupportStreetViewPanoramaFragment)
            getSupportFragmentManager().findFragmentById(R.id.streetviewpanorama);
    streetViewPanoramaFragment.getStreetViewPanoramaAsync(
        panorama -> {
          streetViewPanorama = panorama;
          // Only set the panorama to SYDNEY on startup (when no panoramas have been
          // loaded which is when the savedInstanceState is null).
          if (savedInstanceState == null) {
            streetViewPanorama.setPosition(SYDNEY);
          }
        });
    customDurationBar = (SeekBar) findViewById(R.id.duration_bar);
  }

  /**
   * When the panorama is not ready the PanoramaView cannot be used. This should be called on all
   * entry points that call methods on the Panorama API.
   */
  private boolean checkReady() {
    if (streetViewPanorama == null) {
      Toast.makeText(this, R.string.panorama_not_ready, Toast.LENGTH_SHORT).show();
      return false;
    }
    return true;
  }

  /** Called when the Go To San Fran button is clicked. */
  public void onGoToSanFran(View view) {
    if (!checkReady()) {
      return;
    }
    streetViewPanorama.setPosition(SAN_FRAN, 30);
  }

  /** Called when the Go To San Fran + Outdoor Only button is clicked. */
  public void onGoToSanFranOutdoor(View view) {
    if (!checkReady()) {
      return;
    }
    streetViewPanorama.setPosition(SAN_FRAN, 50, StreetViewSource.OUTDOOR);
  }

  /** Called when the Animate To Sydney button is clicked. */
  public void onGoToSydney(View view) {
    if (!checkReady()) {
      return;
    }
    streetViewPanorama.setPosition(SYDNEY);
  }

  /** Called when the Animate To Invalid button is clicked. */
  public void onGoToInvalid(View view) {
    if (!checkReady()) {
      return;
    }
    streetViewPanorama.setPosition(INVALID);
  }

  public void onZoomIn(View view) {
    if (!checkReady()) {
      return;
    }

    streetViewPanorama.animateTo(
        new StreetViewPanoramaCamera.Builder()
            .zoom(streetViewPanorama.getPanoramaCamera().zoom + ZOOM_BY)
            .tilt(streetViewPanorama.getPanoramaCamera().tilt)
            .bearing(streetViewPanorama.getPanoramaCamera().bearing)
            .build(),
        getDuration());
  }

  public void onZoomOut(View view) {
    if (!checkReady()) {
      return;
    }

    streetViewPanorama.animateTo(
        new StreetViewPanoramaCamera.Builder()
            .zoom(streetViewPanorama.getPanoramaCamera().zoom - ZOOM_BY)
            .tilt(streetViewPanorama.getPanoramaCamera().tilt)
            .bearing(streetViewPanorama.getPanoramaCamera().bearing)
            .build(),
        getDuration());
  }

  public void onPanLeft(View view) {
    if (!checkReady()) {
      return;
    }

    streetViewPanorama.animateTo(
        new StreetViewPanoramaCamera.Builder()
            .zoom(streetViewPanorama.getPanoramaCamera().zoom)
            .tilt(streetViewPanorama.getPanoramaCamera().tilt)
            .bearing(streetViewPanorama.getPanoramaCamera().bearing - PAN_BY_DEG)
            .build(),
        getDuration());
  }

  public void onPanRight(View view) {
    if (!checkReady()) {
      return;
    }

    streetViewPanorama.animateTo(
        new StreetViewPanoramaCamera.Builder()
            .zoom(streetViewPanorama.getPanoramaCamera().zoom)
            .tilt(streetViewPanorama.getPanoramaCamera().tilt)
            .bearing(streetViewPanorama.getPanoramaCamera().bearing + PAN_BY_DEG)
            .build(),
        getDuration());
  }

  public void onPanUp(View view) {
    if (!checkReady()) {
      return;
    }

    float currentTilt = streetViewPanorama.getPanoramaCamera().tilt;
    float newTilt = currentTilt + PAN_BY_DEG;

    newTilt = Math.min(90, newTilt);

    streetViewPanorama.animateTo(
        new StreetViewPanoramaCamera.Builder()
            .zoom(streetViewPanorama.getPanoramaCamera().zoom)
            .tilt(newTilt)
            .bearing(streetViewPanorama.getPanoramaCamera().bearing)
            .build(),
        getDuration());
  }

  public void onPanDown(View view) {
    if (!checkReady()) {
      return;
    }

    float currentTilt = streetViewPanorama.getPanoramaCamera().tilt;
    float newTilt = currentTilt - PAN_BY_DEG;

    newTilt = Math.max(-90, newTilt);

    streetViewPanorama.animateTo(
        new StreetViewPanoramaCamera.Builder()
            .zoom(streetViewPanorama.getPanoramaCamera().zoom)
            .tilt(newTilt)
            .bearing(streetViewPanorama.getPanoramaCamera().bearing)
            .build(),
        getDuration());
  }

  public void onRequestPosition(View view) {
    if (!checkReady()) {
      return;
    }
    if (streetViewPanorama.getLocation() != null) {
      Toast.makeText(
              view.getContext(),
              streetViewPanorama.getLocation().position.toString(),
              Toast.LENGTH_SHORT)
          .show();
    }
  }

  public void onMovePosition(View view) {
    StreetViewPanoramaLocation location = streetViewPanorama.getLocation();
    StreetViewPanoramaCamera camera = streetViewPanorama.getPanoramaCamera();
    if ((location != null) && (location.links != null) && (location.links.length > 0)) {
      StreetViewPanoramaLink link = findClosestLinkToBearing(location.links, camera.bearing);
      streetViewPanorama.setPosition(link.panoId);
    } else {
      String panoId = (location != null) ? location.panoId : "Empty Pano";
      Toast.makeText(view.getContext(), panoId + " has no nearby panos.", Toast.LENGTH_SHORT)
          .show();
    }
  }

  public static StreetViewPanoramaLink findClosestLinkToBearing(
      StreetViewPanoramaLink[] links, float bearing) {
    float minBearingDiff = 360;
    StreetViewPanoramaLink closestLink = links[0];
    for (StreetViewPanoramaLink link : links) {
      if (minBearingDiff > findNormalizedDifference(bearing, link.bearing)) {
        minBearingDiff = findNormalizedDifference(bearing, link.bearing);
        closestLink = link;
      }
    }
    return closestLink;
  }

  // Find the difference between angle a and b as a value between 0 and 180
  public static float findNormalizedDifference(float a, float b) {
    float diff = a - b;
    float normalizedDiff = diff - (float) (360 * Math.floor(diff / 360.0f));
    return (normalizedDiff < 180.0f) ? normalizedDiff : 360.0f - normalizedDiff;
  }

  private long getDuration() {
    return customDurationBar.getProgress();
  }
}
