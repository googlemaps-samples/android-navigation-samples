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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.MapView;
import com.google.android.libraries.navigation.NavigationView;

/**
 * This shows how to create multiple maps in an activity that has a Jetpack navigation drawer. This
 * activity forwards all of the important lifecycle methods to the view.
 */
public class MultiMapWithNavDrawerDemoActivity extends AppCompatActivity {

  // One of these Views is always null after creation.
  private NavigationView navigationView = null;
  private MapView mapView = null;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getIntent()
        .getBooleanExtra(
            ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
            /* defaultValue= */ false)) {
      setContentView(R.layout.multimap_with_nav_drawer_demo_nav_flavor);
      navigationView = (NavigationView) findViewById(R.id.mapview);
      navigationView.onCreate(savedInstanceState);
    } else {
      setContentView(R.layout.multimap_with_nav_drawer_demo_maps_flavor);
      mapView = (MapView) findViewById(R.id.mapview);
      mapView.onCreate(savedInstanceState);
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (mapView != null) {
      mapView.onStart();
    } else {
      navigationView.onStart();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (mapView != null) {
      mapView.onResume();
    } else {
      navigationView.onResume();
    }
  }

  @Override
  protected void onPause() {
    if (mapView != null) {
      mapView.onPause();
    } else {
      navigationView.onPause();
    }
    super.onPause();
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (mapView != null) {
      mapView.onStop();
    } else {
      navigationView.onStop();
    }
  }

  @Override
  protected void onDestroy() {
    if (mapView != null) {
      mapView.onDestroy();
    } else {
      navigationView.onDestroy();
    }
    super.onDestroy();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (mapView != null) {
      mapView.onSaveInstanceState(outState);
    } else {
      navigationView.onSaveInstanceState(outState);
    }
  }
}
