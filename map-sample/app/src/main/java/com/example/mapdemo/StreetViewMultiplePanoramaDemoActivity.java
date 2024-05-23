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
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment;
import com.google.android.gms.maps.model.LatLng;

/** Demo to show multiple StreetView panoramas in the same Activity. */
public class StreetViewMultiplePanoramaDemoActivity extends AppCompatActivity {

  private static final LatLng SYDNEY = new LatLng(-33.8682624, 151.2083773);
  private static final LatLng SAN_FRAN = new LatLng(37.769263, -122.450727);
  private static final LatLng POLICE_PHONEBOX = new LatLng(51.4921451, -0.1929781);
  private static final LatLng ESPLANADE = new LatLng(-34.9661398, 138.5095255);

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.street_view_multiple_panorama_demo);

    if (savedInstanceState == null) {
      setPanoLocation(R.id.panorama1, SYDNEY);
      setPanoLocation(R.id.panorama2, SAN_FRAN);
      setPanoLocation(R.id.panorama3, POLICE_PHONEBOX);
      setPanoLocation(R.id.panorama4, ESPLANADE);
    }
  }

  private void setPanoLocation(int fragmentId, final LatLng location) {
    SupportStreetViewPanoramaFragment panoFragment =
        (SupportStreetViewPanoramaFragment)
            getSupportFragmentManager().findFragmentById(fragmentId);
    panoFragment.setRetainInstance(true);
    panoFragment.getStreetViewPanoramaAsync(
        panorama -> panorama.setPosition(location, 50 /* radius m */));
  }
}
