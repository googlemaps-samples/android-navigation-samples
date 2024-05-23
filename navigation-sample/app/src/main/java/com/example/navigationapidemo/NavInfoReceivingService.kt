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

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Process
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.libraries.mapsplatform.turnbyturn.TurnByTurnManager
import com.google.android.libraries.mapsplatform.turnbyturn.model.NavInfo

/**
 * Receives turn-by-turn navigation information forwarded from NavSDK and posts each update to live
 * data, which is then displayed on a separate header in `NavInfoDisplayFragment`. This service may
 * be part of a different process aside from the main process, depending on how you want to
 * structure your app. The service binding will be able to handle interprocess communication to
 * receive nav info messages from the main process.
 */
class NavInfoReceivingService : Service() {
  /** The messenger used by the service to receive nav step updates. */
  private lateinit var incomingMessenger: Messenger

  /**
   * Ensure you do not pass a strong reference to the outer service class. This will result in a
   * memory leak.
   */
  private class IncomingNavStepHandler(
    looper: Looper,
    val turnByTurnManager: TurnByTurnManager = TurnByTurnManager.createInstance(),
  ) : Handler(looper) {
    override fun handleMessage(msg: Message) {
      if (TurnByTurnManager.MSG_NAV_INFO == msg.what) {
        // Read the nav info from the message data,
        // and post the value (if it exists) to LiveData to be displayed in the nav info header.
        turnByTurnManager.readNavInfoFromBundle(msg.data).let { navInfo ->
          navInfoMutableLiveData.postValue(navInfo)
        }
      }
    }
  }

  override fun onBind(intent: Intent): IBinder {
    return incomingMessenger.binder
  }

  override fun onUnbind(intent: Intent): Boolean {
    navInfoMutableLiveData.postValue(null)
    return super.onUnbind(intent)
  }

  override fun onCreate() {
    val thread = HandlerThread("NavInfoReceivingService", Process.THREAD_PRIORITY_DEFAULT)
    thread.start()
    incomingMessenger = Messenger(IncomingNavStepHandler(looper = thread.looper))
  }

  companion object {
    private val navInfoMutableLiveData = MutableLiveData<NavInfo?>()
    val navInfoLiveData: LiveData<NavInfo?>
      get() = navInfoMutableLiveData
  }
}
