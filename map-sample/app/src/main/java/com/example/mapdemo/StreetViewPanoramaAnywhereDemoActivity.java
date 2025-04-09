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
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanorama.OnStreetViewPanoramaChangeListener;
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment;
import com.google.android.gms.maps.model.StreetViewPanoramaLocation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A demo to jump to any custom PanoID.
 *
 * <p>Pano IDs of particularly tricky panoramas will be included here, including disabled panos and
 * panos we might do a bad job rendering.
 */
public class StreetViewPanoramaAnywhereDemoActivity extends AppCompatActivity
    implements OnItemSelectedListener, OnStreetViewPanoramaChangeListener, OnEditorActionListener {

  /** Simple class to add a nice display name to the pano strings in the dropdown list. */
  private static class AliasString extends Pair<String, String> {

    public AliasString(String value, String display) {
      super(value, display);
    }

    public String getValue() {
      return first;
    }

    @Override
    public String toString() {
      return second;
    }
  }

  private static final String BEACH = "mMkrjOCLO6H6bYCxxuhmlg";

  public static final List<AliasString> ALLEYCAT_TEST_PANO_IDS =
      Arrays.asList(
          new AliasString("2h_GoKI5_ntYLPlL5iYWPQ", "Very steep street (Lombard St, SF)"),
          new AliasString("WffpgCSo0gywfG3gPLlQbA", "Depth Map Testing (Cole St [outdoor], SF)"),
          new AliasString("95giX2PYIYAiMcXIm3FyxA", "Depth Map Testing (Cole St [indoor], SF)"),
          new AliasString("HRO2oUtn2nC6JSQtdiEbUg", "Alleycat Test Pano"),
          new AliasString(
              "2BKKB1FoUM8AAAQXMKCiCQ", "Very Deprecated Alleycat Key for UGC MEDIA Test Pano #4"));

  public static final List<AliasString> UGC_FIFE_TEST_PANO_IDS =
      Arrays.asList(
          new AliasString(
              "CAMSSi1nR0UydHpJTXRKdy9WLTZieW1lbkhKSS9BQUFBQUFBQXhBZy95QkFKQl9qMEt1a3gxLTNyNjdfTXMtamF5eGZ0NDJPQndDTElC",
              "UGC FIFE Test Pano #1 (Wunderlich County Park)"),
          new AliasString(
              "CAoSLEFGMVFpcFBjWk1RMm04aWxLZnhodDBNOXRHYndSTEtZbTdkT21wVXJDZm9Z",
              "UGC MEDIA Test Pano #2 (Golden Gate View Point)"),
          new AliasString("WddsUw1geEoAAAQIt9RnsQ", "UGC MEDIA Test Pano #3 (Santorini, Greece)"),
          new AliasString(
              "CAoSLEFGMVFpcE1iMDEwRUFqTWRvZ19ZME1zcU9yNUMwWFRFMi1Yd0ZEUzRuOVh0",
              "UGC MEDIA Test Pano #4 (68 Fauborg, FR)"));

  public static final List<AliasString> INNERSPACE_TEST_PANO_IDS =
      Arrays.asList(
          new AliasString("5KoMaToRAuKWnkk7O54LGQ", "Innerspace (Diagon Alley)"),
          new AliasString(
              "CAMSGC1hcURCdENWOU5Gby9WSkV4bzZ1MFlkSQ..", "Zero Links (West Lakes, Adelaide)"),
          new AliasString("J28hZA20FvoAAAQvxTCPhw", "FIFE Innerspace Redirection (Moscow)"));

  private static final List<AliasString> OTHER_TEST_PANO_IDS =
      Arrays.asList(
          new AliasString("zChzPIAn4RIAAAQvxgbyEg", "LatLng meaningless (I.S.S.)"),
          new AliasString("bY6YIBJE8c4AAAQZSNGSJA", "Disabled Pano 1"),
          new AliasString("ihywu_K543nHXQlYnT7XDw", "Disabled Pano 2"));

  @Nullable private StreetViewPanorama streetViewPanorama = null;
  private TextView currentPanoLocationTextView;

  @Override
  protected void onCreate(@Nullable final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.street_view_anywhere_demo);

    EditText panoIdText = (EditText) findViewById(R.id.pano_id_text);
    panoIdText.setText(BEACH);
    panoIdText.setOnEditorActionListener(this);

    List<AliasString> testPanoList = new ArrayList<>();
    testPanoList.addAll(ALLEYCAT_TEST_PANO_IDS);
    testPanoList.addAll(UGC_FIFE_TEST_PANO_IDS);
    testPanoList.addAll(INNERSPACE_TEST_PANO_IDS);
    testPanoList.addAll(OTHER_TEST_PANO_IDS);

    Spinner spinner = (Spinner) findViewById(R.id.pano_id_list_spinner);
    ArrayAdapter<AliasString> adapter =
        new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, testPanoList);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(adapter);
    spinner.setOnItemSelectedListener(this);

    currentPanoLocationTextView = (TextView) findViewById(R.id.currentpanolocationstr);

    SupportStreetViewPanoramaFragment streetViewPanoramaFragment =
        (SupportStreetViewPanoramaFragment)
            getSupportFragmentManager().findFragmentById(R.id.streetviewpanorama);
    streetViewPanoramaFragment.setRetainInstance(true);
    streetViewPanoramaFragment.getStreetViewPanoramaAsync(
        panorama -> {
          streetViewPanorama = panorama;
          streetViewPanorama.setOnStreetViewPanoramaChangeListener(
              StreetViewPanoramaAnywhereDemoActivity.this);

          // Only set the panorama on startup; otherwise Android.View does it for us.
          if (savedInstanceState == null) {
            streetViewPanorama.setPosition(BEACH);
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

  private void goToPano(String nextPanoId) {
    if (!checkReady()) {
      return;
    }

    StreetViewPanoramaLocation currentLocation = streetViewPanorama.getLocation();
    String currPanoId = (currentLocation == null) ? "null" : currentLocation.panoId;

    String toastMessage = String.format("from:%s => to:%s", currPanoId, nextPanoId);
    Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
    streetViewPanorama.setPosition(nextPanoId);
  }

  @Override
  public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
    goToPano(textView.getText().toString());
    return false;
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    goToPano(((AliasString) parent.getItemAtPosition(position)).getValue());
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {}
}
