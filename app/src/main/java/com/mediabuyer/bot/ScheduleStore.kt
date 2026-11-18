package com.mediabuyer.bot

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject

object ScheduleStore {
    private const val PREFS = "media_buyer_bot"
    private const val KEY_POSTS = "posts"

    fun load(context: Context): MutableList<PostItem> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_POSTS, "[]") ?: "[]"
        val result = mutableListOf<PostItem>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                result.add(
                    PostItem(
                        o.getInt("id"),
                        o.getString("title"),
                        o.getString("content"),
                        o.getString("groupUrl"),
                        o.getLong("scheduledAt")
                    )
                )
            }
        } catch (_: Exception) {}
        return result.sortedBy { it.scheduledAt }.toMutableList()
    }

    fun save(context: Context, posts: List<PostItem>) {
        val array = JSONArray()
        posts.forEach { p ->
            array.put(JSONObject().apply {
                put("id", p.id)
                put("title", p.title)
                put("content", p.content)
                put("groupUrl", p.groupUrl)
                put("scheduledAt", p.scheduledAt)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_POSTS, array.toString()).apply()
    }

    fun schedule(context: Context, post: PostItem) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("id", post.id)
            putExtra("title", post.title)
            putExtra("content", post.content)
            putExtra("groupUrl", post.groupUrl)
        }
        val pending = PendingIntent.getBroadcast(
            context, post.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, post.scheduledAt, pending)
    }

    fun cancel(context: Context, post: PostItem) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, post.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(pending)
    }
}
