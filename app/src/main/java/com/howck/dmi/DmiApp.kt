package com.howck.dmi

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class DmiApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SERVICE,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_desc)
                setShowBadge(false)
            }
        )
    }

    companion object {
        const val CHANNEL_SERVICE = "capsule_service"
    }
}
