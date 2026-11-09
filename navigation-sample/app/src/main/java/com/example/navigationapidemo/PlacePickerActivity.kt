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
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import com.example.navigationapidemo.EdgeToEdgeUtil.EdgeToEdgeMarginConfig
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.widget.PlaceAutocomplete
import com.google.android.libraries.places.widget.PlaceAutocompleteActivity

/** An activity to launch PlaceAutocomplete from Places SDK and fetch the selected Place. */
class PlacePickerActivity : AppCompatActivity() {

  private val autocompleteLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      val data = result.data
      when (result.resultCode) {
        RESULT_OK -> {
          val prediction = data?.let { PlaceAutocomplete.getPredictionFromIntent(it) }
          val sessionToken = data?.let { PlaceAutocomplete.getSessionTokenFromIntent(it) }
          if (prediction != null) {
            val fields =
              listOf(
                Place.Field.ID,
                Place.Field.DISPLAY_NAME,
                Place.Field.LOCATION,
                Place.Field.TYPES,
              )
            val request =
              FetchPlaceRequest.builder(prediction.placeId, fields)
                .setSessionToken(sessionToken)
                .build()
            Places.createClient(this)
              .fetchPlace(request)
              .addOnSuccessListener { response ->
                setResult(RESULT_OK, Intent().putExtra("PLACE", response.place))
                finish()
              }
              .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to fetch place details for ${prediction.placeId}", exception)
                setResult(RESULT_CANCELED)
                finish()
              }
          } else {
            setResult(RESULT_CANCELED)
            finish()
          }
        }
        PlaceAutocompleteActivity.RESULT_ERROR -> {
          val status = data?.let { PlaceAutocomplete.getResultStatusFromIntent(it) }
          Log.e(TAG, "PlaceAutocomplete failed with status: $status")
          setResult(RESULT_CANCELED, Intent().putExtra("STATUS", status))
          finish()
        }
        else -> {
          setResult(RESULT_CANCELED)
          finish()
        }
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_place_picker)

    // Margins are only set if the edge-to-edge mode is enabled, it's enabled by default for Android
    // V+ devices.
    // No margins are set for pre-Android V devices.
    EdgeToEdgeUtil.setMarginForEdgeToEdgeSupport(
      listOf(EdgeToEdgeMarginConfig(view = findViewById(R.id.layout_container)))
    )

    if (!Places.isInitialized()) {
      Log.e(TAG, "Places SDK is not initialized; check that MAPS_API_KEY is configured.")
      Toast.makeText(
          this,
          "Places SDK is not initialized. Check your MAPS_API_KEY configuration.",
          Toast.LENGTH_LONG,
        )
        .show()
      setResult(RESULT_CANCELED)
      finish()
      return
    }

    if (savedInstanceState == null) {
      val autocompleteIntent = PlaceAutocomplete.IntentBuilder().build(this)
      autocompleteLauncher.launch(autocompleteIntent)
    }
  }

  companion object {
    private const val TAG = "PlacePickerActivity"

    fun getPlace(data: Intent): Place {
      return requireNotNull(IntentCompat.getParcelableExtra(data, "PLACE", Place::class.java))
    }
  }
}
