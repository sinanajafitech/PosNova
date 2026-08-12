package com.cyebrcina.pos.core.util

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays an alert tone when the Incoming Call popup appears. Uses the
 * device's own default *ringtone* (not notification sound, unlike
 * [WaiterCallAlertPlayer]) — a phone-call event should sound like a phone
 * ringing, and it keeps the two alerts distinguishable by ear.
 *
 * Loops on the alarm audio stream (louder by default than the ringtone
 * stream, and more likely to cut through a busy kitchen) until [stop] is
 * called — callers MUST call [stop] the moment the popup is dismissed for
 * any reason (acknowledged, saved, or converted into an order), or the
 * previous behavior of a single non-looping play() would otherwise still
 * leave it ringing well past when staff have already dealt with the call.
 */
@Singleton
class IncomingCallAlertPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var ringtone: Ringtone? = null

    fun play() {
        stop()
        runCatching {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val newRingtone = RingtoneManager.getRingtone(context, uri) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                newRingtone.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                newRingtone.setLooping(true)
                newRingtone.setVolume(1f)
            }
            newRingtone.play()
            ringtone = newRingtone
        }.onFailure { Log.w(TAG, "Couldn't play the incoming-call alert sound", it) }
    }

    fun stop() {
        runCatching { ringtone?.takeIf { it.isPlaying }?.stop() }
        ringtone = null
    }

    private companion object {
        const val TAG = "IncomingCallAlert"
    }
}
