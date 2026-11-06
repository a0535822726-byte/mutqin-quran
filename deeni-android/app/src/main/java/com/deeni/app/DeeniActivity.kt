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
        tiles("الأذكار", "أذكار مختارة", { showAdhkar() }, "التسبيح", "عداد محفوظ", { showTasbeeh() })
        heading("المكتبة الإسلامية")
        tiles("السيرة", "محطات نبوية", { showSirah() }, "الحديث", "أحاديث مختارة", { showHadith() })
        tiles("الأنبياء", "قصص قرآنية", { showStories(false) }, "الصحابة", "سير مختارة", { showStories(true) })
        tiles("التجويد", "أحكام التلاوة", { showTajweed() }, "الفقه", "مبادئ أساسية", { showFiqh() })
        heading("أدوات أخرى")
        tiles("المسابقة", "أسئلة إسلامية", { showQuiz() }, "الأسماء الحسنى", "معانٍ موجزة", { showNames() })
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
                            results.addView(label("سورة " + row.surahName + " • الآية " + row.ayahNumber, 13f, accent(), true))
                            results.addView(label(row.text, 18f))
                        } else results.addView(label("تعذر البحث الآن؛ تحقق من الاتصال.", 14f, accent()))
                    }
                }.start()
            }
        })
        root?.addView(results)
    }

    private fun showSaved() {
        screen("المحفوظات", "الآيات المحفوظة على هذا الجهاز")
        val saved = prefs.getStringSet("saved_ayahs", emptySet())?.toList().orEmpty().sorted()
        if (saved.isEmpty()) root?.addView(label("لا توجد آيات محفوظة بعد.", 15f))
        saved.forEach { item -> card("الآية " + item, "اضغط لفتح موضعها", "☆") {
            val parts = item.split(":")
            val sn = parts.getOrNull(0)?.toIntOrNull()
            val an = parts.getOrNull(1)?.toIntOrNull()
            if (sn != null && an != null) Thread {
                val r = QuranPageSource.loadAyahDetails(sn, an)
                runOnUiThread { if (r.isSuccess) loadPage(r.getOrThrow().second) }
            }.start()
        } }
    }

    private fun showMemorization() {
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
        screen("التقدم", "ملخص القراءة والحفظ")
        val saved = prefs.getStringSet("saved_ayahs", emptySet())?.size ?: 0
        val tasks = (0..3).count { prefs.getBoolean("task_" + it, false) }
        val tajweedDone = prefs.getStringSet("tajweed_done", emptySet())?.size ?: 0
        val bestQuiz = prefs.getInt("best_quiz_score", 0)
        val dhikrTotal = (0..7).sumOf { prefs.getInt("dhikr_" + it, 0) }
        card("الآيات المحفوظة", saved.toString() + " آية", "☆") { showSaved() }
        card("مهام اليوم", tasks.toString() + " من 4", "✓") { showMemorization() }
        card("دروس التجويد التي راجعتها", tajweedDone.toString() + " درسًا", "۞") { showTajweed() }
        card("مجموع الأذكار المسجلة", dhikrTotal.toString() + " تكرار", "◉") { showAdhkar() }
        card("أفضل نتيجة في المسابقة", bestQuiz.toString() + " من 20", "★") { showQuiz() }
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
        screen("الأذكار", "أذكار مختارة مع عداد تكرار")
        val items = listOf(
            "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
            "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ",
            "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ",
            "سُبْحَانَ اللَّهِ وَالْحَمْدُ لِلَّهِ وَلَا إِلَهَ إِلَّا اللَّهُ وَاللَّهُ أَكْبَرُ",
            "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ",
            "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
            "رَبِّ اغْفِرْ لِي وَارْحَمْنِي",
            "اللَّهُمَّ أَعِنِّي عَلَى ذِكْرِكَ وَشُكْرِكَ وَحُسْنِ عِبَادَتِكَ"
        )
        items.forEachIndexed { i, dhikr ->
            var count = prefs.getInt("dhikr_" + i, 0)
            val counter = label("التكرار: " + count, 14f, ink(), true)
            root?.addView(label(dhikr, 20f, accent(), true))
            root?.addView(counter)
            root?.addView(button("تسبيحة +", true) {
                count++; prefs.edit().putInt("dhikr_" + i, count).apply()
                counter.text = "التكرار: " + count
            })
            root?.addView(button("تصفير هذا العداد", false) {
                count = 0
                prefs.edit().putInt("dhikr_" + i, 0).apply()
                counter.text = "التكرار: 0"
            })
            root?.addView(gap(8))
        }
        root?.addView(label("هذه أذكار مختارة وليست قائمة شاملة لأذكار الصباح والمساء؛ يلزم استكمالها من مصادر معتمدة.", 11f, muted()))
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
        screen("السيرة النبوية", "محطات موجزة للمراجعة")
        listOf(
            "المولد والنشأة" to "نشأ النبي ﷺ في مكة، وتوفي والده قبل مولده، ثم كفله جده وبعده عمه.",
            "نزول الوحي" to "نزل الوحي عليه في غار حراء، وكانت بداية الرسالة بآيات من سورة العلق.",
            "الدعوة في مكة" to "دعا إلى توحيد الله وصبر المؤمنون على الأذى.",
            "الهجرة" to "هاجر النبي ﷺ وأصحابه إلى المدينة وأسس مجتمعًا مسلمًا.",
            "بدر وأحد والخندق" to "محطات من تاريخ المجتمع المسلم في المدينة.",
            "صلح الحديبية" to "كان في السنة السادسة للهجرة.",
            "فتح مكة" to "كان في السنة الثامنة للهجرة.",
            "حجة الوداع" to "كانت في السنة العاشرة للهجرة.",
            "وفاته ﷺ" to "توفي في المدينة في السنة الحادية عشرة للهجرة."
        ).forEachIndexed { i, item -> card((i + 1).toString() + ". " + item.first, item.second, "☾") {} }
        root?.addView(label("هذه خلاصة تمهيدية وليست كتاب سيرة كاملًا؛ تُراجع الروايات في كتب السيرة المحققة.", 11f, muted()))
    }

    private fun showHadith() {
        val items = listOf(
            "إنما الأعمال بالنيات، وإنما لكل امرئ ما نوى." to "صحيح البخاري وصحيح مسلم.",
            "الدين النصيحة." to "صحيح مسلم.",
            "من كان يؤمن بالله واليوم الآخر فليقل خيرًا أو ليصمت." to "صحيح البخاري وصحيح مسلم.",
            "لا يؤمن أحدكم حتى يحب لأخيه ما يحب لنفسه." to "صحيح البخاري وصحيح مسلم.",
            "الكلمة الطيبة صدقة." to "صحيح البخاري وصحيح مسلم.",
            "يسروا ولا تعسروا، وبشروا ولا تنفروا." to "صحيح البخاري وصحيح مسلم.",
            "المسلم من سلم المسلمون من لسانه ويده." to "صحيح البخاري وصحيح مسلم.",
            "من لا يَرحم لا يُرحم." to "صحيح البخاري وصحيح مسلم.",
            "الطهور شطر الإيمان." to "صحيح مسلم.",
            "أحب الأعمال إلى الله أدومها وإن قل." to "صحيح البخاري وصحيح مسلم.",
            "من سلك طريقًا يلتمس فيه علمًا سهل الله له به طريقًا إلى الجنة." to "صحيح مسلم.",
            "لا تغضب." to "صحيح البخاري.",
            "خيركم من تعلم القرآن وعلمه." to "صحيح البخاري.",
            "من غشنا فليس منا." to "صحيح مسلم.",
            "الرفق لا يكون في شيء إلا زانه، ولا ينزع من شيء إلا شانه." to "صحيح مسلم.",
            "إن الله كتب الإحسان على كل شيء." to "صحيح مسلم.",
            "المسلم أخو المسلم، لا يظلمه ولا يخذله ولا يحقره." to "صحيح مسلم.",
            "لا يدخل الجنة من كان في قلبه مثقال ذرة من كبر." to "صحيح مسلم."
        )
        screen("الحديث النبوي", "مختارات صحيحة • " + items.size + " أحاديث في هذه الدفعة")
        root?.addView(label("ابحث في نص الحديث أو مصدره. هذه مختارات وليست موسوعة، ولم نضف أرقام تخريج غير متحقق منها.", 12f, muted()))
        val search = SearchView(this).apply {
            queryHint = "ابحث في الأحاديث…"
            isIconifiedByDefault = false
        }
        root?.addView(search)
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root?.addView(results)
        fun renderHadith(query: String) {
            results.removeAllViews()
            val found = items.filter { it.first.contains(query.trim(), true) || it.second.contains(query.trim(), true) }
            found.forEachIndexed { index, item ->
                results.addView(cardView("حديث " + (index + 1), "«" + item.first + "»\nالمصدر: " + item.second))
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
            Triple("زيد بن حارثة رضي الله عنه", "كان مولى رسول الله ﷺ ثم تبناه قبل تحريم التبني، وكان محبوبًا عند النبي ﷺ. قاد جيش مؤتة واستشهد فيها، وذُكر اسمه صريحًا في القرآن في سورة الأحزاب.", "الأحزاب: 37؛ صحيح البخاري، المغازي؛ سيرة ابن هشام؛ الإصابة في تمييز الصحابة.")
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
            Triple("عيسى عليه السلام", "خلق الله عيسى من غير أب، وجعله آية للناس، وأيده بالبينات، ودعا بني إسرائيل إلى عبادة الله وحده. وهو عبد الله ورسوله وكلمته ألقاها إلى مريم، ولم يُقتل ولم يُصلب بل رفعه الله إليه.", "آل عمران: 45–59؛ النساء: 157–158؛ مريم: 16–36؛ المائدة: 110–120.")
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
            Triple("الاستعلاء والإطباق","الاستعلاء ارتفاع أقصى اللسان عند النطق بحروفه السبعة، والإطباق انحصار جزء من اللسان مع الحنك الأعلى في حروفه الأربعة.","حروف الاستعلاء: خص ضغط قظ. وحروف الإطباق: ص ض ط ظ. قارن بين الصاد والسين.")
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
        screen("المسابقة الإسلامية", "أسئلة تعليمية • نقاط افتراضية فقط")
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
            Triple("كم سنة استمر نزول القرآن تقريبًا؟", listOf("10 سنوات","15 سنة","23 سنة","40 سنة"), 2)
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
                        .setMessage("الإجابة الصحيحة: " + q.second[q.third] + "\n" + if (correct) "أحسنت، أُضيفت نقطة." else "راجع الإجابة الصحيحة ثم تابع.")
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
        screen("الأسماء الحسنى", "أسماء ومعانٍ موجزة")
        listOf(
            "الرحمن" to "واسع الرحمة بخلقه.",
            "الرحيم" to "ذو الرحمة بعباده.",
            "الملك" to "المالك المتصرف في خلقه.",
            "القدوس" to "المنزه عن كل نقص.",
            "السلام" to "السالم من كل نقص.",
            "العزيز" to "الغالب الذي لا يُغلب.",
            "الغفور" to "كثير المغفرة لعباده.",
            "الرزاق" to "الذي يرزق خلقه.",
            "العليم" to "المحيط علمه بكل شيء.",
            "الحكيم" to "ذو الحكمة في خلقه وأمره."
        ).forEachIndexed { i, item -> card((i + 1).toString() + ". " + item.first, item.second, "✧") {} }
        root?.addView(label("هذه عشرة أسماء كبداية وليست القائمة الكاملة.", 11f, muted()))
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
        card("السيرة والحديث", "المكتبة الإسلامية", "📜") { showSirah() }
        card("قصص الأنبياء", "قصص قرآنية", "✧") { showStories(false) }
        card("قصص الصحابة", "سير مختارة", "☼") { showStories(true) }
        card("الأذكار والتسبيح", "الأذكار والعداد", "◉") { showAdhkar() }
        card("التجويد والفقه", "التعلم الأساسي", "۞") { showTajweed() }
        card("المسابقة", "أسئلة إسلامية", "★") { showQuiz() }
        root?.addView(button(if (light) "الوضع الفاتح" else "الوضع الداكن", true) {
            light = !light; prefs.edit().putBoolean("light", light).apply(); showMore()
        })
    }
}
