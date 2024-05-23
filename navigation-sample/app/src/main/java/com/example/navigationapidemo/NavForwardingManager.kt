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

package com.example.navigationapidemo

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.libraries.navigation.Navigator

/** Starts and stops the forwarding of turn-by-turn nav info from Nav SDK. */
object NavForwardingManager {
  /**
   * Registers a service to receive navigation updates and creates a fragment to display the
   * received nav info.
   */
  fun startNavForwarding(
    navigator: Navigator,
    context: Context,
    fragmentManager: FragmentManager,
  ): Fragment {
    val success =
      navigator.registerServiceForNavUpdates(
        context.packageName,
        NavInfoReceivingService::class.java.name,
        Int.MAX_VALUE,
      ) // Send all remaining steps.
    if (success) {
      Toast.makeText(context, "Successfully registered service for nav updates", Toast.LENGTH_SHORT)
        .show()
    } else {
      Toast.makeText(context, "Failed to register service for nav updates", Toast.LENGTH_SHORT)
        .show()
    }
    val navInfoDisplayFragment: Fragment = NavInfoDisplayFragment()
    fragmentManager.beginTransaction().add(R.id.nav_info_frame, navInfoDisplayFragment).commit()
    return navInfoDisplayFragment
  }

  /**
   * Unregisters the service receiving navigation updates and removes the nav info display fragment.
   */
  fun stopNavForwarding(
    navigator: Navigator,
    context: Context,
    fragmentManager: FragmentManager,
    navInfoFragment: Fragment,
  ) {
    // Remove the display header.
    fragmentManager.beginTransaction().remove(navInfoFragment).commit()
    // Unregister the nav info receiving service.
    val success = navigator.unregisterServiceForNavUpdates()
    if (success) {
      Toast.makeText(context, "Unregistered service for nav updates", Toast.LENGTH_SHORT).show()
    } else {
      // This may happen if no service had been registered.
      Toast.makeText(context, "Failed to unregister service for nav updates", Toast.LENGTH_SHORT)
        .show()
    }
  }
}
