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

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import android.view.ViewGroup;
import java.util.HashMap;
import java.util.Map;

/** Polygon control fragment pager adapter. */
public class PolygonControlFragmentPagerAdapter extends FragmentPagerAdapter {
  private static final int NUM_ITEMS = 6;

  private final Map<Integer, PolygonControlFragment> positionsToFragments;

  public PolygonControlFragmentPagerAdapter(FragmentManager fragmentManager) {
    super(fragmentManager);
    positionsToFragments = new HashMap<Integer, PolygonControlFragment>();
  }

  @Override
  public int getCount() {
    return NUM_ITEMS;
  }

  @Override
  public Object instantiateItem(ViewGroup container, int position) {
    PolygonControlFragment fragment =
        (PolygonControlFragment) super.instantiateItem(container, position);
    positionsToFragments.put(position, fragment);
    return fragment;
  }

  @Override
  public void destroyItem(ViewGroup container, int position, Object object) {
    super.destroyItem(container, position, object);
    positionsToFragments.remove(position);
  }

  @Override
  public Fragment getItem(int position) {
    switch (position) {
      case 0:
        return new PolygonColorControlFragment();
      case 1:
        return new PolygonWidthControlFragment();
      case 2:
        return new PolygonJointControlFragment();
      case 3:
        return new PolygonPatternControlFragment();
      case 4:
        return new PolygonPointsControlFragment();
      case 5:
        return new PolygonOtherOptionsControlFragment();
      default:
        return null;
    }
  }

  @Override
  public CharSequence getPageTitle(int position) {
    // Ideally these strings should be localised, but let's not bother for a demo app.
    switch (position) {
      case 0:
        return "Stroke/fill color";
      case 1:
        return "Stroke width";
      case 2:
        return "Stroke joint";
      case 3:
        return "Stroke pattern";
      case 4:
        return "Points";
      case 5:
        return "Other Options";
      default:
        return null;
    }
  }

  PolygonControlFragment getFragmentAtPosition(int position) {
    return positionsToFragments.get(position);
  }
}
