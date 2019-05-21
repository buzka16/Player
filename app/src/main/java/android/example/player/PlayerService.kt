package android.example.player

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.IBinder
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager


class PlayerService : Service() {

    private var playerNotificationManager: PlayerNotificationManager? = null
    private val context: Context = this


    override fun onDestroy() {
        playerNotificationManager?.setPlayer(null)
        AudioPlayer.player?.release()
        super.onDestroy()
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        playerNotificationManager =
            PlayerNotificationManager.createWithNotificationChannel(
                context,
                "1",
                R.string.channel_name,
                1,
                object : PlayerNotificationManager.MediaDescriptionAdapter {

                    override fun createCurrentContentIntent(player: Player?): PendingIntent? {
                        return PendingIntent.getActivity(
                            context,
                            0,
                            Intent(context, MainActivity::class.java),
                            PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    }

                    override fun getCurrentContentText(player: Player?): String? {
                        return AudioPlayer.songInfo[AudioPlayer.player?.currentWindowIndex ?: 0][0]
                    }

                    override fun getCurrentContentTitle(player: Player?): String {
                        return AudioPlayer.songInfo[AudioPlayer.player?.currentWindowIndex ?: 0][1]
                    }

                    override fun getCurrentLargeIcon(
                        player: Player?,
                        callback: PlayerNotificationManager.BitmapCallback?
                    ): Bitmap? {
                        return AudioPlayer.albumImages[AudioPlayer.player?.currentWindowIndex ?: 0].icon
                    }

                },
                object : PlayerNotificationManager.NotificationListener {
                    override fun onNotificationCancelled(notificationId: Int) {
                        stopSelf()
                    }

                    override fun onNotificationStarted(notificationId: Int, notification: Notification?) {
                        startForeground(notificationId, notification)
                    }
                }).apply {
                setUsePlayPauseActions(true)
                setUseStopAction(true)
                setRewindIncrementMs(0)
                setFastForwardIncrementMs(0)
            }
        playerNotificationManager?.setPlayer(AudioPlayer.player)

        return START_STICKY
    }

}