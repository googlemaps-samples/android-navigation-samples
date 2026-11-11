/*
 * Copyright 2026 Google LLC
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

import static com.google.common.truth.Truth.assertThat;
import static com.google.maps.android.robolectric.truth.MapsTruth.assertThat;

import android.content.Intent;
import android.widget.CheckBox;
import androidx.appcompat.app.AppCompatActivity;
import androidx.test.core.app.ApplicationProvider;
import com.example.mapdemo.testing.NavDelegateReflection;
import com.example.mapdemo.testing.NavMapsRobolectricTest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.robolectric.truth.Distance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/**
 * Java headless Robolectric JVM tests for {@code map-sample} demo activities using <b>Google
 * Truth</b> ({@link com.google.common.truth.Truth#assertThat}) and {@code android-maps-testing}'s
 * {@link com.google.maps.android.robolectric.truth.MapsTruth#assertThat(GoogleMap)} domain
 * subjects.
 *
 * <p>Validates Java interoperability across {@link BasicMapDemoActivity}, {@link
 * CameraDemoActivity}, {@link CameraClampingDemoActivity}, {@link CircleDemoActivity}, {@link
 * MarkerDemoActivity}, {@link PolygonDemoActivity}, {@link PolylineDemoActivity}, {@link
 * TagsDemoActivity}, {@link UiSettingsDemoActivity}, and {@link LayersDemoActivity}.
 */
@RunWith(RobolectricTestRunner.class)
public class MapActivitiesTruthTest extends NavMapsRobolectricTest {

  private static <T extends AppCompatActivity> T launchActivity(
      Class<T> activityClass, boolean useNavFlavor) {
    Intent intent = new Intent(ApplicationProvider.getApplicationContext(), activityClass);
    intent.putExtra(ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO, useNavFlavor);
    return Robolectric.buildActivity(activityClass, intent).setup().get();
  }

  private static GoogleMap extractMap(AppCompatActivity activity) {
    Object fragment = activity.getSupportFragmentManager().findFragmentById(R.id.map);
    return NavDelegateReflection.extractActivityMap(fragment);
  }

  @Test
  public void basicMapDemo_hasMarkerAtOriginWithMapsTruthSubject() {
    BasicMapDemoActivity activity = launchActivity(BasicMapDemoActivity.class, false);
    GoogleMap map = extractMap(activity);

    assertThat(map).hasMarkerCount(1);
    assertThat(map).hasMarkerWithTitle("Marker").isAt(new LatLng(0, 0)).isVisible();
  }

  @Test
  public void cameraDemo_movesBetweenBondiAndSydneyWithExpectedZoomBearingAndTilt() {
    LatLng bondi = new LatLng(-33.891614, 151.276417);
    LatLng sydney = new LatLng(-33.87365, 151.20689);

    CameraDemoActivity activity = launchActivity(CameraDemoActivity.class, false);
    GoogleMap map = extractMap(activity);

    assertThat(map).isCenteredOn(sydney, new Distance(5.0)).hasZoom(10f);

    activity.onGoToBondi(null);
    assertThat(map).isCenteredOn(bondi, new Distance(5.0)).hasZoom(15.5f);
    assertThat(map.getCameraPosition().bearing).isEqualTo(300f);
    assertThat(map.getCameraPosition().tilt).isEqualTo(50f);

    activity.onGoToSydney(null);
    assertThat(map).isCenteredOn(sydney, new Distance(5.0)).hasZoom(15.5f);
    assertThat(map.getCameraPosition().tilt).isEqualTo(25f);
  }

  @Test
  public void circlePolygonAndPolylineDemos_populateExpectedOverlaysOnMap() {
    CircleDemoActivity circleActivity = launchActivity(CircleDemoActivity.class, false);
    GoogleMap circleMap = extractMap(circleActivity);
    assertThat(circleMap).hasCircleCount(1);
    assertThat(circleMap).hasMarkerCount(2);
    assertThat(circleMap).hasPolylineCount(1);
    assertThat(circleMap).hasContentDescription("Google Map with circles.");

    PolygonDemoActivity polygonActivity = launchActivity(PolygonDemoActivity.class, true);
    GoogleMap polygonMap = extractMap(polygonActivity);
    assertThat(polygonMap).hasPolygonCount(4);
    assertThat(polygonMap).hasContentDescription("Google Map with polygons.");

    PolylineDemoActivity polylineActivity = launchActivity(PolylineDemoActivity.class, false);
    GoogleMap polylineMap = extractMap(polylineActivity);
    assertThat(polylineMap).hasPolylineCount(4);
    assertThat(polylineMap).hasContentDescription("Google Map with polylines.");
  }

  @Test
  public void tagsAndUiSettingsDemos_configureUiSettingsAndOverlaysViaTruthSubjects() {
    LatLng hobart = new LatLng(-42.8823388, 147.311042);
    TagsDemoActivity tagsActivity = launchActivity(TagsDemoActivity.class, false);
    GoogleMap tagsMap = extractMap(tagsActivity);

    assertThat(tagsMap.getUiSettings().isZoomControlsEnabled()).isFalse();
    assertThat(tagsMap.getUiSettings().isScrollGesturesEnabled()).isFalse();
    assertThat(tagsMap.getUiSettings().isZoomGesturesEnabled()).isFalse();
    assertThat(tagsMap.getUiSettings().isTiltGesturesEnabled()).isFalse();
    assertThat(tagsMap.getUiSettings().isRotateGesturesEnabled()).isFalse();
    assertThat(tagsMap).hasCircleCount(1);
    assertThat(tagsMap).hasGroundOverlayCount(1);
    assertThat(tagsMap).hasMarkerNear(hobart);
    assertThat(tagsMap).hasPolygonCount(1);
    assertThat(tagsMap).hasPolylineCount(1);

    UiSettingsDemoActivity uiActivity = launchActivity(UiSettingsDemoActivity.class, false);
    GoogleMap uiMap = extractMap(uiActivity);
    assertThat(uiMap.getUiSettings().isZoomControlsEnabled()).isTrue();
    assertThat(uiMap.getUiSettings().isCompassEnabled()).isTrue();

    CheckBox unchecked = new CheckBox(uiActivity);
    unchecked.setChecked(false);
    uiActivity.setZoomButtonsEnabled(unchecked);
    uiActivity.setCompassEnabled(unchecked);

    assertThat(uiMap.getUiSettings().isZoomControlsEnabled()).isFalse();
    assertThat(uiMap.getUiSettings().isCompassEnabled()).isFalse();
  }

  @Test
  public void layersDemo_togglesTrafficAndBuildingsLayersViaMapsTruth() {
    LayersDemoActivity activity = launchActivity(LayersDemoActivity.class, false);
    GoogleMap map = extractMap(activity);

    assertThat(map.isTrafficEnabled()).isFalse();
    assertThat(map.isBuildingsEnabled()).isTrue();

    CheckBox trafficCheckbox = activity.findViewById(R.id.traffic);
    CheckBox buildingsCheckbox = activity.findViewById(R.id.buildings);

    trafficCheckbox.setChecked(true);
    activity.onTrafficToggled(trafficCheckbox);
    buildingsCheckbox.setChecked(false);
    activity.onBuildingsToggled(buildingsCheckbox);

    assertThat(map.isTrafficEnabled()).isTrue();
    assertThat(map.isBuildingsEnabled()).isFalse();
  }
}
