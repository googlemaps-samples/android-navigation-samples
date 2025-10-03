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
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.example.navigationapidemo.EdgeToEdgeUtil.EdgeToEdgeMarginConfig

/** Main activity that lets the user choose a demo to launch. */
class MainActivity : AppCompatActivity() {
  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)

    // Margins are only set if the edge-to-edge mode is enabled, it's enabled by default for Android
    // V+ devices.
    // No margins are set for pre-Android V devices.
    EdgeToEdgeUtil.setMarginForEdgeToEdgeSupport(
      listOf(EdgeToEdgeMarginConfig(view = findViewById(R.id.main_layout_container)))
    )

    val listAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, DEMOS.keys.toList())
    val listView = findViewById<ListView>(R.id.list_view)
    listView.adapter = listAdapter
    listView.onItemClickListener = OnItemClickListener { parent, view, position, _ ->
      val demoName = parent.getItemAtPosition(position) as String
      startActivity(Intent(view.context, DEMOS[demoName]))
    }
  }

  companion object {
    private val DEMOS =
      mapOf<String, Class<*>>(
        "NavViewActivity" to NavViewActivity::class.java,
        "NavFragmentActivity" to NavFragmentActivity::class.java,
        "SwappingMapAndNavActivity" to SwappingMapAndNavActivity::class.java,
      )
  }
}
