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

import android.app.Activity
import android.app.Application
import android.content.Context
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
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.NavigationApi.NavigatorListener
import com.google.android.libraries.navigation.NavigationView
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.SupportNavigationFragment
import org.mockito.Mockito
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowViewGroup
import org.robolectric.util.ReflectionHelpers.ClassParameter

/**
 * Robolectric shadow for [SupportNavigationFragment] so Navigation-flavor activities can be tested
 * headlessly on the JVM alongside `SupportMapFragment`.
 */
@Suppress("DEPRECATION")
@Implements(SupportNavigationFragment::class)
class NavShadowSupportNavigationFragment {
  @RealObject private lateinit var realFragment: SupportNavigationFragment

  var googleMap: GoogleMap? = null
    private set

  var mapView: MapView? = null
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
    val context = realFragment.requireContext()
    val view = FrameLayout(context)
    view.layout(0, 0, 400, 400)
    val internalMapView = MapView(context)
    internalMapView.layout(0, 0, 400, 400)
    view.addView(internalMapView)
    mapView = internalMapView
    pendingCallback?.let { cb ->
      internalMapView.getMapAsync { readyMap ->
        googleMap = readyMap
        cb.onMapReady(readyMap)
      }
      pendingCallback = null
    }
    return view
  }

  @Implementation
  fun getMapAsync(callback: OnMapReadyCallback) {
    val currentMapView = mapView
    if (currentMapView != null) {
      currentMapView.getMapAsync { readyMap ->
        googleMap = readyMap
        callback.onMapReady(readyMap)
      }
    } else if (realFragment.view != null || realFragment.activity != null) {
      if (googleMap == null) {
        googleMap = NavDelegateReflection.createGoogleMapInstance()
      }
      val map = googleMap!!
      Handler(Looper.getMainLooper()).post { callback.onMapReady(map) }
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
}

/** Robolectric shadow for [NavigationView] tracking lifecycle forwarding and map readiness. */
@Implements(NavigationView::class)
class NavShadowNavigationView : ShadowViewGroup() {
  @RealObject private lateinit var realNavigationView: NavigationView

  var googleMap: GoogleMap? = null
    private set

  var isOnCreateCalled: Boolean = false
    private set
  var isOnStartCalled: Boolean = false
    private set
  var isOnResumeCalled: Boolean = false
    private set
  var isOnPauseCalled: Boolean = false
    private set
  var isOnStopCalled: Boolean = false
    private set
  var isOnDestroyCalled: Boolean = false
    private set
  var isOnSaveInstanceStateCalled: Boolean = false
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

  @Implementation fun onCreate(savedInstanceState: Bundle?) { isOnCreateCalled = true }

  @Implementation fun onStart() { isOnStartCalled = true }

  @Implementation fun onResume() { isOnResumeCalled = true }

  @Implementation fun onPause() { isOnPauseCalled = true }

  @Implementation fun onStop() { isOnStopCalled = true }

  @Implementation fun onDestroy() { isOnDestroyCalled = true }

  @Implementation fun onSaveInstanceState(bundle: Bundle?) { isOnSaveInstanceStateCalled = true }

  @Implementation
  fun getMapAsync(callback: OnMapReadyCallback) {
    if (googleMap == null) {
      googleMap = NavDelegateReflection.createGoogleMapInstance()
    }
    callback.onMapReady(googleMap!!)
  }
}

/** Robolectric shadow for [NavigationApi] supplying a configurable [Navigator] or error code. */
@Implements(NavigationApi::class)
class NavShadowNavigationApi {
  companion object {
    @JvmStatic var configuredNavigator: Navigator? = null
    @JvmStatic var configuredErrorCode: Int? = null
    @JvmStatic var autoDeliverResult: Boolean = true
    @JvmStatic var lastListener: NavigatorListener? = null

    @JvmStatic
    fun reset() {
      configuredNavigator = null
      configuredErrorCode = null
      autoDeliverResult = true
      lastListener = null
    }

    @Implementation @JvmStatic
    fun getNavigator(application: Application, listener: NavigatorListener) {
      deliverResult(listener)
    }

    @Implementation @JvmStatic
    fun getNavigator(activity: Activity, listener: NavigatorListener) {
      deliverResult(listener)
    }

    @Implementation @JvmStatic
    fun getNavigator(activity: FragmentActivity, listener: NavigatorListener) {
      deliverResult(listener)
    }

    private fun deliverResult(listener: NavigatorListener) {
      lastListener = listener
      if (!autoDeliverResult) {
        return
      }
      val errorCode = configuredErrorCode
      if (errorCode != null) {
        listener.onError(errorCode)
      } else {
        val navigator = configuredNavigator ?: Mockito.mock(Navigator::class.java)
        listener.onNavigatorReady(navigator)
      }
    }
  }
}
