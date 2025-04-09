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

import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.libraries.navigation.SupportNavigationFragment;

/** This shows how the Developer can clamp the camera. */
public class CameraClampingDemoActivity extends AppCompatActivity
    implements OnMapReadyCallback, OnCameraMoveStartedListener {

  private static final String TAG = CameraClampingDemoActivity.class.getSimpleName();

  private static final float ZOOM_DELTA = 2.0f;
  private static final float DEFAULT_MIN_ZOOM = 2.0f;
  private static final float DEFAULT_MAX_ZOOM = 22.0f;

  private static final LatLng ADELAIDE_LL = new LatLng(-34.92873, 138.59995);
  private static final LatLng ADELAIDE_LL_SW = new LatLng(-35.0, 138.58);
  private static final LatLng ADELAIDE_LL_NE = new LatLng(-34.9, 138.61);
  private static final LatLngBounds ADELAIDE_LLB = new LatLngBounds(ADELAIDE_LL_SW, ADELAIDE_LL_NE);
  private static final CameraPosition ADELAIDE_CP =
      new CameraPosition.Builder().target(ADELAIDE_LL).zoom(20.0f).bearing(0).tilt(0).build();

  private static final LatLng PACIFIC_LL_SW = new LatLng(-15.0, 165.0);
  private static final LatLng PACIFIC_LL_NE = new LatLng(15.0, -165.0);
  private static final LatLngBounds PACIFIC_LLB = new LatLngBounds(PACIFIC_LL_SW, PACIFIC_LL_NE);
  private static final CameraPosition PACIFIC_CP =
      new CameraPosition.Builder()
          .target(new LatLng(0, -180))
          .zoom(4.0f)
          .bearing(0)
          .tilt(0)
          .build();

  private GoogleMap map;
  private float minZoomPref;
  private float maxZoomPref;
  private TextView cameraTextView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getIntent()
        .getBooleanExtra(
            ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
            /* defaultValue= */ false)) {
      setContentView(R.layout.camera_clamping_demo_nav_flavor);
      performAdditionalSetup();
      SupportNavigationFragment navFragment =
          (SupportNavigationFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      navFragment.getMapAsync(this);
    } else {
      setContentView(R.layout.camera_clamping_demo_maps_flavor);
      performAdditionalSetup();
      SupportMapFragment mapFragment =
          (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      mapFragment.getMapAsync(this);
    }
  }

  private void performAdditionalSetup() {
    map = null;
    resetMinMaxZoomPrefsInternal();
    cameraTextView = (TextView) findViewById(R.id.camera_text);
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  @Override
  public void onMapReady(GoogleMap map) {
    this.map = map;
    map.setOnCameraMoveStartedListener(this);
  }

  @Override
  public void onCameraMoveStarted(int reason) {
    Resources res = getResources();
    String text =
        res.getString(
            R.string.demo_on_camera_move,
            ConversionUtils.toAnimationReasonString(reason),
            map.getCameraPosition().toString());
    cameraTextView.setText(text);
  }

  /**
   * Before the map is ready many calls will fail. This should be called on all entry points that
   * call methods on the Maps SDK for Android.
   */
  private boolean checkReady() {
    if (map == null) {
      Toast.makeText(this, R.string.map_not_ready, Toast.LENGTH_SHORT).show();
      return false;
    }
    return true;
  }

  private void toast(String msg) {
    Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
  }

  private void resetMinMaxZoomPrefsInternal() {
    minZoomPref = DEFAULT_MIN_ZOOM;
    maxZoomPref = DEFAULT_MAX_ZOOM;
  }

  public void onClampToAdelaide(View view) {
    if (!checkReady()) {
      return;
    }
    map.setLatLngBoundsForCameraTarget(ADELAIDE_LLB);

    // NOTE: Developers should use moveTo. I'm using animate for debugging purposes.
    map.animateCamera(CameraUpdateFactory.newCameraPosition(ADELAIDE_CP));
  }

  public void onClampToPacific(View view) {
    if (!checkReady()) {
      return;
    }
    map.setLatLngBoundsForCameraTarget(PACIFIC_LLB);

    // NOTE: Developers should use moveTo. I'm using animate for debugging purposes.
    map.animateCamera(CameraUpdateFactory.newCameraPosition(PACIFIC_CP));
  }

  public void onLatLngClampReset(View view) {
    if (!checkReady()) {
      return;
    }
    map.setLatLngBoundsForCameraTarget(null);
    toast("LatLngBounds clamp reset.");
  }

  public void onSetMinZoomClamp(View view) {
    if (!checkReady()) {
      return;
    }
    minZoomPref += ZOOM_DELTA;
    map.setMinZoomPreference(minZoomPref);
    toast("Min zoom preference set to: " + minZoomPref);
  }

  public void onSetMaxZoomClamp(View view) {
    if (!checkReady()) {
      return;
    }
    maxZoomPref -= ZOOM_DELTA;
    map.setMaxZoomPreference(maxZoomPref);
    toast("Max zoom preference set to: " + maxZoomPref);
  }

  public void onMinMaxZoomClampReset(View view) {
    if (!checkReady()) {
      return;
    }
    resetMinMaxZoomPrefsInternal();
    map.resetMinMaxZoomPreference();
    toast("Min/Max zoom preferences reset.");
  }
}
