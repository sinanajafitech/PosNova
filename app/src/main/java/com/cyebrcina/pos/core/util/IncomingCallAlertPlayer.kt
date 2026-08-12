package com.cyebrcina.pos.core.util

import android.content.Context
import android.media.RingtoneManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays an alert tone when the Incoming Call popup appears. Uses the
 * device's own default *ringtone* (not notification sound, unlike
 * [WaiterCallAlertPlayer]) — a phone-call event should sound like a phone
 * ringing, and it keeps the two alerts distinguishable by ear.
 */
@Singleton
class IncomingCallAlertPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun play() {
        runCatching {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            RingtoneManager.getRingtone(context, uri)?.play()
        }.onFailure { Log.w(TAG, "Couldn't play the incoming-call alert sound", it) }
    }

    private companion object {
        const val TAG = "IncomingCallAlert"
    }
}
