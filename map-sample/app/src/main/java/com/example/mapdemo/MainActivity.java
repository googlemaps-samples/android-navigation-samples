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

import android.Manifest.permission;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ToggleButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * The main activity of the API library demo gallery.
 *
 * <p>The main layout lists the demonstrated features, with buttons to launch them.
 */
public final class MainActivity extends AppCompatActivity {

  public static final int PERMISSIONS_ID = 456;
  public static final String[] PERMISSIONS_STRINGS_ARRAY = {permission.ACCESS_FINE_LOCATION};

  private boolean isLocationPermissionDenied;

  /** A custom array adapter that shows a {@link FeatureView} containing details about the demo. */
  private static class CustomArrayAdapter extends ArrayAdapter<DemoDetails> {

    /** @param demos An array containing the details of the demos to be displayed. */
    public CustomArrayAdapter(Context context, DemoDetails[] demos) {
      super(context, R.layout.feature, R.id.title, demos);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      FeatureView featureView;
      if (convertView instanceof FeatureView) {
        featureView = (FeatureView) convertView;
      } else {
        featureView = new FeatureView(getContext());
      }

      DemoDetails demo = getItem(position);

      featureView.setTitleId(demo.titleId);
      featureView.setDescriptionId(demo.descriptionId);

      Resources resources = getContext().getResources();
      String title = resources.getString(demo.titleId);
      String description = resources.getString(demo.descriptionId);
      featureView.setContentDescription(title + ". " + description);

      return featureView;
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    ToggleButton shouldUseNavigationFlavorDemoToggleButton =
        (ToggleButton) findViewById(R.id.view_source_toggle_btn);
    ListAdapter adapter = new CustomArrayAdapter(this, DemoDetailsList.DEMOS);

    ListView demoListView = (ListView) findViewById(R.id.demoListView);
    if (demoListView != null) {
      demoListView.setAdapter(adapter);
      demoListView.setOnItemClickListener(
          new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

              DemoDetails demo = (DemoDetails) parent.getItemAtPosition(position);
              boolean shouldUseNavigationFlavorDemo =
                  shouldUseNavigationFlavorDemoToggleButton.isChecked();
              Intent startIntent =
                  new Intent(view.getContext(), demo.activityClass)
                      .putExtra(
                          ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
                          shouldUseNavigationFlavorDemo);
              startActivity(startIntent);
            }
          });
    }
  }

  @Override
  protected void onResume() {
    super.onResume();

    isLocationPermissionDenied =
        ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_DENIED;
    if (isLocationPermissionDenied) {
      ActivityCompat.requestPermissions(this, PERMISSIONS_STRINGS_ARRAY, PERMISSIONS_ID);
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    switch (requestCode) {
      case PERMISSIONS_ID:
        {
          // If request is cancelled, the result arrays are empty.
          if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            isLocationPermissionDenied = false;
          } else {
            isLocationPermissionDenied = true;
          }
          return;
        }
    }
  }
}
