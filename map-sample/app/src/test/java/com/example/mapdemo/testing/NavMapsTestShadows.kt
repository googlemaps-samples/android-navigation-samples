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

package com.example.mapdemo.testing

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
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
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.NavigationApi.NavigatorListener
import com.google.android.libraries.navigation.NavigationView
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.SupportNavigationFragment
import com.google.maps.android.robolectric.shadows.ShadowBitmapDescriptor
import com.google.maps.android.robolectric.shadows.ShadowCameraUpdate
import com.google.maps.android.robolectric.shadows.ShadowCircle
import com.google.maps.android.robolectric.shadows.ShadowGoogleMap
import com.google.maps.android.robolectric.shadows.ShadowGroundOverlay
import com.google.maps.android.robolectric.shadows.ShadowMapView
import com.google.maps.android.robolectric.shadows.ShadowMarker
import com.google.maps.android.robolectric.shadows.ShadowPolygon
import com.google.maps.android.robolectric.shadows.ShadowPolyline
import com.google.maps.android.robolectric.shadows.ShadowProjection
import com.google.maps.android.robolectric.shadows.ShadowSupportMapFragment
import com.google.maps.android.robolectric.shadows.ShadowTileOverlay
import com.google.maps.android.robolectric.shadows.ShadowUiSettings
import java.lang.reflect.Proxy
import org.mockito.Mockito
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.Shadows
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowViewGroup
import org.robolectric.util.ReflectionHelpers.ClassParameter

/**
 * Base class for Robolectric tests in `map-sample` that configures SDK 34, native graphics, and
 * Robolectric shadows for both the Navigation SDK's bundled Maps classes and the Navigation SDK UI
 * containers (`SupportNavigationFragment`, `NavigationView`, `NavigationApi`).
 *
 * Because Robolectric's `ConfigMerger` inspects `@Config` on the test class and its superclass
 * hierarchy (rather than meta-annotations), extending [NavMapsRobolectricTest] guarantees that all
 * custom shadows are active for every test method.
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
  sdk = [34],
  shadows =
    [
      NavShadowGoogleMap::class,
      NavShadowMapView::class,
      NavShadowSupportMapFragment::class,
      ShadowMarker::class,
      ShadowPolyline::class,
      ShadowPolygon::class,
      ShadowCircle::class,
      ShadowGroundOverlay::class,
      ShadowTileOverlay::class,
      ShadowCameraUpdate::class,
      NavShadowCameraUpdateFactory::class,
      ShadowBitmapDescriptor::class,
      NavShadowBitmapDescriptorFactory::class,
      NavShadowUiSettings::class,
      ShadowProjection::class,
      ShadowSupportNavigationFragment::class,
      ShadowNavigationView::class,
      ShadowNavigationApi::class,
    ],
)
open class NavMapsRobolectricTest

/**
 * Reflective constructor helper that creates instances of Navigation SDK's bundled Maps classes by
 * supplying a dynamic proxy for their single-argument obfuscated internal delegate interface.
 */
object NavDelegateReflection {

  /**
   * Constructs an instance of [clazz] using its single-argument constructor and a no-op dynamic
   * proxy implementing that parameter's interface.
   */
  @JvmStatic
  fun <T> constructWithDummyDelegate(clazz: Class<T>): T {
    val constructor =
      clazz.declaredConstructors.firstOrNull { it.parameterCount == 1 }
        ?: throw IllegalStateException("No single-arg constructor found on ${clazz.name}")
    constructor.isAccessible = true
    val paramType = constructor.parameterTypes[0]
    val dummyDelegate =
      if (paramType.isInterface) {
        Proxy.newProxyInstance(paramType.classLoader, arrayOf(paramType)) { _, method, _ ->
          when (method.returnType) {
            Boolean::class.javaPrimitiveType -> false
            Int::class.javaPrimitiveType -> 0
            Long::class.javaPrimitiveType -> 0L
            Float::class.javaPrimitiveType -> 0f
            Double::class.javaPrimitiveType -> 0.0
            else -> null
          }
        }
      } else {
        Mockito.mock(paramType)
      }
    @Suppress("UNCHECKED_CAST")
    return constructor.newInstance(dummyDelegate) as T
  }

  /** Creates a [GoogleMap] wired to a [ShadowGoogleMap]. */
  @JvmStatic
  fun createGoogleMapInstance(): GoogleMap {
    return constructWithDummyDelegate(GoogleMap::class.java)
  }

  /** Extracts the [GoogleMap] held by either a [SupportMapFragment] or [SupportNavigationFragment]. */
  @JvmStatic
  fun extractActivityMap(fragment: Any?): GoogleMap {
    Shadows.shadowOf(Looper.getMainLooper()).idle()
    return when (fragment) {
      is SupportNavigationFragment -> {
        val shadow = Shadow.extract<ShadowSupportNavigationFragment>(fragment)
        shadow.googleMap
          ?: throw IllegalStateException("GoogleMap was not initialized on SupportNavigationFragment")
      }
      is SupportMapFragment -> {
        val shadow = Shadow.extract<NavShadowSupportMapFragment>(fragment)
        shadow.resolveGoogleMap()
          ?: throw IllegalStateException("GoogleMap was not initialized on SupportMapFragment")
      }
      else -> throw IllegalArgumentException("Unexpected fragment type: $fragment")
    }
  }
}

/**
 * Subclass of [ShadowGoogleMap] that handles Navigation SDK's obfuscated `UiSettings` and
 * `Projection` constructors.
 */
@Implements(GoogleMap::class)
class NavShadowGoogleMap : ShadowGoogleMap() {
  private var navUiSettings: UiSettings? = null

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
}

/**
 * Subclass of [ShadowUiSettings] adding full state tracking for
 * `isScrollGesturesEnabledDuringRotateOrZoom`, which `UiSettingsDemoActivity` toggles.
 */
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

/** Subclass of [ShadowMapView] that creates a [GoogleMap] compatible with Navigation SDK's classes. */
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
 * Subclass of [ShadowSupportMapFragment] that creates a [GoogleMap] compatible with Navigation
 * SDK's bundled Maps classes and lays out its container view with default 400x400 bounds so
 * `OnMapAndViewReadyListener` and screen-projection calculations work out of the box.
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
    callback.onMapReady(currentMap)
  }

  fun resolveGoogleMap(): GoogleMap? {
    val view = getMapView()
    if (view != null) {
      val shadowView = Shadow.extract<ShadowMapView>(view)
      if (shadowView.getGoogleMap() != null) {
        return shadowView.getGoogleMap()
      }
    }
    val field = ShadowSupportMapFragment::class.java.getDeclaredField("googleMap")
    field.isAccessible = true
    return field.get(this) as? GoogleMap
  }
}

/**
 * Shadow for [CameraUpdateFactory] that creates [CameraUpdate] instances via Navigation SDK's
 * single-argument delegate constructor and configures their [ShadowCameraUpdate] state.
 */
@Implements(CameraUpdateFactory::class)
class NavShadowCameraUpdateFactory {
  companion object {
    private fun createUpdate(configure: (ShadowCameraUpdate) -> Unit): CameraUpdate {
      val update = NavDelegateReflection.constructWithDummyDelegate(CameraUpdate::class.java)
      val shadow = Shadow.extract<ShadowCameraUpdate>(update)
      configure(shadow)
      return update
    }

    @Implementation
    @JvmStatic
    fun newCameraPosition(cameraPosition: CameraPosition): CameraUpdate = createUpdate { shadow ->
      shadow.cameraPosition = cameraPosition
    }

    @Implementation
    @JvmStatic
    fun newLatLng(latLng: LatLng): CameraUpdate = createUpdate { shadow ->
      shadow.latLng = latLng
    }

    @Implementation
    @JvmStatic
    fun newLatLngZoom(latLng: LatLng, zoom: Float): CameraUpdate = createUpdate { shadow ->
      shadow.latLng = latLng
      shadow.zoom = zoom
    }

    @Implementation
    @JvmStatic
    fun newLatLngBounds(bounds: LatLngBounds, padding: Int): CameraUpdate = createUpdate { shadow ->
      shadow.setLatLngBounds(bounds, padding)
    }

    @Implementation
    @JvmStatic
    fun newLatLngBounds(
      bounds: LatLngBounds,
      width: Int,
      height: Int,
      padding: Int,
    ): CameraUpdate = createUpdate { shadow ->
      shadow.setLatLngBounds(bounds, width, height, padding)
    }

    @Implementation
    @JvmStatic
    fun zoomIn(): CameraUpdate = zoomBy(1.0f)

    @Implementation
    @JvmStatic
    fun zoomOut(): CameraUpdate = zoomBy(-1.0f)

    @Implementation
    @JvmStatic
    fun zoomTo(zoom: Float): CameraUpdate = createUpdate { shadow ->
      shadow.zoom = zoom
    }

    @Implementation
    @JvmStatic
    fun zoomBy(amount: Float): CameraUpdate = createUpdate { shadow ->
      shadow.zoomBy = amount
    }

    @Implementation
    @JvmStatic
    fun zoomBy(amount: Float, focus: Point): CameraUpdate = zoomBy(amount)

    @Implementation
    @JvmStatic
    fun scrollBy(xPixel: Float, yPixel: Float): CameraUpdate = createUpdate { shadow ->
      shadow.setScroll(xPixel, yPixel)
    }
  }
}

/**
 * Shadow for [BitmapDescriptorFactory] that constructs [BitmapDescriptor] instances via Navigation
 * SDK's single-argument delegate constructor and configures their [ShadowBitmapDescriptor].
 */
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

    @Implementation
    @JvmStatic
    fun defaultMarker(): BitmapDescriptor = defaultMarker(HUE_RED)

    @Implementation
    @JvmStatic
    fun defaultMarker(hue: Float): BitmapDescriptor =
      createDescriptor(null, "defaultMarker:hue=$hue")

    @Implementation
    @JvmStatic
    fun fromResource(resourceId: Int): BitmapDescriptor =
      createDescriptor(null, "resource:$resourceId")

    @Implementation
    @JvmStatic
    fun fromBitmap(image: Bitmap?): BitmapDescriptor =
      createDescriptor(image, "bitmap:${System.identityHashCode(image)}")

    @Implementation
    @JvmStatic
    fun fromAsset(assetName: String): BitmapDescriptor =
      createDescriptor(null, "asset:$assetName")

    @Implementation
    @JvmStatic
    fun fromFile(fileName: String): BitmapDescriptor =
      createDescriptor(null, "file:$fileName")

    @Implementation
    @JvmStatic
    fun fromPath(absolutePath: String): BitmapDescriptor =
      createDescriptor(null, "path:$absolutePath")
  }
}

/**
 * Robolectric shadow for [SupportNavigationFragment] so activities using the Navigation SDK flavor
 * (`SupportNavigationFragment.getMapAsync(...)`) can be tested headlessly on the JVM alongside
 * `SupportMapFragment`.
 */
@Implements(SupportNavigationFragment::class)
class ShadowSupportNavigationFragment {
  @RealObject private lateinit var realFragment: SupportNavigationFragment

  var googleMap: GoogleMap? = null
    private set

  private var pendingCallback: OnMapReadyCallback? = null
  var isHeaderEnabledState: Boolean = true
    private set
  var isEtaCardEnabledState: Boolean = true
    private set

  @Implementation
  protected fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    val view = FrameLayout(realFragment.requireContext())
    view.layout(0, 0, 400, 400)
    pendingCallback?.let { cb ->
      triggerOnMapReady(cb)
      pendingCallback = null
    }
    return view
  }

  @Implementation
  fun getMapAsync(callback: OnMapReadyCallback) {
    if (googleMap == null) {
      googleMap = NavDelegateReflection.createGoogleMapInstance()
    }
    if (realFragment.view != null || realFragment.activity != null) {
      triggerOnMapReady(callback)
    } else {
      pendingCallback = callback
    }
  }

  @Implementation
  fun setHeaderEnabled(isEnabled: Boolean) {
    isHeaderEnabledState = isEnabled
  }

  @Implementation
  fun setEtaCardEnabled(isEnabled: Boolean) {
    isEtaCardEnabledState = isEnabled
  }

  private fun triggerOnMapReady(callback: OnMapReadyCallback) {
    if (googleMap == null) {
      googleMap = NavDelegateReflection.createGoogleMapInstance()
    }
    val map = googleMap!!
    Handler(Looper.getMainLooper()).post { callback.onMapReady(map) }
  }
}

/** Robolectric shadow for [NavigationView]. */
@Implements(NavigationView::class)
class ShadowNavigationView : ShadowViewGroup() {
  @RealObject private lateinit var realNavigationView: NavigationView

  var googleMap: GoogleMap? = null
    private set

  @Implementation
  protected fun __constructor__(context: Context) {
    Shadow.invokeConstructor(
      RelativeLayout::class.java,
      realNavigationView,
      ClassParameter.from(Context::class.java, context),
    )
  }

  @Implementation
  protected fun __constructor__(context: Context, attrs: AttributeSet?) {
    Shadow.invokeConstructor(
      RelativeLayout::class.java,
      realNavigationView,
      ClassParameter.from(Context::class.java, context),
      ClassParameter.from(AttributeSet::class.java, attrs),
    )
  }

  @Implementation
  protected fun __constructor__(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
    Shadow.invokeConstructor(
      RelativeLayout::class.java,
      realNavigationView,
      ClassParameter.from(Context::class.java, context),
      ClassParameter.from(AttributeSet::class.java, attrs),
      ClassParameter.from(Int::class.javaPrimitiveType, defStyleAttr),
    )
  }

  @Implementation fun onCreate(savedInstanceState: Bundle?) {}

  @Implementation fun onStart() {}

  @Implementation fun onResume() {}

  @Implementation fun onPause() {}

  @Implementation fun onStop() {}

  @Implementation fun onDestroy() {}

  @Implementation fun onSaveInstanceState(bundle: Bundle?) {}

  @Implementation
  fun getMapAsync(callback: OnMapReadyCallback) {
    if (googleMap == null) {
      googleMap = NavDelegateReflection.createGoogleMapInstance()
    }
    callback.onMapReady(googleMap!!)
  }
}

/** Robolectric shadow for [NavigationApi]. */
@Implements(NavigationApi::class)
class ShadowNavigationApi {
  companion object {
    @JvmStatic var mockNavigator: Navigator = Mockito.mock(Navigator::class.java)

    @Implementation
    @JvmStatic
    fun getNavigator(application: Application, listener: NavigatorListener) {
      listener.onNavigatorReady(mockNavigator)
    }

    @Implementation
    @JvmStatic
    fun getNavigator(activity: Activity, listener: NavigatorListener) {
      listener.onNavigatorReady(mockNavigator)
    }

    @Implementation
    @JvmStatic
    fun getNavigator(activity: FragmentActivity, listener: NavigatorListener) {
      listener.onNavigatorReady(mockNavigator)
    }
  }
}
