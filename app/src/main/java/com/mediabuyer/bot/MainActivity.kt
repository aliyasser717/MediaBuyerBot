package com.mediabuyer.bot

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var listBox: LinearLayout
    private lateinit var titleInput: EditText
    private lateinit var contentInput: EditText
    private lateinit var groupInput: EditText
    private lateinit var scheduleButton: Button
    private var selectedTime = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
    private var selectedMillis: Long = selectedTime.timeInMillis
    private val dateFormat = SimpleDateFormat("EEE، d MMM yyyy - hh:mm a", Locale("ar"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        buildScreen()
        renderPosts()

        val reminderContent = intent.getStringExtra("reminderContent")
        if (!reminderContent.isNullOrBlank()) {
            contentInput.setText(reminderContent)
            groupInput.setText(intent.getStringExtra("reminderGroupUrl") ?: "")
        }
    }

    private fun buildScreen() {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(28))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(0xFFF6F7F9.toInt())
        }
        scroll.addView(root)
        setContentView(scroll)

        val header = TextView(this).apply {
            text = "Media Buyer Bot"
            textSize = 25f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF182230.toInt())
            gravity = Gravity.CENTER
        }
        root.addView(header, match())
        root.addView(label("منظّم منشورات جروبات فيسبوك — نسخة مجانية"), wrap())

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        root.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })

        card.addView(label("إضافة منشور جديد", true), wrap())
        titleInput = edit("عنوان مختصر للمنشور")
        contentInput = edit("اكتب نص البوست هنا...")
        contentInput.minLines = 5
        contentInput.gravity = Gravity.TOP or Gravity.RIGHT
        groupInput = edit("رابط جروب فيسبوك")
        card.addView(titleInput, fieldParams())
        card.addView(contentInput, fieldParams())
        card.addView(groupInput, fieldParams())

        scheduleButton = Button(this).apply {
            text = "اختيار موعد النشر: ${dateFormat.format(Date(selectedMillis))}"
            setOnClickListener { chooseDateTime() }
        }
        card.addView(scheduleButton, fieldParams())

        card.addView(label("مواعيد مقترحة لـ 6 منشورات يوميًا", true), fieldParams())
        val slots = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        listOf(10, 12, 14, 16, 18, 20).forEach { hour ->
            val slot = Button(this).apply {
                text = String.format(Locale.US, "%02d:00", hour)
                textSize = 11f
                setOnClickListener { setSuggestedTime(hour) }
            }
            slots.addView(slot, LinearLayout.LayoutParams(0, -2, 1f).apply {
                marginEnd = dp(2)
            })
        }
        card.addView(slots, fieldParams())

        val add = Button(this).apply {
            text = "حفظ وجدولة التذكير"
            setOnClickListener { addPost() }
        }
        card.addView(add, fieldParams())

        root.addView(label("المنشورات المجدولة", true).apply {
            textSize = 19f
            setPadding(0, dp(20), 0, dp(8))
        }, wrap())

        listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(listBox, wrap())

        root.addView(label("مهم: التطبيق يذكّرك بالموعد ويفتح الجروب؛ لا ينشر داخل فيسبوك تلقائيًا.").apply {
            textSize = 12f
            setTextColor(0xFF7A4B00.toInt())
            setPadding(dp(4), dp(18), dp(4), 0)
        }, wrap())
    }

    private fun setSuggestedTime(hour: Int) {
        selectedTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        selectedMillis = selectedTime.timeInMillis
        scheduleButton.text = "موعد النشر: ${dateFormat.format(Date(selectedMillis))}"
    }

    private fun chooseDateTime() {
        val now = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            selectedTime.set(Calendar.YEAR, year)
            selectedTime.set(Calendar.MONTH, month)
            selectedTime.set(Calendar.DAY_OF_MONTH, day)
            TimePickerDialog(this, { _, hour, minute ->
                selectedTime.set(Calendar.HOUR_OF_DAY, hour)
                selectedTime.set(Calendar.MINUTE, minute)
                selectedTime.set(Calendar.SECOND, 0)
                selectedTime.set(Calendar.MILLISECOND, 0)
                selectedMillis = selectedTime.timeInMillis
                scheduleButton.text = "موعد النشر: ${dateFormat.format(Date(selectedMillis))}"
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show()
        }, selectedTime.get(Calendar.YEAR), selectedTime.get(Calendar.MONTH), selectedTime.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun addPost() {
        val title = titleInput.text.toString().trim().ifBlank { "منشور جديد" }
        val content = contentInput.text.toString().trim()
        val url = groupInput.text.toString().trim()
        if (content.isBlank()) {
            toast("اكتب نص المنشور الأول")
            return
        }
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            toast("اكتب رابط الجروب كاملًا ويبدأ بـ https://")
            return
        }
        if (selectedMillis <= System.currentTimeMillis()) {
            toast("اختار موعدًا في المستقبل")
            return
        }
        val posts = ScheduleStore.load(this)
        val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val post = PostItem(id, title, content, url, selectedMillis)
        posts.add(post)
        ScheduleStore.save(this, posts)
        ScheduleStore.schedule(this, post)
        titleInput.text.clear()
        contentInput.text.clear()
        groupInput.text.clear()
        selectedTime = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
        selectedMillis = selectedTime.timeInMillis
        scheduleButton.text = "اختيار موعد النشر: ${dateFormat.format(Date(selectedMillis))}"
        renderPosts()
        toast("اتحفظ المنشور واتحدد له تذكير")
    }

    private fun renderPosts() {
        if (!::listBox.isInitialized) return
        listBox.removeAllViews()
        val posts = ScheduleStore.load(this)
        if (posts.isEmpty()) {
            listBox.addView(label("لسه مفيش منشورات مجدولة. أضف أول بوست من فوق."))
            return
        }
        posts.forEach { post ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                setBackgroundColor(0xFFFFFFFF.toInt())
            }
            val heading = label(post.title, true)
            card.addView(heading, wrap())
            card.addView(label(dateFormat.format(Date(post.scheduledAt))).apply {
                setTextColor(0xFF52606D.toInt())
                textSize = 13f
            }, wrap())
            card.addView(label(post.content).apply { maxLines = 3 }, wrap())

            val buttons = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            val copy = Button(this).apply {
                text = "نسخ وفتح الجروب"
                setOnClickListener {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Facebook post", post.content))
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(post.groupUrl)))
                        toast("اتنسخ البوست واتفتح الجروب — راجع المنشور واضغط نشر بنفسك")
                    } catch (_: Exception) {
                        toast("اتنسخ البوست، لكن مش قادر أفتح الرابط")
                    }
                }
            }
            val open = Button(this).apply {
                text = "فتح الجروب"
                setOnClickListener {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(post.groupUrl)))
                    } catch (_: Exception) {
                        toast("مش قادر أفتح الرابط")
                    }
                }
            }
            val delete = Button(this).apply {
                text = "حذف"
                setOnClickListener {
                    ScheduleStore.cancel(this@MainActivity, post)
                    val updated = ScheduleStore.load(this@MainActivity).filterNot { it.id == post.id }
                    ScheduleStore.save(this@MainActivity, updated)
                    renderPosts()
                }
            }
            buttons.addView(copy, LinearLayout.LayoutParams(0, -2, 1f))
            buttons.addView(open, LinearLayout.LayoutParams(0, -2, 1f))
            buttons.addView(delete, LinearLayout.LayoutParams(0, -2, 0.7f))
            card.addView(buttons, wrap())
            listBox.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(8)
            })
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }

    private fun edit(hintText: String) = EditText(this).apply {
        hint = hintText
        textSize = 15f
        setPadding(dp(10), dp(10), dp(10), dp(10))
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setBackgroundColor(0xFFF0F2F5.toInt())
    }

    private fun label(textValue: String, bold: Boolean = false) = TextView(this).apply {
        text = textValue
        textSize = 15f
        setTextColor(0xFF182230.toInt())
        if (bold) setTypeface(null, Typeface.BOLD)
        gravity = Gravity.RIGHT
    }

    private fun fieldParams() = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(9) }
    private fun match() = LinearLayout.LayoutParams(-1, -2)
    private fun wrap() = LinearLayout.LayoutParams(-1, -2)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
