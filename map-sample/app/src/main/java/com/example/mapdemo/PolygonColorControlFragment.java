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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

/** Fragment with "color" UI controls for polygons, to be used in ViewPager. */
public class PolygonColorControlFragment extends PolygonControlFragment
    implements SeekBar.OnSeekBarChangeListener {
  private static final int HUE_MAX = 359;
  private static final int ALPHA_MAX = 255;

  private SeekBar strokeAlphaBar;
  private SeekBar strokeHueBar;
  private TextView strokeArgbTextView;
  private SeekBar fillAlphaBar;
  private SeekBar fillHueBar;
  private TextView fillArgbTextView;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
    View view = inflater.inflate(R.layout.polygon_color_control_fragment, container, false);

    strokeAlphaBar = (SeekBar) view.findViewById(R.id.strokeAlphaSeekBar);
    strokeAlphaBar.setMax(ALPHA_MAX);
    strokeAlphaBar.setOnSeekBarChangeListener(this);

    strokeHueBar = (SeekBar) view.findViewById(R.id.strokeHueSeekBar);
    strokeHueBar.setMax(HUE_MAX);
    strokeHueBar.setOnSeekBarChangeListener(this);

    strokeArgbTextView = (TextView) view.findViewById(R.id.strokeArgbTextView);

    fillAlphaBar = (SeekBar) view.findViewById(R.id.fillAlphaSeekBar);
    fillAlphaBar.setMax(ALPHA_MAX);
    fillAlphaBar.setOnSeekBarChangeListener(this);

    fillHueBar = (SeekBar) view.findViewById(R.id.fillHueSeekBar);
    fillHueBar.setMax(HUE_MAX);
    fillHueBar.setOnSeekBarChangeListener(this);

    fillArgbTextView = (TextView) view.findViewById(R.id.fillArgbTextView);
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

    if (seekBar == strokeHueBar) {
      polygon.setStrokeColor(
          Color.HSVToColor(Color.alpha(polygon.getStrokeColor()), new float[] {progress, 1, 1}));
      strokeArgbTextView.setText(String.format("0x%08X", polygon.getStrokeColor()));
    } else if (seekBar == strokeAlphaBar) {
      float[] prevHSV = new float[3];
      Color.colorToHSV(polygon.getStrokeColor(), prevHSV);
      polygon.setStrokeColor(Color.HSVToColor(progress, prevHSV));
      strokeArgbTextView.setText(String.format("0x%08X", polygon.getStrokeColor()));
    } else if (seekBar == fillHueBar) {
      polygon.setFillColor(
          Color.HSVToColor(Color.alpha(polygon.getFillColor()), new float[] {progress, 1, 1}));
      fillArgbTextView.setText(String.format("0x%08X", polygon.getFillColor()));
    } else if (seekBar == fillAlphaBar) {
      float[] prevHSV = new float[3];
      Color.colorToHSV(polygon.getFillColor(), prevHSV);
      polygon.setFillColor(Color.HSVToColor(progress, prevHSV));
      fillArgbTextView.setText(String.format("0x%08X", polygon.getFillColor()));
    }
  }

  @Override
  public void refresh() {
    if (polygon == null) {
      strokeAlphaBar.setEnabled(false);
      strokeAlphaBar.setProgress(0);
      strokeHueBar.setEnabled(false);
      strokeHueBar.setProgress(0);
      strokeArgbTextView.setText("");
      fillAlphaBar.setEnabled(false);
      fillAlphaBar.setProgress(0);
      fillHueBar.setEnabled(false);
      fillHueBar.setProgress(0);
      fillArgbTextView.setText("");
      return;
    }

    int color = polygon.getStrokeColor();
    strokeAlphaBar.setEnabled(true);
    strokeAlphaBar.setProgress(Color.alpha(color));

    float[] hsv = new float[3];
    Color.colorToHSV(color, hsv);
    strokeHueBar.setEnabled(true);
    strokeHueBar.setProgress((int) hsv[0]);

    strokeArgbTextView.setText(String.format("0x%08X", color));

    color = polygon.getFillColor();
    fillAlphaBar.setEnabled(true);
    fillAlphaBar.setProgress(Color.alpha(color));

    Color.colorToHSV(color, hsv);
    fillHueBar.setEnabled(true);
    fillHueBar.setProgress((int) hsv[0]);

    fillArgbTextView.setText(String.format("0x%08X", color));
  }

  @Override
  public void onDestroyView() {
    strokeAlphaBar.setOnSeekBarChangeListener(null);
    strokeHueBar.setOnSeekBarChangeListener(null);
    fillAlphaBar.setOnSeekBarChangeListener(null);
    fillHueBar.setOnSeekBarChangeListener(null);

    strokeAlphaBar = null;
    strokeHueBar = null;
    fillAlphaBar = null;
    fillHueBar = null;
    strokeArgbTextView = null;
    fillArgbTextView = null;

    super.onDestroyView();
  }
}
