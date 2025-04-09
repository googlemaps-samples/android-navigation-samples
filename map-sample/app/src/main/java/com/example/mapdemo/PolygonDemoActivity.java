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
import android.os.Bundle;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnPolygonClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.libraries.navigation.SupportNavigationFragment;
import java.util.Arrays;
import java.util.List;

/** This showcases polygons and their styling features. */
public class PolygonDemoActivity extends AppCompatActivity
    implements ViewPager.OnPageChangeListener,
        OnPolygonClickListener,
        OnMapReadyCallback,
        RadioGroup.OnCheckedChangeListener {
  private static final LatLng SYDNEY = new LatLng(-33.87365, 151.20689);

  private Polygon northwestPolygon;
  private Polygon centralPolygon;
  private Polygon southeastPolygon;
  private Polygon selectedPolygon;
  private Polygon worldWrappedPolygon;

  private ViewPager pager;
  private PolygonControlFragmentPagerAdapter pagerAdapter;
  private RadioGroup polygonRadio;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getIntent()
        .getBooleanExtra(
            ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
            /* defaultValue= */ false)) {
      setContentView(R.layout.polygon_demo_nav_flavor);
      performAdditionalSetup();
      SupportNavigationFragment navFragment =
          (SupportNavigationFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      navFragment.getMapAsync(this);
    } else {
      setContentView(R.layout.polygon_demo_maps_flavor);
      performAdditionalSetup();
      SupportMapFragment mapFragment =
          (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      mapFragment.getMapAsync(this);
    }
  }

  private void performAdditionalSetup() {
    pagerAdapter = new PolygonControlFragmentPagerAdapter(getSupportFragmentManager());
    pager = (ViewPager) findViewById(R.id.pager);
    pager.setAdapter(pagerAdapter);

    // onPageSelected(0) isn't invoked once views are ready, so post a Runnable to
    // refreshControlPanel() for the first time instead...
    pager.post(
        new Runnable() {
          @Override
          public void run() {
            refreshControlPanel();
          }
        });

    polygonRadio = (RadioGroup) findViewById(R.id.polygon_radio);
  }

  @Override
  public void onMapReady(GoogleMap map) {
    // For accessibility mode. Ideally this string would be localised.
    map.setContentDescription("Google Map with polygons.");

    northwestPolygon =
        map.addPolygon(
            new PolygonOptions()
                .addAll(createRectangle(new LatLng(-20, 130), 5, 5))
                .addHole(createRectangle(new LatLng(-22, 128), 1, 1))
                .addHole(createRectangle(new LatLng(-18, 133), 0.5, 1.5))
                .fillColor(Color.CYAN)
                .strokeColor(Color.BLUE)
                .strokeWidth(5)
                .strokePattern(Arrays.<PatternItem>asList(new Dot(), new Gap(20.0f)))
                .clickable(true));

    int radius = 5;
    southeastPolygon =
        map.addPolygon(
            new PolygonOptions()
                .add(new LatLng(SYDNEY.latitude + radius, SYDNEY.longitude + radius))
                .add(new LatLng(SYDNEY.latitude + radius, SYDNEY.longitude - radius))
                .add(new LatLng(SYDNEY.latitude - radius, SYDNEY.longitude - radius))
                .add(new LatLng(SYDNEY.latitude - radius, SYDNEY.longitude))
                .add(new LatLng(SYDNEY.latitude - radius, SYDNEY.longitude + radius))
                .add(new LatLng(SYDNEY.latitude + radius, SYDNEY.longitude + radius))
                .strokePattern(Arrays.<PatternItem>asList(new Dash(45.0f), new Gap(10.0f)))
                .strokeColor(Color.RED)
                .strokeWidth(5));

    centralPolygon =
        map.addPolygon(
            new PolygonOptions()
                .addAll(createRectangle(new LatLng(-27, 140), 10, 7))
                .fillColor(Color.MAGENTA)
                .strokeColor(Color.GREEN));

    worldWrappedPolygon =
        map.addPolygon(
            new PolygonOptions()
                .fillColor(Color.BLUE)
                .addAll(createWorldWrappedRectangle())
                .strokeColor(Color.CYAN)
                .strokeWidth(3));

    map.moveCamera(CameraUpdateFactory.newLatLng(SYDNEY));
    selectedPolygon = centralPolygon;
    polygonRadio.check(R.id.polygon_radio_central);

    pager.setOnPageChangeListener(this);
    polygonRadio.setOnCheckedChangeListener(this);
    map.setOnPolygonClickListener(this);
  }

  @Override
  public void onPolygonClick(Polygon polygon) {
    // Flip the r, g and b components of the polygon's stroke color.
    int strokeColor = polygon.getStrokeColor() ^ 0x00ffffff;
    polygon.setStrokeColor(strokeColor);
    refreshControlPanel();
  }

  @Override
  public void onCheckedChanged(RadioGroup group, int checkedId) {
    if (checkedId == R.id.polygon_radio_northwest) {
      selectedPolygon = northwestPolygon;
    } else if (checkedId == R.id.polygon_radio_central) {
      selectedPolygon = centralPolygon;
    } else if (checkedId == R.id.polygon_radio_southeast) {
      selectedPolygon = southeastPolygon;
    } else if (checkedId == R.id.polygon_radio_world_wrapped) {
      selectedPolygon = worldWrappedPolygon;
    }
    refreshControlPanel();
  }

  @Override
  public void onPageSelected(int position) {
    refreshControlPanel();
  }

  private void refreshControlPanel() {
    PolygonControlFragment fragment = pagerAdapter.getFragmentAtPosition(pager.getCurrentItem());
    if (fragment != null) {
      fragment.setPolygon(selectedPolygon);
    }
  }

  @Override
  public void onPageScrollStateChanged(int state) {
    // Don't do anything here.
  }

  @Override
  public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    // Don't do anything here.
  }

  private List<LatLng> createRectangle(LatLng center, double halfWidth, double halfHeight) {
    return Arrays.asList(
        new LatLng(center.latitude - halfHeight, center.longitude - halfWidth),
        new LatLng(center.latitude - halfHeight, center.longitude + halfWidth),
        new LatLng(center.latitude + halfHeight, center.longitude + halfWidth),
        new LatLng(center.latitude + halfHeight, center.longitude - halfWidth),
        new LatLng(center.latitude - halfHeight, center.longitude - halfWidth));
  }

  private List<LatLng> createWorldWrappedRectangle() {
    // The lower line of this world-wrapping rectangle follows the equation y = mx + b, where
    // the slope (m) is -0.1 and the y-intercept (b) is 45. The upper portion follows the same
    // equation, except with 55 as the y-intercept. The following points are ordered from the
    // lower leftmost point to the rightmost, followed by the upper rightmost point to the upper
    // leftmost.
    return Arrays.asList(
        new LatLng(51, -60),
        new LatLng(45, 1),
        new LatLng(39, 62),
        new LatLng(33, 123),
        new LatLng(27, -176),
        new LatLng(21, -115),
        new LatLng(14, -54),
        new LatLng(8, 7),
        new LatLng(2, 68),
        new LatLng(-4, 129),
        new LatLng(-10, -170),
        new LatLng(-16, -109),
        new LatLng(-6, -109),
        new LatLng(0, -170),
        new LatLng(6, 129),
        new LatLng(12, 68),
        new LatLng(18, 7),
        new LatLng(24, -54),
        new LatLng(31, -115),
        new LatLng(37, -176),
        new LatLng(43, 123),
        new LatLng(49, 62),
        new LatLng(55, 1),
        new LatLng(61, -60));
  }
}
