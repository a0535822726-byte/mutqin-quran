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
    private val dark = Color.rgb(8, 31, 27)
    private val green = Color.rgb(17, 51, 43)
    private val green2 = Color.rgb(27, 69, 57)
    private val gold = Color.rgb(218, 184, 112)
    private val paper = Color.rgb(250, 246, 233)
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
        screen("المصحف", "نص القرآن الكامل مقسّم إلى صفحات المصحف الـ604")
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        bar.addView(button("السابق", false) { loadPage(page - 1) }, LinearLayout.LayoutParams(0, -2, 1f))
        bar.addView(button("صفحة " + page + " / 604", true) { askPage() }, LinearLayout.LayoutParams(0, -2, 1.3f))
        bar.addView(button("التالي", false) { loadPage(page + 1) }, LinearLayout.LayoutParams(0, -2, 1f))
        root?.addView(bar)
        root?.addView(gap(7))
        root?.addView(button("اختيار السورة", false) { chooseSurah() })
        val status = label("جارٍ تحميل الصفحة كاملة…", 12f, muted())
        root?.addView(status)
        val mushaf = TextView(this).apply {
            text = " "; textSize = 23f; setTextColor(Color.rgb(47,55,44))
            gravity = Gravity.CENTER; textDirection = View.TEXT_DIRECTION_RTL
            typeface = Typeface.create("serif", Typeface.NORMAL)
            setLineSpacing(12f, 1.12f); background = shape(paper, 14f)
            setPadding(18, 22, 18, 22); setTextIsSelectable(true)
        }
        root?.addView(mushaf)
        root?.addView(gap(8))
        root?.addView(button("حفظ موضع القراءة", false) {
            prefs.edit().putInt("page", page).apply()
            Toast.makeText(this, "تم حفظ موضع القراءة", Toast.LENGTH_SHORT).show()
        })
        root?.addView(button("حفظ آية", false) { saveAyahDialog() })
        root?.addView(button("ابدأ التسميع", true) { startMic(mushaf) })
        root?.addView(button("اختر قارئًا واستمع", false) { chooseReciter() })
        root?.addView(button("التفسير", false) { showTafsir() })
        root?.addView(label("لا يعرض التطبيق نصًا ناقصًا عند فشل التحميل؛ أعد المحاولة بعد التحقق من الإنترنت.", 11f, muted()))
        loadPageText(page, mushaf, status)
        if (startMic) root?.post { startMic(mushaf) }
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

    private fun startMic(output: TextView) {
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
            override fun onReadyForSpeech(params: Bundle?) { output.text = "🎙 أستمع إليك… اقرأ بوضوح." }
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
        screen(if (companions) "قصص الصحابة" else "قصص الأنبياء", "نبذات مختصرة مع الإحالة للمصادر")
        val items = if (companions) listOf(
            "أبو بكر الصديق رضي الله عنه" to "صاحب النبي ﷺ في الهجرة وأول الخلفاء الراشدين.",
            "عمر بن الخطاب رضي الله عنه" to "ثاني الخلفاء الراشدين وعُرف بالعدل.",
            "عثمان بن عفان رضي الله عنه" to "ثالث الخلفاء الراشدين ومن أعمال عهده جمع الناس على المصحف الإمام.",
            "علي بن أبي طالب رضي الله عنه" to "ابن عم رسول الله ﷺ ورابع الخلفاء الراشدين.",
            "خديجة رضي الله عنها" to "زوج النبي ﷺ الأولى ومن أوائل من آمن به.",
            "بلال بن رباح رضي الله عنه" to "من السابقين إلى الإسلام واشتهر بالأذان."
        ) else listOf(
            "آدم عليه السلام" to "ذكر القرآن خلقه وتعليمه وتوبته في سورتي البقرة وطه.",
            "نوح عليه السلام" to "دعا قومه إلى عبادة الله وصبر على دعوتهم.",
            "إبراهيم عليه السلام" to "دعا إلى التوحيد وابتلي فصبر.",
            "يوسف عليه السلام" to "قصته مفصلة في سورة يوسف.",
            "موسى عليه السلام" to "أرسله الله إلى فرعون وأيده بالآيات.",
            "يونس عليه السلام" to "ذكر القرآن دعاءه ونجاة الله له."
        )
        items.forEachIndexed { i, item -> card((i + 1).toString() + ". " + item.first, item.second, "✧") {} }
        root?.addView(label("هذه نبذات وليست قصصًا أو تراجم كاملة؛ تُراجع التفاصيل في مصادرها.", 11f, muted()))
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
            Triple("ما أطول سورة؟", listOf("آل عمران","النساء","البقرة","المائدة"), 2)
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
