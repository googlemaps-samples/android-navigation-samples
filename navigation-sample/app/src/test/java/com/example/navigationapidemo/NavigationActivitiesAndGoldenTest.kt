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

package com.example.navigationapidemo

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import android.widget.ListView
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import com.example.navigationapidemo.EdgeToEdgeUtil.EdgeToEdgeMarginConfig
import com.example.navigationapidemo.testing.NavMapsRobolectricTest
import com.example.navigationapidemo.testing.NavShadowNavigationApi
import com.example.navigationapidemo.testing.NavShadowNavigationView
import com.example.navigationapidemo.testing.NavShadowSupportNavigationFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.NavigationView
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.Simulator
import com.google.android.libraries.navigation.SupportNavigationFragment
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.robolectric.ktx.createTestMap
import com.google.maps.android.robolectric.powerassert.assertGeo
import com.google.maps.android.robolectric.powerassert.assertMap
import com.google.maps.android.robolectric.powerassert.assertMarker
import com.google.maps.android.robolectric.powerassert.connects
import com.google.maps.android.robolectric.powerassert.isCenteredOn
import com.google.maps.android.robolectric.powerassert.meters
import com.google.maps.android.testing.golden.GoldenTileStrategy
import com.google.maps.android.testing.golden.assertMatchesGolden
import com.google.maps.android.testing.golden.enableGoldenTesting
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowToast

/**
 * Headless JVM Robolectric unit and deterministic golden screenshot tests for `navigation-sample`
 * activities ([MainActivity], [NavViewActivity], [NavFragmentActivity],
 * [SwappingMapAndNavActivity], [PlacePickerActivity]) and [EdgeToEdgeUtil].
 */
@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
class NavigationActivitiesAndGoldenTest : NavMapsRobolectricTest() {

  @Before
  fun setUp() {
    NavShadowNavigationApi.reset()
  }

  @After
  fun tearDown() {
    NavShadowNavigationApi.reset()
  }

  @Test
  fun edgeToEdgeUtil_appliesSystemBarInsetsSelectivelyToConfiguredMargins() {
    val activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
    val targetView =
      View(activity).apply { layoutParams = MarginLayoutParams(100, 100) }
    val parent = FrameLayout(activity).apply { addView(targetView) }

    EdgeToEdgeUtil.setMarginForEdgeToEdgeSupport(
      EdgeToEdgeMarginConfig(
        view = targetView,
        shouldSetTopMargin = true,
        shouldSetBottomMargin = false,
        shouldSetLeftMargin = true,
        shouldSetRightMargin = false,
      )
    )

    val windowInsets =
      WindowInsetsCompat.Builder()
        .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(12, 24, 36, 48))
        .build()
    ViewCompat.dispatchApplyWindowInsets(parent, windowInsets)

    val mlp = targetView.layoutParams as MarginLayoutParams
    assertGeo(
      mlp.leftMargin == 12 &&
        mlp.topMargin == 24 &&
        mlp.rightMargin == 0 &&
        mlp.bottomMargin == 0
    )
  }

  @Test
  fun mainActivity_populatesDemoListAndLaunchesSelectedDemoActivity() {
    val activity = Robolectric.buildActivity(MainActivity::class.java).create().start().resume().get()
    val listView = activity.findViewById<ListView>(R.id.list_view)

    assertGeo(listView.adapter.count == 3)

    val expectedActivities =
      mapOf(
        "NavViewActivity" to NavViewActivity::class.java.name,
        "NavFragmentActivity" to NavFragmentActivity::class.java.name,
        "SwappingMapAndNavActivity" to SwappingMapAndNavActivity::class.java.name,
      )

    for (i in 0 until listView.adapter.count) {
      val itemName = listView.adapter.getItem(i) as String
      listView.performItemClick(listView.adapter.getView(i, null, listView), i, i.toLong())
      val nextIntent = shadowOf(activity).nextStartedActivity
      assertGeo(
        nextIntent != null && nextIntent.component?.className == expectedActivities[itemName]
      )
    }
  }

  @Test
  fun navViewActivity_executesPendingNavigatorActionsAndForwardsLifecycleToNavigationView() {
    val mockNavigator = mock(Navigator::class.java)
    NavShadowNavigationApi.configuredNavigator = mockNavigator

    val controller = Robolectric.buildActivity(NavViewActivity::class.java).create().start().resume()
    val activity = controller.get()
    ShadowLooper.idleMainLooper()

    val navView = activity.findViewById<NavigationView>(R.id.navigation_view)
    val shadowNavView = Shadow.extract<NavShadowNavigationView>(navView)

    assertGeo(
      shadowNavView.isOnCreateCalled &&
        shadowNavView.isOnStartCalled &&
        shadowNavView.isOnResumeCalled &&
        activity.navigatorScope?.navigator === mockNavigator &&
        activity.pendingNavActions.isEmpty()
    )

    verify(mockNavigator).setTaskRemovedBehavior(Navigator.TaskRemovedBehavior.QUIT_SERVICE)

    // Toggle Trip Progress Bar and Navigation UI on NavViewActivity
    activity.toggleTripProgressBarUi(null)
    activity.toggleNavigationUiEnabled(null)

    // Verify lifecycle forwarding on pause, stop, saveInstanceState, and destroy
    val outState = Bundle()
    controller.saveInstanceState(outState).pause().stop().destroy()
    assertGeo(
      shadowNavView.isOnSaveInstanceStateCalled &&
        shadowNavView.isOnPauseCalled &&
        shadowNavView.isOnStopCalled &&
        shadowNavView.isOnDestroyCalled
    )
    verify(mockNavigator).cleanup()
  }

  @Test
  fun navViewActivity_displaysExpectedErrorToastWhenNavigationApiFailsAuthorization() {
    NavShadowNavigationApi.configuredErrorCode = NavigationApi.ErrorCode.NOT_AUTHORIZED

    Robolectric.buildActivity(NavViewActivity::class.java).create().start().resume()
    ShadowLooper.idleMainLooper()

    val latestToast = ShadowToast.getTextOfLatestToast()
    assertThat(latestToast).contains("Your API key is invalid or not authorized")
  }

  @Test
  fun navFragmentActivity_initializesSupportNavigationFragmentAndHandlesPanelInteractions() {
    val mockNavigator = mock(Navigator::class.java)
    val mockSimulator = mock(Simulator::class.java)
    `when`(mockNavigator.simulator).thenReturn(mockSimulator)
    NavShadowNavigationApi.configuredNavigator = mockNavigator

    val controller =
      Robolectric.buildActivity(NavFragmentActivity::class.java).create().start().resume()
    val activity = controller.get()
    ShadowLooper.idleMainLooper()

    val navFragment =
      activity.supportFragmentManager.findFragmentById(R.id.navigation_fragment)
        as SupportNavigationFragment
    val shadowNavFragment = Shadow.extract<NavShadowSupportNavigationFragment>(navFragment)

    // Move camera to Melbourne via NavFragmentActivity's Map panel handler
    activity.moveCameraToMelbourne(null)
    val map =
      checkNotNull(shadowNavFragment.mapView).let { view ->
        var readyMap: GoogleMap? = null
        view.getMapAsync { readyMap = it }
        ShadowLooper.idleMainLooper()
        checkNotNull(readyMap)
      }

    val melbourne = LatLng(-37.813, 144.962)
    with(map) {
      assertMap(isCenteredOn(melbourne, expectedZoom = 10f, tolerance = 5.meters))
    }

    controller.destroy()
    verify(mockSimulator).unsetUserLocation()
    verify(mockNavigator).cleanup()
  }

  @Test
  fun swappingMapAndNavActivity_startsWithSupportMapFragmentAndHandlesStopTrip() {
    val mockNavigator = mock(Navigator::class.java)
    NavShadowNavigationApi.configuredNavigator = mockNavigator

    val controller =
      Robolectric.buildActivity(SwappingMapAndNavActivity::class.java).create().start().resume()
    val activity = controller.get()
    ShadowLooper.idleMainLooper()

    val initialFragment = activity.supportFragmentManager.findFragmentById(R.id.container)
    assertGeo(initialFragment is SupportMapFragment)

    activity.stopTripAndShowMapFragment(null)
    verify(mockNavigator).clearDestinations()

    controller.destroy()
    verify(mockNavigator).cleanup()
  }

  @Test
  fun navigationSampleApplication_skipsPlacesInitOnDefaultKeyAndInitializesOnValidKey() {
    Places.deinitialize()
    val app = ApplicationProvider.getApplicationContext<NavigationSampleApplication>()
    val packageInfo =
      app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_META_DATA)
    val appInfo = checkNotNull(packageInfo.applicationInfo)
    if (appInfo.metaData == null) {
      appInfo.metaData = Bundle()
    }

    // 1. Default placeholder key ("DEFAULT_API_KEY") skips Places initialization
    appInfo.metaData.putString("com.google.android.geo.API_KEY", "DEFAULT_API_KEY")
    shadowOf(app.packageManager).installPackage(packageInfo)
    app.onCreate()
    assertGeo(!Places.isInitialized())

    // 2. "YOUR_API_KEY" placeholder also skips Places initialization
    appInfo.metaData.putString("com.google.android.geo.API_KEY", "YOUR_API_KEY")
    shadowOf(app.packageManager).installPackage(packageInfo)
    app.onCreate()
    assertGeo(!Places.isInitialized())

    // 3. Valid non-placeholder key initializes Places SDK with new Places API enabled
    appInfo.metaData.putString("com.google.android.geo.API_KEY", "AIzaSyTestUnitTestingKey123456")
    shadowOf(app.packageManager).installPackage(packageInfo)
    app.onCreate()
    assertGeo(Places.isInitialized())
    Places.deinitialize()
  }

  @Test
  fun initializedScopes_queueAndExecutePendingMapAndNavigatorActions() {
    val mockNavigator = mock(Navigator::class.java)
    NavShadowNavigationApi.autoDeliverResult = false

    val activity = Robolectric.buildActivity(NavViewActivity::class.java).create().get()

    var executedNavigator: Navigator? = null
    val navRunnable: InitializedNavRunnable = { executedNavigator = navigator }
    activity.pendingNavActions.add(navRunnable)
    assertGeo(activity.navigatorScope == null && activity.pendingNavActions.contains(navRunnable))

    // Deliver the ready Navigator to the listener captured during onCreate()
    checkNotNull(NavShadowNavigationApi.lastListener).onNavigatorReady(mockNavigator)

    assertGeo(
      executedNavigator === mockNavigator &&
        activity.navigatorScope?.navigator === mockNavigator &&
        activity.pendingNavActions.isEmpty()
    )

    // Verify InitializedMapScope contract directly with createTestMap()
    val testMap = createTestMap(width = 100, height = 100)
    val mapScope =
      object : InitializedMapScope {
        override val map: GoogleMap = testMap
      }
    assertGeo(mapScope.map === testMap)
  }

  @Test
  fun placePickerActivity_handlesUninitializedAndInitializedStatesAndExtractsPlaceExtra() {
    Places.deinitialize()
    val controller = Robolectric.buildActivity(PlacePickerActivity::class.java).create()
    val activity = controller.get()

    // 1. When Places is not initialized, PlacePickerActivity shows a Toast and finishes
    assertGeo(activity.isFinishing)
    assertThat(ShadowToast.getTextOfLatestToast()).contains("Places SDK is not initialized")

    // 2. When Places is initialized, PlacePickerActivity launches PlaceAutocomplete intent
    val appContext = ApplicationProvider.getApplicationContext<Context>()
    Places.initializeWithNewPlacesApiEnabled(appContext, "AIzaSyTestUnitTestingKey123456")
    val initializedActivity = Robolectric.buildActivity(PlacePickerActivity::class.java).create().get()
    val startedForResult = shadowOf(initializedActivity).nextStartedActivityForResult
    assertGeo(!initializedActivity.isFinishing && startedForResult != null)
    Places.deinitialize()

    // 3. Verify PlacePickerActivity.getPlace(intent) extracts a Place parcelable and rejects empty Intent
    val expectedPlace =
      Place.builder()
        .setId("place_sydney_opera_house")
        .setDisplayName("Sydney Opera House")
        .setLocation(LatLng(-33.8568, 151.2153))
        .build()
    val resultIntent = Intent().putExtra("PLACE", expectedPlace)
    val extracted = PlacePickerActivity.getPlace(resultIntent)

    assertGeo(
      extracted.id == "place_sydney_opera_house" &&
        extracted.displayName == "Sydney Opera House" &&
        extracted.location == LatLng(-33.8568, 151.2153)
    )
    assertThrows(IllegalArgumentException::class.java) {
      PlacePickerActivity.getPlace(Intent())
    }
  }

  @Test
  fun navigationRouteOverlay_matchesCommittedGoldenBaseline() = runTest {
    val map = createTestMap(width = 200, height = 200)
    map.enableGoldenTesting(strategy = GoldenTileStrategy.SolidColorHash())

    val melbourne = LatLng(-37.813, 144.962)
    val destination = LatLng(-37.805, 144.970)
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(melbourne, 13f))

    val originMarker =
      checkNotNull(
        map.addMarker(MarkerOptions().position(melbourne).title("Origin: Melbourne"))
      )
    val routeLine =
      checkNotNull(
        map.addPolyline(
          PolylineOptions().add(melbourne, destination).color(Color.BLUE).width(6f)
        )
      )
    map.addCircle(
      CircleOptions()
        .center(destination)
        .radius(300.0)
        .fillColor(Color.GREEN)
        .strokeWidth(2f)
    )

    originMarker.assertMarker(originMarker.title == "Origin: Melbourne")
    assertGeo(routeLine.connects(melbourne, destination, tolerance = 5.meters))

    map.assertMatchesGolden(
      goldenFile = File("src/test/resources/goldens/navigation_route_overlay.png"),
      maxDiffPercentage = 0.0,
      message = "Navigation route overlay must match committed golden baseline PNG",
    )
  }

  @Test
  fun navFragmentActivity_melbourneCameraViewMatchesCommittedGoldenBaseline() = runTest {
    val mockNavigator = mock(Navigator::class.java)
    val mockSimulator = mock(Simulator::class.java)
    `when`(mockNavigator.simulator).thenReturn(mockSimulator)
    NavShadowNavigationApi.configuredNavigator = mockNavigator

    val activity =
      Robolectric.buildActivity(NavFragmentActivity::class.java).create().start().resume().get()
    ShadowLooper.idleMainLooper()

    val navFragment =
      activity.supportFragmentManager.findFragmentById(R.id.navigation_fragment)
        as SupportNavigationFragment
    val shadowNavFragment = Shadow.extract<NavShadowSupportNavigationFragment>(navFragment)
    val mapView = checkNotNull(shadowNavFragment.mapView)
    mapView.measure(
      View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
    )
    mapView.layout(0, 0, 200, 200)

    var readyMap: GoogleMap? = null
    mapView.getMapAsync { readyMap = it }
    ShadowLooper.idleMainLooper()
    val map = checkNotNull(readyMap)

    map.enableGoldenTesting(strategy = GoldenTileStrategy.CoordinateGrid())
    activity.moveCameraToMelbourne(null)
    map.addMarker(MarkerOptions().position(LatLng(-37.813, 144.962)).title("Melbourne"))

    map.assertMatchesGolden(
      goldenFile = File("src/test/resources/goldens/nav_fragment_melbourne_grid.png"),
      maxDiffPercentage = 0.0,
      message = "NavFragmentActivity Melbourne camera view must match committed golden baseline PNG",
    )
  }
}
