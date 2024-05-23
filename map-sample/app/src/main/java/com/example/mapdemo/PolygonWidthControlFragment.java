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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

/** Fragment with "stroke width" UI controls for polygons, to be used in ViewPager. */
public class PolygonWidthControlFragment extends PolygonControlFragment
    implements SeekBar.OnSeekBarChangeListener {
  private static final int WIDTH_MAX = 50;

  private SeekBar widthBar;
  private TextView widthTextView;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
    View view = inflater.inflate(R.layout.width_control_fragment, container, false);
    widthBar = (SeekBar) view.findViewById(R.id.widthSeekBar);
    widthBar.setMax(WIDTH_MAX);
    widthBar.setOnSeekBarChangeListener(this);
    widthTextView = (TextView) view.findViewById(R.id.widthTextView);
    return view;
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
    if (polygon == null) {
      return;
    }

    polygon.setStrokeWidth(progress);
    Resources res = getResources();
    String text = res.getString(R.string.polygon_px, polygon.getStrokeWidth());
    widthTextView.setText(text);
  }

  @Override
  public void refresh() {
    if (polygon == null) {
      widthBar.setEnabled(false);
      widthBar.setProgress(0);
      widthTextView.setText("");
      return;
    }

    widthBar.setEnabled(true);
    float strokeWidth = polygon.getStrokeWidth();
    widthBar.setProgress((int) strokeWidth);
    Resources res = getResources();
    String text = String.format(res.getString(R.string.polygon_px), strokeWidth);
    widthTextView.setText(text);
  }

  @Override
  public void onDestroyView() {
    widthBar.setOnSeekBarChangeListener(null);
    widthBar = null;
    widthTextView = null;

    super.onDestroyView();
  }
}
