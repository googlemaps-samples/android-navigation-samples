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
import android.os.Parcel;
import android.os.Parcelable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.Nullable;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.navigation.SupportNavigationFragment;
import java.util.Random;

/**
 * This activity shows how to save the state of a MapFragment when the activity is recreated, like
 * after rotation of the device.
 */
public class SaveStateDemoActivity extends AppCompatActivity {

  /** Default marker position when the activity is first created. */
  private static final LatLng DEFAULT_MARKER_POSITION = new LatLng(48.858179, 2.294576);

  /** List of hues to use for the marker */
  private static final float[] MARKER_HUES =
      new float[] {
        BitmapDescriptorFactory.HUE_RED,
        BitmapDescriptorFactory.HUE_ORANGE,
        BitmapDescriptorFactory.HUE_YELLOW,
        BitmapDescriptorFactory.HUE_GREEN,
        BitmapDescriptorFactory.HUE_CYAN,
        BitmapDescriptorFactory.HUE_AZURE,
        BitmapDescriptorFactory.HUE_BLUE,
        BitmapDescriptorFactory.HUE_VIOLET,
        BitmapDescriptorFactory.HUE_MAGENTA,
        BitmapDescriptorFactory.HUE_ROSE,
      };

  // Bundle keys.
  private static final String MARKER_POSITION = "markerPosition";
  private static final String MARKER_INFO = "markerInfo";

  /** Extra info about a marker. */
  static class MarkerInfo implements Parcelable {

    public static final Parcelable.Creator<MarkerInfo> CREATOR =
        new Parcelable.Creator<MarkerInfo>() {
          @Override
          public MarkerInfo createFromParcel(Parcel in) {
            return new MarkerInfo(in);
          }

          @Override
          public MarkerInfo[] newArray(int size) {
            return new MarkerInfo[size];
          }
        };

    float hue;

    public MarkerInfo(float color) {
      hue = color;
    }

    private MarkerInfo(Parcel in) {
      hue = in.readFloat();
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeFloat(hue);
    }
  }

  /**
   * Example of a custom {@code MapFragment} showing how the position of a marker and other custom
   * {@link Parcelable}s objects can be saved after rotation of the device.
   */
  public static class SaveStateMapFragment extends SupportMapFragment
      implements OnMarkerClickListener, OnMarkerDragListener, OnMapReadyCallback {

    private LatLng markerPosition;
    private MarkerInfo markerInfo;
    private boolean moveCameraToMarker;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      if (savedInstanceState == null) {
        // Activity created for the first time.
        markerPosition = DEFAULT_MARKER_POSITION;
        markerInfo = new MarkerInfo(BitmapDescriptorFactory.HUE_RED);
        moveCameraToMarker = true;
      } else {
        // Extract the state of the MapFragment:
        markerPosition = savedInstanceState.getParcelable(MARKER_POSITION);
        markerInfo = savedInstanceState.getParcelable(MARKER_INFO);
        moveCameraToMarker = false; // The API will remember the last camera.
      }

      getMapAsync(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      outState.putParcelable(MARKER_POSITION, markerPosition);
      outState.putParcelable(MARKER_INFO, markerInfo);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
      float newHue = MARKER_HUES[new Random().nextInt(MARKER_HUES.length)];
      markerInfo.hue = newHue;
      marker.setIcon(BitmapDescriptorFactory.defaultMarker(newHue));
      return true;
    }

    @Override
    public void onMapReady(GoogleMap map) {
      MarkerOptions markerOptions =
          new MarkerOptions()
              .position(markerPosition)
              .icon(BitmapDescriptorFactory.defaultMarker(markerInfo.hue))
              .draggable(true);
      map.addMarker(markerOptions);
      map.setOnMarkerDragListener(this);
      map.setOnMarkerClickListener(this);

      if (moveCameraToMarker) {
        map.animateCamera(CameraUpdateFactory.newLatLng(markerPosition));
      }
    }

    @Override
    public void onMarkerDragStart(Marker marker) {}

    @Override
    public void onMarkerDrag(Marker marker) {}

    @Override
    public void onMarkerDragEnd(Marker marker) {
      markerPosition = marker.getPosition();
    }
  }

  /**
   * Example of a custom {@code SupportNavigationFragment} showing how the position of a marker and
   * other custom {@link Parcelable}s objects can be saved after rotation of the device.
   */
  public static class SaveStateNavFragment extends SupportNavigationFragment
      implements OnMarkerClickListener, OnMarkerDragListener, OnMapReadyCallback {

    private LatLng markerPosition;
    private MarkerInfo markerInfo;
    private boolean moveCameraToMarker;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      if (savedInstanceState == null) {
        // Activity created for the first time.
        markerPosition = DEFAULT_MARKER_POSITION;
        markerInfo = new MarkerInfo(BitmapDescriptorFactory.HUE_RED);
        moveCameraToMarker = true;
      } else {
        // Extract the state of the MapFragment:
        markerPosition = savedInstanceState.getParcelable(MARKER_POSITION);
        markerInfo = savedInstanceState.getParcelable(MARKER_INFO);
        moveCameraToMarker = false; // The API will remember the last camera.
      }

      getMapAsync(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      outState.putParcelable(MARKER_POSITION, markerPosition);
      outState.putParcelable(MARKER_INFO, markerInfo);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
      float newHue = MARKER_HUES[new Random().nextInt(MARKER_HUES.length)];
      markerInfo.hue = newHue;
      marker.setIcon(BitmapDescriptorFactory.defaultMarker(newHue));
      return true;
    }

    @Override
    public void onMapReady(GoogleMap map) {
      MarkerOptions markerOptions =
          new MarkerOptions()
              .position(markerPosition)
              .icon(BitmapDescriptorFactory.defaultMarker(markerInfo.hue))
              .draggable(true);
      map.addMarker(markerOptions);
      map.setOnMarkerDragListener(this);
      map.setOnMarkerClickListener(this);

      if (moveCameraToMarker) {
        map.animateCamera(CameraUpdateFactory.newLatLng(markerPosition));
      }
    }

    @Override
    public void onMarkerDragStart(Marker marker) {}

    @Override
    public void onMarkerDrag(Marker marker) {}

    @Override
    public void onMarkerDragEnd(Marker marker) {
      markerPosition = marker.getPosition();
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (getIntent()
        .getBooleanExtra(
            ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
            /* defaultValue= */ false)) {
      setContentView(R.layout.save_state_demo_nav_flavor);
    } else {
      setContentView(R.layout.save_state_demo_maps_flavor);
    }
  }
}
