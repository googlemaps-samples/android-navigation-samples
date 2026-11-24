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

import android.os.Looper
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.libraries.navigation.SupportNavigationFragment
import com.google.maps.android.robolectric.shadows.ShadowBitmapDescriptor
import com.google.maps.android.robolectric.shadows.ShadowCameraUpdate
import com.google.maps.android.robolectric.shadows.ShadowCircle
import com.google.maps.android.robolectric.shadows.ShadowGroundOverlay
import com.google.maps.android.robolectric.shadows.ShadowMarker
import com.google.maps.android.robolectric.shadows.ShadowPolygon
import com.google.maps.android.robolectric.shadows.ShadowPolyline
import com.google.maps.android.robolectric.shadows.ShadowProjection
import com.google.maps.android.robolectric.shadows.ShadowTileOverlay
import java.lang.reflect.Proxy
import org.mockito.Mockito
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadow.api.Shadow

/**
 * Shared base class for headless Robolectric JVM tests across `navigation-sample` and `map-sample`.
 *
 * Configures SDK 34, native graphics, and Robolectric shadows for both the Navigation SDK's bundled
 * Maps classes and the Navigation SDK UI containers (`SupportNavigationFragment`, `NavigationView`,
 * `NavigationApi`). Extending this base class guarantees Robolectric's `ConfigMerger` activates all
 * shadows for every test method.
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
      NavShadowSupportNavigationFragment::class,
      NavShadowNavigationView::class,
      NavShadowNavigationApi::class,
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

  /** Creates a [GoogleMap] wired to a [NavShadowGoogleMap]. */
  @JvmStatic
  fun createGoogleMapInstance(): GoogleMap {
    return constructWithDummyDelegate(GoogleMap::class.java)
  }

  /** Extracts the [GoogleMap] held by either a [SupportMapFragment] or [SupportNavigationFragment]. */
  @Suppress("DEPRECATION")
  @JvmStatic
  fun extractActivityMap(fragment: Any?): GoogleMap {
    Shadows.shadowOf(Looper.getMainLooper()).idle()
    return when (fragment) {
      is SupportNavigationFragment -> {
        val shadow = Shadow.extract<NavShadowSupportNavigationFragment>(fragment)
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
