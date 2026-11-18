package com.mediabuyer.bot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ScheduleStore.load(context).forEach { post ->
                if (post.scheduledAt > System.currentTimeMillis()) {
                    ScheduleStore.schedule(context, post)
                }
            }
        }
    }
}
