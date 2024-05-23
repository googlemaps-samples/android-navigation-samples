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
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mapdemo.OnMapAndViewReadyListener.OnGlobalLayoutAndMapReadyListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.navigation.SupportNavigationFragment;

/**
 * A demo to show the effects of rapidly changing the center of a circle by simulating the day/night
 * cycle as the Earth rotates.
 */
public class DayNightCircleDemoActivity extends AppCompatActivity
    implements OnGlobalLayoutAndMapReadyListener {

  private static final long ANIMATION_DELAY_MS = (long) Math.floor(1000.0 / 30.0); // 30fps.
  // Longitude is arbitrary, but the latitude positions the circle in such a way that the poles
  // are covered and tests the day night terminator bug.
  private static final LatLng CIRCLE_CENTER = new LatLng(16.399514102698678, 0.0);
  // This circle radius is calculated so that half the map is covered by the circle and therefore
  // create a day night terminator.
  private static final double CIRCLE_RADIUS = 6371 * 1000 * Math.PI * 2 / 4;
  // Set delta such that at 30fps, it'll take 60 seconds to rotate 180 degrees.
  private static final float LONGITUDE_TRANSLATION_DELTA = 180f / (30 * 60);
  private static final int PARTIALLY_TRANSPARENT_BLACK = 0xB000_0000;
  private boolean destroyed = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getIntent()
        .getBooleanExtra(
            ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
            /* defaultValue= */ false)) {
      setContentView(R.layout.day_night_circle_demo_nav_flavor);
      SupportNavigationFragment navFragment =
          (SupportNavigationFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      new OnMapAndViewReadyListener(navFragment, this);
    } else {
      setContentView(R.layout.day_night_circle_demo_maps_flavor);
      SupportMapFragment mapFragment =
          (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      new OnMapAndViewReadyListener(mapFragment, this);
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    destroyed = true;
  }

  @Override
  public void onMapReady(GoogleMap map) {
    final Circle circle =
        map.addCircle(
            new CircleOptions()
                .center(CIRCLE_CENTER)
                .fillColor(PARTIALLY_TRANSPARENT_BLACK)
                .radius(CIRCLE_RADIUS));

    Runnable animationRunner =
        new Runnable() {
          private final Handler uiHandler = new Handler();

          @Override
          public void run() {
            // Continue animation indefinitely.
            LatLng center = circle.getCenter();
            circle.setCenter(
                new LatLng(center.latitude, center.longitude + LONGITUDE_TRANSLATION_DELTA));
            if (!destroyed) {
              uiHandler.postDelayed(this, ANIMATION_DELAY_MS);
            }
          }
        };
    animationRunner.run();
  }
}
