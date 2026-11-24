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

import android.content.Intent
import android.os.HandlerThread
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.google.android.libraries.mapsplatform.turnbyturn.model.NavInfo
import com.google.android.libraries.mapsplatform.turnbyturn.model.NavState
import com.google.android.libraries.navigation.Navigator
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.robolectric.powerassert.assertGeo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowToast
import org.robolectric.util.ReflectionHelpers

/**
 * Headless Robolectric unit tests for [NavInfoReceivingService] lifecycle/threading and
 * [NavForwardingManager] service registration and fragment transactions.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class NavForwardingAndServiceTest {

  private lateinit var hostActivity: AppCompatActivity
  private lateinit var liveData: MutableLiveData<NavInfo?>

  @Before
  fun setUp() {
    hostActivity = Robolectric.buildActivity(AppCompatActivity::class.java).create().start().resume().get()
    hostActivity.setContentView(FrameLayout(hostActivity).apply { id = R.id.nav_info_frame })
    liveData = ReflectionHelpers.getStaticField(NavInfoReceivingService::class.java, "navInfoMutableLiveData")
    liveData.value = null
  }

  @After
  fun tearDown() {
    liveData.value = null
  }

  @Test
  fun navInfoReceivingService_bindsMessengerClearsLiveDataOnUnbindAndQuitsThreadOnDestroy() {
    val serviceController = Robolectric.buildService(NavInfoReceivingService::class.java).create()
    val service = serviceController.get()
    val intent = Intent(hostActivity, NavInfoReceivingService::class.java)

    assertThat(service.onBind(intent)).isNotNull()

    val workerThread = ReflectionHelpers.getField<HandlerThread?>(service, "handlerThread")
    assertGeo(workerThread != null && workerThread.isAlive)

    liveData.value =
      NavInfo.builder().setNavState(NavState.STOPPED).setRemainingSteps(emptyArray()).build()
    assertGeo(NavInfoReceivingService.navInfoLiveData.value != null)

    service.onUnbind(intent)
    ShadowLooper.idleMainLooper()
    assertGeo(NavInfoReceivingService.navInfoLiveData.value == null)

    serviceController.destroy()
    assertGeo(ReflectionHelpers.getField<HandlerThread?>(service, "handlerThread") == null)
  }

  @Test
  fun navForwardingManager_registersAndUnregistersServiceAndManagesDisplayFragment() {
    val mockNavigator = mock(Navigator::class.java)
    `when`(
        mockNavigator.registerServiceForNavUpdates(
          hostActivity.packageName,
          NavInfoReceivingService::class.java.name,
          Int.MAX_VALUE,
        )
      )
      .thenReturn(true)
    `when`(mockNavigator.unregisterServiceForNavUpdates()).thenReturn(true)

    val addedFragment =
      NavForwardingManager.startNavForwarding(
        navigator = mockNavigator,
        context = hostActivity,
        fragmentManager = hostActivity.supportFragmentManager,
      )
    ShadowLooper.idleMainLooper()

    assertGeo(addedFragment is NavInfoDisplayFragment && addedFragment.isAdded)
    assertThat(ShadowToast.getTextOfLatestToast())
      .isEqualTo("Successfully registered service for nav updates")
    verify(mockNavigator)
      .registerServiceForNavUpdates(
        hostActivity.packageName,
        NavInfoReceivingService::class.java.name,
        Int.MAX_VALUE,
      )

    NavForwardingManager.stopNavForwarding(
      navigator = mockNavigator,
      context = hostActivity,
      fragmentManager = hostActivity.supportFragmentManager,
      navInfoFragment = addedFragment,
    )
    ShadowLooper.idleMainLooper()

    assertGeo(!addedFragment.isAdded)
    assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo("Unregistered service for nav updates")
    verify(mockNavigator).unregisterServiceForNavUpdates()
  }

  @Test
  fun navForwardingManager_showsFailureToastsWhenServiceRegistrationOrUnregistrationFails() {
    val mockNavigator = mock(Navigator::class.java)
    `when`(
        mockNavigator.registerServiceForNavUpdates(
          hostActivity.packageName,
          NavInfoReceivingService::class.java.name,
          Int.MAX_VALUE,
        )
      )
      .thenReturn(false)
    `when`(mockNavigator.unregisterServiceForNavUpdates()).thenReturn(false)

    val addedFragment =
      NavForwardingManager.startNavForwarding(
        navigator = mockNavigator,
        context = hostActivity,
        fragmentManager = hostActivity.supportFragmentManager,
      )
    ShadowLooper.idleMainLooper()
    assertThat(ShadowToast.getTextOfLatestToast())
      .isEqualTo("Failed to register service for nav updates")

    NavForwardingManager.stopNavForwarding(
      navigator = mockNavigator,
      context = hostActivity,
      fragmentManager = hostActivity.supportFragmentManager,
      navInfoFragment = addedFragment,
    )
    ShadowLooper.idleMainLooper()
    assertThat(ShadowToast.getTextOfLatestToast())
      .isEqualTo("Failed to unregister service for nav updates")
  }
}
