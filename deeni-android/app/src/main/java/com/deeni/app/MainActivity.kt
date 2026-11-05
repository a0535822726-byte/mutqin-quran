package com.deeni.app

import android.Manifest
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
        card("مكتبة القراء", "اختر قارئك المفضل", "♫") { chooseReader() }
        card("الأذكار والتسبيح", "ذكر يومي وعداد محفوظ", "✧") { showTasbeeh() }
        card("الحفظ والمراجعة", "خطتك ومواضع مراجعتك", "۞") { showComing("الحفظ والمراجعة") }
        card("السيرة والحديث", "محتوى علمي بمصادره", "☾") { showComing("السيرة والحديث") }
        root.addView(label("نسخة تطوير: المصحف المعروض حاليًا سورة الفاتحة كنموذج أولي، وبقية السور ومصادر الصوت قيد الإضافة.", 12f, muted))
    }

    private fun showQuran(startMic: Boolean = false) {
        currentPage = "quran"
        base("المصحف الشريف", "سورة الفاتحة • نموذج القراءة والمتابعة")
        val readerBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; background = shape(panel, 20f); setPadding(16, 12, 16, 12)
            addView(label("القارئ المحدد", 13f, muted))
            addView(button("♫  $selectedReader  ·  تغيير القارئ", false) { chooseReader() })
            addView(label("تشغيل تلاوة القارئ ومزامنة الآيات ستضاف بعد ربط مصدر صوت موثوق وتوقيت الآيات؛ هذا الزر لا يدّعي تشغيل صوت غير موجود.", 12f, muted))
        }
        root.addView(readerBox)
        root.addView(space(14))
        val mushaf = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(Color.rgb(248, 243, 228), 24f)
            setPadding(18, 18, 18, 18)
        }
        mushaf.addView(TextView(this).apply {
            text = "سُورَةُ الْفَاتِحَةِ"; textSize = 20f; setTextColor(Color.rgb(104, 79, 39))
            gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD; setPadding(0, 4, 0, 16)
        })
        val ayahViews = mutableListOf<TextView>()
        fatiha.forEachIndexed { i, ayah ->
            val tv = TextView(this).apply {
                text = "﴿${i + 1}﴾  $ayah"
                textSize = 22f; setTextColor(Color.rgb(53, 49, 40)); gravity = Gravity.CENTER
                typeface = Typeface.create("serif", Typeface.NORMAL)
                setPadding(5, 11, 5, 11); textDirection = View.TEXT_DIRECTION_RTL
            }
            ayahViews.add(tv); mushaf.addView(tv)
        }
        root.addView(mushaf)
        root.addView(space(14))
        root.addView(label("التسميع الذكي", 21f, cream, true))
        root.addView(label("اسمح للميكروفون، ثم اقرأ الفاتحة. سيعرض التعرف الصوتي النص الذي التقطه ويقارن الكلمات بالنص المعروض. قد يخطئ التعرف الصوتي في التشكيل أو النطق؛ راجع النتيجة ولا تعتمد عليه وحده.", 13f, muted))
        val status = label("جاهز للتسميع", 15f, gold, true)
        root.addView(status)
        val live = TextView(this).apply {
            text = "سيظهر النص الملتقط هنا أثناء القراءة…"; textSize = 18f; setTextColor(cream)
            background = shape(panel, 18f); setPadding(15, 15, 15, 15); gravity = Gravity.RIGHT
        }
        root.addView(live)
        root.addView(space(10))
        root.addView(button("🎙  ابدأ التسميع", true) { beginRecognition(status, live, ayahViews) })
        root.addView(space(8))
        root.addView(button("إيقاف التسميع", false) { stopRecognition(); status.text = "تم إيقاف التسميع" })
        root.addView(label("ملاحظة: المقارنة الحالية نموذج أولي للفـاتحة باستخدام خدمة التعرف على الكلام في الجهاز، وليست محرك ترتيل متخصصًا ولا تصحيحًا للتجويد أو المخارج.", 12f, muted))
        root.addView(button("العودة للرئيسية", false) { stopRecognition(); showHome() })
        if (startMic) root.post { beginRecognition(status, live, ayahViews) }
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
                status.text = "انتهت جولة الالتقاط — راجع الكلمات المظللة"
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
        live.text = "ما التقطه الجهاز:\\n$spoken"
        val got = normalize(spoken).split(" ").filter { it.isNotBlank() }
        val expected = expectedWords.map { normalize(it) }
        var matched = 0
        for (word in got) if (matched < expected.size && word == expected[matched]) matched++
        val correctRatio = if (got.isEmpty()) 0 else (matched * 100 / got.size)
        live.append("\\n\\nتطابق تقريبي: $correctRatio٪")
        ayahViews.forEachIndexed { index, view ->
            val rangeStart = fatiha.take(index).sumOf { normalize(it).split(" ").size }
            val rangeEnd = rangeStart + normalize(fatiha[index]).split(" ").size
            val color = when {
                matched >= rangeEnd -> Color.rgb(37, 125, 76)
                matched > rangeStart -> Color.rgb(168, 112, 25)
                else -> Color.rgb(53, 49, 40)
            }
            val span = SpannableString("﴿${index + 1}﴾  ${fatiha[index]}")
            span.setSpan(ForegroundColorSpan(color), 0, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            view.text = span
        }
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

    override fun onStop() { stopRecognition(); super.onStop() }
}
