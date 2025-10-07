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

import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.mapdemo.EdgeToEdgeUtil.EdgeToEdgeMarginConfig;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCircleClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.navigation.SupportNavigationFragment;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** This shows how to draw circles on a map. */
public class CircleDemoActivity extends AppCompatActivity
    implements OnSeekBarChangeListener,
        OnMarkerDragListener,
        OnMapLongClickListener,
        OnMapReadyCallback {
  private static final LatLng SYDNEY = new LatLng(-33.87365, 151.20689);
  private static final double DEFAULT_RADIUS = 1000000;
  public static final double RADIUS_OF_EARTH_METERS = 6371009;

  private static final int WIDTH_MAX = 50;
  private static final int HUE_MAX = 359;
  private static final int ALPHA_MAX = 255;

  private static final int MAX_DASH_LENGTH = 200;
  private static final int MAX_GAP_LENGTH = 100;
  private static final Random RANDOM = new Random();

  private GoogleMap map;

  private List<DraggableCircle> circles = new ArrayList<DraggableCircle>(1);

  private SeekBar colorBar;
  private SeekBar alphaBar;
  private SeekBar widthBar;
  private int strokeColor;
  private int fillColor;
  private CheckBox clickabilityCheckbox;

  private class DraggableCircle {
    private final Marker centerMarker;
    private final Marker radiusMarker;
    private final Polyline radiusLine;
    private final Circle circle;
    private double radius;

    public DraggableCircle(LatLng center, double radius, boolean clickable) {
      this.radius = radius;
      centerMarker = map.addMarker(new MarkerOptions().position(center).draggable(true));
      LatLng radiusLatLng = toRadiusLatLng(center, radius);
      radiusMarker =
          map.addMarker(
              new MarkerOptions()
                  .position(radiusLatLng)
                  .draggable(true)
                  .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
      circle =
          map.addCircle(
              new CircleOptions()
                  .center(center)
                  .radius(radius)
                  .strokeWidth(widthBar.getProgress())
                  .strokeColor(strokeColor)
                  .fillColor(fillColor)
                  .clickable(clickable));
      radiusLine = map.addPolyline(new PolylineOptions().add(center, radiusLatLng));
    }

    public DraggableCircle(LatLng center, LatLng radiusLatLng, boolean clickable) {
      this.radius = toRadiusMeters(center, radiusLatLng);
      centerMarker = map.addMarker(new MarkerOptions().position(center).draggable(true));
      radiusMarker =
          map.addMarker(
              new MarkerOptions()
                  .position(radiusLatLng)
                  .draggable(true)
                  .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
      circle =
          map.addCircle(
              new CircleOptions()
                  .center(center)
                  .radius(radius)
                  .strokeWidth(widthBar.getProgress())
                  .strokeColor(strokeColor)
                  .strokePattern(generateRandomStrokePattern())
                  .fillColor(fillColor)
                  .clickable(clickable));
      radiusLine = map.addPolyline(new PolylineOptions().add(center, radiusLatLng));
    }

    public boolean onMarkerMoved(Marker marker) {
      if (marker.equals(centerMarker)) {
        circle.setCenter(marker.getPosition());
        LatLng radiusLatLng = toRadiusLatLng(marker.getPosition(), radius);
        radiusMarker.setPosition(radiusLatLng);
        radiusLine.setPoints(Arrays.asList(marker.getPosition(), radiusLatLng));
        return true;
      }
      if (marker.equals(radiusMarker)) {
        radius = toRadiusMeters(centerMarker.getPosition(), radiusMarker.getPosition());
        circle.setRadius(radius);
        radiusLine.setPoints(Arrays.asList(centerMarker.getPosition(), radiusMarker.getPosition()));
        return true;
      }
      return false;
    }

    public void onStyleChange() {
      circle.setStrokeWidth(widthBar.getProgress());
      circle.setFillColor(fillColor);
    }

    public void setClickable(boolean clickable) {
      circle.setClickable(clickable);
    }
  }

  /** Generate LatLng of radius marker */
  private static LatLng toRadiusLatLng(LatLng center, double radius) {
    double radiusAngle =
        Math.toDegrees(radius / RADIUS_OF_EARTH_METERS) / Math.cos(Math.toRadians(center.latitude));
    return new LatLng(center.latitude, center.longitude + radiusAngle);
  }

  private static double toRadiusMeters(LatLng center, LatLng radius) {
    float[] result = new float[1];
    Location.distanceBetween(
        center.latitude, center.longitude, radius.latitude, radius.longitude, result);
    return result[0];
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getIntent()
        .getBooleanExtra(
            ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
            /* defaultValue= */ false)) {
      setContentView(R.layout.circle_demo_nav_flavor);
      setMarginForEdgeToEdgeSupport();
      performAdditionalSetup();
      SupportNavigationFragment navFragment =
          (SupportNavigationFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      navFragment.getMapAsync(this);
    } else {
      setContentView(R.layout.circle_demo_maps_flavor);
      setMarginForEdgeToEdgeSupport();
      performAdditionalSetup();
      SupportMapFragment mapFragment =
          (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      mapFragment.getMapAsync(this);
    }
  }

  private void performAdditionalSetup() {
    colorBar = (SeekBar) findViewById(R.id.hueSeekBar);
    colorBar.setMax(HUE_MAX);
    colorBar.setProgress(0);

    alphaBar = (SeekBar) findViewById(R.id.alphaSeekBar);
    alphaBar.setMax(ALPHA_MAX);
    alphaBar.setProgress(127);

    widthBar = (SeekBar) findViewById(R.id.widthSeekBar);
    widthBar.setMax(WIDTH_MAX);
    widthBar.setProgress(10);

    clickabilityCheckbox = (CheckBox) findViewById(R.id.toggleClickability);
  }

  @Override
  public void onMapReady(GoogleMap map) {
    this.map = map;

    // Override the default content description on the view, for accessibility mode.
    // Ideally this string would be localised.
    map.setContentDescription("Google Map with circles.");

    colorBar.setOnSeekBarChangeListener(this);
    alphaBar.setOnSeekBarChangeListener(this);
    widthBar.setOnSeekBarChangeListener(this);
    this.map.setOnMarkerDragListener(this);
    this.map.setOnMapLongClickListener(this);

    fillColor =
        Color.HSVToColor(alphaBar.getProgress(), new float[] {colorBar.getProgress(), 1, 1});
    this.strokeColor = Color.BLACK;

    DraggableCircle circle =
        new DraggableCircle(SYDNEY, DEFAULT_RADIUS, clickabilityCheckbox.isChecked());
    circles.add(circle);

    // Move the map so that it is centered on the initial circle
    this.map.moveCamera(CameraUpdateFactory.newLatLngZoom(SYDNEY, 4.0f));

    map.setOnCircleClickListener(
        new OnCircleClickListener() {
          @Override
          public void onCircleClick(Circle circle) {
            // Flip the r, g and b components of the circle's stroke color.
            int strokeColor = circle.getStrokeColor() ^ 0x00ffffff;
            circle.setStrokeColor(strokeColor);
            circle.setStrokePattern(generateRandomStrokePattern());
          }
        });
  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {
    // Don't do anything here.
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {
    // Don't do anything here.
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    if (seekBar == colorBar) {
      fillColor = Color.HSVToColor(Color.alpha(fillColor), new float[] {progress, 1, 1});
    } else if (seekBar == alphaBar) {
      fillColor =
          Color.argb(progress, Color.red(fillColor), Color.green(fillColor), Color.blue(fillColor));
    }

    for (DraggableCircle draggableCircle : circles) {
      draggableCircle.onStyleChange();
    }
  }

  @Override
  public void onMarkerDragStart(Marker marker) {
    onMarkerMoved(marker);
  }

  @Override
  public void onMarkerDragEnd(Marker marker) {
    onMarkerMoved(marker);
  }

  @Override
  public void onMarkerDrag(Marker marker) {
    onMarkerMoved(marker);
  }

  private void onMarkerMoved(Marker marker) {
    for (DraggableCircle draggableCircle : circles) {
      if (draggableCircle.onMarkerMoved(marker)) {
        break;
      }
    }
  }

  @Override
  public void onMapLongClick(LatLng point) {
    // We know the center, let's place the outline at a point 3/4 along the view.
    View view;
    Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.map);
    if (fragment instanceof SupportNavigationFragment) {
      view = ((SupportNavigationFragment) fragment).getView();
    } else {
      view = ((SupportMapFragment) fragment).getView();
    }

    LatLng radiusLatLng =
        map.getProjection()
            .fromScreenLocation(new Point(view.getHeight() * 3 / 4, view.getWidth() * 3 / 4));

    // ok create it
    DraggableCircle circle =
        new DraggableCircle(point, radiusLatLng, clickabilityCheckbox.isChecked());
    circles.add(circle);
  }

  public void toggleClickability(View view) {
    for (DraggableCircle draggableCircle : circles) {
      draggableCircle.setClickable(((CheckBox) view).isChecked());
    }
  }

  private static List<PatternItem> generateRandomStrokePattern() {
    switch (RANDOM.nextInt(3)) {
      case 0: // dotted
        return Arrays.<PatternItem>asList(new Dot(), new Gap(RANDOM.nextFloat() * MAX_GAP_LENGTH));
      case 1: // dashed
        return Arrays.<PatternItem>asList(
            new Dash(RANDOM.nextFloat() * MAX_DASH_LENGTH),
            new Gap(RANDOM.nextFloat() * MAX_GAP_LENGTH));
      case 2:
      default: // solid
        return null;
    }
  }

  private void setMarginForEdgeToEdgeSupport() {
    // Margins are only set if the edge-to-edge mode is enabled, it's enabled by default for Android
    // V+ devices.
    // No margins are set for pre-Android V devices.
    EdgeToEdgeUtil.setMarginForEdgeToEdgeSupport(
        ImmutableList.of(
            EdgeToEdgeMarginConfig.builder().setView(findViewById(R.id.layout_container)).build()));
  }
}
