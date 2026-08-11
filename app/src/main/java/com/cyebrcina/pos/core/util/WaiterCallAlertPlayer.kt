package com.cyebrcina.pos.core.util

import android.content.Context
import android.media.RingtoneManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays an alert tone when a "Call Waiter" popup appears. Uses the device's
 * own default notification sound via [RingtoneManager] rather than a
 * bundled asset — always present on every Android device, nothing to
 * source, ship, or maintain, and needs no runtime permission.
 */
@Singleton
class WaiterCallAlertPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun play() {
        runCatching {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(context, uri)?.play()
        }.onFailure { Log.w(TAG, "Couldn't play the waiter-call alert sound", it) }
    }

    private companion object {
        const val TAG = "WaiterCallAlert"
    }
}
