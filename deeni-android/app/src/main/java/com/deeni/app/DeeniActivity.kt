package com.deeni.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.View
import android.widget.*

/** Redesigned Deeni entry point. The reader loads complete validated Quran pages, not a Fatiha-only sample. */
class DeeniActivity : Activity(), SensorEventListener {
    private val dark = Color.rgb(3, 7, 18)
    private val green = Color.rgb(17, 28, 53)
    private val green2 = Color.rgb(11, 19, 41)
    private val gold = Color.rgb(245, 158, 11)
    private val paper = Color.rgb(249, 250, 251)
    private val prefs by lazy { getSharedPreferences("deeni_redesign", MODE_PRIVATE) }
    private var light = false
    private var page = 1
    private var currentScreen = ""
    private var root: LinearLayout? = null
    private var player: MediaPlayer? = null
    private var recognizer: SpeechRecognizer? = null
    private var currentRead: ReciterRead? = null
    private val sensorManager by lazy { getSystemService(SENSOR_SERVICE) as SensorManager }
    private var compassView: QiblaCompassView? = null
    private var qiblaBearing = 0f
    private val abuArishLatitude = 16.9689
    private val abuArishLongitude = 42.8325

    private val surahs = listOf(
        "الفاتحة","البقرة","آل عمران","النساء","المائدة","الأنعام","الأعراف","الأنفال","التوبة","يونس",
        "هود","يوسف","الرعد","إبراهيم","الحجر","النحل","الإسراء","الكهف","مريم","طه","الأنبياء","الحج",
        "المؤمنون","النور","الفرقان","الشعراء","النمل","القصص","العنكبوت","الروم","لقمان","السجدة",
        "الأحزاب","سبأ","فاطر","يس","الصافات","ص","الزمر","غافر","فصلت","الشورى","الزخرف","الدخان",
        "الجاثية","الأحقاف","محمد","الفتح","الحجرات","ق","الذاريات","الطور","النجم","القمر","الرحمن",
        "الواقعة","الحديد","المجادلة","الحشر","الممتحنة","الصف","الجمعة","المنافقون","التغابن","الطلاق",
        "التحريم","الملك","القلم","الحاقة","المعارج","نوح","الجن","المزمل","المدثر","القيامة","الإنسان",
        "المرسلات","النبأ","النازعات","عبس","التكوير","الانفطار","المطففين","الانشقاق","البروج","الطارق",
        "الأعلى","الغاشية","الفجر","البلد","الشمس","الليل","الضحى","الشرح","التين","العلق","القدر",
        "البينة","الزلزلة","العاديات","القارعة","التكاثر","العصر","الهمزة","الفيل","قريش","الماعون",
        "الكوثر","الكافرون","النصر","المسد","الإخلاص","الفلق","الناس"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        light = prefs.getBoolean("light", false)
        page = prefs.getInt("page", 1).coerceIn(1, 604)
        showHome()
    }

    override fun onResume() {
        super.onResume()
        if (currentScreen == "القبلة") startCompassSensor()
    }

    override fun onPause() {
        stopCompassSensor()
        super.onPause()
    }

    override fun onStop() {
        stopMic()
        stopAudio()
        super.onStop()
    }

    private fun startCompassSensor() {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensor != null) sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    private fun stopCompassSensor() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ROTATION_VECTOR) return
        val rotation = FloatArray(9)
        val orientation = FloatArray(3)
        SensorManager.getRotationMatrixFromVector(rotation, event.values)
        SensorManager.getOrientation(rotation, orientation)
        val magneticAzimuth = (Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f
        val field = GeomagneticField(
            abuArishLatitude.toFloat(),
            abuArishLongitude.toFloat(),
            0f,
            System.currentTimeMillis()
        )
        val trueAzimuth = (magneticAzimuth + field.declination + 360f) % 360f
        compassView?.setDirection(qiblaBearing, trueAzimuth)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBackPressed() {
        if (currentScreen != "الرئيسية") showHome() else super.onBackPressed()
    }

    private fun bg() = if (light) Color.rgb(246,243,233) else dark
    private fun panel() = if (light) Color.WHITE else green
    private fun panel2() = if (light) Color.rgb(235,229,211) else green2
    private fun ink() = if (light) Color.rgb(37,55,46) else Color.rgb(247,243,228)
    private fun muted() = if (light) Color.rgb(102,116,103) else Color.rgb(190,207,195)
    private fun accent() = if (light) Color.rgb(128,91,35) else gold
    private fun shape(c: Int, r: Float = 20f) = GradientDrawable().apply {
        setColor(c)
        cornerRadius = r
        setStroke(1, if (light) Color.rgb(222, 211, 190) else Color.rgb(43, 59, 87))
    }

    private fun label(value: String, size: Float = 16f, color: Int = ink(), bold: Boolean = false) =
        TextView(this).apply {
            text = value; textSize = size; setTextColor(color); gravity = Gravity.RIGHT
            textDirection = View.TEXT_DIRECTION_RTL
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(0, 6, 0, 6)
        }

    private fun button(value: String, primary: Boolean = false, click: () -> Unit) =
        TextView(this).apply {
            text = value; textSize = 15f; gravity = Gravity.CENTER
            setTextColor(if (primary) bg() else ink())
            background = shape(if (primary) accent() else panel2(), 17f)
            setPadding(12, 14, 12, 14); minHeight = 48
            setOnClickListener { click() }
        }

    private fun gap(h: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, h) }

    private fun screen(title: String, subtitle: String) {
        currentScreen = title
        if (title != "القبلة") {
            stopCompassSensor()
            compassView = null
        }
        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(bg())
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(20, 14, 20, 8); setBackgroundColor(bg())
            addView(label("دِينِي", 14f, accent(), true))
            addView(label(title, 25f, ink(), true))
            addView(label(subtitle, 12f, muted()))
        }
        shell.addView(header)
        val scroll = ScrollView(this).apply { isFillViewport = true }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(16, 10, 16, 18); setBackgroundColor(bg())
        }
        scroll.addView(root)
        shell.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER; setPadding(6, 8, 6, 8); background = shape(panel(), 18f)
        }
        val destinations = listOf("الرئيسية", "المصحف", "الحفظ", "التقدم", "المزيد")
        destinations.forEach { destination ->
            val item = TextView(this).apply {
                text = destination; textSize = 11f; gravity = Gravity.CENTER
                setTextColor(if (destination == title) accent() else muted())
                setPadding(3, 10, 3, 10)
                setOnClickListener {
                    when (destination) {
                        "الرئيسية" -> showHome()
                        "المصحف" -> showQuran()
                        "الحفظ" -> showMemorization()
                        "التقدم" -> showProgress()
                        else -> showMore()
                    }
                }
            }
            nav.addView(item, LinearLayout.LayoutParams(0, -2, 1f))
        }
        val footer = LinearLayout(this).apply {
            setPadding(12, 4, 12, 10); setBackgroundColor(bg()); addView(nav)
        }
        shell.addView(footer)
        setContentView(shell)
    }

    private fun card(title: String, desc: String, icon: String, click: () -> Unit) {
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; background = shape(panel())
            elevation = 3f
            setPadding(15, 12, 15, 12)
            addView(label(icon, 21f, accent(), true))
            addView(label(title, 17f, ink(), true))
            addView(label(desc, 12f, muted()))
            setOnClickListener { click() }
        }
        val p = LinearLayout.LayoutParams(-1, -2); p.bottomMargin = 9
        root?.addView(view, p)
    }

    private fun heading(value: String) {
        root?.addView(gap(12))
        root?.addView(label(value, 17f, accent(), true))
    }

    private fun tiles(a: String, ad: String, ac: () -> Unit, b: String, bd: String, bc: () -> Unit) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(Triple(a, ad, ac), Triple(b, bd, bc)).forEach { item ->
            val v = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                background = shape(panel()); elevation = 2f; setPadding(7, 14, 7, 14)
                addView(label(item.first, 15f, accent(), true).apply { gravity = Gravity.CENTER })
                addView(label(item.second, 11f, muted()).apply { gravity = Gravity.CENTER })
                setOnClickListener { item.third() }
            }
            val p = LinearLayout.LayoutParams(0, -2, 1f); p.setMargins(4, 4, 4, 4)
            row.addView(v, p)
        }
        root?.addView(row)
    }

    private fun showHome() {
        screen("الرئيسية", "منصة إسلامية للقرآن والعبادة والتعلّم")
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, if (light) intArrayOf(Color.rgb(255, 250, 235), Color.rgb(232, 222, 197)) else intArrayOf(Color.rgb(21, 38, 68), Color.rgb(8, 18, 38))).apply {
                cornerRadius = 24f
                setStroke(1, accent())
            }
            elevation = 5f
            setPadding(18, 18, 18, 18)
            addView(label("وَرَتِّلِ الْقُرْآنَ تَرْتِيلًا", 24f, accent(), true))
            addView(label("ابدأ من آخر موضع وصلت إليه، واجعل لك وردًا ثابتًا.", 13f))
            addView(button("متابعة المصحف • الصفحة " + page, true) { showQuran() })
        }
        root?.addView(hero)
        heading("القرآن الكريم")
        tiles("المصحف", "604 صفحة كاملة", { showQuran() }, "التسميع", "التدريب الصوتي", { showQuran(true) })
        tiles("الاستماع", "القراء والسور", { chooseReciter() }, "التفسير", "التفسير الميسر", { showTafsir() })
        tiles("البحث", "البحث في الآيات", { showSearch() }, "المحفوظات", "آيات محفوظة", { showSaved() })
        heading("العبادة اليومية")
        tiles("الصلاة", "مواقيت أبو عريش", { showPrayer() }, "القبلة", "اتجاه مكة", { showQibla() })
        tiles("الأذكار", "12 ذكرًا مع مصادر", { showAdhkar() }, "التسبيح", "عداد محفوظ", { showTasbeeh() })
        heading("المكتبة الإسلامية")
        tiles("السيرة", "9 محطات مع مراجع", { showSirah() }, "الحديث", "28 حديثًا مع الشرح والمصدر", { showHadith() })
        tiles("الأنبياء", "24 قصة مع مراجع", { showStories(false) }, "الصحابة", "25 سيرة مع مراجع", { showStories(true) })
        tiles("التجويد", "60 درسًا قابلًا للبحث", { showTajweed() }, "الفقه", "12 بابًا قابلًا للبحث", { showFiqh() })
        heading("أدوات أخرى")
        tiles("المسابقة", "30 سؤالًا مع شرح", { showQuiz() }, "الأسماء الحسنى", "30 اسمًا ومعنى", { showNames() })
        tiles("ملاحظاتي", "فوائدك الخاصة", { showNotes() }, "تقدمك", "متابعة الإنجاز", { showProgress() })
        root?.addView(button(if (light) "الوضع الفاتح • التبديل للداكن" else "الوضع الداكن • التبديل للفاتح", false) {
            light = !light; prefs.edit().putBoolean("light", light).apply(); showHome()
        })
        root?.addView(label("التسميع يستخدم التعرف الصوتي العام في الهاتف، ولا يصحح التجويد أو مخارج الحروف.", 11f, muted()))
    }

    private fun showQuran(startMic: Boolean = false) {
        // Reuse the full SVG Madinah Mushaf reader rather than displaying a plain-text substitute.
        getSharedPreferences("deeni", MODE_PRIVATE).edit().putInt("mushaf_page", page).apply()
        startActivity(Intent(this, MainActivity::class.java).apply {
            putExtra("open_mushaf", true)
            putExtra("start_mic", startMic)
        })
    }

    private fun loadPage(requested: Int) {
        page = requested.coerceIn(1, 604)
        prefs.edit().putInt("page", page).apply()
        showQuran()
    }

    private fun loadPageText(target: Int, output: TextView, status: TextView) {
        Thread {
            val result = QuranPageSource.load(target)
            runOnUiThread {
                if (currentScreen != "المصحف" || page != target) return@runOnUiThread
                if (result.isSuccess) {
                    output.text = result.getOrThrow()
                    status.text = "صفحة " + target + " من 604 • تم تحميل نص الصفحة"
                } else {
                    output.text = "تعذر تحميل الصفحة. تحقق من الإنترنت وأعد المحاولة."
                    output.setTextColor(Color.rgb(150, 65, 45))
                    status.text = "لم يتم تحميل النص؛ لم نستبدله بمحتوى ناقص."
                }
            }
        }.start()
    }

    private fun askPage() {
        val input = EditText(this).apply { inputType = 2; setText(page.toString()); gravity = Gravity.CENTER }
        AlertDialog.Builder(this).setTitle("الانتقال إلى صفحة").setView(input)
            .setPositiveButton("فتح") { _, _ -> loadPage(input.text.toString().toIntOrNull() ?: page) }
            .setNegativeButton("إلغاء", null).show()
    }

    private fun chooseSurah() {
        AlertDialog.Builder(this).setTitle("اختر السورة")
            .setItems(surahs.mapIndexed { i, n -> (i + 1).toString() + ". " + n }.toTypedArray()) { _, which ->
                Thread {
                    val result = QuranPageSource.loadAyahDetails(which + 1, 1)
                    runOnUiThread {
                        if (result.isSuccess) loadPage(result.getOrThrow().second)
                        else Toast.makeText(this, "تعذر تحديد صفحة السورة.", Toast.LENGTH_LONG).show()
                    }
                }.start()
            }.setNegativeButton("إلغاء", null).show()
    }

    private fun saveAyahDialog() {
        val fields = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(14, 4, 14, 4) }
        val s = EditText(this).apply { hint = "رقم السورة"; inputType = 2 }
        val a = EditText(this).apply { hint = "رقم الآية"; inputType = 2 }
        fields.addView(s); fields.addView(a)
        AlertDialog.Builder(this).setTitle("حفظ آية").setView(fields)
            .setPositiveButton("حفظ") { _, _ ->
                val sn = s.text.toString().toIntOrNull()
                val an = a.text.toString().toIntOrNull()
                if (sn == null || an == null || !QuranReference.isValid(sn, an)) {
                    Toast.makeText(this, "رقم السورة أو الآية غير صحيح.", Toast.LENGTH_LONG).show()
                } else {
                    val saved = prefs.getStringSet("saved_ayahs", emptySet())?.toMutableSet() ?: mutableSetOf()
                    saved.add(sn.toString() + ":" + an)
                    prefs.edit().putStringSet("saved_ayahs", saved).apply()
                    Toast.makeText(this, "تم حفظ الآية.", Toast.LENGTH_SHORT).show()
                }
            }.setNegativeButton("إلغاء", null).show()
    }

    private fun showRecitationModes(output: TextView) {
        val modes = arrayOf("التسميع من الحفظ", "استمع للقارئ", "اختبار الحفظ")
        AlertDialog.Builder(this)
            .setTitle("اختر وضع الميكروفون")
            .setItems(modes) { _, which ->
                when (which) {
                    0 -> startMic(output, "التسميع من الحفظ")
                    1 -> chooseReciter()
                    2 -> {
                        output.text = "اختبار الحفظ: اقرأ المقطع الذي حفظته دون النظر. سيعرض الهاتف تفريغًا صوتيًا تقريبيًا لتراجعه بنفسك؛ لا يصدر حكمًا آليًا على صحة الحفظ أو التجويد."
                        startMic(output, "اختبار الحفظ")
                    }
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun startMic(output: TextView, mode: String = "التسميع") {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "التعرف الصوتي غير متاح على الجهاز.", Toast.LENGTH_LONG).show(); return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 501); return
        }
        stopMic()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { output.text = "🎙 وضع $mode • أستمع إليك… اقرأ بوضوح." }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { output.text = "انتهى الالتقاط أو تعذر تمييز الكلام؛ يمكنك المحاولة مجددًا." }
            override fun onResults(results: Bundle?) {
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                output.text = "النص الذي التقطه الهاتف:\n" + spoken + "\n\nتفريغ استرشادي قد يخطئ في سماع القرآن، ولا يقيس التجويد."
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val spoken = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (spoken.isNotBlank()) output.text = "متابعة القراءة…\n" + spoken
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer?.startListening(intent)
    }

    private fun stopMic() {
        try { recognizer?.cancel(); recognizer?.destroy() } catch (_: Exception) {}
        recognizer = null
    }

    private fun chooseReciter() {
        Toast.makeText(this, "جارٍ تحميل القراء…", Toast.LENGTH_SHORT).show()
        Thread {
            val result = ReciterSource.loadReciters()
            runOnUiThread {
                if (result.isFailure) {
                    Toast.makeText(this, "تعذر تحميل القراء؛ تحقق من الإنترنت.", Toast.LENGTH_LONG).show(); return@runOnUiThread
                }
                val reads = result.getOrThrow()
                AlertDialog.Builder(this).setTitle("اختر القارئ والرواية")
                    .setItems(reads.map { it.reciterName + " • " + it.readName }.toTypedArray()) { _, which ->
                        currentRead = reads[which]
                        chooseAudioSurah()
                    }.setNegativeButton("إلغاء", null).show()
            }
        }.start()
    }

    private fun chooseAudioSurah() {
        val read = currentRead ?: return
        val ids = read.surahIds.sorted()
        AlertDialog.Builder(this).setTitle("اختر السورة")
            .setItems(ids.map { it.toString() + ". " + surahs[it - 1] }.toTypedArray()) { _, which ->
                stopAudio()
                val p = MediaPlayer()
                player = p
                p.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                p.setOnPreparedListener { it.start() }
                p.setOnErrorListener { _, _, _ -> stopAudio(); Toast.makeText(this, "تعذر تشغيل التلاوة.", Toast.LENGTH_LONG).show(); true }
                try { p.setDataSource(read.audioUrl(ids[which])); p.prepareAsync() }
                catch (_: Exception) { stopAudio() }
            }.setNegativeButton("إلغاء", null).show()
    }

    private fun stopAudio() {
        val old = player; player = null
        try { old?.stop() } catch (_: Exception) {}
        try { old?.release() } catch (_: Exception) {}
    }

    private fun showTafsir() {
        screen("التفسير", "التفسير الميسر مع نص الآية")
        val s = EditText(this).apply { hint = "رقم السورة"; inputType = 2; setText("1") }
        val a = EditText(this).apply { hint = "رقم الآية"; inputType = 2; setText("1") }
        root?.addView(s); root?.addView(a)
        val resultBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root?.addView(button("عرض التفسير", true) {
            val sn = s.text.toString().toIntOrNull()
            val an = a.text.toString().toIntOrNull()
            if (sn == null || an == null || !QuranReference.isValid(sn, an)) {
                Toast.makeText(this, "تحقق من رقم السورة والآية.", Toast.LENGTH_SHORT).show()
            } else {
                resultBox.removeAllViews()
                resultBox.addView(label("جارٍ التحميل…", 13f, accent()))
                Thread {
                    val loaded = TafsirSource.load(sn, an)
                    runOnUiThread {
                        resultBox.removeAllViews()
                        if (loaded.isSuccess) {
                            val item = loaded.getOrThrow()
                            resultBox.addView(label(item.surahName + " • الآية " + item.ayahNumber, 14f, accent(), true))
                            resultBox.addView(label(item.ayahText, 23f, ink(), true))
                            resultBox.addView(label(item.tafsirText, 17f))
                            resultBox.addView(label("المصدر: التفسير الميسر عبر AlQuran Cloud.", 11f, muted()))
                        } else resultBox.addView(label("تعذر تحميل التفسير. تحقق من الاتصال.", 14f, accent()))
                    }
                }.start()
            }
        })
        root?.addView(resultBox)
    }

    private fun showSearch() {
        screen("البحث في القرآن", "ابحث عن كلمة أو عبارة في نص القرآن")
        val q = EditText(this).apply { hint = "اكتب كلمة البحث" }
        root?.addView(q)
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root?.addView(button("بحث", true) {
            val query = q.text.toString().trim()
            if (query.length < 2) Toast.makeText(this, "اكتب حرفين على الأقل.", Toast.LENGTH_SHORT).show()
            else {
                results.removeAllViews()
                results.addView(label("جارٍ البحث…", 13f, accent()))
                Thread {
                    val found = QuranSearchSource.search(query)
                    runOnUiThread {
                        results.removeAllViews()
                        if (found.isSuccess) found.getOrThrow().take(30).forEach { row ->
                            results.addView(button(
                                "سورة " + row.surahName + " • الآية " + row.ayahNumber + "\n" + row.text,
                                false
                            ) {
                                Thread {
                                    val pageResult = QuranPageSource.loadAyahDetails(row.surahNumber, row.ayahNumber)
                                    runOnUiThread {
                                        if (pageResult.isSuccess) loadPage(pageResult.getOrThrow().second)
                                        else Toast.makeText(this, "تعذر فتح موضع الآية.", Toast.LENGTH_SHORT).show()
                                    }
                                }.start()
                            })
                            results.addView(gap(7))
                        } else results.addView(label("تعذر البحث الآن؛ تحقق من الاتصال.", 14f, accent()))
                    }
                }.start()
            }
        })
        root?.addView(results)
    }

    private fun showSaved() {
        val saved = prefs.getStringSet("saved_ayahs", emptySet())?.toList().orEmpty().sorted()
        screen("المحفوظات", saved.size.toString() + " آية محفوظة على هذا الجهاز")
        if (saved.isEmpty()) root?.addView(label("لا توجد آيات محفوظة بعد.", 15f))
        saved.forEach { item -> card("الآية " + item, "فتح الموضع أو إزالة الحفظ", "☆") {
            val parts = item.split(":")
            val sn = parts.getOrNull(0)?.toIntOrNull()
            val an = parts.getOrNull(1)?.toIntOrNull()
            AlertDialog.Builder(this)
                .setTitle("الآية " + item)
                .setPositiveButton("فتح موضعها") { _, _ ->
                    if (sn != null && an != null) Thread {
                        val loaded = QuranPageSource.loadAyahDetails(sn, an)
                        runOnUiThread { if (loaded.isSuccess) loadPage(loaded.getOrThrow().second) }
                    }.start()
                }
                .setNeutralButton("إزالة من المحفوظات") { _, _ ->
                    val current = prefs.getStringSet("saved_ayahs", emptySet())?.toMutableSet() ?: mutableSetOf()
                    current.remove(item)
                    prefs.edit().putStringSet("saved_ayahs", current).apply()
                    showSaved()
                }
                .setNegativeButton("إلغاء", null)
                .show()
        } }
    }

    private fun resetDailyTasksIfNeeded() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        if (prefs.getString("task_date", "") != today) {
            val editor = prefs.edit().putString("task_date", today)
            for (i in 0..3) editor.putBoolean("task_" + i, false)
            editor.apply()
        }
    }

    private fun showMemorization() {
        resetDailyTasksIfNeeded()
        screen("الحفظ والمراجعة", "خطة يومية تحفظ إنجازك على الجهاز")
        listOf("حفظ جديد", "مراجعة محفوظ الأمس", "تسميع دون نظر", "الاستماع ثم الترديد").forEachIndexed { i, item ->
            val check = CheckBox(this).apply {
                text = item; setTextColor(ink()); isChecked = prefs.getBoolean("task_" + i, false)
                setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean("task_" + i, checked).apply() }
            }
            root?.addView(check)
        }
        root?.addView(button("فتح المصحف", true) { showQuran() })
    }

    private fun showProgress() {
        resetDailyTasksIfNeeded()
        screen("التقدم", "ملخص القراءة والحفظ")
        val saved = prefs.getStringSet("saved_ayahs", emptySet())?.size ?: 0
        val tasks = (0..3).count { prefs.getBoolean("task_" + it, false) }
        val tajweedDone = prefs.getStringSet("tajweed_done", emptySet())?.size ?: 0
        val bestQuiz = prefs.getInt("best_quiz_score", 0)
        val dhikrTotal = (0..11).sumOf { prefs.getInt("dhikr_" + it, 0) }
        val tasbeehCount = prefs.getInt("tasbeeh", 0)
        card("الآيات المحفوظة", saved.toString() + " آية", "☆") { showSaved() }
        card("مهام اليوم", tasks.toString() + " من 4", "✓") { showMemorization() }
        card("دروس التجويد التي راجعتها", tajweedDone.toString() + " من 60 درسًا", "۞") { showTajweed() }
        card("مجموع الأذكار المسجلة", dhikrTotal.toString() + " تكرار", "◉") { showAdhkar() }
        card("عداد التسبيح المستقل", tasbeehCount.toString() + " تكرار", "◉") { showTasbeeh() }
        card("أفضل نتيجة في المسابقة", bestQuiz.toString() + " من 30", "★") { showQuiz() }
        card("آخر موضع", "صفحة " + page + " من 604", "۞") { showQuran() }
    }

    private fun showPrayer() {
        screen("مواقيت الصلاة", "أبو عريش • مواقيت اليوم")
        val output = label("جارٍ تحميل المواقيت…", 17f, accent(), true)
        root?.addView(output)
        Thread {
            val result = PrayerTimesSource.load("Abu Arish")
            runOnUiThread {
                if (result.isSuccess) {
                    val data = result.getOrThrow()
                    output.text = data.dateLabel + "\n\n" + data.times.joinToString("\n") { it.first + "     " + it.second }
                } else output.text = "تعذر تحميل المواقيت. تحقق من الإنترنت."
            }
        }.start()
        root?.addView(button("اتجاه القبلة", false) { showQibla() })
    }

    private fun showQibla() {
        screen("القبلة", "بوصلة حية من أبو عريش إلى الكعبة")
        qiblaBearing = QiblaMath.bearingDegrees(abuArishLatitude, abuArishLongitude).toFloat()
        compassView = QiblaCompassView(this)
        compassView?.setDirection(qiblaBearing, 0f)
        root?.addView(compassView, LinearLayout.LayoutParams(280, 280).apply {
            gravity = Gravity.CENTER
        })
        root?.addView(label("اتجاه القبلة: " + qiblaBearing.toInt() + "° من الشمال الحقيقي تقريبًا.", 18f, accent(), true))
        val sensorAvailable = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
        root?.addView(label(
            if (sensorAvailable) "حرّك الهاتف ببطء بشكل أفقي. إذا لم يستقر الاتجاه، عاير البوصلة بعيدًا عن المعادن والأجهزة المغناطيسية."
            else "مستشعر البوصلة غير متاح في هذا الجهاز؛ يظهر اتجاه القبلة الحسابي فقط.",
            12f,
            muted()
        ))
        if (sensorAvailable) startCompassSensor()
    }

    private fun showAdhkar() {
        val items = listOf(
            Triple("سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", "صحيح البخاري وصحيح مسلم.", "ورد في فضل قولها مائة مرة في اليوم."),
            Triple("أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ", "صحيح البخاري.", "كان النبي ﷺ يكثر من الاستغفار والتوبة في اليوم."),
            Triple("لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ", "صحيح البخاري وصحيح مسلم.", "ورد في فضل قولها مائة مرة في اليوم."),
            Triple("سُبْحَانَ اللَّهِ وَالْحَمْدُ لِلَّهِ وَلَا إِلَهَ إِلَّا اللَّهُ وَاللَّهُ أَكْبَرُ", "صحيح مسلم، كتاب الذكر والدعاء.", "من الكلمات المحبوبة إلى الله، ويجوز ذكرها دون عدد محدد هنا."),
            Triple("لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", "صحيح البخاري وصحيح مسلم.", "كنز من كنوز الجنة كما ورد في الحديث."),
            Triple("اللَّهُمَّ أَعِنِّي عَلَى ذِكْرِكَ وَشُكْرِكَ وَحُسْنِ عِبَادَتِكَ", "سنن أبي داود والنسائي.", "وصية نبوية تقال في دبر الصلاة."),
            Triple("بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ", "سنن أبي داود وجامع الترمذي.", "ورد قولها ثلاث مرات صباحًا وثلاثًا مساءً."),
            Triple("رَضِيتُ بِاللَّهِ رَبًّا وَبِالْإِسْلَامِ دِينًا وَبِمُحَمَّدٍ ﷺ نَبِيًّا", "سنن أبي داود.", "ورد في أذكار الصباح والمساء، ويقال ثلاث مرات."),
            Triple("اللَّهُمَّ بِكَ أَصْبَحْنَا وَبِكَ أَمْسَيْنَا وَبِكَ نَحْيَا وَبِكَ نَمُوتُ وَإِلَيْكَ النُّشُورُ", "جامع الترمذي وسنن أبي داود.", "يقال في الصباح، وعند المساء يقال: اللهم بك أمسينا وبك أصبحنا."),
            Triple("اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي، فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ", "صحيح البخاري.", "هذا سيد الاستغفار، وله فضل عظيم إذا قاله العبد موقنًا به."),
            Triple("سُبْحَانَ اللَّهِ وَبِحَمْدِهِ عَدَدَ خَلْقِهِ وَرِضَا نَفْسِهِ وَزِنَةَ عَرْشِهِ وَمِدَادَ كَلِمَاتِهِ", "صحيح مسلم.", "ذكر عظيم علّمه النبي ﷺ لجويرية رضي الله عنها."),
            Triple("اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي الدُّنْيَا وَالْآخِرَةِ", "سنن أبي داود وابن ماجه.", "دعاء جامع في سؤال العفو والعافية.")
        )
        screen("الأذكار", items.size.toString() + " أذكار مختارة مع المصادر والعداد")
        root?.addView(label("ابحث عن الذكر أو مصدره. راجع عدد التكرار الوارد في المصدر، فالعداد هنا للتسجيل ولا يفرض عددًا على الذكر.", 12f, muted()))
        val search = SearchView(this).apply {
            queryHint = "ابحث في الأذكار…"
            isIconifiedByDefault = false
        }
        root?.addView(search)
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root?.addView(results)
        fun renderAdhkar(query: String) {
            results.removeAllViews()
            val found = items.withIndex().filter { (_, item) ->
                item.first.contains(query.trim(), true) ||
                    item.second.contains(query.trim(), true) ||
                    item.third.contains(query.trim(), true)
            }
            found.forEach { (index, item) ->
                var count = prefs.getInt("dhikr_" + index, 0)
                results.addView(label(item.first, 19f, accent(), true))
                results.addView(label("المصدر: " + item.second, 11f, muted()))
                results.addView(label(item.third, 12f, ink()))
                val counter = label("التكرار المسجل: " + count, 14f, ink(), true)
                results.addView(counter)
                results.addView(button("تسجيل تكرار +", true) {
                    count++
                    prefs.edit().putInt("dhikr_" + index, count).apply()
                    counter.text = "التكرار المسجل: " + count
                })
                results.addView(button("تصفير هذا العداد", false) {
                    count = 0
                    prefs.edit().putInt("dhikr_" + index, 0).apply()
                    counter.text = "التكرار المسجل: 0"
                })
                results.addView(gap(12))
            }
            if (found.isEmpty()) results.addView(label("لا توجد أذكار تطابق البحث.", 14f, muted()))
        }
        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                renderAdhkar(query.orEmpty())
                search.clearFocus()
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                renderAdhkar(newText.orEmpty())
                return true
            }
        })
        renderAdhkar("")
    }

    private fun showTasbeeh() {
        screen("التسبيح", "عداد يحفظ العدد على جهازك")
        var count = prefs.getInt("tasbeeh", 0)
        val display = label(count.toString(), 38f, accent(), true).apply { gravity = Gravity.CENTER }
        root?.addView(display)
        root?.addView(button("سبحان الله  +", true) {
            count++; prefs.edit().putInt("tasbeeh", count).apply(); display.text = count.toString()
        })
        root?.addView(button("إعادة العداد", false) {
            count = 0; prefs.edit().putInt("tasbeeh", 0).apply(); display.text = "0"
        })
    }

    private fun showSirah() {
        val events = listOf(
            Triple("المولد والنشأة", "وُلد النبي ﷺ في مكة، ونشأ يتيم الأب، ثم كفله جده عبد المطلب وبعده عمه أبو طالب. عُرف قبل البعثة بالصدق والأمانة.", "سيرة ابن هشام؛ الطبقات الكبرى لابن سعد. بعض تفاصيل التواريخ والروايات تحتاج إلى تمييز درجاتها."),
            Triple("نزول الوحي", "كان يتعبد في غار حراء، فجاءه جبريل عليه السلام بأول الوحي من سورة العلق، ثم بدأت مرحلة تبليغ الرسالة.", "صحيح البخاري، كتاب بدء الوحي؛ العلق: 1–5؛ المدثر: 1–7."),
            Triple("الدعوة في مكة", "بدأت الدعوة بتعليم التوحيد وتزكية النفوس، ثم جهر النبي ﷺ بالدعوة، وصبر هو وأصحابه على الأذى والثبات على الحق.", "سيرة ابن هشام؛ المدثر: 1–7؛ الحجر: 94–99."),
            Triple("الهجرة إلى المدينة", "أذن الله للمؤمنين بالهجرة، وخرج النبي ﷺ مع أبي بكر رضي الله عنه، ثم وصل إلى المدينة وأسس مجتمعًا يقوم على الإيمان والأخوة.", "صحيح البخاري، كتاب مناقب الأنصار والهجرة؛ التوبة: 40؛ الأنفال: 72."),
            Triple("بدر وأحد والخندق", "شهدت المدينة أحداثًا كبرى ثبت فيها المؤمنون وتعلموا الطاعة والصبر والشورى، ووردت في القرآن إشارات مفصلة إلى هذه الوقائع.", "آل عمران: 121–180؛ الأنفال: 5–19؛ الأحزاب: 9–27؛ كتب المغازي الصحيحة."),
            Triple("صلح الحديبية", "خرج النبي ﷺ وأصحابه يريدون العمرة، فوقع الصلح مع قريش، وكان في عاقبته فتح وخير للمسلمين رغم ما بدا في بعض شروطه أول الأمر.", "الفتح: 1–29؛ صحيح البخاري، كتاب الشروط والمغازي."),
            Triple("فتح مكة", "دخل النبي ﷺ مكة في العام الثامن للهجرة، وأزال مظاهر الشرك من البيت، وأعلن العفو والصفح عن كثير ممن آذوه.", "صحيح البخاري، كتاب المغازي؛ التوبة: 25–27؛ النصر: 1–3."),
            Triple("حجة الوداع", "حج النبي ﷺ بالناس في السنة العاشرة للهجرة، وبيّن لهم مناسكهم وأوصاهم بالحقوق والتمسك بالدين.", "صحيح مسلم، كتاب الحج؛ البقرة: 197؛ المائدة: 3."),
            Triple("وفاته ﷺ", "توفي النبي ﷺ في المدينة في السنة الحادية عشرة للهجرة بعد أن بلّغ الرسالة وأدى الأمانة ونصح الأمة، ودفن في حجرة عائشة رضي الله عنها.", "صحيح البخاري وصحيح مسلم، كتاب المغازي والفضائل؛ سيرة ابن هشام.")
        )
        screen("السيرة النبوية", events.size.toString() + " محطات مختارة مع مراجع")
        root?.addView(label("ابحث عن مرحلة أو حدث. هذه خلاصة تعليمية، وتفاصيل السيرة تُراجع في المصادر المحققة.", 12f, muted()))
        val search = SearchView(this).apply {
            queryHint = "ابحث في السيرة…"
            isIconifiedByDefault = false
        }
        root?.addView(search)
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root?.addView(results)
        fun renderEvents(query: String) {
            results.removeAllViews()
            val found = events.filter {
                it.first.contains(query.trim(), true) ||
                    it.second.contains(query.trim(), true) ||
                    it.third.contains(query.trim(), true)
            }
            found.forEachIndexed { index, event ->
                results.addView(button((index + 1).toString() + ". " + event.first, false) {
                    AlertDialog.Builder(this)
                        .setTitle(event.first)
                        .setMessage(event.second + "\n\nالمراجع: " + event.third)
                        .setPositiveButton("إغلاق", null)
                        .show()
                })
                results.addView(gap(7))
            }
            if (found.isEmpty()) results.addView(label("لا توجد نتائج في السيرة.", 14f, muted()))
        }
        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                renderEvents(query.orEmpty())
                search.clearFocus()
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                renderEvents(newText.orEmpty())
                return true
            }
        })
        renderEvents("")
    }

    private fun showHadith() {
        val items = listOf(
            Triple("إنما الأعمال بالنيات، وإنما لكل امرئ ما نوى.", "صحيح البخاري وصحيح مسلم.", "يبين الحديث أن النية أساس العمل، وأن ثواب الإنسان مرتبط بما قصده من عمله."),
            Triple("الدين النصيحة.", "صحيح مسلم.", "النصيحة إرادة الخير وإخلاصه لله، واتباع الحق، وبذل النصح للمسلمين برفق وصدق."),
            Triple("من كان يؤمن بالله واليوم الآخر فليقل خيرًا أو ليصمت.", "صحيح البخاري وصحيح مسلم.", "يرشد الحديث إلى حفظ اللسان، وألا يتكلم المسلم إلا بما فيه خير أو مصلحة."),
            Triple("لا يؤمن أحدكم حتى يحب لأخيه ما يحب لنفسه.", "صحيح البخاري وصحيح مسلم.", "يدعو الحديث إلى محبة الخير للآخرين وترك الأنانية والحسد."),
            Triple("الكلمة الطيبة صدقة.", "صحيح البخاري وصحيح مسلم.", "الكلام الحسن وإدخال السرور والإصلاح بين الناس من أبواب الخير."),
            Triple("يسروا ولا تعسروا، وبشروا ولا تنفروا.", "صحيح البخاري وصحيح مسلم.", "فيه توجيه إلى الرفق والتيسير في التعليم والدعوة، مع الالتزام بالحق."),
            Triple("المسلم من سلم المسلمون من لسانه ويده.", "صحيح البخاري وصحيح مسلم.", "من كمال إسلام المرء أن يكف أذاه عن الناس قولًا وفعلًا."),
            Triple("من لا يَرحم لا يُرحم.", "صحيح البخاري وصحيح مسلم.", "يرشد إلى الرحمة بالخلق، وأن الجزاء من جنس العمل."),
            Triple("الطهور شطر الإيمان.", "صحيح مسلم.", "يبين فضل الطهارة ومكانتها في عبادة المسلم ونظافته."),
            Triple("أحب الأعمال إلى الله أدومها وإن قل.", "صحيح البخاري وصحيح مسلم.", "يدعو إلى المداومة على العمل الصالح ولو كان قليلًا بدل الانقطاع."),
            Triple("من سلك طريقًا يلتمس فيه علمًا سهل الله له به طريقًا إلى الجنة.", "صحيح مسلم.", "يبين فضل طلب العلم النافع والسعي إليه."),
            Triple("لا تغضب.", "صحيح البخاري.", "وصية جامعة بضبط النفس واجتناب أسباب الغضب وما يترتب عليه من ظلم."),
            Triple("خيركم من تعلم القرآن وعلمه.", "صحيح البخاري.", "يدل على فضل تعلم القرآن وتعليمه للناس بإتقان وإخلاص."),
            Triple("من غشنا فليس منا.", "صحيح مسلم.", "تحذير من الغش والخداع في المعاملات وسائر شؤون الناس."),
            Triple("الرفق لا يكون في شيء إلا زانه، ولا ينزع من شيء إلا شانه.", "صحيح مسلم.", "الرفق يحسن الأعمال والتعامل، والعنف بغير حق يفسدها."),
            Triple("إن الله كتب الإحسان على كل شيء.", "صحيح مسلم.", "يدعو إلى الإتقان والإحسان في الأعمال والتعامل مع الخلق."),
            Triple("المسلم أخو المسلم، لا يظلمه ولا يخذله ولا يحقره.", "صحيح مسلم.", "يقرر الحديث حقوق الأخوة من النصرة وترك الظلم والاحتقار."),
            Triple("لا يدخل الجنة من كان في قلبه مثقال ذرة من كبر.", "صحيح مسلم.", "يحذر من الكبر واحتقار الناس ورد الحق، ويبين خطورة هذا الخلق."),
            Triple("آية المنافق ثلاث: إذا حدث كذب، وإذا وعد أخلف، وإذا اؤتمن خان.", "صحيح البخاري وصحيح مسلم.", "يحذر الحديث من خصال خطيرة تناقض الأمانة والصدق والوفاء."),
            Triple("من كان يؤمن بالله واليوم الآخر فليكرم جاره.", "صحيح البخاري وصحيح مسلم.", "يبين حق الجار ووجوب الإحسان إليه وحفظ حقه."),
            Triple("لا يدخل الجنة نمام.", "صحيح البخاري وصحيح مسلم.", "يحذر من نقل الكلام بين الناس بقصد الإفساد وإيقاع العداوة."),
            Triple("إن الله جميل يحب الجمال.", "صحيح مسلم.", "يبين أن التجمل المباح لا ينافي التواضع، وأن المذموم هو الكبر ورد الحق واحتقار الناس."),
            Triple("من حسن إسلام المرء تركه ما لا يعنيه.", "جامع الترمذي.", "يرشد إلى الاشتغال بما ينفع وترك التدخل فيما لا فائدة فيه."),
            Triple("اتق الله حيثما كنت، وأتبع السيئة الحسنة تمحها، وخالق الناس بخلق حسن.", "جامع الترمذي.", "يجمع الحديث بين التقوى والتوبة والعمل الصالح وحسن معاملة الناس."),
            Triple("تبسمك في وجه أخيك لك صدقة.", "جامع الترمذي.", "يبين أن البشاشة وحسن استقبال الناس من أعمال البر."),
            Triple("الراحمون يرحمهم الرحمن، ارحموا من في الأرض يرحمكم من في السماء.", "سنن أبي داود وجامع الترمذي.", "يدعو إلى الرحمة بالناس وسائر المخلوقات طلبًا لرحمة الله."),
            Triple("من صلى علي صلاة صلى الله عليه بها عشرًا.", "صحيح مسلم.", "يبين فضل الصلاة والسلام على النبي ﷺ وعظيم أجرها."),
            Triple("أكمل المؤمنين إيمانًا أحسنهم خلقًا.", "سنن أبي داود وجامع الترمذي.", "يربط الحديث كمال الإيمان بحسن الخلق ولين المعاملة.")
        )
        screen("الحديث النبوي", "مختارات صحيحة • " + items.size + " حديثًا في هذه الدفعة")
        root?.addView(label("ابحث في نص الحديث أو مصدره أو شرحه. الشرح موجز للتعلم وليس بديلًا عن كتب الشروح.", 12f, muted()))
        val search = SearchView(this).apply {
            queryHint = "ابحث في الأحاديث…"
            isIconifiedByDefault = false
        }
        root?.addView(search)
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root?.addView(results)
        fun renderHadith(query: String) {
            results.removeAllViews()
            val found = items.filter {
                it.first.contains(query.trim(), true) ||
                    it.second.contains(query.trim(), true) ||
                    it.third.contains(query.trim(), true)
            }
            found.forEachIndexed { index, item ->
                results.addView(cardView(
                    "حديث " + (index + 1),
                    "«" + item.first + "»\nالمصدر: " + item.second + "\nالشرح الموجز: " + item.third
                ))
            }
            if (found.isEmpty()) results.addView(label("لا توجد أحاديث تطابق البحث.", 14f, muted()))
        }
        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                renderHadith(query.orEmpty())
                search.clearFocus()
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                renderHadith(newText.orEmpty())
                return true
            }
        })
        renderHadith("")
    }

    private fun cardView(title: String, description: String): View {
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(panel())
            setPadding(15, 12, 15, 12)
            addView(label(title, 16f, accent(), true))
            addView(label(description, 15f, ink()))
        }
        val params = LinearLayout.LayoutParams(-1, -2)
        params.bottomMargin = 8
        view.layoutParams = params
        return view
    }

    private fun showStories(companions: Boolean) {
        val title = if (companions) "قصص الصحابة" else "قصص الأنبياء"
        val items = if (companions) listOf(
            Triple("أبو بكر الصديق رضي الله عنه", "كان من السابقين إلى الإسلام، وصاحب النبي ﷺ في الهجرة. قال الله تعالى في وصف موقف الغار: «إِذْ يَقُولُ لِصَاحِبِهِ لَا تَحْزَنْ إِنَّ اللَّهَ مَعَنَا». تولّى الخلافة بعد وفاة النبي ﷺ، وثبّت المسلمين في أحداث الردة، وجُمع القرآن في عهده بعد مشورة عمر رضي الله عنهما.", "التوبة: 40؛ صحيح البخاري، كتاب فضائل أصحاب النبي؛ البداية والنهاية، أحداث سنة 11 هـ."),
            Triple("عمر بن الخطاب رضي الله عنه", "أسلم عمر رضي الله عنه فكان إسلامه عزًا للمسلمين، ثم صار ثاني الخلفاء الراشدين. عُرف بالحزم والعدل، واتسعت في عهده الدولة الإسلامية، ونُظّمت شؤون القضاء والولايات والدواوين. قُتل سنة 23 هـ.", "صحيح البخاري، كتاب فضائل أصحاب النبي؛ تاريخ الطبري، حوادث السنوات 13–23 هـ؛ سير أعلام النبلاء."),
            Triple("عثمان بن عفان رضي الله عنه", "من السابقين إلى الإسلام، ولقّبه المسلمون بذي النورين لزواجه ابنتي رسول الله ﷺ. جهّز جيش العسرة، واشترى بئر رومة، وفي خلافته نُسخت المصاحف وأُرسلت إلى الأمصار لتوحيد الناس على الرسم العثماني. استشهد سنة 35 هـ.", "صحيح البخاري، كتاب فضائل أصحاب النبي؛ سنن الترمذي، المناقب؛ سير أعلام النبلاء."),
            Triple("علي بن أبي طالب رضي الله عنه", "ابن عم النبي ﷺ وزوج فاطمة رضي الله عنها، ومن أوائل من أسلم. نام في فراش النبي ﷺ ليلة الهجرة وردّ الأمانات إلى أهلها، ثم شارك في أحداث المدينة. صار رابع الخلفاء الراشدين، واستشهد سنة 40 هـ.", "سيرة ابن هشام؛ الطبقات الكبرى لابن سعد؛ سير أعلام النبلاء."),
            Triple("خديجة بنت خويلد رضي الله عنها", "أول زوجات النبي ﷺ وأول من آمن به من الناس. لما رجع النبي ﷺ من غار حراء ثبّتته وذكّرته بخصاله الكريمة، ثم أخذته إلى ورقة بن نوفل. وقفت معه في بداية الدعوة، وتوفيت قبل الهجرة في عام الحزن.", "صحيح البخاري، بدء الوحي؛ سيرة ابن هشام؛ الطبقات الكبرى لابن سعد."),
            Triple("بلال بن رباح رضي الله عنه", "كان من السابقين إلى الإسلام، وعانى التعذيب بسبب توحيده لله، فاشتراه أبو بكر رضي الله عنه وأعتقه. اختاره النبي ﷺ مؤذنًا، وكان من أصحاب الهجرة والمشاهد.", "سير أعلام النبلاء؛ الإصابة في تمييز الصحابة؛ صحيح البخاري، الأذان."),
            Triple("مصعب بن عمير رضي الله عنه", "نشأ في مكة في نعمة، ثم ترك الترف بعد إسلامه. بعثه النبي ﷺ إلى المدينة قبل الهجرة ليعلّم أهلها القرآن والإسلام، فكان سببًا في إسلام بيوت كثيرة. استشهد في غزوة أحد سنة 3 هـ.", "سيرة ابن هشام، خبر بيعة العقبة والهجرة؛ الطبقات الكبرى لابن سعد؛ سير أعلام النبلاء."),
            Triple("أسماء بنت أبي بكر رضي الله عنها", "لُقبت بذات النطاقين؛ إذ شقّت نطاقها لتجعل منه زادًا للنبي ﷺ وأبيها أثناء الهجرة. كانت ثابتة شجاعة، وعاشت عمرًا طويلًا، وروت أحاديث عن النبي ﷺ.", "صحيح البخاري، مناقب الأنصار والهجرة؛ الإصابة في تمييز الصحابة؛ سير أعلام النبلاء."),
            Triple("أبو عبيدة بن الجراح رضي الله عنه", "من السابقين إلى الإسلام، وهاجر وشهد المشاهد مع النبي ﷺ. عُرف بالأمانة، ووصفه النبي ﷺ بأنه أمين هذه الأمة، وتولى قيادة جيوش المسلمين في الشام.", "صحيح البخاري، فضائل أصحاب النبي؛ صحيح مسلم، فضائل الصحابة؛ سير أعلام النبلاء."),
            Triple("سعد بن أبي وقاص رضي الله عنه", "من أوائل من أسلم، وكان من الرماة المعروفين، وشهد بدرًا وما بعدها. تولى قيادة جيش المسلمين في القادسية، وكان من أصحاب الشورى الستة.", "صحيح البخاري، المغازي وفضائل الصحابة؛ الطبقات الكبرى لابن سعد؛ سير أعلام النبلاء."),
            Triple("سلمان الفارسي رضي الله عنه", "بحث عن الدين الحق وتنقل في طلبه حتى هداه الله إلى الإسلام. آخى النبي ﷺ بينه وبين أبي الدرداء، وأشار يوم الخندق بحفر الخندق حول المدينة، فكان رأيه سببًا في الأخذ بخطة دفاعية نافعة.", "سيرة ابن هشام، غزوة الخندق؛ الطبقات الكبرى لابن سعد؛ سير أعلام النبلاء."),
            Triple("زيد بن حارثة رضي الله عنه", "كان مولى رسول الله ﷺ ثم تبناه قبل تحريم التبني، وكان محبوبًا عند النبي ﷺ. قاد جيش مؤتة واستشهد فيها، وذُكر اسمه صريحًا في القرآن في سورة الأحزاب.", "الأحزاب: 37؛ صحيح البخاري، المغازي؛ سيرة ابن هشام؛ الإصابة في تمييز الصحابة."),
            Triple("أبو هريرة رضي الله عنه", "لازم النبي ﷺ وحفظ عنه أحاديث كثيرة، وكان حريصًا على العلم والتحديث. عُرف بكثرة روايته وبملازمته للنبي ﷺ في أواخر حياته بالمدينة.", "صحيح البخاري وصحيح مسلم، فضائل الصحابة والعلم؛ سير أعلام النبلاء؛ الإصابة في تمييز الصحابة."),
            Triple("أنس بن مالك رضي الله عنه", "خدم النبي ﷺ عشر سنين، ودعا له النبي ﷺ بالبركة، وعاش بعده زمنًا طويلًا يعلّم الناس ويروي الحديث.", "صحيح البخاري وصحيح مسلم، فضائل الصحابة؛ سير أعلام النبلاء."),
            Triple("عبد الله بن مسعود رضي الله عنه", "من السابقين إلى الإسلام، ومن كبار قراء الصحابة وفقهائهم. عُرف بعلمه بالقرآن وملازمته للنبي ﷺ وخدمته له.", "صحيح البخاري وصحيح مسلم، فضائل أصحاب النبي؛ سير أعلام النبلاء؛ طبقات ابن سعد."),
            Triple("عمار بن ياسر رضي الله عنه", "كان من السابقين إلى الإسلام، وابتُلي هو وأهله بسبب إيمانهم، ثم شهد المشاهد مع النبي ﷺ. ورد في الحديث الصحيح خبر مقتله على يد الفئة الباغية.", "صحيح البخاري وصحيح مسلم، الفتن والمغازي؛ سير أعلام النبلاء؛ الإصابة في تمييز الصحابة."),
            Triple("معاذ بن جبل رضي الله عنه", "كان من علماء الصحابة، وأرسله النبي ﷺ إلى اليمن ليعلّم الناس ويدعوهم. عُرف بالعلم والفقه وحسن التعليم.", "صحيح البخاري وصحيح مسلم، الزكاة والبعث إلى اليمن؛ سير أعلام النبلاء."),
            Triple("حمزة بن عبد المطلب رضي الله عنه", "عم النبي ﷺ وأخوه من الرضاعة، أسلم فنصر الله به الإسلام، وشهد بدرًا واستشهد في غزوة أحد، ولُقب بسيد الشهداء.", "سيرة ابن هشام، غزوة أحد؛ الطبقات الكبرى لابن سعد؛ سير أعلام النبلاء."),
            Triple("عائشة بنت أبي بكر رضي الله عنها", "زوج النبي ﷺ، وكانت من أعلم نساء الأمة، نقلت أحاديث كثيرة وعلّمت الصحابة والتابعين، ورُويت عنها مسائل في العلم والفقه.", "صحيح البخاري وصحيح مسلم، فضائل عائشة والعلم؛ سير أعلام النبلاء."),
            Triple("حفصة بنت عمر رضي الله عنها", "زوج النبي ﷺ، وكانت من العابدات، وحُفظت عندها الصحف التي جُمعت في عهد أبي بكر رضي الله عنهما حتى نُسخت المصاحف في عهد عثمان.", "صحيح البخاري، فضائل القرآن وجمع المصحف؛ الطبقات الكبرى لابن سعد."),
            Triple("أم سلمة رضي الله عنها", "من أمهات المؤمنين، هاجرت وصبرت على البلاء، وكان لها رأي سديد يوم الحديبية حين أشارت على النبي ﷺ أن ينحر ويحلق ليقتدي به أصحابه.", "صحيح البخاري، الشروط والمغازي؛ سيرة ابن هشام؛ سير أعلام النبلاء."),
            Triple("فاطمة الزهراء رضي الله عنها", "ابنة النبي ﷺ وأحب أهله إليه، عُرفت بالصبر والعبادة، وهي زوج علي رضي الله عنه وأم الحسن والحسين رضي الله عنهما.", "صحيح البخاري وصحيح مسلم، فضائل فاطمة وأهل البيت؛ الطبقات الكبرى لابن سعد."),
            Triple("أبو الدرداء رضي الله عنه", "من أصحاب النبي ﷺ، عُرف بالحكمة والعبادة وتعليم القرآن، وروى أحاديث في العلم والذكر وحسن الخلق.", "صحيح البخاري، العلم والأدب؛ سير أعلام النبلاء؛ الطبقات الكبرى لابن سعد."),
            Triple("أبو طلحة الأنصاري رضي الله عنه", "من الأنصار، شهد مع النبي ﷺ مشاهد، ولما نزل قوله تعالى: لن تنالوا البر حتى تنفقوا مما تحبون تصدق ببستان بيرحاء، وكان النبي ﷺ يثني على صدقته.", "آل عمران: 92؛ صحيح البخاري، الزكاة والوصايا؛ سير أعلام النبلاء."),
            Triple("أبو موسى الأشعري رضي الله عنه", "صحابي جليل، عُرف بحسن الصوت بالقرآن والعلم، وولاه عمر رضي الله عنه بعض أعمال المسلمين، وروى أحاديث كثيرة.", "صحيح البخاري وصحيح مسلم، فضائل الصحابة والقرآن؛ سير أعلام النبلاء.")
        ) else listOf(
            Triple("آدم عليه السلام", "خلق الله آدم وعلّمه الأسماء، وأمر الملائكة بالسجود له سجود تكريم، فسجدوا إلا إبليس أبى واستكبر. أسكنه الله الجنة مع زوجه، ثم تاب عليه بعد أن تلقّى كلمات من ربه. وفي قصته بيان فضل العلم وخطر الكبر وفتح باب التوبة.", "البقرة: 30–39؛ طه: 115–123؛ الأعراف: 11–27."),
            Triple("نوح عليه السلام", "لبث نوح في قومه يدعوهم إلى عبادة الله وحده زمنًا طويلًا، ليلًا ونهارًا وسرًا وجهارًا. كذّبه أكثر قومه، فأمره الله بصنع السفينة، فنجّاه ومن آمن معه، وأغرق المكذبين. وتعلّم قصته الصبر والثبات وعدم اليأس من الدعوة.", "سورة نوح كاملة؛ هود: 25–49؛ المؤمنون: 23–30؛ العنكبوت: 14."),
            Triple("إبراهيم عليه السلام", "نظر إبراهيم في ملكوت السماوات والأرض، وأنكر عبادة الأصنام والكواكب، ودعا أباه وقومه إلى التوحيد. حطّم الأصنام ليبيّن عجزها، فألقاه قومه في النار فجعلها الله بردًا وسلامًا. وابتلاه الله بأوامر عظيمة فاستجاب، ورفع قواعد البيت مع إسماعيل.", "الأنبياء: 51–73؛ الصافات: 83–113؛ البقرة: 124–129؛ الأنعام: 74–79."),
            Triple("يوسف عليه السلام", "بدأت قصته برؤيا رآها صغيرًا، ثم حسده إخوته وألقوه في الجب. بيع في مصر، وابتُلي بمراودة امرأة العزيز والسجن، فصبر واستعان بالله. ثم أكرمه الله بالتمكين وجمع شمله بأهله، وعفا عن إخوته عند قدرته عليهم.", "سورة يوسف كاملة، وهي المرجع القرآني المفصل للقصة."),
            Triple("موسى عليه السلام", "وُلد موسى في زمن كان فرعون يقتل أبناء بني إسرائيل، فأوحى الله إلى أمه أن تلقيه في اليم مع وعد الحفظ. نشأ في بيت فرعون، ثم أرسله الله إليه بالآيات، ودعا بني إسرائيل إلى النجاة. شق الله له البحر فنجّاه ومن معه وأغرق فرعون وجنده.", "القصص: 3–46؛ طه: 9–79؛ الشعراء: 10–68؛ يونس: 90–92."),
            Triple("يونس عليه السلام", "دعا يونس قومه ثم خرج مغاضبًا قبل أن يؤذن له، فكان في بطن الحوت، فنادى ربه معترفًا بتقصيره ومسبحًا له. فاستجاب الله له ونجّاه، وأعاد قومه إلى الإيمان فانتفعوا بذلك. وفي قصته فضل الدعاء والتسبيح والرجوع إلى الله.", "الأنبياء: 87–88؛ الصافات: 139–148؛ القلم: 48–50."),
            Triple("أيوب عليه السلام", "ابتُلي أيوب عليه السلام بضرّ، فصبر ولم ييأس من رحمة الله، ودعا ربه بأدب وتضرع: «أَنِّي مَسَّنِيَ الضُّرُّ وَأَنْتَ أَرْحَمُ الرَّاحِمِينَ». فاستجاب الله له وكشف ما به، وردّ عليه أهله ومثلهم معهم رحمة منه.", "الأنبياء: 83–84؛ ص: 41–44."),
            Triple("داود عليه السلام", "آتاه الله الملك والحكمة، وقتل جالوت بإذن الله، وأنزل عليه الزبور. وعلّمه الله صناعة الدروع، وسخّر معه الجبال والطير يسبحن. وكان نبيًا ملكًا يحكم بالحق، وفي قصته تذكير بالعدل وشكر النعمة.", "البقرة: 251؛ النساء: 163؛ الأنبياء: 78–80؛ سبأ: 10–11؛ ص: 17–26."),
            Triple("هود عليه السلام", "أرسل الله هودًا إلى عاد يدعوهم إلى عبادة الله وترك الطغيان، فكذبوه واغتروا بقوتهم. فأنذرهم بالعذاب، ثم نجّى الله هودًا والذين آمنوا معه وأهلك عادًا بريح شديدة.", "هود: 50–60؛ الأحقاف: 21–26؛ الحاقة: 6–8."),
            Triple("صالح عليه السلام", "أرسل الله صالحًا إلى ثمود، فدعاهم إلى التوحيد وجعل لهم الناقة آية، وأمرهم ألا يمسوها بسوء. فعقرها المكذبون واستكبروا، فأخذتهم الرجفة والصيحة ونجّى الله صالحًا والمؤمنين.", "هود: 61–68؛ الأعراف: 73–79؛ الشمس: 11–15."),
            Triple("إسماعيل عليه السلام", "كان صادق الوعد، يأمر أهله بالصلاة والزكاة، وابتلاه الله مع أبيه إبراهيم بأمر الذبح فاستسلما لأمر الله، ففداه الله بذبح عظيم. وشارك أباه في رفع قواعد الكعبة.", "مريم: 54–55؛ الصافات: 102–107؛ البقرة: 125–129."),
            Triple("زكريا عليه السلام", "دعا زكريا ربه أن يرزقه ولدًا صالحًا مع كبر سنه، فاستجاب الله له وبشره بيحيى. وفي قصته حسن الظن بالله والإلحاح في الدعاء مع اليقين بقدرته.", "آل عمران: 37–41؛ مريم: 2–11؛ الأنبياء: 89–90."),
            Triple("عيسى عليه السلام", "خلق الله عيسى من غير أب، وجعله آية للناس، وأيده بالبينات، ودعا بني إسرائيل إلى عبادة الله وحده. وهو عبد الله ورسوله وكلمته ألقاها إلى مريم، ولم يُقتل ولم يُصلب بل رفعه الله إليه.", "آل عمران: 45–59؛ النساء: 157–158؛ مريم: 16–36؛ المائدة: 110–120."),
            Triple("إسحاق عليه السلام", "بشّر الله إبراهيم وزوجه سارة بإسحاق، وجعله نبيًا من الصالحين، ومن ذريته يعقوب. وفي قصته بيان فضل رحمة الله وإجابة الدعاء.", "هود: 71–73؛ الصافات: 112–113؛ العنكبوت: 27."),
            Triple("يعقوب عليه السلام", "كان نبيًا صابرًا، ابتُلي بفقد يوسف ثم طال انتظاره، فلم ييأس من روح الله، وأوصى أبناءه بالتوحيد. جمع الله شمله بيوسف في نهاية القصة.", "سورة يوسف: 83–98؛ البقرة: 132–133."),
            Triple("لوط عليه السلام", "أرسل الله لوطًا إلى قومه يدعوهم إلى الطهارة وترك الفاحشة، فكذبوه، فنجّاه الله وأهله إلا امرأته، وأهلك القوم الظالمين.", "هود: 77–83؛ الأعراف: 80–84؛ العنكبوت: 28–35."),
            Triple("شعيب عليه السلام", "أرسل إلى مدين يدعوهم إلى عبادة الله وإيفاء الكيل والميزان وترك الفساد في الأرض، فكذبه الملأ، فنجّى الله شعيبًا والذين آمنوا معه.", "هود: 84–95؛ الأعراف: 85–93؛ الشعراء: 176–191."),
            Triple("هارون عليه السلام", "كان أخا موسى ووزيره في الدعوة، فسأل موسى ربه أن يشد أزره به، وشارك في تبليغ الرسالة لقوم فرعون، وصبر على فتنة قومه في غياب موسى.", "طه: 29–36؛ الأعراف: 142–150؛ طه: 90–94."),
            Triple("إدريس عليه السلام", "ذكره الله بالصديقية والنبوة ورفعة المكانة، وجاء ذكره في سياق الأنبياء الصابرين.", "مريم: 56–57؛ الأنبياء: 85–86."),
            Triple("ذو الكفل عليه السلام", "ذكره الله مع إسماعيل وإدريس في الصبر، ومع الأنبياء الأخيار في سورة ص، من غير تفصيل قصته في القرآن.", "الأنبياء: 85–86؛ ص: 48."),
            Triple("إلياس عليه السلام", "دعا قومه إلى عبادة الله وترك عبادة بعل، فكذب أكثرهم، وأثنى الله عليه وجعله من عباده المؤمنين.", "الصافات: 123–132؛ الأنعام: 85–86."),
            Triple("اليسع عليه السلام", "ذكره الله في جملة الأنبياء الأخيار، وأثنى عليه مع إسماعيل وذي الكفل، ولم يفصل القرآن أحداث حياته.", "الأنعام: 86؛ ص: 48."),
            Triple("يحيى عليه السلام", "بشر الله زكريا به غلامًا، وآتاه الحكم صبيًا، وكان تقيًا بارًا بوالديه ولم يكن جبارًا عصيًا.", "آل عمران: 39؛ مريم: 7–15."),
            Triple("سليمان عليه السلام", "ورث داود في النبوة والملك، وآتاه الله فهمًا وحكمة، وسخر له الريح والجن، وابتلاه بقصة الهدهد وملكة سبأ فدعاها إلى التوحيد.", "النمل: 15–44؛ سبأ: 12–14؛ ص: 30–40.")
        )
        screen(title, "قصص مع مراجع للرجوع • " + items.size + " قصة في هذه المجموعة")
        root?.addView(label("ابحث باسم الشخصية أو بكلمة من أحداث القصة. العدد الظاهر هو عدد القصص الموجودة فعلًا.", 12f, muted()))
        val search = SearchView(this).apply {
            queryHint = "ابحث في القصص…"
            isIconifiedByDefault = false
            setBackgroundColor(panel())
            setQuery("", false)
        }
        root?.addView(search)
        val results = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        root?.addView(results)
        fun renderStories(query: String) {
            results.removeAllViews()
            val filtered = items.filter { item ->
                query.isBlank() || item.first.contains(query.trim(), ignoreCase = true) ||
                    item.second.contains(query.trim(), ignoreCase = true) ||
                    item.third.contains(query.trim(), ignoreCase = true)
            }
            if (filtered.isEmpty()) {
                results.addView(label("لا توجد قصة تطابق عبارة البحث.", 14f, muted()))
                return
            }
            filtered.forEachIndexed { i, item ->
                results.addView(button((i + 1).toString() + ". " + item.first + "\n" + item.second.take(135) + "…", false) {
                    AlertDialog.Builder(this)
                        .setTitle(item.first)
                        .setMessage(item.second + "\n\nالمراجع: " + item.third)
                        .setPositiveButton("إغلاق", null)
                        .show()
                })
                results.addView(gap(8))
            }
        }
        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                renderStories(query.orEmpty())
                search.clearFocus()
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                renderStories(newText.orEmpty())
                return true
            }
        })
        renderStories("")
    }

    private fun showTajweed() {
        val lessons = listOf(
            Triple("مفهوم علم التجويد","إعطاء الحروف حقها ومستحقها من المخرج والصفة والأحكام؛ والغاية صيانة اللسان عن الخطأ في التلاوة.","تطبيق: استمع لقارئ متقن وردد الآية. المرجع: المقدمة الجزرية."),
            Triple("مراتب القراءة","التحقيق والترتيل والحدر والتدوير مراتب للأداء، وتبقى صحة الحروف والأحكام واجبة في جميعها.","تطبيق: اقرأ الفاتحة بتمهل ووضوح دون تكلف."),
            Triple("الاستعاذة","يستعيذ القارئ بالله من الشيطان الرجيم قبل القراءة؛ قال تعالى: فإذا قرأت القرآن فاستعذ بالله من الشيطان الرجيم.","الموضع: النحل 98. انتبه لنطق الذال في أعوذ."),
            Triple("البسملة","تقرأ البسملة في أوائل السور عدا سورة التوبة، وأوجه الوصل بين السورتين تؤخذ بالتلقي.","مثال: بسم الله الرحمن الرحيم في أول الفاتحة."),
            Triple("المخارج العامة","المخارج العامة خمسة: الجوف والحلق واللسان والشفتان والخيشوم.","تدريب: قارن بين الهمزة والهاء والقاف والكاف مع معلم متقن."),
            Triple("مخرج الجوف","تخرج حروف المد من الجوف: الألف والواو والياء بشروطها المعروفة.","أمثلة: قال، يقول، قيل. راقب امتداد الصوت."),
            Triple("مخرج الحلق","للحلق ثلاثة مواضع: أقصاه للهمزة والهاء، ووسطه للعين والحاء، وأدناه للغين والخاء.","أمثلة: أنعمت، الحمد، غير. الموضع: الفاتحة."),
            Triple("مخرج اللسان","تتوزع مخارج حروف اللسان بين أقصاه ووسطه وحافته وطرفه.","تدريب: ميّز بين القاف والكاف والجيم والشين واللام والنون والراء."),
            Triple("الشفتان والخيشوم","من الشفتين تخرج الفاء والواو والباء والميم، والغنة صوت من الخيشوم.","أمثلة: في، وعد، به، مالك. استمع للغنة في إنّ."),
            Triple("صفات الحروف","من الصفات: الهمس والجهر والشدة والرخاوة والاستعلاء والاستفال والإطباق والانفتاح.","تدريب: قارن بين السين والزاي والطاء والتاء. المرجع: الجزرية."),
            Triple("النون الساكنة والتنوين","أحكامهما أربعة: الإظهار الحلقي والإدغام والإقلاب والإخفاء الحقيقي.","تعلّم كل حكم مع أمثلته واستمع للأداء من قارئ متقن."),
            Triple("الإظهار الحلقي","يكون عند الهمزة والهاء والعين والحاء والغين والخاء، فتظهر النون أو التنوين.","أمثلة: من هاد، من علم."),
            Triple("الإدغام بغنة","يدغم التنوين أو النون الساكنة في الياء والنون والميم والواو مع الغنة.","مثال: من يعمل. الموضع: الزلزلة 7."),
            Triple("الإدغام بغير غنة","يكون في اللام والراء، من غير غنة.","مثال: من ربهم. الموضع: البقرة 5."),
            Triple("الإقلاب","إذا جاء بعد النون الساكنة أو التنوين حرف الباء تقلب ميمًا مخفاة مع الغنة.","مثال: من بعد. الموضع: البقرة 27."),
            Triple("الإخفاء الحقيقي","يكون مع خمسة عشر حرفًا، بالنطق بين الإظهار والإدغام مع الغنة.","مثال: من قبل. الموضع: البقرة 4."),
            Triple("أحكام الميم الساكنة","أحكامها: الإخفاء الشفوي عند الباء، والإدغام الشفوي عند الميم، والإظهار الشفوي مع بقية الحروف.","أمثلة: ترميهم بحجارة، لهم ما، أنعمت."),
            Triple("الغنة","صوت أغن يخرج من الخيشوم، ويظهر في النون والميم، ويتأكد في المشددتين وأحكام مخصوصة.","مثال: إنّ وثمّ. لا تعتمد على العد دون التلقي."),
            Triple("المد الطبيعي","مقداره حركتان، ولا يتوقف على همز أو سكون.","أمثلة: قال، فيه، يقول."),
            Triple("المد المتصل والمنفصل","المتصل يجتمع فيه حرف المد والهمز في كلمة، والمنفصل يكون الهمز في أول الكلمة التالية؛ ومقدار الأداء يتبع طريق القراءة.","أمثلة: جاء (متصل)، بما أنزل (منفصل)."),
            Triple("المد العارض للسكون","يكون عند الوقف على كلمة قبل آخرها حرف مد وبعده حرف متحرك يسكن لأجل الوقف؛ ويجوز مده حركتين أو أربعًا أو ستًا في رواية حفص من طريق الشاطبية.","مثال: العالمين عند الوقف عليها. يراعى توحيد مقدار المد في القراءة."),
            Triple("المد اللازم","هو أن يأتي بعد حرف المد سكون أصلي ثابت وصلاً ووقفًا، ومقداره ست حركات في رواية حفص.","مثال: الضالّين في الفاتحة، وهو مد لازم كلمي مثقل."),
            Triple("مد البدل","أن يتقدم الهمز على حرف المد في كلمة واحدة، ومقداره حركتان في رواية حفص من طريق الشاطبية.","أمثلة: آمنوا، إيمانًا، أوتوا."),
            Triple("مد اللين","يكون في الواو أو الياء الساكنتين المفتوح ما قبلهما إذا وقع بعدهما سكون عارض بسبب الوقف؛ وله أوجه عند الوقف.","مثال: خَوْف، بَيْت عند الوقف. يؤخذ ضبطه بالمشافهة."),
            Triple("صلة هاء الضمير","تلحق هاء الضمير بواو لفظية إذا ضمت أو بياء لفظية إذا كسرت عند تحقق شروط الصلة، وتُراعى أحكام المد بحسب ما بعدها.","مثال مشهور: إِنَّهُ كَانَ. لا تُصل كل هاء دون تحقق الشروط."),
            Triple("القلقلة","اضطراب صوت حرف من حروف قطب جد عند سكونه حتى يسمع له نبرة من غير إضافة حركة.","الحروف: ق ط ب ج د. أمثلة: يَقْطَعُون، أَحَدْ عند الوقف."),
            Triple("مراتب القلقلة","تظهر القلقلة في الحرف الساكن وسط الكلمة أو عند الوقف، وتكون أوضح عند الوقف على المشدد؛ والتقسيم التفصيلي للمراتب يدرّس على رواية وطريقة أداء معلومة.","تدريب: قارن بين أَطْعَمَهُم وبين الفَلَقْ عند الوقف، دون توليد حركة بعد الحرف."),
            Triple("تفخيم حروف الاستعلاء","حروف الاستعلاء سبعة: خ ص ض غ ط ق ظ، وهي مفخمة، وتتفاوت درجات التفخيم بحسب الحركة والسياق.","اجمعها في: خص ضغط قظ. قارن بين صراط وسراط في المخرج والصفة."),
            Triple("ترقيق حروف الاستفال","الأصل في حروف الاستفال الترقيق، مع استثناءات وأحكام عارضة معروفة كالراء ولام لفظ الجلالة.","تدريب: قارن بين تاء طيبة وطاء طيبة، ولا تخلط بين الترقيق وضعف الصوت."),
            Triple("لام لفظ الجلالة","تفخم لام لفظ الجلالة إذا سبقها فتح أو ضم، وترقق إذا سبقها كسر.","أمثلة: قالَ الله، عبدُ الله، بِالله. انتبه للحركة السابقة مباشرة."),
            Triple("أحكام الراء","للراء أحوال تفخم فيها وأحوال ترقق، ويتعلق الحكم بحركتها وحركة ما قبلها والسكون والعارض؛ ولها مسائل مستثناة.","تدريب: اقرأ رَبِّ و رِزقًا و فِرْعَوْن من المصحف مع معلم."),
            Triple("همزة الوصل","تثبت في الابتداء وتسقط في درج الكلام، ويُعرف الابتداء بها في الأسماء والأفعال والحروف وفق قواعدها.","أمثلة: ٱهْدِنَا عند الابتداء، وبِسْمِ الله في الوصل."),
            Triple("همزة القطع","تثبت نطقًا في الابتداء والوصل، وتكتب همزة ظاهرة فوق الألف أو تحتها بحسب الحركة.","أمثلة: أَنْعَمْتَ، إِيَّاكَ. ميّزها من همزة الوصل."),
            Triple("الوقف والابتداء","علم الوقف والابتداء يعين على أداء المعنى؛ فلا يتعمد القارئ وقفًا يفسد المعنى أو ابتداءً لا يستقيم معه الكلام.","تدريب: تعلّم علامات الوقف في مصحفك، وارجع إلى كتب الوقف والابتداء."),
            Triple("علامات الوقف في المصحف","من العلامات المشهورة: م للوقف اللازم، لا للنهي عن الوقف، ج لجواز الوقف، صلى للوصل أولى، قلى للوقف أولى؛ وقد تختلف بعض تفاصيل العرض بين الطبعات.","لا تجعل العلامة بديلًا عن فهم المعنى، وتعلمها من دليل المصحف."),
            Triple("السكت","قطع الصوت زمنًا لطيفًا من غير تنفس مع نية متابعة القراءة، وله مواضع مخصوصة في رواية حفص من طريق الشاطبية.","من مواضعه المشهورة: عوجا ۜ قيما، مرقدنا ۜ هذا، من راق، بل ران. تلقّ الأداء من شيخ."),
            Triple("الإدغام المتماثلين","إذا اتفق الحرفان مخرجًا وصفة وكان الأول ساكنًا والثاني متحركًا فالأصل إدغام الأول في الثاني في مواضع مقررة.","مثال: اضْرِبْ بِعَصَاكَ عند الباءين. توجد تفصيلات واستثناءات تُدرس."),
            Triple("الإدغام المتجانسين والمتقاربين","المتجانسان يتحدان مخرجًا ويختلفان صفة، والمتقاربان تقارب مخرجيهما أو صفتيهما؛ ولكل منهما مواضع وأحكام.","تدريب: ادرس أمثلة قد تبينها كتب التجويد مثل قُل رَّبِّ، مع سماع الأداء المتقن."),
            Triple("آداب التلاوة","من آدابها الإخلاص والتدبر والطهارة عند مس المصحف واحترام القرآن وتحسين الصوت دون تكلف أو إخلال بالحروف.","تطبيق: اقرأ وردًا يسيرًا بتأنٍ، وافهم معنى ما تقرأ، وداوم على المراجعة."),
            Triple("الإظهار المطلق","يظهر صوت النون الساكنة إذا جاء بعدها واو أو ياء في كلمة واحدة في مواضع مخصوصة، ولا تدغم؛ وذلك في كلمات معدودة مشهورة في القرآن.","الأمثلة: الدنيا، بنيان، صنوان، قنوان. راجع مواضعها في المصحف ولا تعمم الحكم على كل نون."),
            Triple("النون والميم المشددتان","إذا جاءت النون أو الميم مشددة وجبت الغنة فيهما بمقدار حركتين في الأداء المعتاد لحفص.","أمثلة: إِنَّ، ثُمَّ. استمع للغنة دون مبالغة أو ضغط في الصوت."),
            Triple("لام التعريف الشمسية والقمرية","تدغم لام التعريف في الحرف الشمسي ويشدد الحرف بعدها، وتظهر اللام قبل الحرف القمري.","أمثلة: الشَّمْس (شمسية)، الْقَمَر (قمرية). لاحظ الشدة في المثال الأول."),
            Triple("مخرج الضاد","تخرج الضاد من إحدى حافتي اللسان أو كلتيهما مع ما يحاذيها من الأضراس العليا، ولها صفة الاستطالة.","المثال: الضَّالِّين في الفاتحة. يحتاج ضبطها إلى سماع وتصحيح مباشر؛ لا تستبدل بها الظاء."),
            Triple("مخرج الطاء والدال والتاء","تخرج هذه الحروف من طرف اللسان مع أصول الثنايا العليا، وتختلف في صفاتها؛ فالطاء مستعلية مطبقة، والدال والتاء مستفلتان.","أمثلة: طاب، دِين، تَاب. قارن بين التفخيم والترقيق مع المحافظة على المخرج."),
            Triple("مخرج الصاد والسين والزاي","تخرج من طرف اللسان مع ما فوق الثنايا السفلى، ويجري الصوت في ممر ضيق؛ وتتميز الصاد بالإطباق والاستعلاء.","أمثلة: صراط، سراط من جهة الحروف المتقاربة، وزكّى. لا تجعل الصاد سينًا."),
            Triple("مخرج القاف والكاف","تخرج القاف من أقصى اللسان مع ما يحاذيه من الحنك الأعلى، والكاف أسفل منها قليلًا إلى جهة الفم.","أمثلة: قُلْ، كُلْ. ميّز موضع القاف الخلفي عن الكاف."),
            Triple("صفة الهمس","الهمس جريان النفس عند النطق بالحرف لضعف الاعتماد على المخرج، وحروفه مجموعة في: فحثه شخص سكت.","تدريب: انطق الفاء والسين والحاء بهدوء، ولا تضف حركة بعد الحرف الساكن."),
            Triple("صفة الشدة","الشدة انحباس جريان الصوت عند النطق بالحرف لقوة الاعتماد على المخرج، وحروفها مجموعة في: أجد قط بكت.","تدريب: قارن بين التاء والسين في موضعهما، ولا تحول الحرف إلى حركة."),
            Triple("صفة التوسط","التوسط صفة بين الشدة والرخاوة، يجري معها بعض الصوت وينحبس بعضه، وحروفه مجموعة في: لن عمر.","تدريب: استمع إلى اللام والنون والميم والراء والعين، ولا تطل الصوت تكلفًا."),
            Triple("الاستعلاء والإطباق","الاستعلاء ارتفاع أقصى اللسان عند النطق بحروفه السبعة، والإطباق انحصار جزء من اللسان مع الحنك الأعلى في حروفه الأربعة.","حروف الاستعلاء: خص ضغط قظ. وحروف الإطباق: ص ض ط ظ. قارن بين الصاد والسين."),
            Triple("الإخفاء الشفوي","يكون عند وقوع الميم الساكنة قبل الباء، فتخفى الميم مع غنة من غير إطباق شديد للشفتين.","المثال: تَرْمِيهِمْ بِحِجَارَةٍ، سورة الفيل: 4. لاحظ الميم الساكنة قبل الباء."),
            Triple("الإدغام الشفوي","إذا جاءت الميم الساكنة قبل ميم متحركة أدغمت الأولى في الثانية مع غنة، ويسمى إدغام المتماثلين الصغير.","تدريب: ابحث في المصحف عن ميم ساكنة بعدها ميم متحركة، واستمع إلى الأداء من قارئ متقن."),
            Triple("الإظهار الشفوي","تظهر الميم الساكنة إذا جاء بعدها حرف غير الباء والميم، مع الانتباه إلى وضوحها قبل الواو والفاء.","مثال: الْحَمْدُ لِلَّهِ، الفاتحة: 2. الميم قبل الدال ظاهرة."),
            Triple("مد العوض","عند الوقف على تنوين الفتح يعوض عنه ألف تمد حركتين، ويستثنى من ذلك ما كان آخره تاء مربوطة.","تدريب: قارن بين الوقف على كلمة منونة بالفتح والوقف على كلمة مختومة بتاء مربوطة."),
            Triple("مد الصلة الصغرى","تلحق هاء الضمير بواو لفظية أو ياء لفظية إذا وقعت بين متحركين ولم يأت بعدها همز، ومقدارها حركتان في الوصل.","مثال: إِنَّهُ كَانَ، مع مراعاة شروط هاء الكناية في رواية حفص."),
            Triple("مد الصلة الكبرى","إذا وقعت همزة بعد هاء الضمير المستوفية لشروط الصلة كان المد صلة كبرى، ومقداره يتبع طريق القراءة.","مثال: مَالَهُ أَخْلَدَهُ، الهمزة: 3. تلقّ مقدار المد من معلم متقن."),
            Triple("مد التمكين","يأتي عند اجتماع ياء مشددة مكسورة بعدها ياء مدية، فيمد حرف المد لتمييز الياءين ومنع الإدغام أو سقوط إحداهما.","تدريب: راجع أمثلة باب مد التمكين في متن التجويد، واستمع إلى الأداء قبل التطبيق."),
            Triple("المد اللازم الحرفي","يكون في الحروف المقطعة في أوائل بعض السور إذا كان هجاء الحرف ثلاثة أحرف أوسطها حرف مد وبعده سكون أصلي، ومقداره ست حركات.","أمثلة: اللام في الم، والسين في طسم. يختلف الحكم باختلاف هجاء الحرف."),
            Triple("أنواع الوقف","يقسم الوقف من جهة المعنى إلى تام وكاف وحسن وقبيح؛ ويختلف الحكم بحسب تعلق الكلام بما بعده واستقامة المعنى.","تدريب: لا تقف على موضع يفسد المعنى، وراجع كتب الوقف والابتداء مع معلم."),
            Triple("الوقف على التاء المربوطة","إذا وقف القارئ على التاء المربوطة نطقها هاء ساكنة، أما عند الوصل فينطقها تاء.","أمثلة: رَحْمَةٌ عند الوقف: رحْمَهْ، وعند الوصل: رحمتُ الله. راجع مواضعها في المصحف.")
        )
        val completedLessons = prefs.getStringSet("tajweed_done", emptySet())?.toMutableSet() ?: mutableSetOf()
        screen("التجويد", "منهج تأسيسي • " + lessons.size + " درسًا في هذه الدفعة")
        root?.addView(label("أتممت مراجعة " + completedLessons.size + " من " + lessons.size + " درسًا في هذه الدفعة.", 14f, accent(), true))
        root?.addView(label("كل درس يتضمن تعريفًا وتطبيقًا وموضعًا أو مثالًا. ضبط الأداء يكون بالتلقي من معلم متقن.", 12f, muted()))
        val search = SearchView(this).apply { queryHint = "ابحث عن حكم أو درس…"; isIconifiedByDefault = false }
        root?.addView(search)
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root?.addView(results)
        fun render(q: String) {
            results.removeAllViews()
            val found = lessons.filter { it.first.contains(q, true) || it.second.contains(q, true) || it.third.contains(q, true) }
            found.forEachIndexed { n, lesson ->
                val isDone = completedLessons.contains(lesson.first)
                results.addView(button((if (isDone) "✓ " else "") + "الدرس " + (n + 1) + ": " + lesson.first, false) {
                    AlertDialog.Builder(this)
                        .setTitle(lesson.first)
                        .setMessage("الشرح والحكم:\n" + lesson.second + "\n\nالمثال والموضع أو التدريب:\n" + lesson.third + "\n\nطريقة المراجعة:\nاقرأ المثال من المصحف، واستمع إلى قارئ متقن، ثم اعرض أداءك على معلم؛ فالنص وحده لا يكفي لتصحيح المخارج والصفات.")
                        .setPositiveButton(if (completedLessons.contains(lesson.first)) "إلغاء علامة المراجعة" else "تمت المراجعة") { _, _ ->
                            if (completedLessons.contains(lesson.first)) completedLessons.remove(lesson.first) else completedLessons.add(lesson.first)
                            prefs.edit().putStringSet("tajweed_done", completedLessons).apply()
                            showTajweed()
                        }
                        .setNegativeButton("إغلاق", null)
                        .show()
                })
                results.addView(gap(7))
            }
            if (found.isEmpty()) results.addView(label("لا توجد نتائج.", 14f, muted()))
        }
        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean { render(query.orEmpty()); search.clearFocus(); return true }
            override fun onQueryTextChange(newText: String?): Boolean { render(newText.orEmpty()); return true }
        })
        render("")
    }

    private fun showFiqh() {
        val topics = listOf(
            "الطهارة" to "مدخل إلى أحكام المياه والنجاسات والوضوء والغسل والتيمم، مع التعرف على شروط الطهارة ومقاصدها.",
            "الوضوء" to "دراسة صفة الوضوء وفرائضه وسننه ونواقضه والمسائل التي يكثر السؤال عنها، مع الرجوع إلى الدليل.",
            "الغسل والتيمم" to "أسباب الغسل وصفته، ومتى يشرع التيمم وصفته وأحكامه العامة.",
            "الصلاة" to "شروط الصلاة وأركانها وواجباتها وسننها، وأحكام الجماعة والجمعة والسهو.",
            "صلاة المسافر والمريض" to "الرخص المتعلقة بالسفر والمرض، مثل القصر والجمع والصلاة بحسب الاستطاعة.",
            "الزكاة" to "شروط وجوب الزكاة والأموال الزكوية والأنصبة والمصارف، وتفاصيل الحساب تحتاج إلى ضبط.",
            "الصيام" to "النية والإمساك والمفطرات والرخص وأحكام القضاء والكفارة في مواضعها.",
            "الحج والعمرة" to "الإحرام والمواقيت والطواف والسعي والوقوف بعرفة والهدي ومحظورات الإحرام.",
            "الأطعمة والذبائح" to "الأصل في الأطعمة، وأحكام الذكاة، وما يحل وما يحرم وفق النصوص الشرعية.",
            "المعاملات المالية" to "البيع والإجارة والديون والربا والغرر والغش والوفاء بالشروط.",
            "الأسرة" to "موضوعات النكاح والحقوق الأسرية والطلاق والعدة والميراث، وهي أبواب تحتاج إلى سؤال أهل العلم.",
            "الأيمان والنذور" to "معاني اليمين والنذر والكفارة وأحكام الوفاء، وتفاصيل كل واقعة تختلف باختلاف ألفاظها وظروفها."
        )
        screen("الفقه", "مداخل تعليمية • " + topics.size + " أبواب في هذه الدفعة")
        root?.addView(label("اختر بابًا لعرض موضوعاته العامة. هذه ليست فتاوى ولا تكفي للحكم على واقعة شخصية.", 12f, muted()))
        val search = SearchView(this).apply {
            queryHint = "ابحث في أبواب الفقه…"
            isIconifiedByDefault = false
        }
        root?.addView(search)
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root?.addView(results)
        fun renderTopics(query: String) {
            results.removeAllViews()
            val found = topics.filter { it.first.contains(query.trim(), true) || it.second.contains(query.trim(), true) }
            found.forEachIndexed { index, topic ->
                results.addView(button((index + 1).toString() + ". " + topic.first, false) {
                    AlertDialog.Builder(this)
                        .setTitle(topic.first)
                        .setMessage(topic.second + "\n\nتنبيه: قد تختلف بعض التفاصيل باختلاف المذهب والدليل والواقعة؛ راجع عالمًا موثوقًا أو مرجعًا فقهيًا معتمدًا.")
                        .setPositiveButton("إغلاق", null)
                        .show()
                })
                results.addView(gap(7))
            }
            if (found.isEmpty()) results.addView(label("لا توجد أبواب تطابق البحث.", 14f, muted()))
        }
        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                renderTopics(query.orEmpty())
                search.clearFocus()
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                renderTopics(newText.orEmpty())
                return true
            }
        })
        renderTopics("")
    }

    private fun showQuiz() {
        screen("المسابقة الإسلامية", "30 سؤالًا مع شرح الإجابات • نقاط افتراضية فقط")
        val qs = listOf(
            Triple("كم عدد سور القرآن؟", listOf("110","112","114","120"), 2),
            Triple("ما أم الكتاب؟", listOf("البقرة","الفاتحة","الإخلاص","الناس"), 1),
            Triple("في أي شهر يصوم المسلمون؟", listOf("شعبان","رمضان","شوال","محرم"), 1),
            Triple("من أول الخلفاء الراشدين؟", listOf("عمر","عثمان","أبو بكر","علي"), 2),
            Triple("ما أطول سورة في القرآن؟", listOf("آل عمران","النساء","البقرة","المائدة"), 2),
            Triple("ما أقصر سورة في القرآن؟", listOf("العصر","الكوثر","الإخلاص","النصر"), 1),
            Triple("ما السورة التي لا تبدأ بالبسملة؟", listOf("التوبة","الأنفال","يونس","هود"), 0),
            Triple("ما آخر سورة في ترتيب المصحف؟", listOf("الفلق","الإخلاص","الناس","الكوثر"), 2),
            Triple("كم عدد أجزاء القرآن؟", listOf("20","25","30","40"), 2),
            Triple("أين نزل الوحي أول مرة على النبي ﷺ؟", listOf("غار ثور","غار حراء","المسجد النبوي","منى"), 1),
            Triple("ما أول ما نزل من القرآن؟", listOf("الفاتحة","آيات من سورة العلق","المدثر كاملة","البقرة"), 1),
            Triple("من الملك الذي نزل بالوحي؟", listOf("ميكائيل","إسرافيل","جبريل","ملك الموت"), 2),
            Triple("إلى أي مدينة هاجر النبي ﷺ؟", listOf("الطائف","القدس","المدينة المنورة","دمشق"), 2),
            Triple("في أي سنة هجرية وقعت غزوة بدر؟", listOf("الأولى","الثانية","الثالثة","الخامسة"), 1),
            Triple("إلى أين يتجه المسلمون في الصلاة؟", listOf("المسجد النبوي","الكعبة","المسجد الأقصى","جبل عرفات"), 1),
            Triple("كم عدد أركان الإسلام؟", listOf("ثلاثة","أربعة","خمسة","ستة"), 2),
            Triple("من أول مؤذن في الإسلام؟", listOf("أبو هريرة","بلال بن رباح","زيد بن ثابت","سلمان الفارسي"), 1),
            Triple("ما اسم والد النبي ﷺ؟", listOf("أبو طالب","عبد الله","عبد المطلب","حمزة"), 1),
            Triple("من أول زوجات النبي ﷺ؟", listOf("عائشة","حفصة","خديجة","أم سلمة"), 2),
            Triple("كم سنة استمر نزول القرآن تقريبًا؟", listOf("10 سنوات","15 سنة","23 سنة","40 سنة"), 2),
            Triple("ما أول سورة في ترتيب المصحف؟", listOf("البقرة","الفاتحة","الناس","الإخلاص"), 1),
            Triple("ما السورة التي وردت فيها البسملة مرتين؟", listOf("النمل","البقرة","مريم","الكهف"), 0),
            Triple("كم عدد أركان الإيمان؟", listOf("خمسة","ستة","سبعة","ثمانية"), 1),
            Triple("ما القبلة الأولى للمسلمين؟", listOf("المسجد الأقصى","الكعبة","المسجد النبوي","عرفات"), 0),
            Triple("من النبي الذي التقمه الحوت؟", listOf("موسى","يونس","نوح","عيسى"), 1),
            Triple("من النبي الذي تكلم في المهد؟", listOf("عيسى","موسى","يحيى","إبراهيم"), 0),
            Triple("في أي شهر تؤدى مناسك الحج؟", listOf("رمضان","محرم","ذو الحجة","شوال"), 2),
            Triple("ما اسم الغزوة التي حفر فيها المسلمون الخندق؟", listOf("بدر","أحد","الأحزاب","تبوك"), 2),
            Triple("من الصحابي الملقب بذي النورين؟", listOf("علي بن أبي طالب","عثمان بن عفان","أبو بكر","طلحة"), 1),
            Triple("ما الكتاب الذي أنزله الله على داود عليه السلام؟", listOf("التوراة","الزبور","الإنجيل","صحف إبراهيم"), 1)
        )
        val explanations = listOf(
            "يضم المصحف 114 سورة، من الفاتحة إلى الناس.",
            "الفاتحة تسمى أم الكتاب، وهي السبع المثاني.",
            "فرض صيام شهر رمضان على المسلمين.",
            "أبو بكر الصديق رضي الله عنه أول الخلفاء الراشدين.",
            "سورة البقرة أطول سور القرآن.",
            "سورة الكوثر أقصر سور القرآن من حيث عدد الآيات.",
            "سورة التوبة لا تبدأ بالبسملة في أولها.",
            "سورة الناس آخر سورة في ترتيب المصحف.",
            "القرآن مقسم إلى ثلاثين جزءًا.",
            "بدأ نزول الوحي على النبي ﷺ في غار حراء.",
            "أول ما نزل من القرآن أوائل سورة العلق.",
            "جبريل عليه السلام هو الملك الذي نزل بالوحي.",
            "هاجر النبي ﷺ من مكة إلى المدينة المنورة.",
            "وقعت غزوة بدر في السنة الثانية للهجرة.",
            "قبلة المسلمين هي الكعبة المشرفة.",
            "أركان الإسلام خمسة كما في حديث بني الإسلام على خمس.",
            "بلال بن رباح رضي الله عنه من أشهر مؤذني النبي ﷺ وأولهم.",
            "والد النبي ﷺ هو عبد الله بن عبد المطلب.",
            "خديجة بنت خويلد رضي الله عنها أول زوجات النبي ﷺ.",
            "استمر نزول القرآن نحو ثلاث وعشرين سنة.",
            "الفاتحة أول سورة في ترتيب المصحف، والناس آخرها.",
            "وردت البسملة في أول سورة النمل، وضمن كتاب سليمان في الآية 30.",
            "أركان الإيمان ستة كما في حديث جبريل المشهور.",
            "كان المسلمون يتجهون أولًا إلى بيت المقدس قبل تحويل القبلة إلى الكعبة.",
            "يونس عليه السلام التقمه الحوت ثم نجاه الله.",
            "ذكر القرآن أن عيسى عليه السلام تكلم في المهد ودعا إلى عبادة الله.",
            "الحج يكون في أشهر الحج، وأعماله الأساسية في شهر ذي الحجة.",
            "غزوة الأحزاب تسمى أيضًا غزوة الخندق.",
            "عثمان بن عفان رضي الله عنه لقب بذي النورين.",
            "أنزل الله الزبور على داود عليه السلام."
        )
        var index = 0
        var score = 0
        val prompt = label("", 20f, accent(), true)
        val status = label("النقاط: 0", 13f, muted())
        val options = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root?.addView(status); root?.addView(prompt); root?.addView(options)
        fun render() {
            options.removeAllViews()
            if (index >= qs.size) {
                val best = maxOf(prefs.getInt("best_quiz_score", 0), score)
                prefs.edit().putInt("best_quiz_score", best).apply()
                prompt.text = "انتهت المسابقة. نتيجتك: " + score + " من " + qs.size + "\nأفضل نتيجة محفوظة: " + best
                options.addView(button("إعادة المسابقة", true) {
                    index = 0
                    score = 0
                    status.text = "النقاط: 0"
                    render()
                })
                return
            }
            val q = qs[index]
            prompt.text = "السؤال " + (index + 1) + ": " + q.first
            q.second.forEachIndexed { i, answer ->
                options.addView(button(answer, false) {
                    val correct = i == q.third
                    if (correct) score++
                    AlertDialog.Builder(this)
                        .setTitle(if (correct) "إجابة صحيحة" else "ليست الإجابة الصحيحة")
                        .setMessage("الإجابة الصحيحة: " + q.second[q.third] + "\n" + (if (correct) "أحسنت، أُضيفت نقطة." else "راجع الإجابة الصحيحة ثم تابع.") + "\nالتوضيح: " + explanations[index])
                        .setPositiveButton("السؤال التالي") { _, _ ->
                            index++
                            status.text = "النقاط: " + score
                            render()
                        }
                        .show()
                })
            }
        }
        render()
    }

    private fun showNames() {
        val names = listOf(
            "الرحمن" to "واسع الرحمة بخلقه.",
            "الرحيم" to "ذو الرحمة بعباده.",
            "الملك" to "المالك المتصرف في خلقه.",
            "القدوس" to "المنزه عن كل نقص.",
            "السلام" to "السالم من كل نقص، ومنه السلامة.",
            "المؤمن" to "الذي يؤمن عباده ويصدق رسله.",
            "المهيمن" to "الرقيب الحافظ على كل شيء.",
            "العزيز" to "الغالب الذي لا يُغلب.",
            "الجبار" to "العالي القاهر، وجابر كسر عباده.",
            "المتكبر" to "المتعالي عن صفات النقص.",
            "الخالق" to "الذي أوجد المخلوقات وقدرها.",
            "البارئ" to "الذي أنشأ الخلق وأوجدهم.",
            "المصور" to "الذي أعطى كل مخلوق صورته.",
            "الغفار" to "كثير المغفرة لعباده.",
            "القهار" to "الغالب القاهر لكل شيء.",
            "الوهاب" to "كثير العطاء والهبات.",
            "الرزاق" to "الذي يرزق خلقه.",
            "الفتاح" to "الذي يفتح أبواب الرحمة ويحكم بين عباده بالحق.",
            "العليم" to "المحيط علمه بكل شيء.",
            "السميع" to "الذي يسمع جميع الأصوات.",
            "البصير" to "الذي يرى جميع الأشياء.",
            "الحكيم" to "ذو الحكمة في خلقه وأمره.",
            "اللطيف" to "البر بعباده، العالم بدقائق الأمور.",
            "الخبير" to "العالم ببواطن الأشياء وخفاياها.",
            "الحليم" to "الذي لا يعجل بالعقوبة مع قدرته.",
            "العظيم" to "المتصف بالعظمة والكمال.",
            "الشكور" to "الذي يثيب على العمل القليل ثوابًا كثيرًا.",
            "العلي" to "المتعالي فوق خلقه، وله علو القدر والقهر.",
            "الكبير" to "العظيم الذي كل شيء دونه.",
            "الحفيظ" to "الحافظ لكل شيء، لا يعزب عنه شيء."
        )
        screen("الأسماء الحسنى", names.size.toString() + " اسمًا ومعنى موجزًا")
        root?.addView(label("ابحث عن الاسم أو معناه. هذه دفعة تعليمية من الأسماء وليست ادعاءً بحصر الأسماء التسعة والتسعين.", 12f, muted()))
        val search = SearchView(this).apply {
            queryHint = "ابحث في الأسماء الحسنى…"
            isIconifiedByDefault = false
        }
        root?.addView(search)
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root?.addView(results)
        fun renderNames(query: String) {
            results.removeAllViews()
            val found = names.filter { it.first.contains(query.trim(), true) || it.second.contains(query.trim(), true) }
            found.forEachIndexed { index, item ->
                results.addView(button((index + 1).toString() + ". " + item.first, false) {
                    AlertDialog.Builder(this)
                        .setTitle(item.first)
                        .setMessage(item.second)
                        .setPositiveButton("إغلاق", null)
                        .show()
                })
                results.addView(gap(7))
            }
            if (found.isEmpty()) results.addView(label("لا توجد أسماء تطابق البحث.", 14f, muted()))
        }
        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                renderNames(query.orEmpty())
                search.clearFocus()
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                renderNames(newText.orEmpty())
                return true
            }
        })
        renderNames("")
    }

    private fun showNotes() {
        screen("ملاحظاتي", "احفظ فوائدك على جهازك")
        val input = EditText(this).apply {
            minLines = 7; gravity = Gravity.TOP or Gravity.RIGHT
            setText(prefs.getString("notes", "")); hint = "اكتب ملاحظاتك…"
            setTextColor(ink()); background = shape(panel()); setPadding(12, 12, 12, 12)
        }
        root?.addView(input)
        root?.addView(button("حفظ", true) {
            prefs.edit().putString("notes", input.text.toString()).apply()
            Toast.makeText(this, "تم حفظ الملاحظات.", Toast.LENGTH_SHORT).show()
        })
    }

    private fun showMore() {
        screen("المزيد", "أقسام المنصة")
        card("السيرة النبوية", "9 محطات مع مراجع", "📜") { showSirah() }
        card("الأحاديث النبوية", "28 حديثًا مع الشرح والمصدر", "📖") { showHadith() }
        card("قصص الأنبياء", "24 قصة مع مراجع", "✧") { showStories(false) }
        card("قصص الصحابة", "25 سيرة مع مراجع", "☼") { showStories(true) }
        card("الأذكار", "12 ذكرًا مع مصادر وعدّادات محفوظة", "◉") { showAdhkar() }
        card("التسبيح", "عداد مستقل محفوظ", "◉") { showTasbeeh() }
        card("التجويد", "60 درسًا قابلًا للبحث", "۞") { showTajweed() }
        card("الفقه", "12 بابًا تعليميًا", "⚖") { showFiqh() }
        card("الأسماء الحسنى", "30 اسمًا ومعنى", "✧") { showNames() }
        card("المسابقة", "30 سؤالًا مع شرح الإجابة", "★") { showQuiz() }
        root?.addView(button(if (light) "الوضع الفاتح" else "الوضع الداكن", true) {
            light = !light; prefs.edit().putBoolean("light", light).apply(); showMore()
        })
    }
}
