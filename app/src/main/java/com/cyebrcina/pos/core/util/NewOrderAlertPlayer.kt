package com.cyebrcina.pos.core.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.util.Base64
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays the "new order" alert configured in Admin (Settings -> Notification Sounds) when a
 * website/QR order lands on the till. [PendingOrdersResponse.notificationSoundUrl]'s own doc
 * comment says null means "use the sound bundled with the app itself" — no such asset actually
 * ships, so the device's own default notification tone serves that role instead, same pattern
 * [WaiterCallAlertPlayer] already uses. A non-null value is a `data:audio/...;base64,...` URL
 * (see SoundUpload.tsx — an admin-uploaded file, max 3MB, read client-side and stored inline,
 * not a real HTTP URL) — MediaPlayer has no direct `data:` URI support, so it's decoded once to
 * a cache file and played from there.
 */
@Singleton
class NewOrderAlertPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var mediaPlayer: MediaPlayer? = null
    private var lastDecodedSoundUrl: String? = null
    private var cachedFile: File? = null

    fun play(soundUrl: String?) {
        stop()
        if (soundUrl == null) {
            playDefaultNotificationSound()
            return
        }
        runCatching {
            val file = decodedFileFor(soundUrl)
            val player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            player.setDataSource(file.absolutePath)
            player.setOnCompletionListener { it.release() }
            player.prepare()
            player.start()
            mediaPlayer = player
        }.onFailure {
            Log.w(TAG, "Couldn't play the custom new-order sound, falling back to default", it)
            playDefaultNotificationSound()
        }
    }

    fun stop() {
        runCatching { mediaPlayer?.takeIf { it.isPlaying }?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
    }

    /** Decoded once per distinct sound URL and reused from then on — every later new-order alert
     * with the same Admin-selected sound plays straight from the cache file instead of
     * re-decoding a base64 payload (up to 3MB) on every single order. */
    private fun decodedFileFor(soundUrl: String): File {
        cachedFile?.takeIf { lastDecodedSoundUrl == soundUrl && it.exists() }?.let { return it }

        val base64Payload = soundUrl.substringAfter(",", missingDelimiterValue = "")
        require(base64Payload.isNotEmpty()) { "Not a data: URL" }
        val bytes = Base64.decode(base64Payload, Base64.DEFAULT)

        val file = File(context.cacheDir, "new_order_alert_sound")
        FileOutputStream(file).use { it.write(bytes) }
        lastDecodedSoundUrl = soundUrl
        cachedFile = file
        return file
    }

    private fun playDefaultNotificationSound() {
        runCatching {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(context, uri)?.play()
        }.onFailure { Log.w(TAG, "Couldn't play the default new-order alert sound", it) }
    }

    private companion object {
        const val TAG = "NewOrderAlert"
    }
}
