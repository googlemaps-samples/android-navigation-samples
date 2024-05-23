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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;
import java.io.ByteArrayOutputStream;

/**
 * Provider for custom raster tiles that show borders around tiles, tile coords text "(x, y,
 * zoom=...)" in the middle, and transparency everywhere else.
 */
public class TileCoordsTileProvider implements TileProvider {

  private static final int TILE_SIZE_DP = 256;

  private final float scaleFactor;

  // @GuardedBy("borderTile")
  private final Bitmap borderTile;

  public TileCoordsTileProvider(float displayDensityRatio) {
    // Scale factor based on density, with a 0.6 multiplier to increase tile generation speed.
    scaleFactor = displayDensityRatio * 0.6f;
    Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    borderPaint.setStyle(Paint.Style.STROKE);
    borderTile =
        Bitmap.createBitmap(
            (int) (TILE_SIZE_DP * scaleFactor),
            (int) (TILE_SIZE_DP * scaleFactor),
            android.graphics.Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(borderTile);
    canvas.drawRect(0, 0, TILE_SIZE_DP * scaleFactor, TILE_SIZE_DP * scaleFactor, borderPaint);
  }

  @Override
  public Tile getTile(int x, int y, int zoom) {
    Bitmap coordTile = drawTileCoords(x, y, zoom);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    coordTile.compress(Bitmap.CompressFormat.PNG, 0, stream);
    byte[] bitmapData = stream.toByteArray();
    return new Tile(
        (int) (TILE_SIZE_DP * scaleFactor), (int) (TILE_SIZE_DP * scaleFactor), bitmapData);
  }

  private Bitmap drawTileCoords(int x, int y, int zoom) {
    // Synchronize copying the bitmap to avoid a race condition in some devices.
    Bitmap copy;
    synchronized (borderTile) {
      copy = borderTile.copy(android.graphics.Bitmap.Config.ARGB_8888, true);
    }
    Canvas canvas = new Canvas(copy);
    String tileCoords = "(" + x + ", " + y + ")";
    String zoomLevel = "zoom = " + zoom;
    // Paint is not thread safe.
    Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mTextPaint.setTextAlign(Paint.Align.CENTER);
    mTextPaint.setTextSize(18 * scaleFactor);
    canvas.drawText(
        tileCoords, TILE_SIZE_DP * scaleFactor / 2, TILE_SIZE_DP * scaleFactor / 2, mTextPaint);
    canvas.drawText(
        zoomLevel, TILE_SIZE_DP * scaleFactor / 2, TILE_SIZE_DP * scaleFactor * 2 / 3, mTextPaint);
    return copy;
  }
}
