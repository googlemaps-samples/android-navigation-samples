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
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowCloseListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.navigation.SupportNavigationFragment;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** This shows how to place markers on a map. */
public class MarkerDemoActivity extends AppCompatActivity
    implements OnMarkerClickListener,
        OnInfoWindowClickListener,
        OnInfoWindowLongClickListener,
        OnInfoWindowCloseListener,
        OnMarkerDragListener,
        OnSeekBarChangeListener,
        OnMapAndViewReadyListener.OnGlobalLayoutAndMapReadyListener {

  private static final String TAG = MarkerDemoActivity.class.getSimpleName();
  private static final LatLng ADELAIDE = new LatLng(-34.92873, 138.59995);
  private static final LatLng BRISBANE = new LatLng(-27.47093, 153.0235);
  private static final LatLng DARWIN = new LatLng(-12.459501, 130.839915);
  private static final LatLng MELBOURNE = new LatLng(-37.81319, 144.96298);
  private static final LatLng PERTH = new LatLng(-31.952854, 115.857342);
  private static final LatLng SYDNEY = new LatLng(-33.87365, 151.20689);

  /** Toggles the given string between "Title case" and "UPPER CASE". */
  @Nullable
  private static String toggleCase(@Nullable String str) {
    if ((str == null) || (str.length() < 2)) {
      return str;
    }

    if (Character.isUpperCase(str.charAt(1))) {
      return str.substring(0, 1) + str.substring(1).toLowerCase(Locale.getDefault());
    } else {
      return str.toUpperCase(Locale.getDefault());
    }
  }

  /** Demonstrates customizing the info window and/or its contents. */
  class CustomInfoWindowAdapter implements InfoWindowAdapter {
    // These a both viewgroups containing an ImageView with id "badge" and two TextViews with id
    // "title" and "snippet".
    private final View window;
    private final View contents;

    CustomInfoWindowAdapter() {
      //noinspection AndroidLintInflateParams
      window = getLayoutInflater().inflate(R.layout.custom_info_window, null);
      //noinspection AndroidLintInflateParams
      contents = getLayoutInflater().inflate(R.layout.custom_info_contents, null);
    }

    @Nullable
    @SuppressWarnings({"nullness:override.return.invalid", "nullness:return.type.incompatible"})
    public View getInfoWindow(Marker marker) {
      if (options.getCheckedRadioButtonId() != R.id.custom_info_window) {
        // This means that getInfoContents will be called.
        return null;
      }
      if (perth != null && marker.equals(perth)) {
        // Perth should have no info window at all.
        return null;
      }
      render(marker, window);
      return window;
    }

    @Nullable
    @SuppressWarnings({"nullness:override.return.invalid", "nullness:return.type.incompatible"})
    public View getInfoContents(Marker marker) {
      if (options.getCheckedRadioButtonId() != R.id.custom_info_contents) {
        // This means that the default info contents will be used.
        return null;
      }
      if (perth != null && marker.equals(perth)) {
        // Perth should have no info window at all.
        return null;
      }
      render(marker, contents);
      return contents;
    }

    private void render(Marker marker, View view) {
      int badge;
      // Use the equals() method on a Marker to check for equals.  Do not use ==.
      if (brisbane != null && marker.equals(brisbane)) {
        badge = R.drawable.badge_qld;
      } else if (adelaide != null && marker.equals(adelaide)) {
        badge = R.drawable.badge_sa;
      } else if (sydney != null && marker.equals(sydney)) {
        badge = R.drawable.badge_nsw;
      } else if (melbourne != null && marker.equals(melbourne)) {
        badge = R.drawable.badge_victoria;
      } else {
        badge = 0;
      }
      ImageView imageView = (ImageView) view.findViewById(R.id.badge);
      if (badge == 0) {
        // Passing null to setImageDrawable will clear the image view.
        imageView.setImageDrawable(null);
      } else {
        imageView.setImageResource(badge);
      }

      String title = marker.getTitle();
      TextView titleUi = ((TextView) view.findViewById(R.id.title));
      if (title != null) {
        // Spannable string allows us to edit the formatting of the text.
        SpannableString titleText = new SpannableString(title);
        titleText.setSpan(new ForegroundColorSpan(Color.RED), 0, titleText.length(), 0);
        titleUi.setText(titleText);
      } else {
        titleUi.setText("");
      }

      String snippet = marker.getSnippet();
      TextView snippetUi = ((TextView) view.findViewById(R.id.snippet));
      if (snippet != null && snippet.length() > 12) {
        SpannableString snippetText = new SpannableString(snippet);
        snippetText.setSpan(new ForegroundColorSpan(Color.MAGENTA), 0, 10, 0);
        snippetText.setSpan(new ForegroundColorSpan(Color.BLUE), 12, snippet.length(), 0);
        snippetUi.setText(snippetText);
      } else {
        snippetUi.setText("");
      }
    }
  }

  private GoogleMap map;
  @Nullable private Marker perth;
  @Nullable private Marker sydney;
  @Nullable private Marker brisbane;
  @Nullable public Marker adelaide; /* Used by MarkerDemoActivityTest. */
  @Nullable private Marker melbourne;

  /**
   * Keeps track of the last selected marker (though it may no longer be selected). This is useful
   * for refreshing the info window.
   */
  private Marker lastSelectedMarker;

  private final List<Marker> markerRainbow = new ArrayList<Marker>();

  private TextView topText;
  private SeekBar rotationBar;
  private SeekBar alphaSeekBar;
  private CheckBox flatBox;
  private CheckBox visibleBox;
  private RadioGroup options;

  private final Random random = new Random();

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getIntent()
        .getBooleanExtra(
            ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
            /* defaultValue= */ false)) {
      setContentView(R.layout.marker_demo_nav_flavor);
      performAdditionalSetup();
      SupportNavigationFragment navFragment =
          (SupportNavigationFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      new OnMapAndViewReadyListener(navFragment, this);
    } else {
      setContentView(R.layout.marker_demo_maps_flavor);
      performAdditionalSetup();
      SupportMapFragment mapFragment =
          (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

      if (mapFragment == null) {
        Log.e(TAG, "mapFragment is null, not registering listener.");
        return;
      }

      new OnMapAndViewReadyListener(mapFragment, this);
    }
  }

  private void performAdditionalSetup() {
    topText = (TextView) findViewById(R.id.top_text);

    rotationBar = (SeekBar) findViewById(R.id.rotationSeekBar);
    rotationBar.setMax(360);
    rotationBar.setOnSeekBarChangeListener(this);

    alphaSeekBar = (SeekBar) findViewById(R.id.alphaSeekBar);
    alphaSeekBar.setMax(100);
    alphaSeekBar.setOnSeekBarChangeListener(this);
    alphaSeekBar.setProgress(100);

    flatBox = (CheckBox) findViewById(R.id.flat);
    visibleBox = (CheckBox) findViewById(R.id.visible);
    visibleBox.setChecked(true);

    options = (RadioGroup) findViewById(R.id.custom_info_window_options);
    options.setOnCheckedChangeListener(
        new OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (lastSelectedMarker != null && lastSelectedMarker.isInfoWindowShown()) {
              // Refresh the info window when the info window's content has changed.
              lastSelectedMarker.showInfoWindow();
            }
          }
        });
  }

  @Override
  public void onMapReady(GoogleMap map) {
    this.map = map;

    // Hide the zoom controls as the button panel will cover it.
    this.map.getUiSettings().setZoomControlsEnabled(false);

    // Add lots of markers to the map.
    addMarkersToMap();

    // Setting an info window adapter allows us to change the both the contents and look of the
    // info window.
    this.map.setInfoWindowAdapter(new CustomInfoWindowAdapter());

    // Set listeners for marker events.  See the bottom of this class for their behavior.
    this.map.setOnMarkerClickListener(this);
    this.map.setOnInfoWindowClickListener(this);
    this.map.setOnInfoWindowLongClickListener(this);
    this.map.setOnInfoWindowCloseListener(this);
    this.map.setOnMarkerDragListener(this);

    // Override the default content description on the view, for accessibility mode.
    // Ideally this string would be localised.
    map.setContentDescription("Map with lots of markers.");

    LatLngBounds bounds =
        new LatLngBounds.Builder()
            .include(PERTH)
            .include(ADELAIDE)
            .include(MELBOURNE)
            .include(SYDNEY)
            .include(DARWIN)
            .include(BRISBANE)
            .build();
    this.map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
  }

  private void addMarkersToMap() {
    // Uses a colored icon.
    brisbane =
        map.addMarker(
            new MarkerOptions()
                .position(BRISBANE)
                .title("Brisbane")
                .snippet("Population: 2,074,200")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

    // Uses a custom icon with the info window popping out of the center of the icon.
    sydney =
        map.addMarker(
            new MarkerOptions()
                .position(SYDNEY)
                .title("Sydney")
                .snippet("Population: 4,627,300")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.arrow))
                .infoWindowAnchor(0.5f, 0.5f));

    // Creates a draggable marker. Long press to drag.
    melbourne =
        map.addMarker(
            new MarkerOptions()
                .position(MELBOURNE)
                .title("Melbourne")
                .snippet("Population: 4,137,400")
                .draggable(true));

    // A few more markers for good measure.
    perth = map.addMarker(new MarkerOptions().position(PERTH));
    adelaide =
        map.addMarker(
            new MarkerOptions()
                .position(ADELAIDE)
                .title("Adelaide")
                .snippet("Population: 1,213,000"));

    // Creates a marker rainbow demonstrating how to create default marker icons of different
    // hues (colors).
    float alpha = alphaSeekBar.getProgress() / 100.0f;
    float rotation = rotationBar.getProgress();
    boolean flat = flatBox.isChecked();
    boolean visible = visibleBox.isChecked();
    int numMarkersInRainbow = 12;
    for (int i = 0; i < numMarkersInRainbow; i++) {
      Marker marker =
          map.addMarker(
              new MarkerOptions()
                  .position(
                      new LatLng(
                          -30 + 10 * Math.sin(i * Math.PI / (numMarkersInRainbow - 1)),
                          135 - 10 * Math.cos(i * Math.PI / (numMarkersInRainbow - 1))))
                  .title("Marker " + i)
                  .snippet("Snippet " + i)
                  .icon(BitmapDescriptorFactory.defaultMarker(i * 360 / numMarkersInRainbow))
                  .flat(flat)
                  .visible(visible)
                  .alpha(alpha)
                  .rotation(rotation));

      if (marker == null) {
        Log.e(TAG, String.format("Unable to add rainbow marker: %s.", i));
        continue;
      }

      markerRainbow.add(marker);
    }
  }

  private boolean checkReady() {
    if (map == null) {
      Toast.makeText(this, R.string.map_not_ready, Toast.LENGTH_SHORT).show();
      return false;
    }
    return true;
  }

  /** Called when the Clear button is clicked. */
  public void onClearMap(View view) {
    if (!checkReady()) {
      return;
    }
    map.clear();
  }

  /** Called when the Reset button is clicked. */
  public void onResetMap(View view) {
    if (!checkReady()) {
      return;
    }
    // Clear the map because we don't want duplicates of the markers.
    map.clear();
    addMarkersToMap();
  }

  /** Called when the flat checkbox is toggled. */
  public void onToggleFlat(View view) {
    if (!checkReady()) {
      return;
    }
    boolean flat = flatBox.isChecked();
    for (Marker marker : markerRainbow) {
      marker.setFlat(flat);
    }
  }

  /** Called when the visible checkbox is toggled. */
  public void onToggleVisible(View view) {
    if (!checkReady()) {
      return;
    }
    boolean visible = visibleBox.isChecked();
    for (Marker marker : markerRainbow) {
      marker.setVisible(visible);
    }
  }

  /** Called when the Change Title and Snippet button is clicked. */
  public void onChangeTitleAndSnippet(View view) {
    for (Marker marker : markerRainbow) {
      marker.setTitle(toggleCase(marker.getTitle()));
      marker.setSnippet(toggleCase(marker.getSnippet()));
    }
  }

  /** Called when the Show Info Window button is clicked. */
  public void onShowInfoWindow(View view) {
    if (sydney == null) {
      return;
    }
    sydney.showInfoWindow();
  }

  /** Called when the Hide Info Window button is clicked. */
  public void onHideInfoWindow(View view) {
    if (sydney == null) {
      return;
    }
    sydney.hideInfoWindow();
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    if (!checkReady()) {
      return;
    }
    if (seekBar == rotationBar) {
      float rotation = seekBar.getProgress();
      for (Marker marker : markerRainbow) {
        marker.setRotation(rotation);
      }
    }

    if (seekBar == alphaSeekBar) {
      float alpha = seekBar.getProgress() / 100.0f;
      for (Marker marker : markerRainbow) {
        marker.setAlpha(alpha);
      }
    }
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {
    // Do nothing.
  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {
    // Do nothing.
  }

  //
  // Marker related listeners.
  //

  @Override
  public boolean onMarkerClick(final Marker marker) {
    if (perth != null && marker.equals(perth)) {
      // This causes the marker at Perth to bounce into position when it is clicked.
      final Handler handler = new Handler();
      final long start = SystemClock.uptimeMillis();
      final long duration = 1500;

      final Interpolator interpolator = new BounceInterpolator();

      handler.post(
          new Runnable() {
            @Override
            public void run() {
              long elapsed = SystemClock.uptimeMillis() - start;
              float t = Math.max(1 - interpolator.getInterpolation((float) elapsed / duration), 0);
              marker.setAnchor(0.5f, 1.0f + 2 * t);

              if (t > 0.0) {
                // Post again 16ms later.
                handler.postDelayed(this, 16);
              }
            }
          });
    } else if (adelaide != null && marker.equals(adelaide)) {
      // This causes the marker at Adelaide to change color and alpha.
      marker.setIcon(BitmapDescriptorFactory.defaultMarker(random.nextFloat() * 360));
      marker.setAlpha(random.nextFloat());
    } else if (brisbane != null && marker.equals(brisbane)) {
      marker.setPosition(
          new LatLng(marker.getPosition().latitude + 2, marker.getPosition().longitude));
    }

    lastSelectedMarker = marker;
    // We return false to indicate that we have not consumed the event and that we wish
    // for the default behavior to occur (which is for the camera to move such that the
    // marker is centered and for the marker's info window to open, if it has one).
    return false;
  }

  @Override
  public void onInfoWindowClick(Marker marker) {
    Toast.makeText(this, "Click Info Window", Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onInfoWindowLongClick(Marker marker) {
    Toast.makeText(this, "Long Click Info Window", Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onInfoWindowClose(Marker marker) {
    Toast.makeText(this, "Close Info Window", Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onMarkerDragStart(Marker marker) {
    topText.setText(R.string.marker_demo_on_marker_drag_start);
  }

  @Override
  public void onMarkerDragEnd(Marker marker) {
    topText.setText(R.string.marker_demo_on_marker_drag_end);
  }

  @Override
  public void onMarkerDrag(Marker marker) {
    Resources res = getResources();
    String text = res.getString(R.string.marker_demo_on_marker_drag, marker.getPosition());
    topText.setText(text);
  }
}
