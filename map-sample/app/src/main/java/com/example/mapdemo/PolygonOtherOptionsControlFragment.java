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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

/**
 * Fragment with clickability, geodesic, and visibility UI controls for Polygons, to be used in
 * ViewPager.
 */
public class PolygonOtherOptionsControlFragment extends PolygonControlFragment
    implements View.OnClickListener {

  private CheckBox clickabilityCheckBox;
  private CheckBox geodesicCheckBox;
  private CheckBox visibilityCheckBox;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
    View view = inflater.inflate(R.layout.other_options_control_fragment, container, false);
    clickabilityCheckBox = (CheckBox) view.findViewById(R.id.clickabilityCheckBox);
    clickabilityCheckBox.setOnClickListener(this);
    geodesicCheckBox = (CheckBox) view.findViewById(R.id.geodesicCheckBox);
    geodesicCheckBox.setOnClickListener(this);
    visibilityCheckBox = (CheckBox) view.findViewById(R.id.visibilityCheckBox);
    visibilityCheckBox.setOnClickListener(this);
    return view;
  }

  @Override
  public void onClick(View view) {
    if (polygon == null) {
      return;
    }
    if (view == clickabilityCheckBox) {
      polygon.setClickable(clickabilityCheckBox.isChecked());
    } else if (view == geodesicCheckBox) {
      polygon.setGeodesic(geodesicCheckBox.isChecked());
    } else if (view == visibilityCheckBox) {
      polygon.setVisible(visibilityCheckBox.isChecked());
    }
  }

  @Override
  public void refresh() {
    clickabilityCheckBox.setChecked((polygon != null) && polygon.isClickable());
    geodesicCheckBox.setChecked((polygon != null) && polygon.isGeodesic());
    visibilityCheckBox.setChecked((polygon != null) && polygon.isVisible());
  }

  @Override
  public void onDestroyView() {
    clickabilityCheckBox.setOnClickListener(null);
    geodesicCheckBox.setOnClickListener(null);
    visibilityCheckBox.setOnClickListener(null);

    clickabilityCheckBox = null;
    geodesicCheckBox = null;
    visibilityCheckBox = null;

    super.onDestroyView();
  }
}
