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

package com.example.navtesting

import android.graphics.Bitmap
import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnCameraFollowLocationCallback
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.UiSettings
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.robolectric.shadows.ShadowBitmapDescriptor
import com.google.maps.android.robolectric.shadows.ShadowCameraUpdate
import com.google.maps.android.robolectric.shadows.ShadowGoogleMap
import com.google.maps.android.robolectric.shadows.ShadowMapView
import com.google.maps.android.robolectric.shadows.ShadowProjection
import com.google.maps.android.robolectric.shadows.ShadowSupportMapFragment
import com.google.maps.android.robolectric.shadows.ShadowUiSettings
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadow.api.Shadow

/**
 * Subclass of [ShadowGoogleMap] that handles Navigation SDK's obfuscated `UiSettings` and
 * `Projection` constructors and supports `followMyLocation` / `OnCameraFollowLocationCallback`.
 */
@Implements(GoogleMap::class)
class NavShadowGoogleMap : ShadowGoogleMap() {
  private var navUiSettings: UiSettings? = null
  private var followLocationCallback: OnCameraFollowLocationCallback? = null
  private var followingMyLocation: Boolean = false

  var lastCameraPerspective: Int? = null
    private set

  @Implementation
  override fun getUiSettings(): UiSettings {
    if (navUiSettings == null) {
      navUiSettings = NavDelegateReflection.constructWithDummyDelegate(UiSettings::class.java)
    }
    return navUiSettings!!
  }

  @Implementation
  override fun getProjection(): Projection {
    val projection = NavDelegateReflection.constructWithDummyDelegate(Projection::class.java)
    val shadow = Shadow.extract<ShadowProjection>(projection)
    val width = if (mapView != null && mapView.width > 0) mapView.width else viewportWidth
    val height = if (mapView != null && mapView.height > 0) mapView.height else viewportHeight
    shadow.setMapState(cameraPosition, width, height)
    return projection
  }

  @Implementation
  fun followMyLocation(perspective: Int) {
    lastCameraPerspective = perspective
    followingMyLocation = true
    followLocationCallback?.onCameraStartedFollowingLocation()
  }

  @Implementation
  fun isCameraFollowingMyLocation(): Boolean = followingMyLocation

  @Implementation
  fun setOnFollowMyLocationCallback(callback: OnCameraFollowLocationCallback?) {
    followLocationCallback = callback
  }

  fun simulateCameraStartedFollowingLocation() {
    followingMyLocation = true
    followLocationCallback?.onCameraStartedFollowingLocation()
  }

  fun simulateCameraStoppedFollowingLocation() {
    followingMyLocation = false
    followLocationCallback?.onCameraStoppedFollowingLocation()
  }
}

/** Subclass of [ShadowUiSettings] tracking `isScrollGesturesEnabledDuringRotateOrZoom`. */
@Implements(UiSettings::class)
class NavShadowUiSettings : ShadowUiSettings() {
  private var scrollGesturesEnabledDuringRotateOrZoom: Boolean = true

  @Implementation
  fun setScrollGesturesEnabledDuringRotateOrZoom(enabled: Boolean) {
    scrollGesturesEnabledDuringRotateOrZoom = enabled
  }

  @Implementation
  fun isScrollGesturesEnabledDuringRotateOrZoom(): Boolean =
    scrollGesturesEnabledDuringRotateOrZoom
}

/** Subclass of [ShadowMapView] that creates a Navigation-SDK-compatible [GoogleMap]. */
@Implements(MapView::class)
class NavShadowMapView : ShadowMapView() {
  @RealObject private lateinit var realMapViewInstance: MapView

  @Implementation
  override fun getMapAsync(callback: OnMapReadyCallback) {
    if (getGoogleMap() == null) {
      val map = NavDelegateReflection.createGoogleMapInstance()
      val shadowMap = Shadow.extract<ShadowGoogleMap>(map)
      shadowMap.setMapView(realMapViewInstance)
      val field = ShadowMapView::class.java.getDeclaredField("googleMap")
      field.isAccessible = true
      field.set(this, map)
    }
    val map = getGoogleMap()
    Handler(Looper.getMainLooper()).post { callback.onMapReady(map) }
  }
}

/**
 * Subclass of [ShadowSupportMapFragment] that lays out its container with default 400x400 bounds
 * and delivers a Navigation-SDK-compatible [GoogleMap].
 */
@Implements(SupportMapFragment::class)
class NavShadowSupportMapFragment : ShadowSupportMapFragment() {
  @Implementation
  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    val view = super.onCreateView(inflater, container, savedInstanceState)
    view.layout(0, 0, 400, 400)
    return view
  }

  @Implementation
  override fun getMapAsync(callback: OnMapReadyCallback) {
    val view = getMapView()
    if (view != null) {
      view.getMapAsync(callback)
      return
    }
    val field = ShadowSupportMapFragment::class.java.getDeclaredField("googleMap")
    field.isAccessible = true
    var currentMap = field.get(this) as? GoogleMap
    if (currentMap == null) {
      currentMap = NavDelegateReflection.createGoogleMapInstance()
      field.set(this, currentMap)
    }
    Handler(Looper.getMainLooper()).post { callback.onMapReady(currentMap) }
  }

  fun resolveGoogleMap(): GoogleMap? {
    val view = getMapView()
    if (view != null) {
      val shadowMapView = Shadow.extract<ShadowMapView>(view)
      if (shadowMapView.getGoogleMap() != null) {
        return shadowMapView.getGoogleMap()
      }
    }
    val field = ShadowSupportMapFragment::class.java.getDeclaredField("googleMap")
    field.isAccessible = true
    return field.get(this) as? GoogleMap
  }
}

/** Shadow for [CameraUpdateFactory] compatible with Navigation SDK's delegate constructor. */
@Implements(CameraUpdateFactory::class)
class NavShadowCameraUpdateFactory {
  companion object {
    private fun createUpdate(configure: (ShadowCameraUpdate) -> Unit): CameraUpdate {
      val update = NavDelegateReflection.constructWithDummyDelegate(CameraUpdate::class.java)
      configure(Shadow.extract(update))
      return update
    }

    @Implementation @JvmStatic
    fun newCameraPosition(cameraPosition: CameraPosition): CameraUpdate = createUpdate {
      it.cameraPosition = cameraPosition
    }

    @Implementation @JvmStatic
    fun newLatLng(latLng: LatLng): CameraUpdate = createUpdate { it.latLng = latLng }

    @Implementation @JvmStatic
    fun newLatLngZoom(latLng: LatLng, zoom: Float): CameraUpdate = createUpdate {
      it.latLng = latLng
      it.zoom = zoom
    }

    @Implementation @JvmStatic
    fun newLatLngBounds(bounds: LatLngBounds, padding: Int): CameraUpdate = createUpdate {
      it.setLatLngBounds(bounds, padding)
    }

    @Implementation @JvmStatic
    fun newLatLngBounds(bounds: LatLngBounds, width: Int, height: Int, padding: Int): CameraUpdate =
      createUpdate {
        it.setLatLngBounds(bounds, width, height, padding)
      }

    @Implementation @JvmStatic fun zoomIn(): CameraUpdate = zoomBy(1.0f)

    @Implementation @JvmStatic fun zoomOut(): CameraUpdate = zoomBy(-1.0f)

    @Implementation @JvmStatic
    fun zoomTo(zoom: Float): CameraUpdate = createUpdate { it.zoom = zoom }

    @Implementation @JvmStatic
    fun zoomBy(amount: Float): CameraUpdate = createUpdate { it.zoomBy = amount }

    @Implementation @JvmStatic
    fun zoomBy(amount: Float, focus: Point): CameraUpdate = zoomBy(amount)

    @Implementation @JvmStatic
    fun scrollBy(xPixel: Float, yPixel: Float): CameraUpdate = createUpdate {
      it.setScroll(xPixel, yPixel)
    }
  }
}

/** Shadow for [BitmapDescriptorFactory] compatible with Navigation SDK's delegate constructor. */
@Implements(BitmapDescriptorFactory::class)
class NavShadowBitmapDescriptorFactory {
  companion object {
    const val HUE_RED = 0.0f

    private fun createDescriptor(bitmap: Bitmap?, identifier: String): BitmapDescriptor {
      val descriptor =
        NavDelegateReflection.constructWithDummyDelegate(BitmapDescriptor::class.java)
      val shadow = Shadow.extract<ShadowBitmapDescriptor>(descriptor)
      shadow.bitmap = bitmap
      shadow.identifier = identifier
      return descriptor
    }

    @Implementation @JvmStatic fun defaultMarker(): BitmapDescriptor = defaultMarker(HUE_RED)

    @Implementation @JvmStatic
    fun defaultMarker(hue: Float): BitmapDescriptor =
      createDescriptor(null, "defaultMarker:hue=$hue")

    @Implementation @JvmStatic
    fun fromResource(resourceId: Int): BitmapDescriptor =
      createDescriptor(null, "resource:$resourceId")

    @Implementation @JvmStatic
    fun fromBitmap(image: Bitmap?): BitmapDescriptor =
      createDescriptor(image, "bitmap:${System.identityHashCode(image)}")

    @Implementation @JvmStatic
    fun fromAsset(assetName: String): BitmapDescriptor =
      createDescriptor(null, "asset:$assetName")

    @Implementation @JvmStatic
    fun fromFile(fileName: String): BitmapDescriptor = createDescriptor(null, "file:$fileName")

    @Implementation @JvmStatic
    fun fromPath(absolutePath: String): BitmapDescriptor =
      createDescriptor(null, "path:$absolutePath")
  }
}
