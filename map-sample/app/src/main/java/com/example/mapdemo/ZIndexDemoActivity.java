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
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mapdemo.OnMapAndViewReadyListener.OnGlobalLayoutAndMapReadyListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCircleClickListener;
import com.google.android.gms.maps.GoogleMap.OnGroundOverlayClickListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnPolygonClickListener;
import com.google.android.gms.maps.GoogleMap.OnPolylineClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.libraries.navigation.SupportNavigationFragment;

/** This shows how different z-indices are shown on the map and how click handling is handled. */
public class ZIndexDemoActivity extends AppCompatActivity
    implements OnCircleClickListener,
        OnGroundOverlayClickListener,
        OnInfoWindowClickListener,
        OnMapClickListener,
        OnMarkerClickListener,
        OnGlobalLayoutAndMapReadyListener,
        OnPolygonClickListener,
        OnPolylineClickListener,
        RadioGroup.OnCheckedChangeListener,
        TextView.OnEditorActionListener {

  private static final MoonTileProvider MOON_TILE_PROVIDER = new MoonTileProvider();

  private static final LatLng MARKER_LOCATION = new LatLng(-29.425, 137.02677172);
  private static final LatLng BRISBANE = new LatLng(-27.47093, 153.0235);
  private static final LatLng DARWIN = new LatLng(-12.425892, 130.86327);
  private static final LatLng HOBART = new LatLng(-42.8823388, 147.311042);
  private static final LatLng PERTH = new LatLng(-31.952854, 115.857342);

  // Radio button indices. To ease reading.
  private static final int CIRCLE = 0;
  private static final int GROUND_OVERLAY = 1;
  private static final int MARKER_BLUE = 2;
  private static final int MARKER_RED = 3;
  private static final int POLYGON = 4;
  private static final int POLYLINE = 5;
  private static final int TILE_OVERLAY_COORDS = 6;
  private static final int TILE_OVERLAY_MOON = 7;

  private int selectedRadio = 0;

  private GoogleMap map = null;

  private Circle circle;
  private GroundOverlay groundOverlay;
  private Marker markerBlue;
  private Marker markerRed;
  private Polygon polygon;
  private Polyline polyline;
  private TileOverlay tileOverlayCoords;
  private TileOverlay tileOverlayMoon;

  private CheckBox clickabilityCheckBox = null;
  private CheckBox visibilityCheckBox = null;
  private StringBuilder tapLogBuilder = new StringBuilder();
  private ScrollView tapLogScroll;
  private TextView tapLog;
  private EditText zIndexPicker;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getIntent()
        .getBooleanExtra(
            ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
            /* defaultValue= */ false)) {
      setContentView(R.layout.z_index_demo_nav_flavor);
      performAdditionalSetup();
      SupportNavigationFragment navFragment =
          (SupportNavigationFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      new OnMapAndViewReadyListener(navFragment, this);
    } else {
      setContentView(R.layout.z_index_demo_maps_flavor);
      performAdditionalSetup();
      SupportMapFragment mapFragment =
          (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      new OnMapAndViewReadyListener(mapFragment, this);
    }
  }

  private void performAdditionalSetup() {
    // Make keyboard appear on top of map instead of resizing the map.
    getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_ADJUST_PAN);

    setupUiWidgets();
  }

  @Override
  public void onMapReady(GoogleMap map) {
    this.map = map;
    addObjectsToMap();

    map.setOnCircleClickListener(this);
    map.setOnGroundOverlayClickListener(this);
    map.setOnInfoWindowClickListener(this);
    map.setOnMapClickListener(this);
    map.setOnMarkerClickListener(this);
    map.setOnPolygonClickListener(this);
    map.setOnPolylineClickListener(this);

    map.setContentDescription(
        "Map with a circle, ground overlay, marker, polygon, polyline and"
            + " tile overlays at various changeable z-indices. Tapping on the map will activate"
            + " the click handler of the object with the highest z-index at the point tapped.");

    // Create bounds that include all locations of the map.
    LatLngBounds bounds =
        new LatLngBounds.Builder()
            .include(BRISBANE)
            .include(DARWIN)
            .include(HOBART)
            .include(PERTH)
            .build();
    map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

    RadioGroup radios = (RadioGroup) findViewById(R.id.object_radio_group);
    ((RadioButton) radios.getChildAt(0)).setChecked(true);
    // onCheckedChanged not called automatically when a radio is programmatically checked.
    // Force call it.
    onCheckedChanged(radios, radios.getCheckedRadioButtonId());
  }

  private void setupUiWidgets() {
    clickabilityCheckBox = (CheckBox) findViewById(R.id.clickability_box);
    visibilityCheckBox = (CheckBox) findViewById(R.id.visibility_box);
    tapLogScroll = (ScrollView) findViewById(R.id.tap_log_scroll);
    tapLog = (TextView) findViewById(R.id.tap_log);
    RadioGroup radios = (RadioGroup) findViewById(R.id.object_radio_group);
    zIndexPicker = (EditText) findViewById(R.id.z_index_picker);

    clickabilityCheckBox.setChecked(true);
    // Stop zIndexPicker from having focus by default.
    visibilityCheckBox.requestFocus();
    visibilityCheckBox.setChecked(true);

    zIndexPicker.setOnEditorActionListener(this);
    radios.setOnCheckedChangeListener(this);
  }

  /** Updates the Z-index for the currently selected radio button. */
  private void updateZIndex(float newZIndex) {
    switch (selectedRadio) {
      case CIRCLE:
        circle.setZIndex(newZIndex);
        break;
      case GROUND_OVERLAY:
        groundOverlay.setZIndex(newZIndex);
        break;
      case MARKER_BLUE:
        markerBlue.setZIndex(newZIndex);
        break;
      case MARKER_RED:
        markerRed.setZIndex(newZIndex);
        break;
      case POLYLINE:
        polyline.setZIndex(newZIndex);
        break;
      case POLYGON:
        polygon.setZIndex(newZIndex);
        break;
      case TILE_OVERLAY_COORDS:
        tileOverlayCoords.setZIndex(newZIndex);
        break;
      case TILE_OVERLAY_MOON:
        tileOverlayMoon.setZIndex(newZIndex);
        break;
      default:
        // fall out
    }
  }

  private void addObjectsToMap() {
    tileOverlayMoon =
        map.addTileOverlay(new TileOverlayOptions().tileProvider(MOON_TILE_PROVIDER).zIndex(-1.0f));
    tileOverlayCoords =
        map.addTileOverlay(
            new TileOverlayOptions()
                .tileProvider(
                    new TileCoordsTileProvider(
                        getApplicationContext().getResources().getDisplayMetrics().density))
                .zIndex(-1.0f));

    circle =
        map.addCircle(
            new CircleOptions()
                .center(MARKER_LOCATION)
                .radius(1500000)
                .fillColor(Color.argb(150, 66, 173, 244))
                .strokeColor(Color.rgb(66, 173, 244))
                .clickable(true));

    groundOverlay =
        map.addGroundOverlay(
            new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.harbour_bridge))
                .position(BRISBANE, 3500000)
                .clickable(true));

    markerBlue =
        map.addMarker(
            new MarkerOptions()
                .title("Blue Marker")
                .position(MARKER_LOCATION)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

    markerRed =
        map.addMarker(
            new MarkerOptions()
                .title("Red Marker")
                .position(new LatLng(MARKER_LOCATION.latitude, MARKER_LOCATION.longitude + 0.5)));

    int offset = 20;
    polygon =
        map.addPolygon(
            new PolygonOptions()
                .add(
                    new LatLng(DARWIN.latitude, DARWIN.longitude), // Top Left
                    new LatLng(DARWIN.latitude, DARWIN.longitude + offset), // Top Right
                    new LatLng(DARWIN.latitude - offset, DARWIN.longitude + offset), // Bottom
                    // Right
                    new LatLng(DARWIN.latitude - offset, DARWIN.longitude)) // Bottom Left
                .fillColor(Color.argb(150, 34, 173, 24))
                .strokeColor(Color.rgb(34, 173, 24))
                .clickable(true));

    polyline =
        map.addPolyline(
            new PolylineOptions()
                .add(PERTH, BRISBANE)
                .color(Color.rgb(103, 24, 173))
                .width(30)
                .clickable(true));
  }

  /** Takes a character sequence and appends it to the tap log. Forces a refresh and a scroll. */
  private void appendToTapLog(CharSequence text) {
    if (tapLogBuilder.length() != 0) {
      tapLogBuilder.append('\n');
    }

    tapLogBuilder.append(text);
    tapLog.setText(tapLogBuilder);
    tapLogScroll.post(
        new Runnable() {
          @Override
          public void run() {
            tapLogScroll.fullScroll(ScrollView.FOCUS_DOWN);
          }
        });
  }

  private boolean checkReady() {
    if (map == null) {
      Toast.makeText(this, R.string.map_not_ready, Toast.LENGTH_SHORT).show();
      return false;
    }
    return true;
  }

  public void onVisibilityCheckboxClicked(View view) {
    if (!checkReady()) {
      return;
    }

    boolean shouldBeVisible = visibilityCheckBox.isChecked();
    switch (selectedRadio) {
      case CIRCLE:
        circle.setVisible(shouldBeVisible);
        break;
      case GROUND_OVERLAY:
        groundOverlay.setVisible(shouldBeVisible);
        break;
      case MARKER_BLUE:
        // Markers have no clickability setting, but visible markers can always be clicked
        // on, and invisible markers cannot be clicked on. Update the clickability checkbox
        // as well to reflect this.
        clickabilityCheckBox.setChecked(shouldBeVisible);
        markerBlue.setVisible(shouldBeVisible);
        break;
      case MARKER_RED:
        // Markers have no clickability setting, but visible markers can always be clicked
        // on, and invisible markers cannot be clicked on. Update the clickability checkbox
        // as well to reflect this.
        clickabilityCheckBox.setChecked(shouldBeVisible);
        markerRed.setVisible(shouldBeVisible);
        break;
      case POLYLINE:
        polyline.setVisible(shouldBeVisible);
        break;
      case POLYGON:
        polygon.setVisible(shouldBeVisible);
        break;
      case TILE_OVERLAY_COORDS:
        tileOverlayCoords.setVisible(shouldBeVisible);
        break;
      case TILE_OVERLAY_MOON:
        tileOverlayMoon.setVisible(shouldBeVisible);
        break;
      default:
        // fall out.
    }
  }

  public void onClickabilityCheckboxClicked(View view) {
    if (!checkReady()) {
      return;
    }

    boolean shouldBeClickable = clickabilityCheckBox.isChecked();
    switch (selectedRadio) {
      case CIRCLE:
        circle.setClickable(shouldBeClickable);
        break;
      case GROUND_OVERLAY:
        groundOverlay.setClickable(shouldBeClickable);
        break;
      case POLYLINE:
        polyline.setClickable(shouldBeClickable);
        break;
      case POLYGON:
        polygon.setClickable(shouldBeClickable);
        break;
      default:
        // Markers and TileOverlays have no clickability setting, so the checkbox is
        // disabled for them.
        // fall out.
    }
  }

  @Override
  public void onCheckedChanged(RadioGroup group, int checkedId) {
    if (!checkReady()) {
      return;
    }

    selectedRadio = group.indexOfChild(group.findViewById(checkedId));
    boolean isClickable = true;
    boolean clickableBoxIsEnabled = true;
    boolean isVisible = true;
    float currentZIndex = 0;
    switch (selectedRadio) {
      case CIRCLE:
        isClickable = circle.isClickable();
        isVisible = circle.isVisible();
        currentZIndex = circle.getZIndex();
        break;
      case GROUND_OVERLAY:
        isClickable = groundOverlay.isClickable();
        isVisible = groundOverlay.isVisible();
        currentZIndex = groundOverlay.getZIndex();
        break;
      case MARKER_BLUE:
        // Clickability depends on visibility for markers.
        isClickable = markerBlue.isVisible();
        clickableBoxIsEnabled = false;
        isVisible = markerBlue.isVisible();
        currentZIndex = markerBlue.getZIndex();
        break;
      case MARKER_RED:
        // Clickability depends on visibility for markers.
        isClickable = markerRed.isVisible();
        clickableBoxIsEnabled = false;
        isVisible = markerRed.isVisible();
        currentZIndex = markerRed.getZIndex();
        break;
      case POLYGON:
        isClickable = polygon.isClickable();
        isVisible = polygon.isVisible();
        currentZIndex = polygon.getZIndex();
        break;
      case POLYLINE:
        isClickable = polyline.isClickable();
        isVisible = polyline.isVisible();
        currentZIndex = polyline.getZIndex();
        break;
      case TILE_OVERLAY_COORDS:
        // Tile overlays are never clickable.
        isClickable = false;
        clickableBoxIsEnabled = false;
        isVisible = tileOverlayCoords.isVisible();
        currentZIndex = tileOverlayCoords.getZIndex();
        break;
      case TILE_OVERLAY_MOON:
        // Tile overlays are never clickable.
        isClickable = false;
        clickableBoxIsEnabled = false;
        isVisible = tileOverlayMoon.isVisible();
        currentZIndex = tileOverlayMoon.getZIndex();
        break;
      default:
        // fall out.
    }
    zIndexPicker.setText(String.valueOf(currentZIndex));
    clickabilityCheckBox.setChecked(isClickable);
    clickabilityCheckBox.setEnabled(clickableBoxIsEnabled);
    visibilityCheckBox.setChecked(isVisible);
  }

  @Override
  public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
    if (!checkReady()) {
      return false;
    }

    try {
      updateZIndex(Float.parseFloat(String.valueOf(textView.getText())));
    } catch (NumberFormatException exception) {
      appendToTapLog(
          "The Z-index that was entered [" + textView.getText() + "] was an invalid float.\n");
    }
    return false;
  }

  //
  // Click event listeners.
  //

  @Override
  public void onCircleClick(Circle circle) {
    appendToTapLog("Circle was clicked");
  }

  @Override
  public void onGroundOverlayClick(GroundOverlay groundOverlay) {
    appendToTapLog("GroundOverlay was clicked");
  }

  @Override
  public void onInfoWindowClick(Marker infoWindow) {
    appendToTapLog("InfoWindow was clicked");
  }

  @Override
  public void onMapClick(LatLng point) {
    appendToTapLog("Map was clicked at " + point);
  }

  @Override
  public boolean onMarkerClick(final Marker marker) {
    appendToTapLog("Marker was clicked");
    return false;
  }

  @Override
  public void onPolygonClick(Polygon polygon) {
    appendToTapLog("Polygon was clicked");
  }

  @Override
  public void onPolylineClick(Polyline polyline) {
    appendToTapLog("Polyline was clicked");
  }
}
