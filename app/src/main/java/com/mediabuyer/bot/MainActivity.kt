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
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var listBox: LinearLayout
    private lateinit var titleInput: EditText
    private lateinit var contentInput: EditText
    private lateinit var groupInput: EditText
    private lateinit var geminiKeyInput: EditText
    private lateinit var scheduleButton: Button
    private var selectedTime = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
    private var selectedMillis: Long = selectedTime.timeInMillis
    private val dateFormat = SimpleDateFormat("EEE، d MMM yyyy - hh:mm a", Locale("ar"))
    private val fixedFooter = """---------------------------------
للتواصل :
01140526578📲
aliyasser42939@gmail.com 📩
---------------------------------
#ميديا_باير_فاهم #تسويق_رقمي #اعلان_صح #ديجيتال_ماركتنج #خبير_اعلانات #Media_Buyer"""

    private fun addFixedFooter(content: String): String {
        val clean = content.trim()
        return if (clean.endsWith(fixedFooter)) clean else "$clean\n\n$fixedFooter"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        buildScreen()
        renderPosts()

        val reminderContent = intent.getStringExtra("reminderContent")
        if (!reminderContent.isNullOrBlank()) {
            contentInput.setText(reminderContent)
            groupInput.setText(intent.getStringExtra("reminderGroupUrl") ?: "")
            if (intent.getBooleanExtra("shareOnOpen", false) && savedInstanceState == null) {
                contentInput.post { sharePost(reminderContent) }
                toast("البوست جاهز للمشاركة — اختَر Facebook ثم الجروب وراجع قبل النشر")
            }
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
        geminiKeyInput = edit("مفتاح Gemini المجاني")
        geminiKeyInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        card.addView(titleInput, fieldParams())
        card.addView(contentInput, fieldParams())
        card.addView(groupInput, fieldParams())
        card.addView(geminiKeyInput, fieldParams())

        val generate = Button(this).apply {
            text = "توليد 6 بوستات بالذكاء الاصطناعي وجدولتها"
            setOnClickListener { generateAndSchedulePosts() }
        }
        card.addView(generate, fieldParams())

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

        root.addView(label("مشاركة البوست تفتح خيارات المشاركة المتاحة على جهازك؛ اختَر Facebook والجروب بنفسك وراجع المنشور قبل النشر.").apply {
            textSize = 12f
            setTextColor(0xFF7A4B00.toInt())
            setPadding(dp(4), dp(18), dp(4), 0)
        }, wrap())
    }

    private fun generateAndSchedulePosts() {
        val apiKey = geminiKeyInput.text.toString().trim()
        val groupUrl = groupInput.text.toString().trim()
        if (apiKey.isBlank()) { toast("أدخل مفتاح Gemini المجاني"); return }
        if (!groupUrl.startsWith("https://") && !groupUrl.startsWith("http://")) {
            toast("أدخل رابط جروب فيسبوك أولًا"); return
        }
        toast("جاري توليد 6 منشورات...")
        Thread {
            try {
                val prompt = """
                    اكتب 6 منشورات تسويقية مختلفة باللهجة المصرية الطبيعية لمتخصص Media Buying.
                    الجمهور: أصحاب البيزنس والمتاجر الإلكترونية والبراندات والشركات وصناع القرار، وليس المدرسين.
                    الموضوعات: إعلانات Meta، تحسين الحملات، جذب العملاء المحتملين، أخطاء الإعلانات، قياس النتائج، ونصائح عملية.
                    اكتب متن كل منشور في حدود 80 إلى 100 كلمة، قبل إضافة التوقيع الثابت. عدد الكلمات يخص محتوى البوست فقط؛ لا تحسب سطور التواصل أو الفواصل أو الهاشتاجات ضمن العدد.
                    كل منشور مفيد وجذاب وغير مكرر، مع Hook قوي ودعوة تواصل مناسبة، بدون أرقام أو ادعاءات مختلقة.
                    اكتب متن المنشور فقط ولا تضف بيانات التواصل أو الهاشتاجات؛ التطبيق سيضيف توقيع التواصل الثابت حرفيًا في نهاية كل منشور.
                    أسلوب الكتابة المطلوب: مصري طبيعي ومهني، يبدأ بهوك قوي أو مشكلة واقعية، يقدم نقطة عملية واضحة، وينتهي بدعوة تفاعل/تواصل غير مبالغ فيها. نوّع الزوايا ولا تكرر نفس المقدمة أو الفكرة.
                    أرجع JSON فقط بالشكل: {"posts":[{"title":"عنوان قصير","content":"نص المنشور"}]}
                """.trimIndent()
                val request = JSONObject().apply {
                    put("contents", org.json.JSONArray().put(JSONObject().put("parts",
                        org.json.JSONArray().put(JSONObject().put("text", prompt)))))
                    put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
                }
                var successfulBody: String? = null
                var lastError = "تعذر الاتصال بـ Gemini"
                // Try the primary model twice for temporary overload, then automatically fall back.
                val models = listOf("gemini-3.8-flash", "gemini-3.5-flash-lite")
                for (model in models) {
                    var modelSucceeded = false
                    val attempts = if (model == models.first()) 2 else 1
                    for (attempt in 0 until attempts) {
                        var connection: HttpURLConnection? = null
                        try {
                            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
                            connection = (url.openConnection() as HttpURLConnection).apply {
                                requestMethod = "POST"; connectTimeout = 20000; readTimeout = 90000; doOutput = true
                                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                            }
                            connection.outputStream.use { it.write(request.toString().toByteArray(Charsets.UTF_8)) }
                            val status = connection.responseCode
                            val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "{}"
                            if (status in 200..299) {
                                successfulBody = body
                                modelSucceeded = true
                                break
                            }
                            val errorObject = JSONObject(body).optJSONObject("error")
                            lastError = errorObject?.optString("message") ?: "تعذر الاتصال بـ Gemini"
                            // Only retry/fallback on server overload; report auth/quota/model errors directly.
                            if (status != 503 && status != 500 && status != 502 && status != 504) {
                                throw Exception(lastError)
                            }
                            if (attempt + 1 < attempts) Thread.sleep(1800L)
                        } finally {
                            connection?.disconnect()
                        }
                    }
                    if (modelSucceeded) break
                }
                val body = successfulBody ?: throw Exception("الموديل الأساسي والبديل مش متاحين حاليًا. جرّب بعد شوية. التفاصيل: $lastError")
                val responseText = JSONObject(body).getJSONArray("candidates").getJSONObject(0)
                    .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                val postsArray = JSONObject(responseText).getJSONArray("posts")
                if (postsArray.length() != 6) throw Exception("Gemini لم يُرجع 6 منشورات")
                val now = Calendar.getInstance()
                val hours = listOf(10, 12, 14, 16, 18, 20)
                val generated = mutableListOf<PostItem>()
                for (i in 0 until 6) {
                    val item = postsArray.getJSONObject(i)
                    val slot = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hours[i]); set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        while (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
                    }
                    val title = item.getString("title").trim()
                    val content = addFixedFooter(item.getString("content"))
                    if (title.isBlank() || content.isBlank()) throw Exception("فيه منشور فاضي، جرّب تاني")
                    generated.add(PostItem(((System.currentTimeMillis() + i) % Int.MAX_VALUE).toInt(),
                        title, content, groupUrl, slot.timeInMillis))
                }
                runOnUiThread {
                    val all = ScheduleStore.load(this).apply { addAll(generated) }
                    ScheduleStore.save(this, all)
                    generated.forEach { ScheduleStore.schedule(this, it) }
                    renderPosts(); toast("اتولدوا واتجدولوا 6 منشورات بنجاح")
                }
            } catch (error: Exception) {
                runOnUiThread { toast(error.message ?: "حصل خطأ أثناء التوليد") }
            }
        }.start()
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
        val content = addFixedFooter(contentInput.text.toString())
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
                    openFacebookGroup(post.groupUrl)
                }
            }
            val share = Button(this).apply {
                text = "مشاركة النص"
                setOnClickListener { sharePost(post.content) }
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
            buttons.addView(share, LinearLayout.LayoutParams(0, -2, 1f))
            buttons.addView(delete, LinearLayout.LayoutParams(0, -2, 0.7f))
            card.addView(buttons, wrap())
            listBox.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(8)
            })
        }
    }

    private fun openFacebookGroup(groupUrl: String) {
        val uri = Uri.parse(groupUrl)
        try {
            val facebookIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.facebook.katana")
            }
            startActivity(facebookIntent)
            toast("اتفتح الجروب في Facebook والنص اتنسخ — الصقه وراجعه قبل النشر")
        } catch (_: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
                toast("Facebook مش متاح لفتح الرابط؛ اتفتح في التطبيق المتاح والنص اتنسخ")
            } catch (_: Exception) {
                toast("اتنسخ النص، لكن تعذر فتح رابط الجروب")
            }
        }
    }

    private fun sharePost(postText: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, postText)
        }
        try {
            startActivity(Intent.createChooser(sendIntent, "مشاركة البوست"))
        } catch (_: Exception) {
            toast("مفيش تطبيق متاح لمشاركة النص")
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
