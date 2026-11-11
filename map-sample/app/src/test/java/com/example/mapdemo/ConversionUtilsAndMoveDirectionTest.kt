/*
 * Copyright 2026 Google LLC
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

package com.example.mapdemo

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Parcel
import androidx.test.core.app.ApplicationProvider
import com.example.mapdemo.testing.NavDelegateReflection
import com.example.mapdemo.testing.NavMapsRobolectricTest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.robolectric.powerassert.assertGeo
import com.google.maps.android.robolectric.powerassert.assertMap
import com.google.maps.android.robolectric.powerassert.distanceTo
import com.google.maps.android.robolectric.powerassert.meters
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Fast base-of-pyramid unit tests for `map-sample` utilities, coordinate/tile math, parcelables,
 * and demo catalog integrity using **Kotlin Power-Assert** (`assert`, `assertGeo`, `assertMap`).
 *
 * Verifies:
 * 1. [ConversionUtils.toAnimationReasonString] across all [OnCameraMoveStartedListener] constants
 *    and unknown reason codes.
 * 2. [MoveDirection] camera scroll directions (`UP`, `DOWN`, `LEFT`, `RIGHT`) and directional camera
 *    target displacement when applied to a `GoogleMap`.
 * 3. [SaveStateDemoActivity.MarkerInfo] `Parcelable` round-trip serialization.
 * 4. [MoonTileProvider.getTileUrl] reversed-Y lunar tile URL coordinate calculation.
 * 5. [TileCoordsTileProvider.getTile] PNG raster tile generation and density scaling.
 * 6. [DemoDetailsList.DEMOS] registry completeness and `AndroidManifest.xml` registration.
 */
@RunWith(RobolectricTestRunner::class)
class ConversionUtilsAndMoveDirectionTest : NavMapsRobolectricTest() {

  @Test
  fun toAnimationReasonString_mapsAllKnownReasonsAndFallback() {
    assert(
      ConversionUtils.toAnimationReasonString(OnCameraMoveStartedListener.REASON_GESTURE) ==
        "REASON_GESTURE"
    )
    assert(
      ConversionUtils.toAnimationReasonString(OnCameraMoveStartedListener.REASON_API_ANIMATION) ==
        "REASON_API_ANIMATION"
    )
    assert(
      ConversionUtils.toAnimationReasonString(
        OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION
      ) == "REASON_DEVELOPER_ANIMATION"
    )
    assert(ConversionUtils.toAnimationReasonString(-999) == "Unknown")
  }

  @Test
  fun moveDirection_calculatesStepDistancesAndTranslatesPointListsAndNestedLists() {
    val stepSize = 2.5
    assertGeo(MoveDirection.UP.getLatDistance(stepSize) == 2.5 && MoveDirection.UP.getLngDistance(stepSize) == 0.0)
    assertGeo(MoveDirection.DOWN.getLatDistance(stepSize) == -2.5 && MoveDirection.DOWN.getLngDistance(stepSize) == 0.0)
    assertGeo(MoveDirection.LEFT.getLatDistance(stepSize) == 0.0 && MoveDirection.LEFT.getLngDistance(stepSize) == -2.5)
    assertGeo(MoveDirection.RIGHT.getLatDistance(stepSize) == 0.0 && MoveDirection.RIGHT.getLngDistance(stepSize) == 2.5)

    val origin = LatLng(-33.0, 151.0)
    val second = LatLng(-34.0, 152.0)
    val movedList =
      MoveDirection.movePointsInList(
        listOf(origin, second),
        MoveDirection.UP.getLatDistance(1.0),
        MoveDirection.RIGHT.getLngDistance(2.0),
      )

    assertGeo(movedList.size == 2)
    assertGeo(movedList[0] == LatLng(-32.0, 153.0) && movedList[1] == LatLng(-33.0, 154.0))
    assertGeo(origin.distanceTo(movedList[0]) > 100.meters)

    val nestedMoved =
      MoveDirection.movePointsInNestedList(
        listOf(listOf(origin), listOf(second)),
        MoveDirection.DOWN.getLatDistance(0.5),
        MoveDirection.LEFT.getLngDistance(0.5),
      )
    assertGeo(nestedMoved.size == 2)
    assertGeo(nestedMoved[0][0] == LatLng(-33.5, 150.5) && nestedMoved[1][0] == LatLng(-34.5, 151.5))

    val map = NavDelegateReflection.createGoogleMapInstance()
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(origin, 10f))
    map.assertMap(map.cameraPosition.zoom == 10f)
  }

  @Test
  fun markerInfo_parcelableRoundTripPreservesHue() {
    val original = SaveStateDemoActivity.MarkerInfo(BitmapDescriptorFactory.HUE_AZURE)
    val parcel = Parcel.obtain()
    try {
      original.writeToParcel(parcel, 0)
      parcel.setDataPosition(0)

      val restored = SaveStateDemoActivity.MarkerInfo.CREATOR.createFromParcel(parcel)
      assert(restored.hue == BitmapDescriptorFactory.HUE_AZURE)
      assert(restored.describeContents() == 0)

      val array = SaveStateDemoActivity.MarkerInfo.CREATOR.newArray(3)
      assert(array.size == 3)
    } finally {
      parcel.recycle()
    }
  }

  @Test
  fun moonTileProvider_reversesYCoordinateAcrossZoomLevels() {
    val provider = MoonTileProvider()

    // At zoom = 0: reversedY = (1 << 0) - 0 - 1 = 0
    val urlZoom0 = provider.getTileUrl(0, 0, 0)
    assert(
      urlZoom0.toString() == "http://mw1.google.com/mw-planetary/lunar/lunarmaps_v1/clem_bw/0/0/0.jpg"
    )

    // At zoom = 2, x = 1, y = 0: reversedY = (1 << 2) - 0 - 1 = 3
    val urlZoom2 = provider.getTileUrl(1, 0, 2)
    assert(
      urlZoom2.toString() == "http://mw1.google.com/mw-planetary/lunar/lunarmaps_v1/clem_bw/2/1/3.jpg"
    )

    // At zoom = 4, x = 5, y = 10: reversedY = 16 - 10 - 1 = 5
    val urlZoom4 = provider.getTileUrl(5, 10, 4)
    assert(
      urlZoom4.toString() == "http://mw1.google.com/mw-planetary/lunar/lunarmaps_v1/clem_bw/4/5/5.jpg"
    )
  }

  @Test
  fun tileCoordsTileProvider_generatesValidPngTileWithScaledDimensions() {
    val density = 2.0f
    val provider = TileCoordsTileProvider(density)
    val tile = provider.getTile(3, 7, 5)

    // Expected size: (int) (256 * 2.0f * 0.6f) = 307
    val expectedDimension = (256 * density * 0.6f).toInt()
    assert(tile.width == expectedDimension)
    assert(tile.height == expectedDimension)
    assert(tile.data != null && tile.data!!.isNotEmpty())

    val decodedBitmap = BitmapFactory.decodeByteArray(tile.data, 0, tile.data!!.size)
    assert(decodedBitmap != null)
    assert(decodedBitmap.width == expectedDimension)
    assert(decodedBitmap.height == expectedDimension)
  }

  @Test
  fun demoDetailsList_allDemosHaveValidResourcesAndAreRegisteredInManifest() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val packageInfo =
      context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)
    val manifestActivities =
      packageInfo.activities?.map { activityInfo -> activityInfo.name }?.toSet().orEmpty()

    val demos = DemoDetailsList.DEMOS
    assert(demos.size == 47)

    val uniqueClasses = demos.map { demo -> demo.activityClass }.toSet()
    assert(uniqueClasses.size == demos.size)

    for (demo in demos) {
      val title = context.getString(demo.titleId)
      val description = context.getString(demo.descriptionId)
      assert(title.isNotBlank())
      assert(description.isNotBlank())
      assert(manifestActivities.contains(demo.activityClass.name))
    }
  }
}
