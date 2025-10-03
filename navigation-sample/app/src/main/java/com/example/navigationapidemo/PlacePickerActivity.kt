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

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.navigationapidemo.EdgeToEdgeUtil.EdgeToEdgeMarginConfig
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import java.util.Arrays

/** An activity to host AutocompleteSupportFragment from Places SDK. */
class PlacePickerActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_place_picker)

    // Margins are only set if the edge-to-edge mode is enabled, it's enabled by default for Android
    // V+ devices.
    // No margins are set for pre-Android V devices.
    EdgeToEdgeUtil.setMarginForEdgeToEdgeSupport(
      listOf(EdgeToEdgeMarginConfig(view = findViewById(R.id.layout_container)))
    )

    // Initialize the AutocompleteSupportFragment.
    val autocompleteFragment =
      supportFragmentManager.findFragmentById(R.id.autocomplete_fragment)
        as AutocompleteSupportFragment?

    // Specify the types of place data to return.
    autocompleteFragment?.setPlaceFields(
      Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.TYPES)
    )

    // Set up a PlaceSelectionListener to handle the response.
    autocompleteFragment?.setOnPlaceSelectedListener(
      object : PlaceSelectionListener {
        override fun onPlaceSelected(place: Place) {
          setResult(RESULT_OK, Intent().putExtra("PLACE", place))
          finish()
        }

        override fun onError(status: Status) {
          setResult(RESULT_CANCELED, Intent().putExtra("STATUS", status))
          finish()
        }
      }
    )
  }

  companion object {
    fun getPlace(data: Intent): Place {
      return data.getParcelableExtra("PLACE")!!
    }
  }
}
