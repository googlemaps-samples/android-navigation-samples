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

import com.google.android.gms.maps.model.UrlTileProvider;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

/** Provider of "moon" raster tiles. */
public class MoonTileProvider extends UrlTileProvider {

  private static final String MOON_MAP_URL_FORMAT =
      "http://mw1.google.com/mw-planetary/lunar/lunarmaps_v1/clem_bw/%d/%d/%d.jpg";

  public MoonTileProvider() {
    super(256 /* width */, 256 /* height */);
  }

  @Override
  public URL getTileUrl(int x, int y, int zoom) {
    // The moon tile coordinate system is reversed. This is not normal.
    int reversedY = (1 << zoom) - y - 1;
    String s = String.format(Locale.US, MOON_MAP_URL_FORMAT, zoom, x, reversedY);
    try {
      return new URL(s);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
}
