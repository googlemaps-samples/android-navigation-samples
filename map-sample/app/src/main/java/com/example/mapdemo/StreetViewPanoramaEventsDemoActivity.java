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
import android.graphics.Point;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanorama.OnStreetViewPanoramaCameraChangeListener;
import com.google.android.gms.maps.StreetViewPanorama.OnStreetViewPanoramaChangeListener;
import com.google.android.gms.maps.StreetViewPanorama.OnStreetViewPanoramaClickListener;
import com.google.android.gms.maps.StreetViewPanorama.OnStreetViewPanoramaLongClickListener;
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.StreetViewPanoramaCamera;
import com.google.android.gms.maps.model.StreetViewPanoramaLocation;
import com.google.android.gms.maps.model.StreetViewPanoramaOrientation;

/** This shows how to listen to some {@link StreetViewPanorama} events. */
public class StreetViewPanoramaEventsDemoActivity extends AppCompatActivity
    implements OnStreetViewPanoramaCameraChangeListener,
        OnStreetViewPanoramaChangeListener,
        OnStreetViewPanoramaClickListener,
        OnStreetViewPanoramaLongClickListener,
        OnStreetViewPanoramaReadyCallback {

  // Pitt St, Sydney
  private static final LatLng SYDNEY = new LatLng(-33.8682624, 151.2083773);

  private StreetViewPanorama streetViewPanorama;
  private boolean createdWithNullBundle;

  private TextView panoChangeTimesTextView;
  private TextView panoCameraChangeTextView;
  private TextView panoClickTextView;
  private TextView panoLongClickTextView;

  private int panoChangeTimes = 0;
  private int panoCameraChangeTimes = 0;
  private int panoClickTimes = 0;
  private int panoLongClickTimes = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.street_view_panorama_events_demo);
    createdWithNullBundle = (savedInstanceState == null);

    panoChangeTimesTextView = (TextView) findViewById(R.id.change_pano);
    panoCameraChangeTextView = (TextView) findViewById(R.id.change_camera);
    panoClickTextView = (TextView) findViewById(R.id.click_pano);
    panoLongClickTextView = (TextView) findViewById(R.id.long_click_pano);

    SupportStreetViewPanoramaFragment streetViewPanoramaFragment =
        (SupportStreetViewPanoramaFragment)
            getSupportFragmentManager().findFragmentById(R.id.streetviewpanorama);
    streetViewPanoramaFragment.setRetainInstance(true);
    streetViewPanoramaFragment.getStreetViewPanoramaAsync(this);
  }

  @Override
  public void onStreetViewPanoramaReady(StreetViewPanorama panorama) {
    streetViewPanorama = panorama;
    streetViewPanorama.setOnStreetViewPanoramaChangeListener(this);
    streetViewPanorama.setOnStreetViewPanoramaCameraChangeListener(this);
    streetViewPanorama.setOnStreetViewPanoramaClickListener(this);
    streetViewPanorama.setOnStreetViewPanoramaLongClickListener(this);

    // Only set the panorama to SYDNEY on startup (when no panoramas have been
    // loaded which is when the savedInstanceState is null).
    if (createdWithNullBundle) {
      streetViewPanorama.setPosition(SYDNEY);
    }
  }

  @Override
  public void onStreetViewPanoramaChange(StreetViewPanoramaLocation location) {
    String panoStr = (location == null) ? "null" : location.panoId;
    panoChangeTimes++;
    Resources res = getResources();
    String text =
        String.format(
            res.getString(R.string.street_view_panorama_pano_changed), panoChangeTimes, panoStr);
    panoChangeTimesTextView.setText(text);
  }

  @Override
  public void onStreetViewPanoramaCameraChange(StreetViewPanoramaCamera camera) {
    panoCameraChangeTimes++;
    Resources res = getResources();
    String text =
        String.format(
            res.getString(R.string.street_view_panorama_camera_changed),
            panoCameraChangeTimes,
            camera);
    panoCameraChangeTextView.setText(text);
  }

  @Override
  public void onStreetViewPanoramaClick(StreetViewPanoramaOrientation orientation) {
    Point point = streetViewPanorama.orientationToPoint(orientation);
    panoClickTimes++;
    Resources res = getResources();
    String text =
        String.format(
            res.getString(R.string.street_view_panorama_times_clicked),
            panoClickTimes,
            orientation,
            point);
    panoClickTextView.setText(text);
    streetViewPanorama.animateTo(
        new StreetViewPanoramaCamera.Builder()
            .orientation(orientation)
            .zoom(streetViewPanorama.getPanoramaCamera().zoom)
            .build(),
        1000);
  }

  @Override
  public void onStreetViewPanoramaLongClick(StreetViewPanoramaOrientation orientation) {
    Point point = streetViewPanorama.orientationToPoint(orientation);
    panoLongClickTimes++;
    Resources res = getResources();
    String text =
        res.getString(
            R.string.street_view_panorama_times_long_clicked,
            panoLongClickTimes,
            orientation,
            point);
    panoLongClickTextView.setText(text);
  }
}
