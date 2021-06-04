package ru.kuchanov.huaweiandgoogleservices.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.inject
import ru.kuchanov.huaweiandgoogleservices.R
import timber.log.Timber

class LocationUpdatesForegroundService : Service() {

    private val compositeDisposable = CompositeDisposable()

    private val locationGateway: LocationGateway by inject()

    companion object {
        const val NOTIFICATION_ID = 1489

        const val CHANNEL_ID = "SYNC_CHANNEL_ID"

        const val CHANNEL_NAME = "SYNC_CHANNEL_NAME"
    }

    override fun onCreate() {
        super.onCreate()

        val notification =
            NotificationCompat.Builder(this, getChannelId(CHANNEL_ID, CHANNEL_NAME))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.get_location_updates))
                .setAutoCancel(false)
                .build()

        startForeground(NOTIFICATION_ID, notification)

        val startTime = System.currentTimeMillis()

        locationGateway
            .requestLocationUpdates()
            .subscribeBy(
                onNext = {
                    Timber.d(
                        "requestLocationUpdates: $it. Elapsed time: ${
                            millisToMinutesAndSeconds(
                                System.currentTimeMillis() - startTime
                            )
                        }"
                    )
                },
                onError = {
                    Timber.e(it, "requestLocationUpdates: ${it.message}")
                }
            )
            .addTo(compositeDisposable)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun getChannelId(
        channelId: String,
        channelName: String
    ): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                this,
                channelId, channelName
            )
        } else {
            ""
        }
    }

    /**
     * converts given millis to minutes:seconds format
     */
    private fun millisToMinutesAndSeconds(duration: Long): Pair<Long, Long> {
        val minutes = duration / 1000 / 60
        val seconds = duration / 1000 % 60
        return minutes to seconds
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        context: Context,
        channelId: String,
        channelName: String
    ): String {
        val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }
}