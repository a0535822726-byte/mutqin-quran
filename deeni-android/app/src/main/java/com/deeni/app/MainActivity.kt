package com.deeni.app

import android.Manifest
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.Locale

class MainActivity : Activity() {
    private val bg = Color.rgb(9, 28, 25)
    private val panel = Color.rgb(18, 48, 42)
    private val panel2 = Color.rgb(25, 61, 53)
    private val gold = Color.rgb(218, 183, 111)
    private val cream = Color.rgb(246, 241, 226)
    private val muted = Color.rgb(174, 194, 184)
    private lateinit var root: LinearLayout
    private var recognizer: SpeechRecognizer? = null
    private var currentPage = "home"
    private var qiblaSensorManager: SensorManager? = null
    private var qiblaSensorListener: SensorEventListener? = null
    private var selectedReader = "ماهر المعيقلي"
    private var tasbeeh = 0
    private val readers = listOf("ماهر المعيقلي", "عبد الرحمن السديس", "ياسر الدوسري", "مشاري العفاسي", "محمود خليل الحصري", "محمد صديق المنشاوي", "عبد الباسط عبد الصمد", "سعد الغامدي", "أحمد العجمي", "علي جابر")
    private val fatiha = listOf(
        "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
        "الرَّحْمَٰنِ الرَّحِيمِ",
        "مَالِكِ يَوْمِ الدِّينِ",
        "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ",
        "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ",
        "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ"
    )
    private val expectedWords = fatiha.joinToString(" ").split(Regex("\\s+"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        tasbeeh = getSharedPreferences("deeni", MODE_PRIVATE).getInt("tasbeeh", 0)
        selectedReader = getSharedPreferences("deeni", MODE_PRIVATE).getString("reader", readers[0]) ?: readers[0]
        showHome()
    }

    private fun shape(color: Int, radius: Float = 24f): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = radius }

    private fun label(value: String, size: Float = 16f, color: Int = cream, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value; textSize = size; setTextColor(color)
            gravity = Gravity.RIGHT
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(0, 6, 0, 6)
            textDirection = View.TEXT_DIRECTION_RTL
        }

    private fun button(value: String, primary: Boolean = false, action: () -> Unit): TextView =
        TextView(this).apply {
            text = value; textSize = 16f; gravity = Gravity.CENTER
            setTextColor(if (primary) bg else cream)
            background = shape(if (primary) gold else panel2, 18f)
            setPadding(18, 15, 18, 15)
            setOnClickListener { action() }
        }

    private fun base(title: String, subtitle: String) {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 22)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(root) }
        root.addView(label("دِينِي  •  رفيق رحلتك", 14f, gold, true))
        root.addView(label(title, 29f, cream, true))
        root.addView(label(subtitle, 14f, muted))
        root.addView(space(12))
        setContentView(scroll)
    }

    private fun space(h: Int): View = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, h) }

    private fun card(title: String, desc: String, icon: String, action: () -> Unit) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(panel, 22f)
            setPadding(18, 13, 18, 13)
            addView(label(icon, 21f, gold, true))
            addView(label(title, 19f, cream, true))
            addView(label(desc, 13f, muted))
            setOnClickListener { action() }
        }
        val p = LinearLayout.LayoutParams(-1, -2); p.bottomMargin = 12
        root.addView(box, p)
    }

    private fun showHome() {
        currentPage = "home"
        base("السلام عليك", "مساحة هادئة للقرآن والتعلم والذكر")
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; background = shape(panel2, 26f); setPadding(20, 18, 20, 18)
            addView(label("وَرَتِّلِ الْقُرْآنَ تَرْتِيلًا", 25f, gold, true))
            addView(label("ابدأ من حيث وصلت، وتقدّم بخطوات ثابتة.", 14f, cream))
            addView(button("افتح المصحف", true) { showQuran() })
        }
        root.addView(hero)
        root.addView(space(18))
        root.addView(label("اكتشف ديني", 20f, cream, true))
        card("التسميع المباشر", "اقرأ وتابع الكلمات أثناء التسميع", "🎙") { showQuran(true) }
        card("البحث في القرآن", "ابحث عن كلمة أو عبارة في الآيات", "⌕") { showQuranSearch() }
        card("مكتبة القراء", "اختر قارئك المفضل", "♫") { chooseReader() }
        card("الأذكار والتسبيح", "ذكر يومي وعداد محفوظ", "✧") { showTasbeeh() }
        card("الحفظ والمراجعة", "خطة يومية وسجل إنجاز محفوظ", "۞") { showRevision() }
        card("السيرة والحديث", "محطات السيرة وأحاديث مع مراجعها", "☾") { showIslamicLibrary() }
        card("ملاحظاتي", "احفظ فوائدك وملاحظاتك محليًا", "✎") { showNotes() }
        card("الأذكار", "أذكار مختارة مع عداد لكل ذكر", "۞") { showAdhkar() }
        card("المسابقات الإسلامية", "اختبر معلوماتك في القرآن والسيرة والحديث", "🏆") { showQuiz() }
        card("أسماء الله الحسنى", "معانٍ مختصرة لأسماء الله الحسنى", "✧") { showNames() }
        card("مواقيت الصلاة", "اعرض مواقيت الصلاة حسب مدينتك", "◷") { showPrayerTimes() }
        card("اتجاه القبلة", "بوصلة تتبع اتجاه القبلة بحساس الهاتف", "🧭") { showQibla() }
        root.addView(label("المصحف يعرض نص الصفحات عند الاتصال بالإنترنت. بعض الأدوات المتقدمة مثل التسميع المتخصص ومزامنة القراء ما زالت تحتاج محركًا واختبارات إضافية.", 12f, muted))
    }

    private var mushafPage = 1
    private var pageTextView: TextView? = null
    private var pageStatus: TextView? = null

    private fun showQuran(startMic: Boolean = false) {
        currentPage = "quran"
        mushafPage = getSharedPreferences("deeni", MODE_PRIVATE).getInt("mushaf_page", 1).coerceIn(1, 604)
        base("المصحف الشريف", "تصفح صفحات القرآن الكريم • النص العثماني")
        root.addView(label("القارئ المحدد: $selectedReader", 14f, gold, true))
        root.addView(button("تغيير القارئ", false) { chooseReader() })
        root.addView(label("مصدر النص: واجهة القرآن الكريم الرقمية. يلزم اتصال بالإنترنت لتحميل الصفحات. هذا عرض نصي للصفحة وليس صورة طبق الأصل من طبعة مصحف المدينة.", 12f, muted))
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; layoutDirection = View.LAYOUT_DIRECTION_LTR }
        controls.addView(button("السابق", false) { loadMushafPage(mushafPage - 1) }, LinearLayout.LayoutParams(0, -2, 1f))
        controls.addView(button("رقم الصفحة", true) {
            val input = EditText(this@MainActivity).apply { inputType = android.text.InputType.TYPE_CLASS_NUMBER; setText(mushafPage.toString()); gravity = Gravity.CENTER }
            android.app.AlertDialog.Builder(this@MainActivity).setTitle("اذهب إلى صفحة (1–604)").setView(input)
                .setPositiveButton("فتح") { _, _ -> loadMushafPage(input.text.toString().toIntOrNull() ?: mushafPage) }
                .setNegativeButton("إلغاء", null).show()
        }, LinearLayout.LayoutParams(0, -2, 1f))
        controls.addView(button("التالي", false) { loadMushafPage(mushafPage + 1) }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(controls)
        pageStatus = label("جارٍ تحميل الصفحة…", 14f, gold, true)
        root.addView(pageStatus!!)
        pageTextView = TextView(this).apply {
            text = "يتم تحميل نص الصفحة…"; textSize = 23f; setTextColor(Color.rgb(53, 49, 40))
            gravity = Gravity.CENTER; textDirection = View.TEXT_DIRECTION_RTL
            typeface = Typeface.create("serif", Typeface.NORMAL)
            setLineSpacing(12f, 1.25f)
            setPadding(22, 28, 22, 28)
            background = shape(Color.rgb(248, 243, 228), 18f)
        }
        root.addView(pageTextView!!)
        root.addView(space(10))
        root.addView(button("حفظ هذه الصفحة كموضع متابعة", true) {
            getSharedPreferences("deeni", MODE_PRIVATE).edit().putInt("mushaf_page", mushafPage).apply()
            Toast.makeText(this, "تم حفظ الصفحة $mushafPage", Toast.LENGTH_SHORT).show()
        })
        root.addView(space(14))
        root.addView(label("التسميع الصوتي", 20f, cream, true))
        root.addView(label("التعرف الصوتي العام قد يخطئ في ألفاظ القرآن؛ هذه الأداة ليست محركًا متخصصًا ولا تصحح التجويد. لا تعرض نتيجة على أنها حكم قطعي.", 13f, muted))
        val status = label("التسميع متاح لسورة الفاتحة فقط في هذه المرحلة", 14f, gold, true)
        root.addView(status)
        val live = TextView(this).apply { text = "النص الملتقط يظهر هنا"; textSize = 17f; setTextColor(cream); background = shape(panel, 16f); setPadding(14, 14, 14, 14) }
        root.addView(live)
        root.addView(button("ابدأ تسميع الفاتحة", true) { beginRecognition(status, live, mutableListOf()) })
        root.addView(button("إيقاف التسميع", false) { stopRecognition(); status.text = "تم إيقاف التسميع" })
        root.addView(button("العودة للرئيسية", false) { stopRecognition(); showHome() })
        loadMushafPage(mushafPage)
        if (startMic) root.post { beginRecognition(status, live, mutableListOf()) }
    }

    private fun loadMushafPage(requested: Int) {
        val page = requested.coerceIn(1, 604)
        mushafPage = page
        pageStatus?.text = "جارٍ تحميل الصفحة $page من 604…"
        pageTextView?.text = "يتم تحميل النص القرآني…"
        Thread {
            val result = QuranPageSource.load(page)
            runOnUiThread {
                if (currentPage != "quran" || mushafPage != page) return@runOnUiThread
                if (result.isSuccess) {
                    pageTextView?.text = result.getOrThrow()
                    pageStatus?.text = "صفحة $page • من 604"
                    getSharedPreferences("deeni", MODE_PRIVATE).edit().putInt("mushaf_page", page).apply()
                } else {
                    pageStatus?.text = "تعذر تحميل الصفحة"
                    pageTextView?.text = "لم نتمكن من جلب النص. تحقق من اتصال الإنترنت ثم اضغط إعادة المحاولة."
                    root.addView(button("إعادة تحميل الصفحة", false) { loadMushafPage(page) })
                }
            }
        }.start()
    }

    private fun normalize(s: String): String =
        s.lowercase(Locale("ar")).replace(Regex("[\\u064B-\\u065F\\u0670ـ]"), "")
            .replace("أ", "ا").replace("إ", "ا").replace("آ", "ا").replace("ى", "ي")
            .replace(Regex("[^\\u0621-\\u064A\\s]"), " ").trim().replace(Regex("\\s+"), " ")

    private fun beginRecognition(status: TextView, live: TextView, ayahViews: List<TextView>) {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "التعرف الصوتي غير متاح على هذا الجهاز"; return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 77)
            status.text = "اسمح باستخدام الميكروفون ثم اضغط ابدأ"; return
        }
        stopRecognition()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { status.text = "أستمع إليك الآن…" }
            override fun onBeginningOfSpeech() { status.text = "تابع القراءة…" }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { status.text = "أحلل آخر ما التقطته…" }
            override fun onError(error: Int) {
                status.text = when(error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "لم أتمكن من تمييز الكلمات؛ حاول بوضوح أكثر"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "لم ألتقط قراءة؛ اضغط ابدأ وحاول مجددًا"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "إذن الميكروفون غير متاح"
                    else -> "توقف التعرف الصوتي؛ يمكنك إعادة المحاولة"
                }
            }
            override fun onResults(results: Bundle?) {
                val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                renderComparison(words, live, ayahViews)
                status.text = "انتهت جولة الالتقاط — راجع النص المسموع بنفسك"
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val words = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                renderComparison(words, live, ayahViews)
                status.text = "متابعة مباشرة للنص الملتقط…"
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        try { recognizer?.startListening(intent) }
        catch (_: Exception) { status.text = "تعذر بدء الميكروفون؛ تحقق من الإذن وحاول مجددًا" }
    }

    private fun renderComparison(spoken: String, live: TextView, ayahViews: List<TextView>) {
        if (spoken.isBlank()) return
        live.text = "النص الذي التقطه الجهاز:\n$spoken\n\nتنبيه: هذا تفريغ آلي للصوت فقط، وليس تصحيحًا قرآنيًا موثوقًا ولا يحدد موضع الخطأ."
    }

    private fun stopRecognition() {
        try { recognizer?.stopListening(); recognizer?.cancel(); recognizer?.destroy() } catch (_: Exception) {}
        recognizer = null
    }

    private fun chooseReader() {
        android.app.AlertDialog.Builder(this)
            .setTitle("اختر القارئ")
            .setSingleChoiceItems(readers.toTypedArray(), readers.indexOf(selectedReader).coerceAtLeast(0)) { dialog, which ->
                selectedReader = readers[which]
                getSharedPreferences("deeni", MODE_PRIVATE).edit().putString("reader", selectedReader).apply()
                dialog.dismiss()
                if (currentPage == "quran") showQuran()
            }
            .setNegativeButton("إغلاق", null).show()
    }

    private fun showTasbeeh() {
        base("الأذكار والتسبيح", "اجعل لسانك رطبًا بذكر الله")
        val count = label("عدد التسبيحات: $tasbeeh", 25f, gold, true).apply { gravity = Gravity.CENTER }
        root.addView(count)
        root.addView(button("سُبْحَانَ اللَّهِ  +", true) {
            tasbeeh++; getSharedPreferences("deeni", MODE_PRIVATE).edit().putInt("tasbeeh", tasbeeh).apply()
            count.text = "عدد التسبيحات: $tasbeeh"
        })
        root.addView(space(8))
        root.addView(button("إعادة العداد", false) {
            tasbeeh = 0; getSharedPreferences("deeni", MODE_PRIVATE).edit().putInt("tasbeeh", 0).apply()
            count.text = "عدد التسبيحات: 0"
        })
        root.addView(button("الرئيسية", false) { showHome() })
    }

    private fun showIslamicLibrary() {
        currentPage = "library"
        base("السيرة والحديث", "محتوى تعليمي مختار مع الإحالة إلى المصادر")
        root.addView(button("السيرة النبوية • خط زمني", true) { showSirahTimeline() })
        root.addView(button("أحاديث نبوية صحيحة", false) { showHadithCollection() })
        root.addView(label("تنبيه علمي: هذه بداية مكتبة منتقاة وليست موسوعة شاملة. عند نقل حديث أو مسألة شرعية، ارجع إلى المصدر وشروح أهل العلم.", 12f, muted))
        root.addView(button("الرئيسية", false) { showHome() })
    }

    private fun showSirahTimeline() {
        currentPage = "sirah"
        val events = listOf(
            "المولد النبوي" to "ولد النبي محمد ﷺ في مكة عام الفيل، والمشهور أنه في ربيع الأول، وتفاصيل تحديد اليوم محل خلاف بين أهل السيرة.",
            "نزول الوحي" to "بدأ الوحي في غار حراء، وكانت أول الآيات نزولًا من سورة العلق.",
            "الدعوة في مكة" to "دعا النبي ﷺ إلى توحيد الله، وصبر المسلمون على الأذى، وهاجر بعضهم إلى الحبشة.",
            "الهجرة إلى المدينة" to "هاجر النبي ﷺ من مكة إلى المدينة، وكانت الهجرة النبوية بداية التأريخ الهجري.",
            "غزوة بدر" to "وقعت في السنة الثانية للهجرة، وكانت أول معركة كبرى بين المسلمين وقريش.",
            "غزوة أحد" to "وقعت في السنة الثالثة للهجرة، وفيها دروس في الطاعة والصبر والثبات.",
            "صلح الحديبية" to "كان في السنة السادسة للهجرة، وكان له أثر كبير في انتشار الدعوة.",
            "فتح مكة" to "كان في السنة الثامنة للهجرة، ودخل النبي ﷺ مكة متواضعًا وعفا عن كثير ممن آذوه.",
            "حجة الوداع" to "كانت في السنة العاشرة للهجرة، وخطب النبي ﷺ خطبته الجامعة.",
            "وفاة النبي ﷺ" to "توفي رسول الله ﷺ في المدينة في السنة الحادية عشرة للهجرة."
        )
        base("محطات من السيرة", "ترتيب زمني مختصر؛ راجع كتب السيرة للتفاصيل والأسانيد")
        events.forEachIndexed { i, item ->
            root.addView(label("${i + 1}. ${item.first}", 18f, gold, true))
            root.addView(label(item.second, 15f, cream))
            root.addView(space(8))
        }
        root.addView(label("للتوسع: السيرة النبوية لابن هشام، وزاد المعاد لابن القيم، مع مراعاة تحقيق الروايات عند التفصيل.", 12f, muted))
        root.addView(button("رجوع", false) { showIslamicLibrary() })
    }

    private fun showHadithCollection() {
        currentPage = "hadith"
        val hadiths = listOf(
            "إنما الأعمال بالنيات، وإنما لكل امرئ ما نوى." to "صحيح البخاري، حديث 1؛ وصحيح مسلم، حديث 1907.",
            "الدين النصيحة." to "صحيح مسلم، حديث 55.",
            "من كان يؤمن بالله واليوم الآخر فليقل خيرًا أو ليصمت." to "صحيح البخاري، حديث 6018؛ وصحيح مسلم، حديث 47.",
            "لا يؤمن أحدكم حتى يحب لأخيه ما يحب لنفسه." to "صحيح البخاري، حديث 13؛ وصحيح مسلم، حديث 45.",
            "الكلمة الطيبة صدقة." to "صحيح البخاري، حديث 2989؛ وصحيح مسلم، حديث 1009."
        )
        base("أحاديث نبوية", "مختارات مشهورة من الصحيحين مع أرقام الأحاديث")
        hadiths.forEachIndexed { i, item ->
            root.addView(label("الحديث ${i + 1}", 14f, gold, true))
            root.addView(label("«${item.first}»", 20f, cream, true))
            root.addView(label(item.second, 13f, muted))
            root.addView(space(10))
        }
        root.addView(label("هذه المختارات ليست بديلًا عن الرجوع إلى نص الحديث كاملًا وشرحه، ولا تتضمن أحكامًا فقهية مستنبطة.", 12f, muted))
        root.addView(button("رجوع", false) { showIslamicLibrary() })
    }

    private fun showRevision() {
        currentPage = "revision"
        val prefs = getSharedPreferences("deeni", MODE_PRIVATE)
        val items = listOf("مراجعة محفوظ الأمس", "حفظ مقدار جديد", "تثبيت آخر موضع", "تسميع ما تم حفظه")
        base("الحفظ والمراجعة", "خطة متابعة يومية — يتم حفظ إنجازك على الجهاز")
        val progress = label("", 16f, gold, true)
        root.addView(progress)
        fun refresh() {
            val done = items.count { prefs.getBoolean("revision_" + it, false) }
            progress.text = "إنجاز اليوم: $done من ${items.size}"
        }
        items.forEach { item ->
            val check = CheckBox(this).apply {
                text = item; textSize = 17f; setTextColor(cream)
                buttonTintList = android.content.res.ColorStateList.valueOf(gold)
                isChecked = prefs.getBoolean("revision_" + item, false)
                setOnCheckedChangeListener { _, checked ->
                    prefs.edit().putBoolean("revision_" + item, checked).apply()
                    refresh()
                }
            }
            root.addView(check)
        }
        refresh()
        root.addView(button("تصفير خطة اليوم", false) {
            prefs.edit().apply { items.forEach { remove("revision_" + it) } }.apply()
            showRevision()
        })
        root.addView(button("الرئيسية", true) { showHome() })
    }

    private fun showNotes() {
        currentPage = "notes"
        val prefs = getSharedPreferences("deeni", MODE_PRIVATE)
        base("ملاحظاتي", "فوائد وتدبر وملاحظات خاصة — تحفظ على هذا الجهاز")
        val editor = EditText(this).apply {
            setText(prefs.getString("personal_notes", ""))
            hint = "اكتب فائدتك أو ملاحظتك هنا…"
            textSize = 17f; setTextColor(cream); setHintTextColor(muted)
            gravity = Gravity.TOP or Gravity.RIGHT
            minLines = 8
            setPadding(16, 16, 16, 16)
            background = shape(panel, 18f)
        }
        root.addView(editor, LinearLayout.LayoutParams(-1, -2))
        root.addView(space(10))
        root.addView(button("حفظ الملاحظات", true) {
            prefs.edit().putString("personal_notes", editor.text.toString()).apply()
            Toast.makeText(this, "تم حفظ ملاحظاتك على الجهاز", Toast.LENGTH_SHORT).show()
        })
        root.addView(button("مسح الملاحظات", false) {
            editor.setText("")
            prefs.edit().remove("personal_notes").apply()
        })
        root.addView(button("الرئيسية", false) { showHome() })
    }

    private fun showAdhkar() {
        currentPage = "adhkar"
        val entries = listOf(
            "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ" to 100,
            "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ" to 100,
            "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ" to 100,
            "سُبْحَانَ اللَّهِ" to 33,
            "الْحَمْدُ لِلَّهِ" to 33,
            "اللَّهُ أَكْبَرُ" to 34
        )
        val prefs = getSharedPreferences("deeni", MODE_PRIVATE)
        base("الأذكار والتسبيح", "اختر الذكر واضغط على عداده — الأعداد أهداف اختيارية وليست إلزامًا")
        entries.forEachIndexed { index, entry ->
            val count = label("", 18f, gold, true).apply { gravity = Gravity.CENTER }
            fun refreshCount() { count.text = "${prefs.getInt("dhikr_$index", 0)} / ${entry.second}" }
            root.addView(label(entry.first, 19f, cream, true))
            root.addView(count)
            root.addView(button("＋ تسبيحة", true) {
                val next = prefs.getInt("dhikr_$index", 0) + 1
                prefs.edit().putInt("dhikr_$index", next).apply()
                refreshCount()
            })
            root.addView(space(7))
            refreshCount()
        }
        root.addView(button("تصفير عدادات الأذكار", false) {
            prefs.edit().apply { entries.indices.forEach { remove("dhikr_$it") } }.apply()
            showAdhkar()
        })
        root.addView(button("الرئيسية", false) { showHome() })
    }

    private fun showQuiz() {
        currentPage = "quiz"
        val questions = listOf(
            Triple("ما السورة التي تُفتتح بها المصاحف؟", listOf("الفاتحة", "البقرة", "الإخلاص", "الناس"), 0),
            Triple("كم عدد أركان الإسلام؟", listOf("أربعة", "خمسة", "ستة", "سبعة"), 1),
            Triple("في أي شهر يصوم المسلمون؟", listOf("شعبان", "رمضان", "شوال", "محرم"), 1),
            Triple("ما أطول سورة في القرآن الكريم؟", listOf("آل عمران", "النساء", "البقرة", "المائدة"), 2),
            Triple("من النبي الذي ابتلعه الحوت؟", listOf("موسى عليه السلام", "يونس عليه السلام", "يوسف عليه السلام", "نوح عليه السلام"), 1),
            Triple("ما اسم الغار الذي نزل فيه الوحي أول مرة؟", listOf("ثور", "حراء", "أحد", "النور"), 1),
            Triple("كم عدد الصلوات المفروضة في اليوم والليلة؟", listOf("ثلاث", "أربع", "خمس", "ست"), 2),
            Triple("ما الكتاب الذي أُنزل على النبي محمد ﷺ؟", listOf("التوراة", "الإنجيل", "الزبور", "القرآن"), 3)
        )
        var index = 0
        var score = 0
        var answered = false
        lateinit var draw: () -> Unit
        draw = {
            base("المسابقة الإسلامية", "ثمانية أسئلة • اختر إجابة واحدة لكل سؤال")
            if (index >= questions.size) {
                root.addView(label("انتهت المسابقة", 25f, gold, true))
                root.addView(label("نتيجتك: ${score} من ${questions.size}", 22f, cream, true))
                root.addView(label(if (score == questions.size) "أحسنت! إجاباتك كلها صحيحة." else "واصل التعلم، ويمكنك إعادة المحاولة.", 15f, muted))
                root.addView(button("إعادة المسابقة", true) { showQuiz() })
                root.addView(button("الرئيسية", false) { showHome() })
            } else {
                val q = questions[index]
                root.addView(label("السؤال ${index + 1} من ${questions.size}", 15f, gold, true))
                root.addView(label(q.first, 22f, cream, true))
                q.second.forEachIndexed { optionIndex, option ->
                    root.addView(button("${'أ' + optionIndex}.  ${option}", false) {
                        if (!answered) {
                            answered = true
                            val correct = optionIndex == q.third
                            if (correct) score++
                            root.addView(label(if (correct) "إجابة صحيحة ✓" else "ليست الإجابة الصحيحة. الإجابة: ${q.second[q.third]}", 16f, if (correct) Color.rgb(100, 210, 140) else gold, true))
                            root.addView(button(if (index == questions.lastIndex) "عرض النتيجة" else "السؤال التالي", true) {
                                index++
                                answered = false
                                draw()
                            })
                        }
                    })
                    root.addView(space(5))
                }
                root.addView(label("النقاط الحالية: ${score}", 13f, muted))
                root.addView(button("إنهاء المسابقة", false) { index = questions.size; draw() })
            }
        }
        draw()
    }

    private fun showNames() {
        currentPage = "names"
        val names = listOf(
            "الرَّحْمَٰنُ" to "واسع الرحمة لجميع خلقه.",
            "الرَّحِيمُ" to "الذي يرحم عباده المؤمنين.",
            "الْمَلِكُ" to "المالك المتصرف في خلقه.",
            "الْقُدُّوسُ" to "المنزّه عن كل نقص.",
            "السَّلَامُ" to "السالم من كل عيب ونقص.",
            "الْمُؤْمِنُ" to "الذي يؤمّن عباده ويصدق رسله.",
            "الْعَزِيزُ" to "الغالب الذي لا يُغلب.",
            "الْخَالِقُ" to "الذي أوجد الخلق.",
            "الْغَفُورُ" to "كثير المغفرة لعباده.",
            "الرَّزَّاقُ" to "الذي يرزق جميع خلقه.",
            "الْعَلِيمُ" to "الذي أحاط علمه بكل شيء.",
            "السَّمِيعُ" to "الذي يسمع جميع الأصوات.",
            "الْبَصِيرُ" to "الذي يرى كل شيء.",
            "الْحَكِيمُ" to "الذي يضع الأمور مواضعها.",
            "اللَّطِيفُ" to "الرفيق بعباده، العالم بدقائق الأمور.",
            "الْحَلِيمُ" to "الذي لا يعاجل عباده بالعقوبة.",
            "الْكَرِيمُ" to "كثير الخير والعطاء.",
            "التَّوَّابُ" to "الذي يقبل توبة عباده.",
            "الْحَفِيظُ" to "الحافظ لكل شيء.",
            "النُّورُ" to "نور السماوات والأرض."
        )
        base("أسماء الله الحسنى", "معانٍ موجزة لأسماء ثابتة في النصوص")
        names.forEachIndexed { i, item ->
            root.addView(label("${i + 1}. ${item.first}", 19f, gold, true))
            root.addView(label(item.second, 15f, cream))
            root.addView(space(5))
        }
        root.addView(label("هذه قائمة مختارة من عشرين اسمًا وليست عرضًا كاملًا للأسماء التسعة والتسعين، ومعاني الأسماء يُرجع فيها إلى كتب العقيدة والتفسير المعتمدة.", 12f, muted))
        root.addView(button("الرئيسية", true) { showHome() })
    }

    private fun showQuranSearch() {
        currentPage = "quran_search"
        base("البحث في القرآن", "ابحث في نص القرآن بالرسم العثماني أو بالكلمات المعتادة")
        val query = EditText(this).apply {
            hint = "اكتب كلمة أو عبارة للبحث"
            textSize = 17f
            setTextColor(cream)
            setHintTextColor(muted)
            gravity = Gravity.RIGHT
            setPadding(16, 14, 16, 14)
            background = shape(panel, 16f)
        }
        root.addView(query)
        val status = label("أدخل كلمة ثم اضغط بحث", 13f, muted)
        root.addView(status)
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(results)
        root.addView(button("بحث", true) {
            val term = query.text.toString().trim()
            if (term.length < 2) {
                status.text = "اكتب حرفين على الأقل"
            } else {
                status.text = "جارٍ البحث…"
                results.removeAllViews()
                Thread {
                    val found = QuranSearchSource.search(term)
                    runOnUiThread {
                        if (currentPage != "quran_search") return@runOnUiThread
                        if (found.isSuccess) {
                            val items = found.getOrThrow()
                            status.text = "عدد النتائج المعروضة: ${items.size}"
                            if (items.isEmpty()) results.addView(label("لم يتم العثور على نتائج.", 15f, muted))
                            items.forEach { item ->
                                results.addView(label("سورة ${item.first} • الآية ${item.second}", 13f, gold, true))
                                results.addView(label(item.third, 18f, cream))
                                results.addView(space(9))
                            }
                        } else {
                            status.text = "تعذر البحث. تحقق من الإنترنت وحاول مجددًا."
                        }
                    }
                }.start()
            }
        })
        root.addView(button("فتح المصحف", false) { showQuran() })
        root.addView(button("الرئيسية", false) { showHome() })
    }

    private fun showPrayerTimes() {
        currentPage = "prayer_times"
        base("مواقيت الصلاة", "المواقيت تُجلب حسب المدينة وطريقة الحساب المختارة")
        val city = EditText(this).apply {
            setText(getSharedPreferences("deeni", MODE_PRIVATE).getString("prayer_city", "أبو عريش") ?: "أبو عريش")
            hint = "اسم المدينة بالإنجليزية، مثال: Abu Arish"
            textSize = 16f
            setTextColor(cream)
            setHintTextColor(muted)
            gravity = Gravity.RIGHT
            setPadding(14, 14, 14, 14)
            background = shape(panel, 16f)
        }
        root.addView(label("المدينة", 15f, gold, true))
        root.addView(city)
        root.addView(label("اكتب اسم المدينة بالإنجليزية لضمان تعرف خدمة المواقيت عليها.", 12f, muted))
        val result = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(result)
        root.addView(button("عرض مواقيت اليوم", true) {
            val name = city.text.toString().trim()
            if (name.isBlank()) {
                Toast.makeText(this, "اكتب اسم المدينة أولًا", Toast.LENGTH_SHORT).show()
            } else {
                getSharedPreferences("deeni", MODE_PRIVATE).edit().putString("prayer_city", name).apply()
                result.removeAllViews()
                result.addView(label("جارٍ جلب المواقيت…", 15f, gold))
                Thread {
                    val response = PrayerTimesSource.load(name)
                    runOnUiThread {
                        if (currentPage != "prayer_times") return@runOnUiThread
                        result.removeAllViews()
                        if (response.isSuccess) {
                            val data = response.getOrThrow()
                            result.addView(label(data.dateLabel, 14f, muted))
                            data.times.forEach { item ->
                                val row = LinearLayout(this).apply {
                                    orientation = LinearLayout.HORIZONTAL
                                    gravity = Gravity.CENTER_VERTICAL
                                    setPadding(14, 10, 14, 10)
                                    background = shape(panel, 14f)
                                }
                                val prayer = label(item.first, 17f, cream, true)
                                prayer.gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                                val time = label(item.second, 19f, gold, true)
                                time.gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
                                row.addView(prayer, LinearLayout.LayoutParams(0, -2, 1f))
                                row.addView(time, LinearLayout.LayoutParams(0, -2, 1f))
                                val lp = LinearLayout.LayoutParams(-1, -2)
                                lp.bottomMargin = 7
                                result.addView(row, lp)
                            }
                            result.addView(label("المصدر: خدمة مواقيت الصلاة عبر الإنترنت. تحقق من إعدادات الجهة الرسمية في بلدك عند اختلاف التوقيت أو طريقة الحساب.", 12f, muted))
                        } else {
                            result.addView(label("تعذر جلب المواقيت. تحقق من اسم المدينة والاتصال بالإنترنت.", 14f, gold))
                        }
                    }
                }.start()
            }
        })
        root.addView(button("الرئيسية", false) { showHome() })
    }

    private fun showQibla() {
        currentPage = "qibla"
        base("اتجاه القبلة", "حدد مدينتك ووجّه الهاتف أفقيًا بعيدًا عن المعادن")
        val prefs = getSharedPreferences("deeni", MODE_PRIVATE)
        val city = prefs.getString("prayer_city", "أبو عريش") ?: "أبو عريش"
        root.addView(label("المدينة المستخدمة: ${city}", 14f, muted))
        val status = label("جارٍ تحديد اتجاه القبلة…", 15f, gold, true)
        root.addView(status)
        val compass = QiblaCompassView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 310)
        }
        root.addView(compass)
        root.addView(label("إذا لم تتحرك البوصلة، فقد لا يدعم جهازك حساس الاتجاه أو يحتاج إلى معايرة.", 12f, muted))
        root.addView(button("تحديث اتجاه القبلة", true) {
            status.text = "جارٍ الحساب…"
            Thread {
                val result = PrayerTimesSource.load(city)
                runOnUiThread {
                    if (currentPage != "qibla") return@runOnUiThread
                    if (result.isSuccess) {
                        val data = result.getOrThrow()
                        val bearing = qiblaBearing(data.latitude, data.longitude)
                        status.text = "اتجاه القبلة: ${"%.1f".format(Locale.US, bearing)}° من الشمال الحقيقي"
                        compass.setDirection(bearing.toFloat(), 0f)
                        startQiblaSensor(compass, bearing.toFloat())
                    } else {
                        status.text = "تعذر تحديد المدينة. افتح مواقيت الصلاة وأدخل اسم المدينة بالإنجليزية."
                    }
                }
            }.start()
        })
        root.addView(button("مواقيت الصلاة", false) { showPrayerTimes() })
        root.addView(button("الرئيسية", false) { showHome() })
        root.post {
            Thread {
                val result = PrayerTimesSource.load(city)
                runOnUiThread {
                    if (currentPage != "qibla") return@runOnUiThread
                    if (result.isSuccess) {
                        val data = result.getOrThrow()
                        val bearing = qiblaBearing(data.latitude, data.longitude)
                        status.text = "اتجاه القبلة: ${"%.1f".format(Locale.US, bearing)}° من الشمال الحقيقي"
                        compass.setDirection(bearing.toFloat(), 0f)
                        startQiblaSensor(compass, bearing.toFloat())
                    } else {
                        status.text = "تعذر تحديد اتجاه القبلة. تحقق من المدينة والإنترنت."
                    }
                }
            }.start()
        }
    }

    private fun qiblaBearing(latitude: Double, longitude: Double): Double {
        val kaabaLat = Math.toRadians(21.4225)
        val kaabaLon = Math.toRadians(39.8262)
        val userLat = Math.toRadians(latitude)
        val deltaLon = kaabaLon - Math.toRadians(longitude)
        val y = kotlin.math.sin(deltaLon)
        val x = kotlin.math.cos(userLat) * kotlin.math.tan(kaabaLat) -
            kotlin.math.sin(userLat) * kotlin.math.cos(deltaLon)
        return (Math.toDegrees(kotlin.math.atan2(y, x)) + 360.0) % 360.0
    }

    private fun startQiblaSensor(compass: QiblaCompassView, bearing: Float) {
        stopQiblaSensor()
        val manager = getSystemService(SENSOR_SERVICE) as SensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensor == null) {
            compass.setDirection(bearing, 0f)
            return
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
                val rotation = FloatArray(9)
                val orientation = FloatArray(3)
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                SensorManager.getOrientation(rotation, orientation)
                val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                compass.setDirection(bearing, azimuth)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
                    Toast.makeText(this@MainActivity, "دقة البوصلة منخفضة؛ أبعد الهاتف عن مصادر التشويش وعايره.", Toast.LENGTH_LONG).show()
                }
            }
        }
        qiblaSensorManager = manager
        qiblaSensorListener = listener
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    private fun stopQiblaSensor() {
        val manager = qiblaSensorManager
        val listener = qiblaSensorListener
        if (manager != null && listener != null) manager.unregisterListener(listener)
        qiblaSensorManager = null
        qiblaSensorListener = null
    }

    private fun showComing(title: String) {
        base(title, "هذا القسم ضمن خطة التطوير")
        root.addView(label("لن نضع محتوى غير موثق أو أزرارًا وهمية. سيُضاف هذا القسم بعد توفير مصادر موثوقة واختبار وظائفه.", 16f, cream))
        root.addView(button("الرئيسية", true) { showHome() })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 77 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "تم السماح بالميكروفون. اضغط ابدأ التسميع.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onStop() { stopRecognition(); stopQiblaSensor(); super.onStop() }
}
