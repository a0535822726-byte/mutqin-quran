package com.deeni.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
class DeeniActivity : Activity() {
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

    override fun onStop() {
        stopMic()
        stopAudio()
        super.onStop()
    }

    override fun onBackPressed() {
        if (currentScreen != "الرئيسية") showHome() else super.onBackPressed()
    }

    private fun bg() = if (light) Color.rgb(246,243,233) else dark
    private fun panel() = if (light) Color.WHITE else green
    private fun panel2() = if (light) Color.rgb(235,229,211) else green2
    private fun ink() = if (light) Color.rgb(37,55,46) else Color.rgb(247,243,228)
    private fun muted() = if (light) Color.rgb(102,116,103) else Color.rgb(190,207,195)
    private fun accent() = if (light) Color.rgb(128,91,35) else gold
    private fun shape(c: Int, r: Float = 20f) = GradientDrawable().apply { setColor(c); cornerRadius = r }

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
                background = shape(panel()); setPadding(7, 14, 7, 14)
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
            orientation = LinearLayout.VERTICAL; background = shape(panel2(), 24f)
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
        card("الآيات المحفوظة", saved.toString() + " آية", "☆") { showSaved() }
        card("مهام اليوم", tasks.toString() + " من 4", "✓") { showMemorization() }
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
        screen("القبلة", "اتجاه تقريبي من أبو عريش إلى الكعبة")
        val bearing = QiblaMath.bearingDegrees(16.9689, 42.8325)
        root?.addView(label("اتجاه القبلة: " + bearing.toInt() + "° من الشمال الحقيقي تقريبًا.", 21f, accent(), true))
        root?.addView(label("هذه نتيجة حساب جغرافي وليست قراءة بوصلة حية؛ قد تتأثر بوصلة الهاتف بالمجال المغناطيسي.", 13f, muted()))
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
        screen("الحديث النبوي", "أحاديث مختارة مع اسم المصدر")
        listOf(
            "إنما الأعمال بالنيات، وإنما لكل امرئ ما نوى." to "صحيح البخاري وصحيح مسلم.",
            "الدين النصيحة." to "صحيح مسلم.",
            "من كان يؤمن بالله واليوم الآخر فليقل خيرًا أو ليصمت." to "صحيح البخاري وصحيح مسلم.",
            "لا يؤمن أحدكم حتى يحب لأخيه ما يحب لنفسه." to "صحيح البخاري وصحيح مسلم.",
            "الكلمة الطيبة صدقة." to "صحيح البخاري وصحيح مسلم.",
            "يسروا ولا تعسروا، وبشروا ولا تنفروا." to "صحيح البخاري وصحيح مسلم."
        ).forEachIndexed { i, item -> card("حديث " + (i + 1), "«" + item.first + "»\nالمصدر: " + item.second, "📜") {} }
        root?.addView(label("هذه مختارات وليست موسوعة حديثية؛ عند النشر أو الشرح يُراجع لفظ الحديث في مصدره.", 11f, muted()))
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
            Triple("أسماء بنت أبي بكر رضي الله عنها", "لُقبت بذات النطاقين؛ إذ شقّت نطاقها لتجعل منه زادًا للنبي ﷺ وأبيها أثناء الهجرة. كانت ثابتة شجاعة، وعاشت عمرًا طويلًا، وروت أحاديث عن النبي ﷺ.", "صحيح البخاري، مناقب الأنصار والهجرة؛ الإصابة في تمييز الصحابة؛ سير أعلام النبلاء.")
        ) else listOf(
            Triple("آدم عليه السلام", "خلق الله آدم وعلّمه الأسماء، وأمر الملائكة بالسجود له سجود تكريم، فسجدوا إلا إبليس أبى واستكبر. أسكنه الله الجنة مع زوجه، ثم تاب عليه بعد أن تلقّى كلمات من ربه. وفي قصته بيان فضل العلم وخطر الكبر وفتح باب التوبة.", "البقرة: 30–39؛ طه: 115–123؛ الأعراف: 11–27."),
            Triple("نوح عليه السلام", "لبث نوح في قومه يدعوهم إلى عبادة الله وحده زمنًا طويلًا، ليلًا ونهارًا وسرًا وجهارًا. كذّبه أكثر قومه، فأمره الله بصنع السفينة، فنجّاه ومن آمن معه، وأغرق المكذبين. وتعلّم قصته الصبر والثبات وعدم اليأس من الدعوة.", "سورة نوح كاملة؛ هود: 25–49؛ المؤمنون: 23–30؛ العنكبوت: 14."),
            Triple("إبراهيم عليه السلام", "نظر إبراهيم في ملكوت السماوات والأرض، وأنكر عبادة الأصنام والكواكب، ودعا أباه وقومه إلى التوحيد. حطّم الأصنام ليبيّن عجزها، فألقاه قومه في النار فجعلها الله بردًا وسلامًا. وابتلاه الله بأوامر عظيمة فاستجاب، ورفع قواعد البيت مع إسماعيل.", "الأنبياء: 51–73؛ الصافات: 83–113؛ البقرة: 124–129؛ الأنعام: 74–79."),
            Triple("يوسف عليه السلام", "بدأت قصته برؤيا رآها صغيرًا، ثم حسده إخوته وألقوه في الجب. بيع في مصر، وابتُلي بمراودة امرأة العزيز والسجن، فصبر واستعان بالله. ثم أكرمه الله بالتمكين وجمع شمله بأهله، وعفا عن إخوته عند قدرته عليهم.", "سورة يوسف كاملة، وهي المرجع القرآني المفصل للقصة."),
            Triple("موسى عليه السلام", "وُلد موسى في زمن كان فرعون يقتل أبناء بني إسرائيل، فأوحى الله إلى أمه أن تلقيه في اليم مع وعد الحفظ. نشأ في بيت فرعون، ثم أرسله الله إليه بالآيات، ودعا بني إسرائيل إلى النجاة. شق الله له البحر فنجّاه ومن معه وأغرق فرعون وجنده.", "القصص: 3–46؛ طه: 9–79؛ الشعراء: 10–68؛ يونس: 90–92."),
            Triple("يونس عليه السلام", "دعا يونس قومه ثم خرج مغاضبًا قبل أن يؤذن له، فكان في بطن الحوت، فنادى ربه معترفًا بتقصيره ومسبحًا له. فاستجاب الله له ونجّاه، وأعاد قومه إلى الإيمان فانتفعوا بذلك. وفي قصته فضل الدعاء والتسبيح والرجوع إلى الله.", "الأنبياء: 87–88؛ الصافات: 139–148؛ القلم: 48–50."),
            Triple("أيوب عليه السلام", "ابتُلي أيوب عليه السلام بضرّ، فصبر ولم ييأس من رحمة الله، ودعا ربه بأدب وتضرع: «أَنِّي مَسَّنِيَ الضُّرُّ وَأَنْتَ أَرْحَمُ الرَّاحِمِينَ». فاستجاب الله له وكشف ما به، وردّ عليه أهله ومثلهم معهم رحمة منه.", "الأنبياء: 83–84؛ ص: 41–44."),
            Triple("داود عليه السلام", "آتاه الله الملك والحكمة، وقتل جالوت بإذن الله، وأنزل عليه الزبور. وعلّمه الله صناعة الدروع، وسخّر معه الجبال والطير يسبحن. وكان نبيًا ملكًا يحكم بالحق، وفي قصته تذكير بالعدل وشكر النعمة.", "البقرة: 251؛ النساء: 163؛ الأنبياء: 78–80؛ سبأ: 10–11؛ ص: 17–26.")
        )
        screen(title, "قصص موثقة بالمراجع • " + items.size + " قصص في هذه المجموعة")
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
                results.addView(button((i + 1).toString() + ". " + item.first + "\\n" + item.second.take(135) + "…", false) {
                    AlertDialog.Builder(this)
                        .setTitle(item.first)
                        .setMessage(item.second + "\\n\\nالمراجع: " + item.third)
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
        screen("التجويد", "دروس تأسيسية • لا يوجد تصحيح آلي للمخارج")
        listOf(
            "معنى التجويد" to "إعطاء الحروف حقها ومستحقها عند التلاوة.",
            "النون الساكنة والتنوين" to "الإظهار والإدغام والإقلاب والإخفاء.",
            "الميم الساكنة" to "الإخفاء الشفوي والإدغام الشفوي والإظهار الشفوي.",
            "المدود" to "المد الطبيعي والمتصل والمنفصل واللازم وغيرها.",
            "القلقلة" to "صفة حروف قطب جد عند سكونها.",
            "مخارج الحروف" to "تُدرس بالمشافهة والتلقي من معلم متقن."
        ).forEachIndexed { i, item -> card("الدرس " + (i + 1) + ": " + item.first, item.second, "۞") {} }
        root?.addView(label("هذه مقدمة وليست منهجًا كاملًا؛ تصحيح التلاوة يحتاج معلمًا متقنًا.", 11f, muted()))
    }

    private fun showFiqh() {
        screen("الفقه", "معلومات تمهيدية وليست فتاوى")
        listOf(
            "الطهارة" to "أحكام الوضوء والغسل والتيمم وإزالة النجاسة.",
            "الصلاة" to "شروط الصلاة وأركانها وواجباتها وسننها.",
            "الصيام" to "أحكام النية والإمساك والمفطرات والرخص.",
            "الزكاة" to "شروط الوجوب والأنصبة ومصارف الزكاة.",
            "المعاملات" to "التراضي والوضوح والعدل واجتناب الربا والغش."
        ).forEach { card(it.first, it.second, "⚖") {} }
        root?.addView(label("المسائل التفصيلية والفتاوى تُسأل فيها جهة علمية موثوقة.", 11f, muted()))
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
            if (index >= qs.size) { prompt.text = "انتهت المسابقة. نتيجتك: " + score + " من " + qs.size; return }
            val q = qs[index]
            prompt.text = "السؤال " + (index + 1) + ": " + q.first
            q.second.forEachIndexed { i, answer ->
                options.addView(button(answer, false) {
                    if (i == q.third) score++
                    index++; status.text = "النقاط: " + score; render()
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
