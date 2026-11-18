package com.mediabuyer.bot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "post_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "مواعيد النشر", NotificationManager.IMPORTANCE_HIGH)
            )
        }

        val title = intent.getStringExtra("title") ?: "موعد نشر بوست"
        val content = intent.getStringExtra("content") ?: "افتح التطبيق لمراجعة المنشور."
        val groupUrl = intent.getStringExtra("groupUrl") ?: ""

        val openApp = PendingIntent.getActivity(
            context, intent.getIntExtra("id", 0),
            Intent(context, MainActivity::class.java).apply {
                putExtra("reminderContent", content)
                putExtra("reminderGroupUrl", groupUrl)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("حان موعد النشر: $title")
                .setContentText("افتح التطبيق لنسخ البوست وفتح الجروب.")
                .setStyle(Notification.BigTextStyle().bigText(content.take(180)))
                .setAutoCancel(true)
                .setContentIntent(openApp)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("حان موعد النشر: $title")
                .setContentText("افتح التطبيق لمراجعة المنشور.")
                .setAutoCancel(true)
                .setContentIntent(openApp)
                .build()
        }
        manager.notify(intent.getIntExtra("id", 1), notification)
    }
}
