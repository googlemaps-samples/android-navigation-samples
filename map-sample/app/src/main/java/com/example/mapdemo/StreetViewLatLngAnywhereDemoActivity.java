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
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanorama.OnStreetViewPanoramaChangeListener;
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.StreetViewPanoramaLocation;
import com.google.android.gms.maps.model.StreetViewSource;
import java.util.Arrays;
import java.util.List;

/**
 * A demo to jump to any custom LatLng.
 *
 * <p>LatLngs of particularly tricky searches will be included here, including disabled panos and
 * panos we might do a bad job rendering.
 */
@SuppressWarnings("StringSplitter") // Don't want to add guava dependency just for this.
public class StreetViewLatLngAnywhereDemoActivity extends AppCompatActivity
    implements OnItemSelectedListener, OnStreetViewPanoramaChangeListener, OnEditorActionListener {

  /** Simple class to add a nice display name to the pano strings in the dropdown list. */
  private static class AliasString {
    public final LatLng latLng;
    public final String display;

    public AliasString(LatLng latLng, String display) {
      this.latLng = latLng;
      this.display = display;
    }

    @Override
    public String toString() {
      return display + " " + latLng;
    }
  }

  private static final LatLng DEFAULT_LOCATION = new LatLng(37.76912, -122.450645);
  private static final String DEFAULT_TEXT_FIELD = "37.76912,-122.450645";

  // Lookup tools: go/alleycat/tools | go/alleycatexplorer | go/gpmsdebug
  private static final List<AliasString> PREPOPULATED_LATLNG_LIST =
      Arrays.asList(
          new AliasString[] {
            new AliasString(DEFAULT_LOCATION, "Default Location (Cole St [outdoor], SF)"),
            new AliasString(
                new LatLng(37.7692657, -122.4507992), "Indoor Location (Cole St [indoor], SF)"),
            new AliasString(
                new LatLng(51.4921451, -0.1929781), "Indoor Innerspace (Police Phonebox)"),
            new AliasString(new LatLng(29.560285, -95.085391), "LatLng Meaningless (I.S.S.)"),
            new AliasString(new LatLng(0, 0), "No secret underground base. [0,0]"),
          });

  @Nullable private StreetViewPanorama streetViewPanorama = null;
  private CompoundButton customRadiusToggle;
  private SeekBar customRadiusBar;
  private CompoundButton outdoorOnlyToggle;
  private TextView currentPanoLocationTextView;

  @Override
  protected void onCreate(@Nullable final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.street_view_latlng_demo);

    EditText panoLatLngText = (EditText) findViewById(R.id.pano_latlng_text);
    panoLatLngText.setText(DEFAULT_TEXT_FIELD);
    panoLatLngText.setOnEditorActionListener(this);

    Spinner spinner = (Spinner) findViewById(R.id.pano_latlng_list_spinner);
    ArrayAdapter<AliasString> adapter =
        new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, PREPOPULATED_LATLNG_LIST);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(adapter);
    spinner.setOnItemSelectedListener(this);

    customRadiusToggle = (CompoundButton) findViewById(R.id.radius_toggle);
    customRadiusBar = (SeekBar) findViewById(R.id.radius_bar);
    customRadiusBar.setEnabled(customRadiusToggle.isChecked());
    outdoorOnlyToggle = (CompoundButton) findViewById(R.id.outdoor_only_toggle);

    currentPanoLocationTextView = (TextView) findViewById(R.id.currentpanolocationstr);

    SupportStreetViewPanoramaFragment streetViewPanoramaFragment =
        (SupportStreetViewPanoramaFragment)
            getSupportFragmentManager().findFragmentById(R.id.streetviewpanorama);
    streetViewPanoramaFragment.setRetainInstance(true);
    streetViewPanoramaFragment.getStreetViewPanoramaAsync(
        panorama -> {
          streetViewPanorama = panorama;
          streetViewPanorama.setOnStreetViewPanoramaChangeListener(
              StreetViewLatLngAnywhereDemoActivity.this);

          // Only set the panorama on startup; otherwise Android.View does it for us.
          if (savedInstanceState == null) {
            streetViewPanorama.setPosition(DEFAULT_LOCATION);
          }
        });
  }

  /**
   * When the panorama is not ready the PanoramaView cannot be used. This should be called on all
   * entry points that call methods on the Panorama API.
   */
  private boolean checkReady() {
    if (streetViewPanorama == null) {
      Toast.makeText(this, R.string.panorama_not_ready, Toast.LENGTH_SHORT).show();
      return false;
    }
    return true;
  }

  @Override
  public void onStreetViewPanoramaChange(@Nullable StreetViewPanoramaLocation location) {
    String panoIdStr = (location == null) ? "null" : (location.panoId + " @ " + location.position);
    Toast.makeText(this, "Entered Pano: " + panoIdStr, Toast.LENGTH_SHORT).show();
    currentPanoLocationTextView.setText(panoIdStr);
  }

  private void goToPano(LatLng nextLatLng) {
    if (!checkReady()) {
      return;
    }

    Integer radiusM = customRadiusToggle.isChecked() ? customRadiusBar.getProgress() : null;
    StreetViewSource source = outdoorOnlyToggle.isChecked() ? StreetViewSource.OUTDOOR : null;
    StreetViewPanoramaLocation currentLocation = streetViewPanorama.getLocation();
    String currPanoId = (currentLocation == null) ? "null" : currentLocation.panoId;

    String toastMessage =
        String.format("from:%s => to:%s,%s,%s", currPanoId, nextLatLng, radiusM, source);
    Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();

    if ((radiusM != null) && (source != null)) {
      streetViewPanorama.setPosition(nextLatLng, radiusM, source);
    } else if (source != null) {
      streetViewPanorama.setPosition(nextLatLng, source);
    } else if (radiusM != null) {
      streetViewPanorama.setPosition(nextLatLng, radiusM);
    } else {
      streetViewPanorama.setPosition(nextLatLng);
    }
  }

  @Override
  public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
    String textBox = textView.getText().toString();
    try {
      String[] latLngStr = textBox.split(",");
      double lat = Double.parseDouble(latLngStr[0]);
      double lng = Double.parseDouble(latLngStr[1]);
      goToPano(new LatLng(lat, lng));
    } catch (Exception e) {
      Toast.makeText(this, "Invalid Text: " + e, Toast.LENGTH_SHORT).show();
    }
    return false;
  }

  public void onToggleCustomRadius(View view) {
    customRadiusBar.setEnabled(customRadiusToggle.isChecked());
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    goToPano(((AliasString) parent.getItemAtPosition(position)).latLng);
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {}
}
