package com.deeni.app

import android.Manifest
import android.hardware.Sensor
import android.hardware.GeomagneticField
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
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.util.Base64
import java.net.HttpURLConnection
import java.net.URL
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.widget.*
import java.util.Locale

class MainActivity : Activity() {
    private var lightTheme = false
    // Deeni visual system: midnight navy, warm gold, and a clear Mushaf paper surface.
    private var bg = Color.rgb(3, 7, 18)
    private var panel = Color.rgb(17, 28, 53)
    private var panel2 = Color.rgb(11, 19, 41)
    private var gold = Color.rgb(245, 158, 11)
    private var cream = Color.rgb(249, 250, 251)
    private var muted = Color.rgb(170, 180, 197)

    private fun applyTheme() {
        if (lightTheme) {
            bg = Color.rgb(246, 243, 234)
            panel = Color.rgb(255, 255, 250)
            panel2 = Color.rgb(232, 224, 204)
            gold = Color.rgb(126, 91, 35)
            cream = Color.rgb(37, 53, 46)
            muted = Color.rgb(94, 111, 101)
        } else {
            bg = Color.rgb(3, 7, 18)
            panel = Color.rgb(17, 28, 53)
            panel2 = Color.rgb(11, 19, 41)
            gold = Color.rgb(245, 158, 11)
            cream = Color.rgb(249, 250, 251)
            muted = Color.rgb(170, 180, 197)
        }
        window.statusBarColor = bg
        window.navigationBarColor = bg
    }
    private lateinit var root: LinearLayout
    private var recognizer: SpeechRecognizer? = null
    private var currentPage = "home"
    private var qiblaSensorManager: SensorManager? = null
    private var qiblaSensorListener: SensorEventListener? = null
    private var quranPlayer: MediaPlayer? = null
    private val audioHandler = Handler(Looper.getMainLooper())
    private var timingRows: List<AyahTiming> = emptyList()
    private var currentTimedAyah = -1
    private var repeatTiming: AyahTiming? = null
    private var playbackSpeed = 1.0f
    private var playbackSurah = 1
    private var playbackStatus: TextView? = null
    private var recitationStatusView: TextView? = null
    private var recitationTextView: TextView? = null
    private var pendingRecognitionAfterPermission = false
    private var pendingHighlight: Pair<Int, Int>? = null
    private var selectedAyah: Pair<Int, Int>? = null
    private var selectedAyahText: String? = null
    private var selectedReader = "ماهر المعيقلي"
    private var tasbeeh = 0
    private val readers = listOf("ماهر المعيقلي", "عبد الرحمن السديس", "ياسر الدوسري", "مشاري العفاسي", "محمود خليل الحصري", "محمد صديق المنشاوي", "عبد الباسط عبد الصمد", "سعد الغامدي", "أحمد العجمي", "علي جابر")
    private val mushafSurahNames = listOf(
            "الفاتحة", "البقرة", "آل عمران", "النساء", "المائدة", "الأنعام", "الأعراف", "الأنفال", "التوبة", "يونس",
            "هود", "يوسف", "الرعد", "إبراهيم", "الحجر", "النحل", "الإسراء", "الكهف", "مريم", "طه",
            "الأنبياء", "الحج", "المؤمنون", "النور", "الفرقان", "الشعراء", "النمل", "القصص", "العنكبوت", "الروم",
            "لقمان", "السجدة", "الأحزاب", "سبأ", "فاطر", "يس", "الصافات", "ص", "الزمر", "غافر",
            "فصلت", "الشورى", "الزخرف", "الدخان", "الجاثية", "الأحقاف", "محمد", "الفتح", "الحجرات", "ق",
            "الذاريات", "الطور", "النجم", "القمر", "الرحمن", "الواقعة", "الحديد", "المجادلة", "الحشر", "الممتحنة",
            "الصف", "الجمعة", "المنافقون", "التغابن", "الطلاق", "التحريم", "الملك", "القلم", "الحاقة", "المعارج",
            "نوح", "الجن", "المزمل", "المدثر", "القيامة", "الإنسان", "المرسلات", "النبأ", "النازعات", "عبس",
            "التكوير", "الانفطار", "المطففين", "الانشقاق", "البروج", "الطارق", "الأعلى", "الغاشية", "الفجر", "البلد",
            "الشمس", "الليل", "الضحى", "الشرح", "التين", "العلق", "القدر", "البينة", "الزلزلة", "العاديات",
            "القارعة", "التكاثر", "العصر", "الهمزة", "الفيل", "قريش", "الماعون", "الكوثر", "الكافرون", "النصر",
            "المسد", "الإخلاص", "الفلق", "الناس"
        )
    private val juzStarts = listOf(
        1 to (1 to 1), 2 to (2 to 142), 3 to (2 to 253), 4 to (3 to 93),
        5 to (4 to 24), 6 to (4 to 148), 7 to (5 to 82), 8 to (6 to 111),
        9 to (7 to 88), 10 to (8 to 41), 11 to (9 to 93), 12 to (11 to 6),
        13 to (12 to 53), 14 to (15 to 1), 15 to (17 to 1), 16 to (18 to 75),
        17 to (21 to 1), 18 to (23 to 1), 19 to (25 to 21), 20 to (27 to 56),
        21 to (29 to 46), 22 to (33 to 31), 23 to (36 to 28), 24 to (39 to 32),
        25 to (41 to 47), 26 to (46 to 1), 27 to (51 to 31), 28 to (58 to 1),
        29 to (67 to 1), 30 to (78 to 1)
    )
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
        window.decorView.systemUiVisibility = 0
        lightTheme = getSharedPreferences("deeni", MODE_PRIVATE).getBoolean("light_theme", true)
        applyTheme()
        tasbeeh = getSharedPreferences("deeni", MODE_PRIVATE).getInt("tasbeeh", 0)
        selectedReader = getSharedPreferences("deeni", MODE_PRIVATE).getString("reader", readers[0]) ?: readers[0]
        if (intent.getBooleanExtra("open_mushaf", false)) {
            showQuran(intent.getBooleanExtra("start_mic", false))
        } else {
            showSplash()
        }
    }

    /** White opening screen, then enter the Deeni dashboard with the full Mushaf one tap away. */
    private fun showSplash() {
        val splash = FrameLayout(this).apply {
            setBackgroundColor(Color.WHITE)
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        val center = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val basmala = TextView(this).apply {
            text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
            textSize = 31f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            typeface = Typeface.create("serif", Typeface.NORMAL)
        }
        val brand = TextView(this).apply {
            text = "ديني  |  DEENI"
            textSize = 17f
            setTextColor(Color.rgb(45, 67, 55))
            gravity = Gravity.CENTER
            letterSpacing = 0.08f
            setPadding(0, 18, 0, 0)
        }
        center.addView(basmala)
        center.addView(brand)
        splash.addView(center, FrameLayout.LayoutParams(-1, -1))
        setContentView(splash)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing && !isDestroyed) {
                applyTheme()
                showHome()
            }
        }, 1500L)
    }

    override fun onStop() {
        // Do not leave the microphone, audio ticker, or compass sensor active in background.
        stopRecognition()
        stopQuranAudio()
        stopQiblaSensor()
        super.onStop()
    }

    override fun onDestroy() {
        stopRecognition()
        stopQuranAudio()
        stopQiblaSensor()
        mushafWebView?.let { view ->
            try {
                view.stopLoading()
                view.removeJavascriptInterface("DeeniMushaf")
                view.loadUrl("about:blank")
                view.destroy()
            } catch (_: Exception) {}
        }
        mushafWebView = null
        super.onDestroy()
    }

    @Deprecated("Use the platform back dispatcher on newer Android versions")
    override fun onBackPressed() {
        if (currentPage != "home") {
            stopRecognition()
            // When the Mushaf was opened from the redesigned Deeni dashboard,
            // return to that dashboard instead of exposing the legacy MainActivity home.
            if (intent.getBooleanExtra("open_mushaf", false)) {
                finish()
            } else {
                showHome()
            }
        } else {
            super.onBackPressed()
        }
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
        // Stop screen-specific resources before building the next screen.
        stopRecognition()
        stopQuranAudio()
        stopQiblaSensor()
        mushafWebView?.let { view ->
            try {
                view.stopLoading()
                view.removeJavascriptInterface("DeeniMushaf")
                view.loadUrl("about:blank")
                view.destroy()
            } catch (_: Exception) {}
        }
        mushafWebView = null

        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(bg)
        }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(18, 18, 18, 20)
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(root)
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 13, 20, 8)
            setBackgroundColor(bg)
        }
        top.addView(label("دِينِي", 17f, gold, true))
        top.addView(label(title, 25f, cream, true))
        top.addView(label(subtitle, 12f, muted))
        shell.addView(top)
        shell.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(8, 9, 8, 9)
            background = shape(panel, 20f)
        }
        fun navItem(text: String, active: Boolean, action: () -> Unit) {
            val item = TextView(this).apply {
                this.text = text
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(if (active) gold else muted)
                setPadding(3, 9, 3, 9)
                typeface = Typeface.create("sans-serif", if (active) Typeface.BOLD else Typeface.NORMAL)
                setOnClickListener { action() }
            }
            nav.addView(item, LinearLayout.LayoutParams(0, -2, 1f))
        }
        navItem("⌂\nالرئيسية", currentPage == "home") { showHome() }
        navItem("▤\nالمصحف", currentPage == "quran") { showQuran() }
        navItem("◉\nالحفظ", currentPage == "revision") { showRevision() }
        navItem("◷\nالتقدم", currentPage == "progress") { showProgress() }
        navItem("•••\nالمزيد", currentPage == "more") { showMore() }
        val navWrap = LinearLayout(this).apply {
            setPadding(12, 5, 12, 10)
            setBackgroundColor(bg)
            addView(nav)
        }
        shell.addView(navWrap)
        setContentView(shell)
    }

    private fun space(h: Int): View = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, h) }

    private fun card(title: String, desc: String, icon: String, action: () -> Unit) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(panel, 20f)
            setPadding(16, 12, 16, 12)
            addView(label(icon, 20f, gold, true))
            addView(label(title, 17f, cream, true))
            addView(label(desc, 12f, muted))
            setOnClickListener { action() }
        }
        val p = LinearLayout.LayoutParams(-1, -2)
        p.bottomMargin = 9
        root.addView(box, p)
    }

    private fun showHome() {
        currentPage = "home"
        base("أهلًا بك في ديني", "رفيقك للقرآن والحفظ والعبادة")
        val prefs = getSharedPreferences("deeni", MODE_PRIVATE)
        val lastPage = prefs.getInt("mushaf_page", 1).coerceIn(1, 604)
        val welcome = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(panel2, 24f)
            setPadding(18, 18, 18, 18)
            addView(label("وَرَتِّلِ الْقُرْآنَ تَرْتِيلًا", 23f, gold, true))
            addView(label("اجعل لك وردًا ثابتًا، وتابع قراءتك خطوة بخطوة.", 13f, cream))
            addView(space(8))
            addView(button("متابعة المصحف • الصفحة " + lastPage, true) { showQuran() })
            addView(space(7))
            addView(button("ابدأ من الفاتحة • الصفحة 1", false) {
                getSharedPreferences("deeni", MODE_PRIVATE).edit().putInt("mushaf_page", 1).apply()
                showQuran()
            })
        }
        root.addView(welcome)
        root.addView(space(10))
        val recite = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = shape(panel, 20f)
            setPadding(10, 12, 10, 12)
        }
        recite.addView(button("🎙 ابدأ التسميع", true) { showQuran(true) }, LinearLayout.LayoutParams(0, -2, 1f))
        recite.addView(button("♫ استمع لقارئ", false) { showQuran(); chooseReader() }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(recite)

        homeSection("ابدأ رحلتك")
        homeTileRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        homeTile("المصحف", "قراءة وتدبر", "۞") { showQuran() }
        homeTile("التسميع", "تدريب صوتي", "🎙") { showQuran(true) }
        root.addView(homeTileRow!!)
        homeTileRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        homeTile("الحفظ", "خطة يومية", "✦") { showRevision() }
        homeTile("التفسير", "معاني الآيات", "📖") { showTafsir() }
        root.addView(homeTileRow!!)

        homeSection("القرآن وعلومه")
        homeTileRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        homeTile("البحث", "ابحث في الآيات", "⌕") { showQuranSearch() }
        homeTile("المحفوظات", "آياتك وعلاماتك", "☆") { showSavedAyahs() }
        root.addView(homeTileRow!!)
        homeTileRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        homeTile("التجويد", "أحكام التلاوة", "۞") { showTajweed() }
        homeTile("القراء", "اختر صوتًا", "♫") { chooseReader() }
        root.addView(homeTileRow!!)

        homeSection("المكتبة الإسلامية")
        homeTileRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        homeTile("الحديث", "أحاديث ومراجع", "📜") { showIslamicLibrary() }
        homeTile("السيرة", "السيرة النبوية", "☾") { showIslamicLibrary() }
        root.addView(homeTileRow!!)
        homeTileRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        homeTile("قصص الأنبياء", "قصص قرآنية", "✧") { showProphetStories() }
        homeTile("قصص الصحابة", "سير مختارة", "☼") { showCompanionStories() }
        root.addView(homeTileRow!!)
        homeTileRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        homeTile("الأذكار", "أذكار وأدعية", "◉") { showAdhkar() }
        homeTile("الفقه", "مبادئ أساسية", "⚖") { showFiqhBasics() }
        root.addView(homeTileRow!!)

        homeSection("التحديات والمسابقات")
        val game = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(panel2, 22f)
            setPadding(16, 14, 16, 14)
            addView(label("من سيربح المليون؟", 21f, gold, true))
            addView(label("15 سؤالًا إسلاميًا • مراحل وجوائز افتراضية ووسائل مساعدة", 12f, cream))
            addView(button("ابدأ اللعبة", true) { showMillionaire() })
        }
        root.addView(game)
        root.addView(space(8))
        root.addView(button("المسابقات الإسلامية", false) { showQuiz() })
        root.addView(button("مواقيت الصلاة والقبلة", false) { showPrayerTimes() })
        root.addView(button("الأسماء الحسنى", false) { showNames() })
        root.addView(button("ملاحظاتي", false) { showNotes() })
        root.addView(button(if (lightTheme) "الوضع الفاتح • التبديل للداكن" else "الوضع الداكن • التبديل للفاتح", false) {
            lightTheme = !lightTheme
            getSharedPreferences("deeni", MODE_PRIVATE).edit().putBoolean("light_theme", lightTheme).apply()
            applyTheme()
            showHome()
        })
        root.addView(label("التسميع الحالي استرشادي باستخدام التعرف الصوتي العام، وليس تصحيحًا متخصصًا للتلاوة أو التجويد.", 11f, muted))
    }

    private fun showProgress() {
        currentPage = "progress"
        val prefs = getSharedPreferences("deeni", MODE_PRIVATE)
        val savedAyahs = prefs.getStringSet("saved_ayahs", emptySet())?.size ?: 0
        val bookmarks = prefs.getStringSet("mushaf_bookmarks", emptySet())?.size ?: 0
        val page = prefs.getInt("mushaf_page", 1)
        base("تقدمك", "ملخص تقدمك المحفوظ على هذا الجهاز")
        card("موضع القراءة", "آخر صفحة محفوظة: " + page, "۞") { showQuran() }
        card("الآيات المحفوظة", savedAyahs.toString() + " آية", "☆") { showSavedAyahs() }
        card("علامات المصحف", bookmarks.toString() + " صفحة", "▤") { showMushafBookmarks() }
        card("خطة الحفظ", "تابع مهامك اليومية", "✦") { showRevision() }
        card("التسبيح", "العداد الحالي: " + tasbeeh, "◉") { showTasbeeh() }
    }

    private fun showMore() {
        currentPage = "more"
        base("المزيد", "مكتبة ديني وأدوات العبادة")
        card("الحديث والسيرة", "أحاديث ومحطات من السيرة", "📜") { showIslamicLibrary() }
        card("قصص الأنبياء", "قصص مختارة", "✧") { showProphetStories() }
        card("قصص الصحابة", "سير مختارة", "☼") { showCompanionStories() }
        card("الأذكار والأدعية", "أذكار مختارة", "◉") { showAdhkar() }
        card("التفسير", "معاني الآيات", "📖") { showTafsir() }
        card("التجويد", "أحكام التلاوة", "۞") { showTajweed() }
        card("الفقه", "مبادئ عامة", "⚖") { showFiqhBasics() }
        card("مواقيت الصلاة", "مواقيت حسب المدينة", "◷") { showPrayerTimes() }
        card("القبلة", "اتجاه القبلة", "🧭") { showQibla() }
        card("الأسماء الحسنى", "معانٍ موجزة", "✧") { showNames() }
        card("الملاحظات", "فوائدك الخاصة", "✎") { showNotes() }
        card("لعبة من سيربح المليون", "15 سؤالًا إسلاميًا", "★") { showMillionaire() }
        card("تبديل المظهر", if (lightTheme) "الوضع الفاتح" else "الوضع الداكن", "⚙") {
            lightTheme = !lightTheme
            getSharedPreferences("deeni", MODE_PRIVATE).edit().putBoolean("light_theme", lightTheme).apply()
            applyTheme()
            showMore()
        }
    }

    private fun homeTile(title: String, subtitle: String, icon: String, action: () -> Unit) {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = shape(panel, 20f)
            setPadding(10, 15, 10, 15)
            addView(label(icon, 25f, gold, true).apply { gravity = Gravity.CENTER })
            addView(label(title, 15f, cream, true).apply { gravity = Gravity.CENTER })
            addView(label(subtitle, 11f, muted).apply { gravity = Gravity.CENTER })
            setOnClickListener { action() }
        }
        tile.minimumHeight = 112
        val params = LinearLayout.LayoutParams(0, -2, 1f)
        params.setMargins(5, 5, 5, 5)
        homeTileRow?.addView(tile, params)
    }

    private var homeTileRow: LinearLayout? = null

    private fun homeSection(title: String) {
        root.addView(space(12))
        root.addView(label(title, 18f, cream, true))
    }

    private var mushafPage = 1
    private var mushafWebView: WebView? = null
    private var pageStatus: TextView? = null
    private var ayahStatus: TextView? = null

    private inner class MushafBridge {
        @JavascriptInterface
        fun onAyahSelected(surah: Int, ayah: Int) {
            if (surah <= 0 || ayah <= 0) return
            runOnUiThread { showSelectedAyah(surah, ayah) }
        }
        @JavascriptInterface
        fun onPageReady() {
            runOnUiThread {
                pageStatus?.text = "صفحة $mushafPage من 604 • مصحف المدينة النبوية"
                val pending = pendingHighlight
                if (pending != null) {
                    pendingHighlight = null
                    highlightMushafAyah(pending.first, pending.second)
                }
            }
        }
        @JavascriptInterface
        fun onPageError() {
            runOnUiThread {
                pageStatus?.text = "تعذر تحميل صفحة المصحف $mushafPage. تحقق من الإنترنت ثم أعد المحاولة."
            }
        }
    }

    private fun highlightMushafAyah(surah: Int, ayah: Int) {
        mushafWebView?.evaluateJavascript("window.highlightAyah && window.highlightAyah($surah,$ayah)", null)
    }

    private fun showSelectedAyah(surah: Int, ayah: Int) {
        if (!QuranReference.isValid(surah, ayah)) return
        selectedAyah = surah to ayah
        selectedAyahText = null
        ayahStatus?.text = "جارٍ تحميل الآية $surah:$ayah…"
        Thread {
            val result = QuranPageSource.loadAyahDetails(surah, ayah)
            runOnUiThread {
                if (currentPage != "quran") return@runOnUiThread
                if (result.isSuccess) {
                    val (text, page) = result.getOrThrow()
                    selectedAyahText = text
                    ayahStatus?.text = "الآية $surah:$ayah\n$text"
                    if (page != mushafPage) {
                        pendingHighlight = surah to ayah
                        loadMushafPage(page)
                    } else {
                        highlightMushafAyah(surah, ayah)
                    }
                } else {
                    ayahStatus?.text = "تعذر تحميل نص الآية المحددة."
                }
            }
        }.start()
    }

    private fun showQuran(startMic: Boolean = false) {
        currentPage = "quran"
        mushafPage = getSharedPreferences("deeni", MODE_PRIVATE).getInt("mushaf_page", 1).coerceIn(1, 604)
        base("القرآن الكريم", "اقرأ • استمع • سمّع • راجع")

        val readerBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = shape(panel, 18f)
            setPadding(14, 9, 14, 9)
        }
        val readerInfo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        readerInfo.addView(label("القارئ الحالي", 11f, muted))
        readerInfo.addView(label(selectedReader, 15f, cream, true))
        readerBar.addView(readerInfo, LinearLayout.LayoutParams(0, -2, 1f))
        readerBar.addView(button("تغيير القارئ", false) { chooseReader() })
        root.addView(readerBar)
        root.addView(space(9))

        val pageBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        pageBar.addView(button("‹", false) { loadMushafPage(mushafPage - 1) }, LinearLayout.LayoutParams(52, -2))
        pageBar.addView(button("الصفحة $mushafPage / 604", true) {
            val input = EditText(this@MainActivity).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                setText(mushafPage.toString())
                gravity = Gravity.CENTER
            }
            android.app.AlertDialog.Builder(this@MainActivity)
                .setTitle("الانتقال إلى صفحة")
                .setView(input)
                .setPositiveButton("فتح") { _, _ -> loadMushafPage(input.text.toString().toIntOrNull() ?: mushafPage) }
                .setNegativeButton("إلغاء", null).show()
        }, LinearLayout.LayoutParams(0, -2, 1f))
        pageBar.addView(button("›", false) { loadMushafPage(mushafPage + 1) }, LinearLayout.LayoutParams(52, -2))
        root.addView(pageBar)

        val indexActions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        indexActions.addView(button("فهرس السور", false) { showMushafSurahIndex() },
            LinearLayout.LayoutParams(0, -2, 1f))
        indexActions.addView(button("فهرس الأجزاء", false) { showJuzIndex() },
            LinearLayout.LayoutParams(0, -2, 1f))
        indexActions.addView(button("علاماتي", false) { showMushafBookmarks() },
            LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(indexActions)

        pageStatus = label("جارٍ تحميل صفحة المصحف…", 12f, muted)
        root.addView(pageStatus!!)
        mushafWebView = WebView(this).apply {
            setBackgroundColor(Color.rgb(255, 253, 248))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.loadsImagesAutomatically = true
            addJavascriptInterface(MushafBridge(), "DeeniMushaf")
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean = true
                override fun onReceivedError(
                    view: WebView?,
                    request: android.webkit.WebResourceRequest?,
                    error: android.webkit.WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        pageStatus?.text = "تعذر فتح صفحة المصحف. تحقق من الإنترنت ثم أعد المحاولة."
                    }
                }
            }
        }
        var swipeStartX = 0f
        mushafWebView!!.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> { swipeStartX = event.x; false }
                android.view.MotionEvent.ACTION_UP -> {
                    val delta = event.x - swipeStartX
                    if (kotlin.math.abs(delta) > 90f) {
                        if (delta < 0) loadMushafPage(mushafPage + 1) else loadMushafPage(mushafPage - 1)
                        true
                    } else false
                }
                else -> false
            }
        }
        val mushafFrame = FrameLayout(this).apply {
            background = shape(Color.rgb(255, 253, 248), 12f)
            setPadding(4, 4, 4, 4)
            addView(mushafWebView!!, FrameLayout.LayoutParams(-1, (resources.displayMetrics.heightPixels / resources.displayMetrics.density * 0.68f).toInt().coerceAtLeast(480)))
        }
        val floatingMic = button("🎙", true) { showMicOptions() }.apply {
            textSize = 25f
            minWidth = 68
            minHeight = 68
            elevation = 12f
            setOnLongClickListener { showMicOptions(); true }
            contentDescription = "اضغط مطولًا للتسميع أو الاستماع أو اختبار التلاوة"
        }
        val micParams = FrameLayout.LayoutParams(68, 68, Gravity.BOTTOM or Gravity.END).apply {
            setMargins(0, 0, 14, 14)
        }
        mushafFrame.addView(floatingMic, micParams)
        root.addView(mushafFrame)
        root.addView(label("اضغط على الآية لتحديدها وبدء التسميع عليها.", 12f, muted))
        ayahStatus = label("اختر آية من المصحف", 18f, cream, true)
        ayahStatus!!.background = shape(panel, 16f)
        ayahStatus!!.setPadding(14, 14, 14, 14)
        root.addView(ayahStatus!!)

        root.addView(space(8))
        root.addView(space(6))
        val utilityActions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        utilityActions.addView(button("حفظ الآية", false) {
            val selected = selectedAyah
            if (selected == null) {
                Toast.makeText(this, "حدد آية أولًا.", Toast.LENGTH_SHORT).show()
            } else {
                val prefs = getSharedPreferences("deeni", MODE_PRIVATE)
                val saved = prefs.getStringSet("saved_ayahs", emptySet())?.toMutableSet() ?: mutableSetOf()
                saved.add("\${selected.first}:\${selected.second}")
                prefs.edit().putStringSet("saved_ayahs", saved).apply()
                Toast.makeText(this, "تم حفظ الآية.", Toast.LENGTH_SHORT).show()
            }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        utilityActions.addView(button("علامة مرجعية", false) {
            val prefs = getSharedPreferences("deeni", MODE_PRIVATE)
            val saved = prefs.getStringSet("mushaf_bookmarks", emptySet())?.toMutableSet() ?: mutableSetOf()
            saved.add(mushafPage.toString())
            prefs.edit().putStringSet("mushaf_bookmarks", saved).apply()
            Toast.makeText(this, "تم حفظ موضع الصفحة.", Toast.LENGTH_SHORT).show()
        }, LinearLayout.LayoutParams(0, -2, 1f))
        utilityActions.addView(button("التفسير", false) { showTafsir() }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(utilityActions)

        root.addView(space(12))
        root.addView(label("التسميع الصوتي", 19f, cream, true))
        root.addView(label("يلتقط الهاتف الكلام ويقارنه نصيًا بالآية المحددة. النتيجة استرشادية وليست تصحيحًا متخصصًا للتلاوة أو التجويد.", 12f, muted))
        recitationStatusView = label("الميكروفون متوقف", 13f, gold, true)
        root.addView(recitationStatusView!!)
        recitationTextView = TextView(this).apply {
            text = "سيظهر هنا النص الذي التقطه الجهاز."
            textSize = 16f
            setTextColor(cream)
            background = shape(panel, 16f)
            setPadding(14, 14, 14, 14)
        }
        root.addView(recitationTextView!!)
        root.addView(button("ابدأ التسميع / أعد المحاولة", true) {
            recitationStatusView?.text = "جارٍ تشغيل الميكروفون…"
            recitationTextView?.text = "ابدأ القراءة؛ سيظهر النص الذي التقطه الهاتف هنا."
            recitationStatusView?.let { status -> recitationTextView?.let { live ->
                beginRecognition(status, live)
            } }
        })
        root.addView(button("إيقاف الميكروفون", false) {
            pendingRecognitionAfterPermission = false
            stopRecognition()
            recitationStatusView?.text = "تم إيقاف الميكروفون"
        })
        root.addView(button("حفظ الصفحة كآخر موضع قراءة", false) {
            getSharedPreferences("deeni", MODE_PRIVATE).edit().putInt("mushaf_page", mushafPage).apply()
            Toast.makeText(this, "تم حفظ الصفحة $mushafPage", Toast.LENGTH_SHORT).show()
        })
        playbackStatus = label("التلاوة متوقفة", 12f, muted)
        root.addView(playbackStatus!!)
        root.addView(button("إيقاف مؤقت / متابعة التلاوة", false) { toggleQuranPlayback() })
        root.addView(button("تكرار الآية الحالية", false) {
            val timing = timingRows.firstOrNull { it.ayah == currentTimedAyah }
            if (timing == null) {
                Toast.makeText(this, "ابدأ تلاوة تتوفر لها بيانات توقيت الآيات أولًا.", Toast.LENGTH_LONG).show()
            } else {
                repeatTiming = if (repeatTiming == null) timing else null
                playbackStatus?.text = if (repeatTiming != null) "تكرار الآية \${timing.ayah} مفعّل" else "تم إيقاف التكرار"
            }
        })
        root.addView(button("تغيير سرعة التلاوة: \${playbackSpeed}×", false) {
            playbackSpeed = when (playbackSpeed) { 1.0f -> 0.8f; 0.8f -> 1.2f; else -> 1.0f }
            try {
                quranPlayer?.let { it.playbackParams = it.playbackParams.setSpeed(playbackSpeed) }
                playbackStatus?.text = "سرعة التلاوة \${playbackSpeed}×"
            } catch (_: Exception) {
                Toast.makeText(this, "تعذر تغيير السرعة على هذا الجهاز.", Toast.LENGTH_SHORT).show()
            }
        })
        root.addView(label("ملاحظة الخصوصية: لا يحفظ التطبيق تسجيلًا صوتيًا. قد تستخدم خدمة التعرف الصوتي في الهاتف مزودًا خارجيًا لمعالجة الكلام.", 11f, muted))
        loadMushafPage(mushafPage)
        if (startMic) root.post { beginRecognition(recitationStatusView!!, recitationTextView!!) }
    }

    private fun showMicOptions() {
        val options = arrayOf(
            "التسميع — ابدأ القراءة مباشرة",
            "استمع للقارئ — اختر السورة والتلاوة",
            "اختبار التلاوة — اقرأ ثم راجع النتيجة الاسترشادية"
        )
        android.app.AlertDialog.Builder(this)
            .setTitle("اختر وضع الميكروفون")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        recitationStatusView?.text = "وضع التسميع: أستمع إلى قراءتك"
                        recitationTextView?.text = "ابدأ القراءة؛ لا يلزم تحديد آية مسبقًا. يمكنك تحديد آية لاحقًا للمقارنة النصية."
                        root.post { recitationStatusView?.let { status -> recitationTextView?.let { live -> beginRecognition(status, live) } } }
                    }
                    1 -> showSurahPicker()
                    2 -> {
                        recitationStatusView?.text = if (selectedAyah == null) {
                            "اختبار استرشادي: اقرأ ما تريد، وحدد آية إذا رغبت بالمقارنة"
                        } else {
                            "اختبار استرشادي للآية المحددة"
                        }
                        recitationTextView?.text = "اقرأ بوضوح؛ ستظهر الكلمات التي التقطها الهاتف. المقارنة مع نص الآية اختيارية وليست تصحيحًا للتجويد."
                        root.post { recitationStatusView?.let { status -> recitationTextView?.let { live -> beginRecognition(status, live) } } }
                    }
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun mushafHtml(page: Int, svgMarkup: String): String {
        val safePage = page.coerceIn(1, 604)
        val cleanSvg = svgMarkup.replace(Regex("<\\?xml[^>]*\\?>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<!DOCTYPE[^>]*>", RegexOption.IGNORE_CASE), "")
        val encodedSvg = Base64.encodeToString(cleanSvg.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return """
            <!doctype html>
            <html lang="ar" dir="rtl">
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=4">
              <style>
                html,body{margin:0;padding:0;background:#fffdf8}
                #page{width:100%;margin:0 auto}
                svg{display:block;width:100%;height:auto;max-width:100%}
                .ayahPolygon{cursor:pointer;pointer-events:all}
              </style>
            </head>
            <body>
              <div id="page"></div>
              <script>
                (function(){
                  const host=document.getElementById('page');
                  try{
                    const binary=atob('$encodedSvg');
                    const bytes=Uint8Array.from(binary,function(c){return c.charCodeAt(0);});
                    const source=new TextDecoder('utf-8').decode(bytes);
                    const parsed=new DOMParser().parseFromString(source,'image/svg+xml');
                    const svg=parsed.documentElement;
                    if(!svg || svg.localName!=='svg' || parsed.querySelector('parsererror')) throw new Error('SVG parse error');
                    svg.querySelectorAll('script,foreignObject,iframe,object,embed,audio,video').forEach(function(node){node.remove();});
                    [svg].concat(Array.from(svg.querySelectorAll('*'))).forEach(function(node){
                      Array.from(node.attributes||[]).forEach(function(attr){
                        const name=attr.name.toLowerCase();
                        const value=attr.value.trim();
                        if(name.startsWith('on') || ((name==='href' || name==='xlink:href') && value && !value.startsWith('#')) || (name==='style' && /url\s*\(|expression\s*\(/i.test(value))) node.removeAttribute(attr.name);
                      });
                    });
                    svg.querySelectorAll('style').forEach(function(node){
                      node.textContent=node.textContent.replace(/@import[^;]*;?/gi,'').replace(/url\s*\([^)]*\)/gi,'');
                    });
                    host.replaceChildren(document.importNode(svg,true));
                    const mounted=host.querySelector('svg');
                    mounted.style.width='100%';
                    mounted.style.height='auto';
                    mounted.setAttribute('preserveAspectRatio','xMidYMid meet');
                    mounted.querySelectorAll('.ayahPolygon').forEach(function(path){
                      path.style.pointerEvents='all';
                      path.addEventListener('click',function(){
                        mounted.querySelectorAll('.ayahPolygon').forEach(function(p){p.setAttribute('fill-opacity','0');});
                        path.setAttribute('fill','#d6b76f');
                        path.setAttribute('fill-opacity','0.38');
                        const surah=Number(path.getAttribute('surah'));
                        const ayah=Number(path.getAttribute('ayah'));
                        if(surah>0&&ayah>0&&window.DeeniMushaf) window.DeeniMushaf.onAyahSelected(surah,ayah);
                      });
                    });
                    window.highlightAyah=function(surah,ayah){
                      const found=Array.from(mounted.querySelectorAll('.ayahPolygon')).find(function(p){
                        return Number(p.getAttribute('surah'))===surah && Number(p.getAttribute('ayah'))===ayah;
                      });
                      if(!found) return false;
                      mounted.querySelectorAll('.ayahPolygon').forEach(function(p){p.setAttribute('fill-opacity','0');});
                      found.setAttribute('fill','#d6b76f');
                      found.setAttribute('fill-opacity','0.38');
                      return true;
                    };
                    if(window.DeeniMushaf) window.DeeniMushaf.onPageReady();
                  }catch(error){
                    host.innerHTML='<p style="font:18px sans-serif;color:#704b20;text-align:center;padding:24px">تعذر عرض صفحة المصحف. أعد المحاولة بعد التحقق من الاتصال.</p>';
                    if(window.DeeniMushaf) window.DeeniMushaf.onPageError();
                  }
                })();
              </script>
            </body>
            </html>
        """.trimIndent()
    }

    private fun loadMushafPage(requested: Int) {
        val page = requested.coerceIn(1, 604)
        mushafPage = page
        pageStatus?.text = "جارٍ تحميل صفحة المصحف $page من 604…"
        ayahStatus?.text = "اضغط على موضع آية في الصفحة لعرض نصها."
        getSharedPreferences("deeni", MODE_PRIVATE).edit().putInt("mushaf_page", page).apply()
        Thread {
            val result = runCatching {
                val url = URL("https://raw.githubusercontent.com/quran-ws/quran-svg/main/mushafs/hafs/kfqc/svg/" + page.toString().padStart(3, '0') + ".svg")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 20000
                    setRequestProperty("User-Agent", "DeeniAndroid/1.2")
                }
                try {
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        throw IllegalStateException("HTTP " + connection.responseCode)
                    }
                    connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } finally {
                    connection.disconnect()
                }
            }
            runOnUiThread {
                if (currentPage != "quran" || mushafPage != page) return@runOnUiThread
                if (result.isSuccess) {
                    mushafWebView?.loadDataWithBaseURL(
                        "https://appassets.androidplatform.net/",
                        mushafHtml(page, result.getOrThrow()),
                        "text/html",
                        "UTF-8",
                        null
                    )
                } else {
                    pageStatus?.text = "تعذر تحميل صفحة المصحف $page. تحقق من الإنترنت ثم اضغط إعادة المحاولة."
                    mushafWebView?.loadDataWithBaseURL(
                        "https://appassets.androidplatform.net/",
                        "<html dir='rtl'><body style='background:#f8f3e4;color:#704b20;text-align:center;font:18px sans-serif;padding:24px'>تعذر تحميل صفحة المصحف.<br>ارجع واضغط رقم الصفحة لإعادة المحاولة.</body></html>",
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            }
        }.start()
    }

    private fun beginRecognition(status: TextView, live: TextView) {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "التعرف الصوتي غير متاح على هذا الجهاز"; return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingRecognitionAfterPermission = true
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 77)
            status.text = "بانتظار إذن الميكروفون…"; return
        }
        pendingRecognitionAfterPermission = false
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
                renderTranscript(words, live)
                status.text = "انتهت جولة الالتقاط — راجع النص المسموع بنفسك"
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val words = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                renderTranscript(words, live)
                status.text = "متابعة مباشرة للنص الملتقط…"
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        try { recognizer?.startListening(intent) }
        catch (_: Exception) { status.text = "تعذر بدء الميكروفون؛ تحقق من الإذن وحاول مجددًا" }
    }

    private fun renderTranscript(spoken: String, live: TextView) {
        if (spoken.isBlank()) return
        val reference = selectedAyahText
        if (reference.isNullOrBlank()) {
            live.text = "النص الذي التقطه الجهاز:\n$spoken\n\nيمكنك تحديد آية من المصحف لعرض مقارنة نصية استرشادية، أو متابعة القراءة دون تحديد آية. هذا التفريغ ليس تصحيحًا قرآنيًا أو حكمًا على صحة التلاوة."
            return
        }
        val alignment = QuranWordAlignmentEngine.align(reference, spoken)
        val missing = alignment.missingExpected.take(8)
            .joinToString("، ") { "${it.index + 1}: ${it.text}" }.ifBlank { "لا توجد" }
        val extra = alignment.extraTranscript.take(8)
            .joinToString("، ") { "${it.index + 1}: ${it.text}" }.ifBlank { "لا توجد" }
        val matchedPositions = alignment.matches.takeLast(12)
            .joinToString("، ") { (it.expected.index + 1).toString() }.ifBlank { "لا توجد بعد" }
        val matched = "${alignment.matches.size} من ${alignment.expectedWordCount}"
        live.text = "النص الذي التقطه الجهاز:\n$spoken\n\nمحاذاة نصية مبدئية لكلمات الآية المحددة:\nمواضع الكلمات المتطابقة في الآية: $matchedPositions\nكلمات لم تظهر في النص الملتقط (رقم الكلمة: النص): $missing\nكلمات إضافية في النص الملتقط: $extra\nالكلمات المتطابقة بالترتيب: $matched\n\nتنبيه مهم: أرقام المواضع هي ترتيب الكلمات داخل نص الآية فقط، وليست تحديدًا بصريًا على صفحة المصحف. قد يخطئ محرك التعرف الصوتي في سماع القرآن؛ هذه مقارنة استرشادية بين نصين، وليست حكمًا بأنك أخطأت في التلاوة، ولا تقيس التجويد أو مخارج الحروف."
    }

    private fun stopRecognition() {
        try { recognizer?.stopListening(); recognizer?.cancel(); recognizer?.destroy() } catch (_: Exception) {}
        recognizer = null
    }

    private fun chooseReader() {
        Toast.makeText(this, "جارٍ تحميل قائمة القراء والتلاوات المتاحة…", Toast.LENGTH_SHORT).show()
        Thread {
            val result = ReciterSource.loadReciters()
            runOnUiThread {
                if (result.isFailure) {
                    Toast.makeText(this, "تعذر تحميل القراء. تحقق من الإنترنت.", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                val reads = result.getOrThrow()
                if (reads.isEmpty()) {
                    Toast.makeText(this, "لا توجد تلاوات متاحة حاليًا.", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                val names = reads.map { "${it.reciterName} — ${it.readName}" }.toTypedArray()
                android.app.AlertDialog.Builder(this)
                    .setTitle("اختر القارئ والرواية")
                    .setItems(names) { dialog, which ->
                        val selected = reads[which]
                        selectedReader = selected.reciterName
                        getSharedPreferences("deeni", MODE_PRIVATE).edit()
                            .putString("reader", selectedReader)
                            .putString("audio_server", selected.audioServer)
                            .putString("audio_surahs", selected.surahIds.joinToString(","))
                            .putInt("audio_read_id", selected.readId)
                            .putInt("timing_read_id", selected.timingReadId ?: -1)
                            .putString("read_name", selected.readName)
                            .apply()
                        dialog.dismiss()
                        if (currentPage == "quran") showQuran()
                    }
                    .setNegativeButton("إغلاق", null)
                    .show()
            }
        }.start()
    }

    private fun showMushafSurahIndex() {
        val labels = mushafSurahNames.mapIndexed { index, name ->
            "${index + 1}. سورة $name"
        }.toTypedArray()
        android.app.AlertDialog.Builder(this)
            .setTitle("فهرس سور القرآن الكريم")
            .setItems(labels) { dialog, which ->
                dialog.dismiss()
                pageStatus?.text = "جارٍ تحديد بداية سورة ${mushafSurahNames[which]}…"
                Thread {
                    val result = QuranPageSource.loadAyahDetails(which + 1, 1)
                    runOnUiThread {
                        if (currentPage != "quran") return@runOnUiThread
                        if (result.isSuccess) {
                            val (text, page) = result.getOrThrow()
                            selectedAyah = (which + 1) to 1
                            selectedAyahText = text
                            loadMushafPage(page)
                        } else {
                            pageStatus?.text = "تعذر تحديد صفحة السورة. تحقق من الإنترنت وحاول مجددًا."
                        }
                    }
                }.start()
            }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    private fun showJuzIndex() {
        val labels = juzStarts.map { (juz, start) -> "الجزء $juz • سورة ${mushafSurahNames[start.first - 1]} • آية ${start.second}" }.toTypedArray()
        android.app.AlertDialog.Builder(this)
            .setTitle("فهرس أجزاء القرآن الكريم")
            .setItems(labels) { dialog, which ->
                dialog.dismiss()
                val (juz, start) = juzStarts[which]
                pageStatus?.text = "جارٍ فتح بداية الجزء $juz…"
                Thread {
                    val result = QuranPageSource.loadAyahDetails(start.first, start.second)
                    runOnUiThread {
                        if (currentPage != "quran") return@runOnUiThread
                        if (result.isSuccess) {
                            val (text, page) = result.getOrThrow()
                            selectedAyah = start.first to start.second
                            selectedAyahText = text
                            loadMushafPage(page)
                        } else {
                            pageStatus?.text = "تعذر تحديد صفحة الجزء $juz. تحقق من الإنترنت وحاول مجددًا."
                        }
                    }
                }.start()
            }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    private fun showSurahPicker() {
        val prefs = getSharedPreferences("deeni", MODE_PRIVATE)
        val server = prefs.getString("audio_server", null)
        if (server.isNullOrBlank()) {
            Toast.makeText(this, "اختر القارئ أولًا", Toast.LENGTH_SHORT).show()
            chooseReader()
            return
        }
        val surahs = mushafSurahNames
        val labels = surahs.mapIndexed { index, name -> "${index + 1}. سورة $name" }.toTypedArray()
        android.app.AlertDialog.Builder(this)
            .setTitle("اختر السورة لتشغيل التلاوة")
            .setItems(labels) { dialog, which ->
                dialog.dismiss()
                playQuranSurah(which + 1)
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun playQuranSurah(surah: Int) {
        val prefs = getSharedPreferences("deeni", MODE_PRIVATE)
        val server = prefs.getString("audio_server", null) ?: return
        val supported = prefs.getString("audio_surahs", "")!!.split(',').mapNotNull { it.toIntOrNull() }.toSet()
        if (surah !in supported) {
            Toast.makeText(this, "هذه السورة غير متاحة بهذه التلاوة.", Toast.LENGTH_LONG).show()
            return
        }
        stopQuranAudio()
        playbackSurah = surah
        currentTimedAyah = -1
        repeatTiming = null
        playbackSpeed = 1.0f
        timingRows = emptyList()
        playbackStatus?.text = "جارٍ تجهيز التلاوة…"
        val url = server.trimEnd('/') + "/" + surah.toString().padStart(3, '0') + ".mp3"
        val player = MediaPlayer()
        quranPlayer = player
        val timingId = prefs.getInt("timing_read_id", -1)
        if (timingId > 0) {
            Thread {
                val timings = ReciterSource.loadTimings(surah, timingId)
                if (timings.isSuccess) {
                    val loadedTimings = timings.getOrThrow()
                    runOnUiThread {
                        // Ignore a late response if the user has already started another surah.
                        if (quranPlayer === player && playbackSurah == surah && currentPage == "quran") {
                            timingRows = loadedTimings
                        }
                    }
                }
            }.start()
        }
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        player.setOnPreparedListener {
            if (quranPlayer !== player) return@setOnPreparedListener
            player.start()
            playbackStatus?.text = "تُتلى السورة رقم $surah — ${selectedReader}"
            audioHandler.removeCallbacks(audioTicker)
            audioHandler.post(audioTicker)
        }
        player.setOnCompletionListener {
            if (quranPlayer === player) {
                playbackStatus?.text = "انتهت التلاوة"
                audioHandler.removeCallbacks(audioTicker)
            }
        }
        player.setOnErrorListener { _, _, _ ->
            playbackStatus?.text = "تعذر تشغيل الصوت؛ تحقق من الاتصال أو اختر تلاوة أخرى."
            stopQuranAudio()
            true
        }
        try {
            player.setDataSource(url)
            player.prepareAsync()
        } catch (_: Exception) {
            playbackStatus?.text = "تعذر فتح ملف التلاوة."
            stopQuranAudio()
        }
    }

    private val audioTicker = object : Runnable {
        override fun run() {
            val player = quranPlayer ?: return
            if (player.isPlaying && timingRows.isNotEmpty()) {
                var position = try { player.currentPosition } catch (_: Exception) { -1 }
                val repeat = repeatTiming
                if (repeat != null && position >= repeat.endMs) {
                    try {
                        player.seekTo(repeat.startMs)
                        position = repeat.startMs
                    } catch (_: Exception) {}
                }
                val active = timingRows.firstOrNull { position >= it.startMs && position < it.endMs }
                if (active != null && active.ayah != currentTimedAyah) {
                    currentTimedAyah = active.ayah
                    showSelectedAyah(playbackSurah, active.ayah)
                }
            }
            if (quranPlayer === player && player.isPlaying) audioHandler.postDelayed(this, 250)
        }
    }

    private fun toggleQuranPlayback() {
        val player = quranPlayer
        if (player == null) {
            showSurahPicker()
            return
        }
        try {
            if (player.isPlaying) {
                player.pause()
                playbackStatus?.text = "التلاوة متوقفة مؤقتًا"
                audioHandler.removeCallbacks(audioTicker)
            } else {
                player.start()
                playbackStatus?.text = "استئناف التلاوة"
                audioHandler.post(audioTicker)
            }
        } catch (_: Exception) {
            playbackStatus?.text = "تعذر التحكم في التلاوة."
        }
    }

    private fun stopQuranAudio() {
        audioHandler.removeCallbacks(audioTicker)
        val player = quranPlayer
        quranPlayer = null
        try { player?.stop() } catch (_: Exception) {}
        try { player?.release() } catch (_: Exception) {}
        timingRows = emptyList()
        currentTimedAyah = -1
        repeatTiming = null
        playbackSpeed = 1.0f
        playbackStatus?.text = "التلاوة متوقفة"
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
            "الكلمة الطيبة صدقة." to "صحيح البخاري، حديث 2989؛ وصحيح مسلم، حديث 1009.",
            "يسروا ولا تعسروا، وبشروا ولا تنفروا." to "صحيح البخاري، حديث 69؛ وصحيح مسلم، حديث 1734.",
            "من لا يرحم لا يُرحم." to "صحيح البخاري، حديث 5997؛ وصحيح مسلم، حديث 2318.",
            "الطهور شطر الإيمان." to "صحيح مسلم، حديث 223.",
            "المسلم من سلم المسلمون من لسانه ويده." to "صحيح البخاري، حديث 10؛ وصحيح مسلم، حديث 40.",
            "خيركم من تعلم القرآن وعلمه." to "صحيح البخاري، حديث 5027.",
            "من سلك طريقًا يلتمس فيه علمًا، سهل الله له به طريقًا إلى الجنة." to "صحيح مسلم، حديث 2699.",
            "لا تغضب." to "صحيح البخاري، حديث 6116.",
            "إن الله كتب الإحسان على كل شيء." to "صحيح مسلم، حديث 1955.",
            "تبسمك في وجه أخيك لك صدقة." to "جامع الترمذي، حديث 1956؛ حسنه الترمذي.",
            "الراحمون يرحمهم الرحمن، ارحموا من في الأرض يرحمكم من في السماء." to "جامع الترمذي، حديث 1924؛ وسنن أبي داود، حديث 4941."
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
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
        if (prefs.getString("revision_day", null) != today) {
            prefs.edit().apply {
                prefs.all.keys.filter { it.startsWith("revision_") && it != "revision_day" }.forEach { remove(it) }
                putString("revision_day", today)
            }.apply()
        }
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
        data class Dhikr(val category: String, val text: String, val target: Int, val source: String)
        val entries = listOf(
            Dhikr("أذكار الصباح والمساء", "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ", 3, "أبو داود 5088؛ الترمذي 3388"),
            Dhikr("أذكار الصباح والمساء", "رَضِيتُ بِاللَّهِ رَبًّا، وَبِالْإِسْلَامِ دِينًا، وَبِمُحَمَّدٍ ﷺ نَبِيًّا", 3, "أبو داود 5072؛ الترمذي 3389"),
            Dhikr("أذكار الصباح والمساء", "سَيِّدُ الِاسْتِغْفَارِ: اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي، فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ", 1, "صحيح البخاري 6306"),
            Dhikr("عند نزول منزل", "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ", 1, "صحيح مسلم 2708"),
            Dhikr("ذكر عام", "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", 100, "صحيح البخاري 6405؛ صحيح مسلم 2691"),
            Dhikr("ذكر عام", "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ", 100, "صحيح البخاري 3293؛ صحيح مسلم 2691"),
            Dhikr("بعد الصلاة", "أَسْتَغْفِرُ اللَّهَ", 3, "صحيح مسلم 591"),
            Dhikr("بعد الصلاة", "اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ، تَبَارَكْتَ يَا ذَا الْجَلَالِ وَالْإِكْرَامِ", 1, "صحيح مسلم 591")
        )
        val prefs = getSharedPreferences("deeni", MODE_PRIVATE)
        base("الأذكار والأدعية", "أذكار مختارة مع عددها ومراجعها — الأعداد المعروضة وفق الروايات المذكورة")
        var lastCategory = ""
        entries.forEachIndexed { index, entry ->
            if (entry.category != lastCategory) {
                lastCategory = entry.category
                root.addView(label(lastCategory, 19f, gold, true))
            }
            val count = label("", 17f, gold, true).apply { gravity = Gravity.CENTER }
            fun refreshCount() { count.text = "${prefs.getInt("verified_dhikr_$index", 0)} / ${entry.target}" }
            root.addView(label(entry.text, 19f, cream, true))
            root.addView(count)
            root.addView(label("المرجع: ${entry.source}", 12f, muted))
            root.addView(button("＋ تسجيل الذكر", true) {
                val next = (prefs.getInt("verified_dhikr_$index", 0) + 1).coerceAtMost(entry.target)
                prefs.edit().putInt("verified_dhikr_$index", next).apply()
                refreshCount()
            })
            root.addView(button("إعادة عداد هذا الذكر", false) {
                prefs.edit().remove("verified_dhikr_$index").apply()
                refreshCount()
            })
            root.addView(space(10))
            refreshCount()
        }
        root.addView(label("تنبيه: هذه مجموعة مختارة وليست حصرًا لكل الأذكار. راجع نصوص الأحاديث وطرقها في مصادرها، ولا تجعل العداد بديلًا عن حضور القلب.", 12f, muted))
        root.addView(button("الرئيسية", false) { showHome() })
    }

    private fun showMillionaire() {
        val questions = listOf(
            Triple("كم عدد الصلوات المفروضة في اليوم والليلة؟", listOf("ثلاث", "أربع", "خمس", "ست"), 2),
            Triple("ما أول سورة في ترتيب المصحف؟", listOf("البقرة", "الفاتحة", "الإخلاص", "الناس"), 1),
            Triple("ما أطول سورة في القرآن الكريم؟", listOf("آل عمران", "النساء", "البقرة", "المائدة"), 2),
            Triple("من النبي الذي التقمه الحوت؟", listOf("موسى عليه السلام", "يونس عليه السلام", "يوسف عليه السلام", "نوح عليه السلام"), 1),
            Triple("في أي شهر يصوم المسلمون؟", listOf("شعبان", "رمضان", "شوال", "محرم"), 1),
            Triple("ما أول ما نزل من القرآن؟", listOf("يا أيها المدثر", "اقرأ باسم ربك", "الفاتحة", "قل هو الله أحد"), 1),
            Triple("كم عدد أركان الإسلام؟", listOf("أربعة", "خمسة", "ستة", "سبعة"), 1),
            Triple("في أي غار كان النبي ﷺ يتعبد قبل البعثة؟", listOf("ثور", "حراء", "أحد", "النور"), 1),
            Triple("من النبي الذي رفع قواعد الكعبة مع ابنه؟", listOf("إبراهيم عليه السلام", "يعقوب عليه السلام", "داود عليه السلام", "زكريا عليه السلام"), 0),
            Triple("ما السورة التي لا تبدأ بالبسملة؟", listOf("الأنفال", "التوبة", "يونس", "مريم"), 1),
            Triple("ما أقصر سورة في القرآن؟", listOf("العصر", "الكوثر", "النصر", "الإخلاص"), 1),
            Triple("من أول الخلفاء الراشدين؟", listOf("عمر بن الخطاب", "عثمان بن عفان", "أبو بكر الصديق", "علي بن أبي طالب"), 2),
            Triple("في أي سنة هجرية وقعت غزوة بدر؟", listOf("الأولى", "الثانية", "الثالثة", "الخامسة"), 1),
            Triple("من أول زوجات النبي ﷺ؟", listOf("عائشة رضي الله عنها", "حفصة رضي الله عنها", "خديجة رضي الله عنها", "زينب رضي الله عنها"), 2),
            Triple("في أي سورة تقع آية الكرسي؟", listOf("آل عمران", "النساء", "البقرة", "المائدة"), 2)
        )
        val prizes = listOf("100", "200", "300", "500", "1,000", "2,000", "4,000", "8,000", "16,000", "32,000", "64,000", "125,000", "250,000", "500,000", "1,000,000")
        var index = 0
        var eliminated = emptySet<Int>()
        var used5050 = false
        var usedAudience = false
        var usedSkip = false
        var finished = false
        var won = false
        var message = ""
        lateinit var draw: () -> Unit
        draw = {
            currentPage = "millionaire"
            base("من سيربح المليون", "مسابقة معرفية إسلامية • 15 مرحلة")
            if (finished || index >= questions.size) {
                root.addView(label(if (won) "مبارك! أكملت جميع المراحل" else "انتهت الجولة", 24f, gold, true))
                val score = if (won) prizes.last() else if (index > 0) prizes[index - 1] else "0"
                root.addView(label("رصيدك الافتراضي: " + score + " نقطة", 19f, cream, true))
                root.addView(label(if (message.isBlank()) "أحسنت على المشاركة، ويمكنك إعادة المحاولة." else message, 14f, muted))
                root.addView(button("العب من جديد", true) { showMillionaire() })
                root.addView(button("العودة للرئيسية", false) { showHome() })
            } else {
                val q = questions[index]
                root.addView(label("المرحلة " + (index + 1) + " من 15", 15f, gold, true))
                root.addView(label("الجائزة الافتراضية: " + prizes[index] + " نقطة", 14f, muted))
                val questionCard = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    background = shape(panel2, 22f)
                    setPadding(18, 20, 18, 20)
                    addView(label("السؤال " + (index + 1), 13f, gold, true))
                    addView(label(q.first, 21f, cream, true))
                }
                root.addView(questionCard)
                root.addView(space(10))
                q.second.forEachIndexed { optionIndex, option ->
                    if (optionIndex !in eliminated) {
                        root.addView(button(('أ' + optionIndex).toString() + "   " + option, false) {
                            if (optionIndex == q.third) {
                                index++
                                eliminated = emptySet()
                                if (index == questions.size) { won = true; finished = true }
                                draw()
                            } else {
                                message = "الإجابة الصحيحة: " + q.second[q.third]
                                finished = true
                                won = false
                                draw()
                            }
                        })
                        root.addView(space(6))
                    }
                }
                root.addView(space(8))
                root.addView(label("وسائل المساعدة — لكل وسيلة استخدام واحد", 13f, muted))
                val aids = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                }
                aids.addView(button(if (used5050) "50:50 ✓" else "50:50", false) {
                    if (!used5050) {
                        used5050 = true
                        eliminated = q.second.indices.filter { it != q.third }.shuffled().take(2).toSet()
                        draw()
                    }
                }, LinearLayout.LayoutParams(0, -2, 1f))
                aids.addView(button(if (usedAudience) "الجمهور ✓" else "الجمهور", false) {
                    if (!usedAudience) {
                        usedAudience = true
                        val correctPct = (52..78).random()
                        val others = q.second.indices.filter { it != q.third && it !in eliminated }
                        val each = if (others.isEmpty()) 0 else (100 - correctPct) / others.size
                        val result = q.second.indices.joinToString("\n") { i ->
                            val pct = if (i == q.third) correctPct else if (i in others) each else 0
                            ('أ' + i).toString() + ": " + pct + "%"
                        }
                        Toast.makeText(this, "تصويت افتراضي للتجربة:\n" + result, Toast.LENGTH_LONG).show()
                    }
                }, LinearLayout.LayoutParams(0, -2, 1f))
                aids.addView(button(if (usedSkip) "تخطي ✓" else "تخطي", false) {
                    if (!usedSkip) {
                        usedSkip = true
                        index++
                        eliminated = emptySet()
                        if (index == questions.size) { won = true; finished = true }
                        draw()
                    }
                }, LinearLayout.LayoutParams(0, -2, 1f))
                root.addView(aids)
                root.addView(button("انسحاب والاحتفاظ بالنقاط", false) {
                    finished = true
                    won = false
                    message = "أنهيت الجولة باختيارك."
                    draw()
                })
            }
        }
        draw()
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
            Triple("ما الكتاب الذي أُنزل على النبي محمد ﷺ؟", listOf("التوراة", "الإنجيل", "الزبور", "القرآن"), 3),
            Triple("كم عدد سور القرآن الكريم؟", listOf("110", "112", "114", "116"), 2),
            Triple("ما أقصر سورة في القرآن؟", listOf("العصر", "الكوثر", "الإخلاص", "الفلق"), 1),
            Triple("ما أطول آية في القرآن؟", listOf("آية الكرسي", "آية الدين", "آخر آية من البقرة", "آية النور"), 1),
            Triple("في أي سورة وردت آية الكرسي؟", listOf("آل عمران", "النساء", "البقرة", "المائدة"), 2),
            Triple("ما السورة التي لا تبدأ بالبسملة؟", listOf("الأنفال", "التوبة", "يونس", "هود"), 1),
            Triple("في أي سورة وردت البسملة مرتين؟", listOf("النمل", "القصص", "مريم", "طه"), 0),
            Triple("ما أول ما نزل من القرآن على المشهور؟", listOf("الفاتحة", "اقرأ باسم ربك", "يا أيها المدثر", "سورة الإخلاص"), 1),
            Triple("ما السورة التي تسمى أم الكتاب؟", listOf("الفاتحة", "البقرة", "يس", "الملك"), 0),
            Triple("إلى أي جهة يتجه المسلمون في الصلاة؟", listOf("المسجد الأقصى", "المسجد النبوي", "الكعبة", "جبل عرفات"), 2),
            Triple("ما اسم الشهر الذي يصومه المسلمون فرضًا؟", listOf("رجب", "شعبان", "رمضان", "شوال"), 2),
            Triple("كم عدد أركان الإيمان؟", listOf("خمسة", "ستة", "سبعة", "ثمانية"), 1),
            Triple("ما اليوم الذي تقام فيه صلاة الجمعة؟", listOf("الخميس", "الجمعة", "السبت", "الأحد"), 1),
            Triple("ما اسم الغار الذي اختبأ فيه النبي ﷺ وأبو بكر أثناء الهجرة؟", listOf("حراء", "ثور", "أحد", "النور"), 1),
            Triple("إلى أي مدينة هاجر النبي ﷺ؟", listOf("الطائف", "القدس", "المدينة المنورة", "خيبر"), 2),
            Triple("ما اسم أم النبي محمد ﷺ؟", listOf("حليمة السعدية", "آمنة بنت وهب", "خديجة بنت خويلد", "فاطمة بنت أسد"), 1),
            Triple("من أول زوجات النبي ﷺ؟", listOf("عائشة", "حفصة", "خديجة", "سودة"), 2),
            Triple("من النبي الذي صنع السفينة بأمر الله؟", listOf("إبراهيم", "نوح", "صالح", "هود"), 1),
            Triple("من النبي الذي كلمه الله تكليمًا؟", listOf("موسى", "يوسف", "داود", "سليمان"), 0),
            Triple("من النبي الذي أُلقي في النار فلم تحرقه؟", listOf("إسماعيل", "يعقوب", "إبراهيم", "أيوب"), 2),
            Triple("ما السورة التي سميت باسم امرأة؟", listOf("مريم", "النساء", "المجادلة", "التحريم"), 0),
            Triple("ما السورة التي سميت باسم نبي وتبدأ بقصة رؤياه؟", listOf("هود", "يوسف", "إبراهيم", "يونس"), 1),
            Triple("كم عدد الصلوات المفروضة في اليوم والليلة؟", listOf("ثلاث", "أربع", "خمس", "ست"), 2),
            Triple("ما الصلاة التي ليس فيها ركوع ولا سجود؟", listOf("الجنازة", "الوتر", "الضحى", "العيد"), 0),
            Triple("ما اسم الليلة التي هي خير من ألف شهر؟", listOf("ليلة النصف من شعبان", "ليلة القدر", "ليلة العيد", "ليلة الإسراء"), 1),
            Triple("في أي شهر يكون الحج؟", listOf("رمضان", "شوال فقط", "أشهر الحج، وأعماله في ذي الحجة", "محرم"), 2),
            Triple("ما اسم البئر المشهورة في مكة؟", listOf("زمزم", "بئر رومة", "بئر أريس", "بئر غرس"), 0),
            Triple("من الملك الموكل بالوحي؟", listOf("ميكائيل", "إسرافيل", "جبريل", "مالك"), 2),
            Triple("ما اسم والد النبي يوسف عليه السلام؟", listOf("إسحاق", "يعقوب", "إبراهيم", "أيوب"), 1),
            Triple("في أي سورة وردت قصة أصحاب الكهف؟", listOf("الكهف", "الإسراء", "مريم", "الأنبياء"), 0),
            Triple("ما السورة التي تبدأ بـ (تبارك الذي بيده الملك)؟", listOf("الواقعة", "الملك", "الرحمن", "الحديد"), 1),
            Triple("ما اسم الصلاة التي تؤدى عند كسوف الشمس؟", listOf("صلاة الاستسقاء", "صلاة الكسوف", "صلاة الضحى", "صلاة الوتر"), 1),
            Triple("من أول مؤذن في الإسلام؟", listOf("عبد الله بن مسعود", "بلال بن رباح", "زيد بن ثابت", "أبو هريرة"), 1),
            Triple("ما السورة التي تسمى أم القرآن؟", listOf("البقرة", "الفاتحة", "الإخلاص", "الملك"), 1),
            Triple("كم عدد أجزاء القرآن الكريم؟", listOf("20", "25", "30", "40"), 2),
            Triple("كم عدد آيات سورة الفاتحة؟", listOf("5", "6", "7", "8"), 2),
            Triple("في أي سورة توجد آية الكرسي؟", listOf("البقرة", "النساء", "المائدة", "الأنعام"), 0),
            Triple("ما رقم آية الكرسي في سورة البقرة؟", listOf("255", "256", "257", "258"), 0),
            Triple("ما أول قبلة للمسلمين؟", listOf("الكعبة", "المسجد النبوي", "بيت المقدس", "مسجد قباء"), 2),
            Triple("ما اللغة التي نزل بها القرآن الكريم؟", listOf("العبرية", "السريانية", "العربية", "الفارسية"), 2),
            Triple("من النبي الذي ألان الله له الحديد؟", listOf("سليمان", "داود", "موسى", "إبراهيم"), 1),
            Triple("ما الكتاب الذي أُنزل على داود عليه السلام؟", listOf("التوراة", "الزبور", "الإنجيل", "القرآن"), 1),
            Triple("ما الكتاب الذي أُنزل على عيسى عليه السلام؟", listOf("الزبور", "التوراة", "الإنجيل", "القرآن"), 2),
            Triple("من النبي الذي أرسله الله إلى قوم عاد؟", listOf("هود", "صالح", "شعيب", "لوط"), 0),
            Triple("من النبي الذي أرسله الله إلى قوم ثمود؟", listOf("هود", "صالح", "شعيب", "إدريس"), 1),
            Triple("من النبي الذي أرسله الله إلى أهل مدين؟", listOf("لوط", "شعيب", "يونس", "أيوب"), 1),
            Triple("ما اسم السورة التي وردت فيها قصة أصحاب الفيل؟", listOf("قريش", "الفيل", "الهمزة", "الماعون"), 1),
            Triple("ما السورة التي تعدل ثلث القرآن في الفضل؟", listOf("الفلق", "الناس", "الإخلاص", "الكوثر"), 2),
            Triple("في أي سورة ورد ذكر النحلة؟", listOf("النحل", "النمل", "العنكبوت", "الرعد"), 0),
            Triple("ما اسم الغزوة التي وقعت في السنة الثانية للهجرة؟", listOf("أحد", "الخندق", "بدر", "حنين"), 2),
            Triple("ما اسم المسجد الذي أسس على التقوى في المدينة؟", listOf("المسجد الحرام", "مسجد قباء", "المسجد الأقصى", "مسجد القبلتين"), 1),
            Triple("كم عدد مصارف الزكاة المذكورة في سورة التوبة؟", listOf("ستة", "سبعة", "ثمانية", "تسعة"), 2),
            Triple("في أي سورة وردت آية الوضوء؟", listOf("البقرة", "النساء", "المائدة", "التوبة"), 2)
        )
        var index = 0
        var score = 0
        var answered = false
        lateinit var draw: () -> Unit
        draw = {
            base("المسابقة الإسلامية", "${questions.size} سؤالًا • اختر إجابة واحدة لكل سؤال")
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
                                results.addView(label("سورة ${item.surahName} • الآية ${item.ayahNumber}", 13f, gold, true))
                                results.addView(label(item.text, 18f, cream))
                                results.addView(space(9))
                                results.addView(button("حفظ الآية في المفضلة ☆", false) {
                                    val prefs = getSharedPreferences("deeni", MODE_PRIVATE)
                                    val saved = prefs.getStringSet("saved_ayahs", emptySet())?.toMutableSet() ?: mutableSetOf()
                                    saved.add("${item.surahNumber}:${item.ayahNumber}")
                                    prefs.edit().putStringSet("saved_ayahs", saved).apply()
                                    Toast.makeText(this, "تم حفظ الآية", Toast.LENGTH_SHORT).show()
                                })
                                results.addView(button("فتح الآية في المصحف", false) {
                                    showQuran()
                                    root.post { showSelectedAyah(item.surahNumber, item.ayahNumber) }
                                })
                                results.addView(space(12))
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

    private fun showSavedAyahs() {
        currentPage = "saved_ayahs"
        base("الآيات المحفوظة", "آيات حفظتها من المصحف أو من نتائج البحث")
        val prefs = getSharedPreferences("deeni", MODE_PRIVATE)
        val saved = prefs.getStringSet("saved_ayahs", emptySet())?.toList()
            ?.mapNotNull { value ->
                val parts = value.split(":")
                val surah = parts.getOrNull(0)?.toIntOrNull()
                val ayah = parts.getOrNull(1)?.toIntOrNull()
                if (surah != null && ayah != null && QuranReference.isValid(surah, ayah)) surah to ayah else null
            }?.sortedWith(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second }) ?: emptyList()
        if (saved.isEmpty()) {
            root.addView(label("لا توجد آيات محفوظة. اضغط على آية في المصحف أو احفظها من نتائج البحث.", 15f, muted))
        } else {
            saved.forEach { item ->
                root.addView(label("السورة رقم ${item.first} • الآية ${item.second}", 16f, gold, true))
                root.addView(button("فتح الآية في المصحف", true) {
                    showQuran()
                    root.post { showSelectedAyah(item.first, item.second) }
                })
                root.addView(button("إزالة من الآيات المحفوظة", false) {
                    val updated = prefs.getStringSet("saved_ayahs", emptySet())?.toMutableSet() ?: mutableSetOf()
                    updated.remove("${item.first}:${item.second}")
                    prefs.edit().putStringSet("saved_ayahs", updated).apply()
                    showSavedAyahs()
                })
                root.addView(space(8))
            }
        }
        root.addView(button("البحث في القرآن", false) { showQuranSearch() })
        root.addView(button("الرئيسية", false) { showHome() })
    }

    private fun showMushafBookmarks() {
        currentPage = "mushaf_bookmarks"
        base("علاماتي في المصحف", "صفحات حفظتها للرجوع إليها")
        val prefs = getSharedPreferences("deeni", MODE_PRIVATE)
        val pages = prefs.getStringSet("mushaf_bookmarks", emptySet())?.mapNotNull { it.toIntOrNull() }?.distinct()?.sorted() ?: emptyList()
        if (pages.isEmpty()) {
            root.addView(label("لا توجد صفحات محفوظة بعد. افتح المصحف ثم أضف الصفحة إلى العلامات المرجعية.", 15f, muted))
        } else {
            pages.forEach { page ->
                root.addView(button("فتح صفحة $page", false) {
                    mushafPage = page
                    getSharedPreferences("deeni", MODE_PRIVATE).edit().putInt("mushaf_page", page).apply()
                    showQuran()
                })
                root.addView(button("حذف علامة الصفحة $page", false) {
                    val updated = prefs.getStringSet("mushaf_bookmarks", emptySet())?.toMutableSet() ?: mutableSetOf()
                    updated.remove(page.toString())
                    prefs.edit().putStringSet("mushaf_bookmarks", updated).apply()
                    showMushafBookmarks()
                })
            }
        }
        root.addView(button("العودة للمصحف", true) { showQuran() })
        root.addView(button("الرئيسية", false) { showHome() })
    }

    private fun showPrayerTimes() {
        currentPage = "prayer_times"
        base("مواقيت الصلاة", "المواقيت تُجلب حسب المدينة وطريقة الحساب المختارة")
        val prefs = getSharedPreferences("deeni", MODE_PRIVATE)
        val savedCity = prefs.getString("prayer_city", null)
        val initialCity = if (savedCity.isNullOrBlank() || savedCity == "أبو عريش") "Abu Arish" else savedCity
        val country = EditText(this).apply {
            setText(prefs.getString("prayer_country", "Saudi Arabia"))
            hint = "اسم الدولة بالإنجليزية، مثال: Saudi Arabia"
            textSize = 16f
            setTextColor(cream)
            setHintTextColor(muted)
            gravity = Gravity.RIGHT
            setPadding(14, 14, 14, 14)
            background = shape(panel, 16f)
        }
        val methodOptions = listOf(
            "أم القرى – السعودية" to 4,
            "رابطة العالم الإسلامي" to 3,
            "الهيئة المصرية العامة للمساحة" to 5,
            "جامعة العلوم الإسلامية – كراتشي" to 1,
            "الجمعية الإسلامية لأمريكا الشمالية" to 2,
            "منطقة الخليج" to 8,
            "الكويت" to 9,
            "قطر" to 10,
            "سنغافورة" to 11,
            "تركيا" to 13,
            "لجنة رؤية الهلال" to 15
        )
        val methodSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, methodOptions.map { it.first })
            val savedMethod = prefs.getInt("prayer_method", 4)
            setSelection(methodOptions.indexOfFirst { it.second == savedMethod }.coerceAtLeast(0))
            background = shape(panel, 14f)
        }
        val city = EditText(this).apply {
            setText(initialCity)
            hint = "اسم المدينة بالإنجليزية، مثال: Abu Arish"
            textSize = 16f
            setTextColor(cream)
            setHintTextColor(muted)
            gravity = Gravity.RIGHT
            setPadding(14, 14, 14, 14)
            background = shape(panel, 16f)
        }
        root.addView(label("الدولة", 15f, gold, true))
        root.addView(country)
        root.addView(label("المدينة", 15f, gold, true))
        root.addView(city)
        root.addView(label("طريقة حساب مواقيت الصلاة", 15f, gold, true))
        root.addView(methodSpinner)
        root.addView(label("اختر الطريقة المعتمدة في بلدك؛ قد تختلف المواقيت عن تقويم المسجد المحلي.", 12f, muted))
        val result = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(result)
        root.addView(button("عرض مواقيت اليوم", true) {
            val name = city.text.toString().trim()
            val countryName = country.text.toString().trim()
            val methodCode = methodOptions[methodSpinner.selectedItemPosition].second
            if (name.isBlank() || countryName.isBlank()) {
                Toast.makeText(this, "اكتب الدولة والمدينة أولًا", Toast.LENGTH_SHORT).show()
            } else {
                getSharedPreferences("deeni", MODE_PRIVATE).edit()
                    .putString("prayer_city", name)
                    .putString("prayer_country", countryName)
                    .putInt("prayer_method", methodCode)
                    .apply()
                result.removeAllViews()
                result.addView(label("جارٍ جلب المواقيت…", 15f, gold))
                Thread {
                    val response = PrayerTimesSource.load(name, countryName, methodCode)
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
        val storedCity = prefs.getString("prayer_city", null)
        val city = if (storedCity.isNullOrBlank() || storedCity == "أبو عريش") "Abu Arish" else storedCity
        val country = prefs.getString("prayer_country", "Saudi Arabia") ?: "Saudi Arabia"
        root.addView(label("الموقع المستخدم: ${city}، ${country}", 14f, muted))
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
                val result = PrayerTimesSource.load(city, country, prefs.getInt("prayer_method", 4))
                runOnUiThread {
                    if (currentPage != "qibla") return@runOnUiThread
                    if (result.isSuccess) {
                        val data = result.getOrThrow()
                        val bearing = qiblaBearing(data.latitude, data.longitude)
                        status.text = "اتجاه القبلة: ${"%.1f".format(Locale.US, bearing)}° من الشمال الحقيقي"
                        compass.setDirection(bearing.toFloat(), 0f)
                        startQiblaSensor(compass, bearing.toFloat(), data.latitude, data.longitude)
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
                val result = PrayerTimesSource.load(city, country, prefs.getInt("prayer_method", 4))
                runOnUiThread {
                    if (currentPage != "qibla") return@runOnUiThread
                    if (result.isSuccess) {
                        val data = result.getOrThrow()
                        val bearing = qiblaBearing(data.latitude, data.longitude)
                        status.text = "اتجاه القبلة: ${"%.1f".format(Locale.US, bearing)}° من الشمال الحقيقي"
                        compass.setDirection(bearing.toFloat(), 0f)
                        startQiblaSensor(compass, bearing.toFloat(), data.latitude, data.longitude)
                    } else {
                        status.text = "تعذر تحديد اتجاه القبلة. تحقق من المدينة والإنترنت."
                    }
                }
            }.start()
        }
    }

    private fun qiblaBearing(latitude: Double, longitude: Double): Double =
        QiblaMath.bearing(latitude, longitude)

    private fun startQiblaSensor(compass: QiblaCompassView, bearing: Float, latitude: Double, longitude: Double) {
        stopQiblaSensor()
        val manager = getSystemService(SENSOR_SERVICE) as SensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensor == null) {
            compass.setDirection(bearing, 0f)
            return
        }
        val geomagneticField = GeomagneticField(latitude.toFloat(), longitude.toFloat(), 0f, System.currentTimeMillis())
        val declination = geomagneticField.declination
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
                val rotation = FloatArray(9)
                val orientation = FloatArray(3)
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                SensorManager.getOrientation(rotation, orientation)
                val magneticAzimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                val trueAzimuth = (magneticAzimuth + declination + 360f) % 360f
                compass.setDirection(bearing, trueAzimuth)
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

    private fun showTafsir() {
        currentPage = "tafsir"
        base("التفسير الميسر", "اختر رقم السورة والآية لعرض النص والتفسير")
        val surahInput = EditText(this).apply {
            setText("1")
            hint = "رقم السورة من 1 إلى 114"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setTextColor(cream)
            setHintTextColor(muted)
            background = shape(panel, 14f)
            setPadding(14, 12, 14, 12)
        }
        val ayahInput = EditText(this).apply {
            setText("1")
            hint = "رقم الآية"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setTextColor(cream)
            setHintTextColor(muted)
            background = shape(panel, 14f)
            setPadding(14, 12, 14, 12)
        }
        root.addView(label("رقم السورة", 14f, gold, true))
        root.addView(surahInput)
        root.addView(label("رقم الآية", 14f, gold, true))
        root.addView(ayahInput)
        val output = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(output)
        root.addView(button("عرض التفسير", true) {
            val surah = surahInput.text.toString().toIntOrNull()
            val ayah = ayahInput.text.toString().toIntOrNull()
            if (surah == null || ayah == null || surah !in 1..114 || ayah !in 1..286) {
                Toast.makeText(this, "تحقق من رقم السورة والآية", Toast.LENGTH_SHORT).show()
            } else {
                output.removeAllViews()
                output.addView(label("جارٍ تحميل النص والتفسير…", 14f, gold))
                Thread {
                    val response = TafsirSource.load(surah, ayah)
                    runOnUiThread {
                        if (currentPage != "tafsir") return@runOnUiThread
                        output.removeAllViews()
                        if (response.isSuccess) {
                            val item = response.getOrThrow()
                            output.addView(label("${item.surahName} • الآية ${item.ayahNumber}", 15f, gold, true))
                            output.addView(label(item.ayahText, 24f, cream, true))
                            output.addView(space(12))
                            output.addView(label("التفسير الميسر", 19f, gold, true))
                            output.addView(label(item.tafsirText, 17f, cream))
                            output.addView(label("المصدر: إصدار التفسير الميسر عبر خدمة AlQuran Cloud. راجع كتب التفسير المعتمدة وأهل العلم عند طلب التفصيل أو الاستنباط.", 12f, muted))
                        } else {
                            output.addView(label("تعذر تحميل التفسير لهذه الآية. تحقق من الاتصال وصحة الأرقام.", 14f, gold))
                        }
                    }
                }.start()
            }
        })
        root.addView(button("فتح المصحف", false) { showQuran() })
        root.addView(button("الرئيسية", false) { showHome() })
    }

    private fun showDuas() {
        currentPage = "duas"
        val duas = listOf(
            Triple("رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ", "البقرة: 201", "دعاء جامع لخيري الدنيا والآخرة."),
            Triple("رَبَّنَا لَا تُزِغْ قُلُوبَنَا بَعْدَ إِذْ هَدَيْتَنَا وَهَبْ لَنَا مِنْ لَدُنْكَ رَحْمَةً إِنَّكَ أَنْتَ الْوَهَّابُ", "آل عمران: 8", "سؤال الله الثبات والرحمة."),
            Triple("رَبِّ اشْرَحْ لِي صَدْرِي ۝ وَيَسِّرْ لِي أَمْرِي ۝ وَاحْلُلْ عُقْدَةً مِنْ لِسَانِي ۝ يَفْقَهُوا قَوْلِي", "طه: 25–28", "دعاء موسى عليه السلام."),
            Triple("رَبَّنَا ظَلَمْنَا أَنْفُسَنَا وَإِنْ لَمْ تَغْفِرْ لَنَا وَتَرْحَمْنَا لَنَكُونَنَّ مِنَ الْخَاسِرِينَ", "الأعراف: 23", "دعاء آدم وحواء عليهما السلام."),
            Triple("لَا إِلَهَ إِلَّا أَنْتَ سُبْحَانَكَ إِنِّي كُنْتُ مِنَ الظَّالِمِينَ", "الأنبياء: 87", "دعاء يونس عليه السلام."),
            Triple("رَبِّ اغْفِرْ لِي وَلِوَالِدَيَّ وَلِلْمُؤْمِنِينَ يَوْمَ يَقُومُ الْحِسَابُ", "إبراهيم: 41", "دعاء بالمغفرة للوالدين والمؤمنين."),
            Triple("رَبَّنَا تَقَبَّلْ مِنَّا إِنَّكَ أَنْتَ السَّمِيعُ الْعَلِيمُ", "البقرة: 127", "دعاء إبراهيم وإسماعيل عليهما السلام عند رفع القواعد."),
            Triple("رَبِّ زِدْنِي عِلْمًا", "طه: 114", "دعاء بطلب زيادة العلم."),
            Triple("رَبَّنَا أَفْرِغْ عَلَيْنَا صَبْرًا وَثَبِّتْ أَقْدَامَنَا وَانْصُرْنَا عَلَى الْقَوْمِ الْكَافِرِينَ", "البقرة: 250", "دعاء بالصبر والثبات."),
            Triple("رَبَّنَا لَا تُؤَاخِذْنَا إِنْ نَسِينَا أَوْ أَخْطَأْنَا", "البقرة: 286", "من دعاء المؤمنين في ختام سورة البقرة.")
        )
        base("أدعية من القرآن الكريم", "نصوص قرآنية مع مواضعها — انسخ الدعاء بسهولة")
        duas.forEachIndexed { index, dua ->
            root.addView(label("${index + 1}. ${dua.second}", 14f, gold, true))
            root.addView(label("﴿${dua.first}﴾", 21f, cream, true))
            root.addView(label(dua.third, 13f, muted))
            root.addView(button("نسخ الدعاء", false) {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("دعاء قرآني", dua.first))
                Toast.makeText(this, "تم نسخ الدعاء", Toast.LENGTH_SHORT).show()
            })
            root.addView(space(10))
        }
        root.addView(label("هذه أدعية قرآنية بنصها ومواضعها، وليست قائمة شاملة لكل الأدعية الواردة في القرآن والسنة.", 12f, muted))
        root.addView(button("الرئيسية", true) { showHome() })
    }

    private fun showTajweed() {
        currentPage = "tajweed"
        base("أكاديمية التجويد", "دروس مصنفة مع أمثلة قرآنية وتطبيق عملي • حفص عن عاصم")
        data class Lesson(val group: String, val title: String, val rule: String, val example: String, val place: String, val exercise: String)
        val lessons = listOf(
            Lesson("المدخل","معنى التجويد","إعطاء الحروف حقها ومستحقها من المخرج والصفة، وصون اللسان عن الخطأ في تلاوة القرآن.","وَرَتِّلِ الْقُرْآنَ تَرْتِيلًا","المزمل: 4","اقرأ بتمهل، واعرض الأداء على معلم متقن."),
            Lesson("المدخل","اللحن الجلي والخفي","الجلي خطأ ظاهر يغيّر حرفًا أو حركة أو بنية كلمة. والخفي خلل في صفة أو حكم أداء لا يدركه غالبًا إلا المتقن.","التطبيق لا يحكم آليًا على المخارج أو التجويد","تنبيه تعليمي","قارن قراءتك بتلاوة معلم، ولا تعتمد على التعرف الصوتي وحده."),
            Lesson("المخارج","المخارج العامة","المخارج العامة خمسة: الجوف والحلق واللسان والشفتان والخيشوم، وتتفرع عنها المخارج الخاصة.","قُلْ أَعُوذُ بِرَبِّ النَّاسِ","الناس: 1","لاحظ اختلاف القاف والعين والذال والسين في المخرج."),
            Lesson("الصفات","الهمس والجهر","حروف الهمس مجموعة في (فحثه شخص سكت)، ويجري معها النفس. والجهر في بقية الحروف.","فَسَبِّحْ بِاسْمِ رَبِّكَ الْعَظِيمِ","الواقعة: 74","قارن جريان النفس في الفاء والحاء بانحباسه في الباء."),
            Lesson("الصفات","الشدة والتوسط والرخاوة","حروف الشدة مجموعة في (أجد قط بكت)، والتوسط في (لن عمر)، وبقية الحروف رخوة.","أَقْسَمُوا","النحل: 38","ميّز انحباس صوت القاف من جريان الصوت في الحروف الرخوة."),
            Lesson("الصفات","الاستعلاء والاستفال","حروف الاستعلاء سبعة في (خص ضغط قظ)، وتفخم في الأصل. وبقية الحروف مستفلة، مع أحكام خاصة للراء ولام لفظ الجلالة.","خَلَقَ السَّمَاوَاتِ","الأنعام: 1","انتبه لاستعلاء الخاء والقاف دون مبالغة."),
            Lesson("النون والتنوين","الإظهار الحلقي","حروفه الهمزة والهاء والعين والحاء والغين والخاء. تظهر النون أو التنوين قبلها من غير إدغام.","مِنْ هَادٍ","الرعد: 33","أظهر النون قبل الهاء بوضوح دون فصل متكلف."),
            Lesson("النون والتنوين","الإدغام بغنة","يكون عند الياء والنون والميم والواو في كلمتين، وتدغم النون أو التنوين مع غنة.","مَن يَقُولُ","البقرة: 8","صل النون بالياء مع غنة معتدلة."),
            Lesson("النون والتنوين","الإدغام بغير غنة","يكون عند اللام والراء في كلمتين، وتدغم النون أو التنوين من غير غنة ظاهرة.","هُدًى لِّلْمُتَّقِينَ","البقرة: 2","أدغم التنوين في اللام دون إظهار نون مستقلة."),
            Lesson("النون والتنوين","الإقلاب","إذا جاء بعد النون الساكنة أو التنوين حرف الباء، تقلب النون ميمًا مخفاة مع الغنة.","أَنْبِئْهُمْ","البقرة: 33","اقلب النون ميمًا مخفاة قبل الباء."),
            Lesson("النون والتنوين","الإخفاء الحقيقي","يكون مع الحروف الخمسة عشر الباقية بعد حروف الإظهار والإدغام والإقلاب، مع غنة وإخفاء للنون.","مِن قَبْلُ","البقرة: 4","هيئ اللسان للقاف مع إبقاء الغنة."),
            Lesson("الميم الساكنة","الإخفاء الشفوي","إذا جاء بعد الميم الساكنة حرف الباء، تخفى الميم مع الغنة.","تَرْمِيهِم بِحِجَارَةٍ","الفيل: 4","أخف الميم عند الباء مع تماس الشفتين برفق."),
            Lesson("الميم الساكنة","الإدغام الشفوي","إذا جاء بعد الميم الساكنة ميم متحركة، تدغم الأولى في الثانية مع الغنة.","لَهُم مَّا يَشَاءُونَ","ق: 35","اجعل الميمين ميمًا مشددة واحدة."),
            Lesson("الميم الساكنة","الإظهار الشفوي","تظهر الميم الساكنة عند بقية الحروف، مع الانتباه إلى وضوحها قبل الواو والفاء.","عَلَيْهِمْ وَلَا","الفاتحة: 7","أظهر الميم قبل الواو دون إخفاء."),
            Lesson("الغنة","النون والميم المشددتان","في النون والميم المشددتين غنة لازمة بمقدار حركتين في الأداء المعتاد.","إِنَّ • ثُمَّ","أمثلة متكررة في القرآن","أد الغنة من الخيشوم باعتدال."),
            Lesson("القلقلة","حروف القلقلة","حروفها (قطب جد)، وتظهر عند سكون الحرف، وتكون أوضح عند الوقف على المشدد.","قُلْ هُوَ اللَّهُ أَحَدٌ","الإخلاص: 1","عند الوقف على (أحد) أظهر القلقلة دون إضافة حركة."),
            Lesson("اللامات","لام لفظ الجلالة","تفخم إذا سبقها فتح أو ضم، وترقق إذا سبقها كسر.","قَالَ اللَّهُ • بِاللَّهِ","أمثلة قرآنية","قارن اللام بعد الفتح باللام بعد الكسر."),
            Lesson("اللامات","اللام الشمسية والقمرية","تدغم لام التعريف في الحروف الشمسية ويشدد الحرف بعدها، وتظهر عند الحروف القمرية.","الشَّمْسُ • الْقَمَرُ","الشمس: 1؛ القمر: 1","لا تنطق اللام في الشمس، وأظهرها في القمر."),
            Lesson("الراء","تفخيم الراء وترقيقها","للراء أحوال تفخم فيها وأحوال ترقق، ويراعى ضبطها وحركتها وما قبلها مع استثناءات معروفة.","رَبِّ الْعَالَمِينَ • فِرْعَوْنَ","الفاتحة: 2؛ البقرة: 49","تدرّب على الراء المفتوحة والمكسورة والساكنة مع معلم."),
            Lesson("المدود","المد الطبيعي","يمد حركتين ولا يتوقف على همز أو سكون بعد حرف المد.","قَالُوا","البقرة: 11","مد الألف حركتين دون زيادة."),
            Lesson("المدود","المد المتصل","يأتي حرف المد وبعده همز في الكلمة نفسها، وهو واجب المد في اصطلاح التجويد.","جَاءَ","البقرة: 71","لاحظ اجتماع حرف المد والهمز في كلمة واحدة."),
            Lesson("المدود","المد المنفصل","يأتي حرف المد في آخر كلمة والهمز في أول الكلمة التالية، ومقداره بحسب الطريق المتلقى.","بِمَا أُنْزِلَ","البقرة: 4","راع مقدار المد المتلقى في رواية حفص وطريقها."),
            Lesson("المدود","المد اللازم الكلمي المثقل","يأتي بعد حرف المد سكون أصلي مدغم أو مشدد في الكلمة نفسها، ويمد ست حركات.","الضَّالِّينَ","الفاتحة: 7","أد المد ست حركات في هذا الموضع."),
            Lesson("المدود","المد العارض للسكون","ينشأ عند الوقف إذا سكن الحرف الأخير لأجل الوقف بعد حرف المد، وله أوجه القصر والتوسط والإشباع.","الْعَالَمِينَ","الفاتحة: 2 عند الوقف","تعلم أوجه الوقف بالتلقي ولا تخلط بينها بلا ضبط."),
            Lesson("المدود","مد اللين","يكون في الواو أو الياء الساكنتين المفتوح ما قبلهما عند الوقف.","خَوْفٍ","قريش: 4 عند الوقف","تأمل لين الواو عند الوقف."),
            Lesson("الوقف","الوقف والابتداء","يراعى تمام المعنى وتعلق الكلام بما بعده، ويجتنب الوقف الذي يوهم معنى فاسدًا.","علامات الوقف في المصحف","مواضع متعددة","تعلّم علامات الوقف مع فهم المعنى من معلم."),
            Lesson("الوقف","الاستئناف والابتداء","يختار القارئ موضع ابتداء مناسبًا للمعنى عند استئناف القراءة، ولا يبدأ بما يفسد المعنى أو يقطعه.","وَإِذَا قُرِئَ الْقُرْآنُ فَاسْتَمِعُوا لَهُ وَأَنْصِتُوا","الأعراف: 204","ابدأ من موضع يستقيم به المعنى."),
            Lesson("المخارج","مخرج الهمزة والهاء","يخرج الهمزة والهاء من أقصى الحلق، مع اختلاف صفتيهما؛ فالهمزة شديدة، والهاء مهموسة رخوة.","أَنْعَمْتَ عَلَيْهِمْ","الفاتحة: 7؛ الهمزة في (أَنْعَمْتَ) والهاء في (عَلَيْهِمْ)","انطق الهمزة محققة من أقصى الحلق، ثم الهاء بنَفَس واضح."),
            Lesson("المخارج","مخرج العين والحاء","العين والحاء من وسط الحلق؛ العين مجهورة متوسطة، والحاء مهموسة رخوة.","الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ","الفاتحة: 2؛ الحاء في (الْحَمْدُ) والعين في (الْعَالَمِينَ)","ميّز صوت العين عن الهمزة، والحاء عن الهاء، دون تكلف."),
            Lesson("المخارج","مخرج الغين والخاء","الغين والخاء من أدنى الحلق مما يلي الفم؛ الغين مجهورة والخاء مهموسة.","غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ","الفاتحة: 7؛ الغين في (غَيْرِ)","أخرج الغين من موضعها دون تحويلها إلى قاف أو خاء."),
            Lesson("المخارج","مخرج القاف والكاف","القاف من أقصى اللسان مع ما يحاذيه من الحنك الأعلى، والكاف أسفل منها قليلًا إلى جهة الفم.","اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ","الفاتحة: 6؛ القاف في (الْمُسْتَقِيمَ)","قارن موضع القاف في (المستقيم) بالكاف في (مالك) من الفاتحة: 4."),
            Lesson("المخارج","مخرج الجيم والشين والياء","تخرج الجيم والشين والياء غير المدية من وسط اللسان مع ما يحاذيه من الحنك الأعلى.","شَرِّ الْوَسْوَاسِ الْخَنَّاسِ","الناس: 4؛ الشين في (شَرِّ)، وتُراجع أمثلة الجيم والياء في مواضع أخرى","تدرّب على وسط اللسان مع شيخ متقن، ولا تعتمد على الوصف النظري وحده."),
            Lesson("المخارج","مخرج الضاد","تخرج الضاد من إحدى حافتي اللسان أو كلتيهما مع ما يحاذيهما من الأضراس العليا، ولها صفة الاستطالة.","وَلَا الضَّالِّينَ","الفاتحة: 7؛ حرف الضاد في (الضَّالِّينَ)","تدرّب على امتداد الصوت بمحاذاة الأضراس دون قلب الضاد ظاءً أو دالًا."),
            Lesson("المخارج","مخرج اللام","تخرج اللام من أدنى حافتي اللسان إلى طرفه مع ما يحاذيه من لثة الأسنان العليا.","الْحَمْدُ لِلَّهِ","الفاتحة: 2؛ اللام في (الْحَمْدُ) و(لِلَّهِ)","اجعل طرف اللسان يلامس موضعه دون ضغط زائد."),
            Lesson("المخارج","مخرج النون","تخرج النون من طرف اللسان مع ما يحاذيه من لثة الثنيتين العلييين، ولها غنة من الخيشوم.","أَنْعَمْتَ","الفاتحة: 7؛ النون الساكنة في (أَنْعَمْتَ)","ميّز مخرج النون عن مصدر الغنة، وراع أحكامها بحسب الحرف التالي."),
            Lesson("المخارج","مخرج الراء","تخرج الراء من طرف اللسان قريبًا من مخرج النون، مع قابلية التكرير التي يجب التحرز من المبالغة فيها.","رَبِّ الْعَالَمِينَ","الفاتحة: 2؛ الراء في (رَبِّ)","انطق الراء بوضوح من غير تكرير زائد."),
            Lesson("المخارج","مخرج الطاء والدال والتاء","تخرج الطاء والدال والتاء من طرف اللسان مع أصول الثنايا العليا، والطاء مستعلية مطبقة بخلاف الدال والتاء.","اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ","الفاتحة: 6؛ الطاء في (الصِّرَاطَ)","قارن التفخيم في الطاء بترقيق الدال والتاء."),
            Lesson("المخارج","مخرج الصاد والزاي والسين","تخرج الصاد والزاي والسين من طرف اللسان مع ما بين الثنايا، والصاد مستعلية مطبقة.","اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ","الفاتحة: 6؛ الصاد في (الصِّرَاطَ)","حافظ على صفير الحروف، وميّز استعلاء الصاد."),
            Lesson("المخارج","مخرج الظاء والذال والثاء","تخرج الظاء والذال والثاء من طرف اللسان مع أطراف الثنايا العليا.","الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ","الفاتحة: 7؛ الذال في (الَّذِينَ)","أظهر طرف اللسان عند المخرج دون تحويل الذال إلى زاي."),
            Lesson("المخارج","مخرج الفاء","تخرج الفاء من بطن الشفة السفلى مع أطراف الثنايا العليا.","الْفَاتِحَةِ","الفاتحة: 1؛ الفاء في (الْفَاتِحَةِ)","لامس أطراف الثنايا للشفة السفلى برفق."),
            Lesson("المخارج","مخرج الباء والميم والواو","تخرج الباء والميم والواو غير المدية من الشفتين؛ الباء بانطباقهما، والميم بانطباقهما مع غنة، والواو بانضمامهما.","بِسْمِ اللَّهِ","البسملة؛ الباء في (بِسْمِ) والميم في (بِسْمِ)","لاحظ اختلاف انطباق الشفتين في الباء والميم عن انضمامهما في الواو."),
            Lesson("المخارج","مخرج الغنة","الغنة صوت أغن يخرج من الخيشوم، ويصاحب النون والميم بحسب أحكامهما.","إِنَّ • ثُمَّ","أمثلة قرآنية؛ النون والميم المشددتان","لا تجعل الغنة صوتًا مستقلًا عن الحرف، وتلق مقدارها بالمشافهة."),
            Lesson("الصفات","صفة الصفير","الصفير صوت زائد يشبه الصفير يصاحب الصاد والزاي والسين.","اهْدِنَا الصِّرَاطَ","الفاتحة: 6؛ الصاد في (الصِّرَاطَ)","استمع إلى الصفير في الصاد والسين والزاي وقارن بينها."),
            Lesson("الصفات","صفة القلقلة الصغرى","تظهر القلقلة في حرف من (قطب جد) إذا كان ساكنًا في وسط الكلمة أو وصلًا، وتكون دون قلقلة الوقف.","يَجْعَلُونَ","مواضع القلقلة في القرآن؛ الجيم الساكنة في (يَجْعَلُونَ)","أظهر نبرة القلقلة من غير توليد حركة."),
            Lesson("الصفات","صفة القلقلة الكبرى","تكون القلقلة عند الوقف على حرف من حروف (قطب جد) الساكن، وتكون أوضح من الصغرى.","أَحَدٌ","الإخلاص: 1؛ الدال عند الوقف","قف على الدال مع بيان القلقلة دون إضافة فتحة أو ضمة."),
            Lesson("الصفات","الانحراف والتكرير","الانحراف صفة اللام والراء، والتكرير صفة الراء، ويجب تجنب المبالغة في تكريرها.","الرَّحْمَٰنِ الرَّحِيمِ","الفاتحة: 1؛ الراء في (الرَّحْمَٰنِ) و(الرَّحِيمِ)","انطق الراء بوضوح مع منع اهتزازات زائدة."),
            Lesson("الصفات","التفشي والاستطالة","التفشي للشين، والاستطالة للضاد؛ ولكل منهما أداء مميز يضبط بالتلقي.","وَلَا الضَّالِّينَ","الفاتحة: 7؛ الضاد في (الضَّالِّينَ)","قارن امتداد الضاد بتفشي الشين في أمثلة الباب."),
            Lesson("المدود","المد الطبيعي الكلمي والحرفي","المد الطبيعي يكون في الكلمة أو في فواتح بعض السور، ومقداره حركتان عند عدم وجود سبب للزيادة.","قَالُوا • طه","البقرة: 11؛ طه: 1، ويُهجّى حرفاها مع مد طبيعي حرفي","ميّز حرف المد في الكلمة عن حروف المد في فواتح السور."),
            Lesson("المدود","المد اللازم الكلمي المخفف","يأتي بعد حرف المد سكون أصلي غير مدغم في كلمة، ويمد ست حركات في رواية حفص.","ءَآلْـَٰٔنَ","يونس: 51 و91؛ موضع (ءَآلْـَٰٔنَ)","راجع ضبط الكلمة في مصحف معتمد، وتلق الأداء من معلم."),
            Lesson("المدود","المد اللازم الحرفي المثقل","يكون في حرف من الحروف المقطعة إذا جاء بعد حرف المد سكون أصلي مدغم فيما بعده، ويمد ست حركات.","الٓمٓ","البقرة: 1؛ اللام من (الم)","حلل هجاء الحرف ومقدار مده وفق رواية حفص."),
            Lesson("المدود","المد اللازم الحرفي المخفف","يكون في الحروف المقطعة إذا جاء بعد حرف المد سكون أصلي غير مدغم، ويمد ست حركات.","صٓ • قٓ • نٓ","ص: 1؛ ق: 1؛ القلم: 1","تدرّب على هجاء الحروف المقطعة ومقدار المد بالتلقي."),
            Lesson("المدود","مد البدل","يتقدم الهمز على حرف المد في كلمة واحدة، ومقداره حركتان في رواية حفص من طريق الشاطبية.","آمَنُوا","البقرة: 9؛ الألف الممدودة بعد الهمزة في (آمَنُوا)","ميّز تقدم الهمزة على حرف المد."),
            Lesson("المدود","مد العوض","يكون عند الوقف على تنوين الفتح، فيبدل التنوين ألفًا تمد حركتين، مع استثناءات معروفة مثل التاء المربوطة.","عَلِيمًا","البقرة: 29؛ مثال تنوين الفتح عند الوقف","تدرب على الوقف بالألف، وانتبه إلى التاء المربوطة."),
            Lesson("المدود","مد الصلة الصغرى","تلحق هاء الضمير بواو لفظية إذا كانت مضمومة أو بياء إذا كانت مكسورة، بين متحركين ولم يأت بعدها همز، ومقداره حركتان.","إِنَّهُ كَانَ","مواضع هاء الضمير في القرآن؛ تحقق من شروط المثال عند التطبيق","افحص حركة الهاء وما قبلها وما بعدها قبل الحكم."),
            Lesson("المدود","مد الصلة الكبرى","تلحق هاء الضمير بواو أو ياء لفظية إذا وقعت بين متحركين وجاء بعدها همز قطع، ومقداره بحسب طريق القراءة.","مَالَهُ أَخْلَدَهُ","الهمزة: 3؛ الهاء في (مَالَهُ أَخْلَدَهُ)","حدد هاء الضمير والهمزة التالية، وراع مقدار المد المتلقى."),
            Lesson("الوقف","الروم","الروم الإتيان ببعض الحركة بصوت خفي عند الوقف على المضموم أو المكسور، ولا يكون في المفتوح.","وَلَا الضَّالِّينَ","الفاتحة: 7؛ يدرس الباب بأمثلة المضموم والمكسور","تعلّم الروم بالمشافهة، ولا تطبقه اعتمادًا على الوصف المكتوب وحده."),
            Lesson("الوقف","الإشمام","الإشمام ضم الشفتين بعد إسكان الحرف المضموم عند الوقف، من غير صوت يُسمع.","لَا تَأْمَنَّا","يوسف: 11؛ موضع الإشمام في النون المشددة له أداء خاص","شاهد أداء الإشمام من معلم متقن؛ لا يكفي الوصف النصي لإتقانه."),
            Lesson("المدخل","الاستعاذة قبل القراءة","يستحب للقارئ أن يستعيذ بالله من الشيطان الرجيم قبل التلاوة، وهي ليست آية من القرآن.","فَإِذَا قَرَأْتَ الْقُرْآنَ فَاسْتَعِذْ بِاللَّهِ","النحل: 98","انطق الاستعاذة بوضوح، وتعلم أوجه وصلها وقطعها من معلم."),
            Lesson("المدخل","البسملة بين السور","تُقرأ البسملة في أوائل السور عدا سورة التوبة، وللوصل بين السورتين أوجه أداء معروفة في رواية حفص.","بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ","أول السور؛ عدا التوبة","تدرّب على أوجه الفصل والسكت والوصل وفق ما تلقيته من شيخك."),
            Lesson("المدخل","همزة الوصل وهمزة القطع","همزة القطع تثبت في الابتداء والوصل، أما همزة الوصل فتثبت ابتداء وتسقط في درج الكلام، ويُعرف ضبطهما من الرسم والحركة.","اهْدِنَا • أَنْعَمْتَ","الفاتحة: 6-7","قارن همزة (اهدنا) بهمزة (أنعمت) في الابتداء والوصل."),
            Lesson("الصفات","الاستعلاء والإطباق","حروف الاستعلاء سبعة، وحروف الإطباق أربعة: الصاد والضاد والطاء والظاء؛ والإطباق أخص من الاستعلاء.","صِرَاطَ الَّذِينَ","الفاتحة: 6-7","قارن الصاد والطاء المطبقين بحروف الاستعلاء غير المطبقة."),
            Lesson("الصفات","الذلاقة والإصمات","الذلاقة صفة لحروف (فر من لب)، والإصمات لبقية الحروف، وهي من الصفات التي تضبط ضمن أبواب الصفات المتقابلة.","فَرَّ • مِنْ • لَبٍّ","أمثلة تعليمية للحروف","راجع حروف الذلاقة واحفظ مجموعتها، ثم صنف أمثلة من المصحف."),
            Lesson("النون والتنوين","مراتب الغنة","للغنة مراتب تتفاوت بحسب موضعها، وأكملها في المشدد والمدغم، ثم الإخفاء والإقلاب، ويضبط مقدارها بالمشافهة.","إِنَّ • مَنْ يَعْمَلْ • مِنْ بَعْدِ","أمثلة من أحكام النون والميم","استمع إلى معلم متقن وقارن الغنة في المشدد والإخفاء دون مبالغة."),
            Lesson("الميم الساكنة","صلة الميم بباب الغنة","تظهر الغنة في الميم بحسب الحكم، وتكون أوضح في الإخفاء الشفوي والإدغام الشفوي، ولا تُزاد في الإظهار.","تَرْمِيهِم بِحِجَارَةٍ • لَهُم مَّا","الفيل: 4؛ ق: 35","قارن الميم المخفاة قبل الباء بالميم المدغمة في الميم."),
            Lesson("المدود","حروف المد وشروطها","الألف الساكنة المفتوح ما قبلها، والواو الساكنة المضموم ما قبلها، والياء الساكنة المكسور ما قبلها هي حروف المد.","قَالَ • يَقُولُ • قِيلَ","أمثلة قرآنية متعددة","حدد حرف المد وحركة الحرف الذي قبله في كل مثال."),
            Lesson("المدود","أسباب المد الفرعي","المد الفرعي يكون بسبب الهمز أو السكون، وتختلف مقاديره باختلاف نوع المد وطريق الرواية.","جَاءَ • الضَّالِّينَ","البقرة: 71؛ الفاتحة: 7","صنف المثال بحسب سببه: همز أم سكون، ثم راجع المقدار المتلقى."),
            Lesson("الوقف","علامات الوقف في المصحف","علامات الوقف إرشادية لفهم مواضع الوقف والوصل، ومن أشهرها ميم الوقف اللازم، ولا، ج، صلى، قلى، وعلامة المعانقة.","علامات الوقف المطبوعة في المصحف","مواضع متعددة","افتح مصحفًا مضبوطًا وتعرف على معنى كل علامة من دليل معتمد."),
            Lesson("الوقف","الوقف على أواخر الكلمات","يراعى عند الوقف تسكين آخر الكلمة غالبًا، مع مراعاة التاء المربوطة وتنوين الفتح والروم والإشمام في مواضعها.","رَحْمَةٌ • عَلِيمًا","أمثلة قرآنية؛ يراجع كل مثال في موضعه","قارن الوصل بالوقف، ولا تطبق وجهًا من أوجه الوقف دون تلقي."),
            Lesson("الوقف","السكت في رواية حفص","السكت قطع الصوت زمنًا لطيفًا من غير تنفس في مواضع مخصوصة من رواية حفص، ويؤخذ ضبط مواضعه وأدائه بالتلقي.","عِوَجًا ۜ قَيِّمًا","الكهف: 1-2؛ ومن مواضع السكت المعروفة مواضع مخصوصة في الرواية","تعرف على مواضع السكت الأربعة المشهورة في طريق الشاطبية، وراجع أداءها مع معلم.")
        )
        val groups = listOf("الكل","المدخل","المخارج","الصفات","النون والتنوين","الميم الساكنة","الغنة","القلقلة","اللامات","الراء","المدود","الوقف")
        var activeGroup = "الكل"
        val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        root.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(chips) })
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(content)
        val tajweedProgress = getSharedPreferences("deeni_tajweed_progress", MODE_PRIVATE)
        fun lessonProgressKey(lesson: Lesson) = "done_" + lesson.group + "_" + lesson.title
        fun drawLessons() {
            content.removeAllViews()
            val completedCount = lessons.count { tajweedProgress.getBoolean(lessonProgressKey(it), false) }
            content.addView(label("إنجاز المنهج: $completedCount من ${lessons.size} درس", 15f, gold, true))
            val filtered = lessons.filter { activeGroup == "الكل" || it.group == activeGroup }
            content.addView(label("عدد الدروس المعروضة: " + filtered.size, 13f, muted, true))
            filtered.forEachIndexed { index, lesson ->
                val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = shape(panel, 18f); setPadding(15, 12, 15, 12) }
                card.addView(label((index + 1).toString() + ". " + lesson.title, 18f, gold, true))
                card.addView(label(lesson.rule, 15f, cream))
                card.addView(label("المثال القرآني: ﴿" + lesson.example + "﴾", 18f, cream, true))
                card.addView(label("الموضع: " + lesson.place, 12f, muted))
                card.addView(label("التطبيق: " + lesson.exercise, 13f, muted))
                val completed = CheckBox(this).apply {
                    text = "أكملت هذا الدرس"
                    setTextColor(cream)
                    isChecked = tajweedProgress.getBoolean(lessonProgressKey(lesson), false)
                    setOnCheckedChangeListener { _, checked ->
                        tajweedProgress.edit().putBoolean(lessonProgressKey(lesson), checked).apply()
                        val totalDone = lessons.count { tajweedProgress.getBoolean(lessonProgressKey(it), false) }
                        content.getChildAt(0)?.let { (it as? TextView)?.text = "إنجاز المنهج: $totalDone من ${lessons.size} درس" }
                    }
                }
                card.addView(completed)
                content.addView(card)
                content.addView(space(8))
            }
            content.addView(label("منهج تعليمي موسع للمراجعة، وليس بديلًا عن التلقي. تختلف بعض أوجه الأداء باختلاف الرواية والطريق؛ اضبطها مع معلم متقن، وارجع إلى المصحف للتحقق من الرسم والضبط.", 12f, muted))
            content.addView(button("التطبيق على المصحف", true) { showQuran() })
        }
        groups.forEach { group ->
            chips.addView(button(group, group == "الكل") {
                activeGroup = group
                drawLessons()
            }, LinearLayout.LayoutParams(-2, -2).apply { setMargins(3, 0, 3, 0) })
        }
        drawLessons()
        root.addView(button("الرئيسية", false) { showHome() })
    }

    private fun showProphetStories() {
        currentPage = "prophet_stories"
        base("قصص من القرآن", "مختارات موجزة مع مواضع الاستدلال — اقرأ الآيات في المصحف")
        val stories = listOf(
            Triple("آدم عليه السلام", "خلق الله آدم وعلّمه، وأمر الملائكة بالسجود له، وذكر القرآن توبته وقبول الله لها.", "البقرة: 30–39؛ طه: 115–123"),
            Triple("نوح عليه السلام", "دعا قومه إلى عبادة الله زمنًا طويلًا، وصبر على دعوتهم، ونجّاه الله ومن آمن معه.", "هود: 25–49؛ نوح: 1–28"),
            Triple("إبراهيم عليه السلام", "حاجّ قومه في التوحيد، وابتلي فصبر، وجعل الله في ذريته النبوة والكتاب.", "الأنبياء: 51–70؛ الصافات: 99–111"),
            Triple("يوسف عليه السلام", "ذكر القرآن رؤياه وما مرّ به من ابتلاءات، ثم مكّن الله له في الأرض وجمع شمله بأهله.", "يوسف: 4–101"),
            Triple("موسى عليه السلام", "أرسله الله إلى فرعون، وأيّده بالآيات، ونجّاه وبني إسرائيل من فرعون.", "طه: 9–98؛ القصص: 3–46"),
            Triple("يونس عليه السلام", "ذكر القرآن دعاءه وتسبيحه، فاستجاب الله له ونجّاه من الغم.", "الأنبياء: 87–88؛ الصافات: 139–148"),
            Triple("مريم عليها السلام", "ذكر القرآن اصطفاءها وبشارتها بعيسى عليه السلام، وجعل قصتها آية للمؤمنين.", "آل عمران: 35–47؛ مريم: 16–36"),
            Triple("عيسى عليه السلام", "ذكر القرآن ولادته المعجزة، ودعوته، وما أيده الله به من آيات بإذنه.", "آل عمران: 45–55؛ المائدة: 110–120")
        )
        stories.forEachIndexed { index, story ->
            root.addView(label("${index + 1}. ${story.first}", 19f, gold, true))
            root.addView(label(story.second, 16f, cream))
            root.addView(label("مواضع القصة: ${story.third}", 13f, muted))
            root.addView(button("افتح المصحف", false) { showQuran() })
            root.addView(space(8))
        }
        root.addView(label("هذه خلاصات موجزة لا تغني عن قراءة الآيات كاملة، ولا تتضمن تفاصيل من الإسرائيليات أو روايات غير موثقة.", 13f, muted))
        root.addView(button("الرئيسية", true) { showHome() })
    }

    private fun showCompanionStories() {
        currentPage = "companions"
        base("قصص الصحابة والصحابيات", "تعريفات موجزة — التفاصيل التاريخية تُراجع في مصادر السيرة والتراجم")
        val people = listOf(
            Triple("أبو بكر الصديق رضي الله عنه", "من أوائل من آمنوا برسول الله ﷺ، وصاحبه في الهجرة، وتولى الخلافة بعد وفاته.", "التوبة: 40؛ كتب السيرة والتراجم"),
            Triple("عمر بن الخطاب رضي الله عنه", "ثاني الخلفاء الراشدين، عُرف بالعدل والحزم، واتسعت الدولة الإسلامية في عهده.", "كتب فضائل الصحابة والتاريخ الإسلامي"),
            Triple("عثمان بن عفان رضي الله عنه", "ثالث الخلفاء الراشدين، ومن أبرز أعمال عهده جمع الناس على المصحف الإمام وإرسال نسخه إلى الأمصار.", "صحيح البخاري، فضائل القرآن؛ كتب التاريخ"),
            Triple("علي بن أبي طالب رضي الله عنه", "ابن عم رسول الله ﷺ، وزوج فاطمة رضي الله عنها، ورابع الخلفاء الراشدين.", "كتب السيرة والحديث والتراجم"),
            Triple("خديجة بنت خويلد رضي الله عنها", "زوج النبي ﷺ الأولى، وآزرته في بداية الدعوة، وهي من السابقات إلى الإيمان.", "صحيح البخاري، بدء الوحي؛ كتب السيرة"),
            Triple("بلال بن رباح رضي الله عنه", "من السابقين إلى الإسلام، واشتهر بالأذان، وصبر على الأذى في مكة.", "كتب السيرة وفضائل الصحابة"),
            Triple("مصعب بن عمير رضي الله عنه", "من السابقين إلى الإسلام، وبعثه النبي ﷺ إلى المدينة قبل الهجرة ليعلّم أهلها القرآن والإسلام.", "كتب السيرة النبوية"),
            Triple("أسماء بنت أبي بكر رضي الله عنها", "لها دور معروف في أحداث الهجرة، وتذكر كتب السيرة لقب ذات النطاقين.", "صحيح البخاري، مناقب الأنصار؛ كتب السيرة")
        )
        people.forEachIndexed { index, person ->
            root.addView(label("${index + 1}. ${person.first}", 18f, gold, true))
            root.addView(label(person.second, 16f, cream))
            root.addView(label("للمراجعة: ${person.third}", 13f, muted))
            root.addView(space(10))
        }
        root.addView(label("هذه نبذات تعريفية وليست تراجم مفصلة. قد تختلف بعض تفاصيل الروايات التاريخية، لذا لا تُنسب الأقوال أو الحكايات إلا بعد التحقق من أسانيدها ومصادرها.", 13f, muted))
        root.addView(button("السيرة النبوية", true) { showSirahTimeline() })
        root.addView(button("الرئيسية", false) { showHome() })
    }

    private fun showFiqhBasics() {
        currentPage = "fiqh_basics"
        base("مبادئ الفقه", "معلومات تمهيدية للتعلم وليست فتوى لحالة خاصة")
        val items = listOf(
            "الطهارة: تشمل أحكام إزالة النجاسة والوضوء والغسل والتيمم، وتفاصيلها تُدرس من أبواب الفقه المعتمدة.",
            "الصلاة: لها شروط وأركان وواجبات وسنن، وينبغي تعلم صفتها من مصدر موثوق ومعلم.",
            "الصيام: الإمساك عن المفطرات من طلوع الفجر إلى غروب الشمس بنية، وله أعذار ورخص شرعية.",
            "الزكاة: عبادة مالية لها شروط وأنصبة ومقادير وأصناف مستحقة، وتفاصيل الحساب تحتاج معرفة المال ونوعه.",
            "المعاملات: الأصل مراعاة التراضي والوضوح والعدل واجتناب الربا والغش والضرر، وتفاصيل العقود تُسأل فيها جهة علمية.",
            "اختلاف الفقهاء: قد تختلف بعض المسائل الاجتهادية بين المذاهب المعتبرة، فلا يُحوّل الخلاف إلى خصومة."
        )
        items.forEachIndexed { index, item ->
            root.addView(label("الموضوع ${index + 1}", 17f, gold, true))
            root.addView(label(item, 16f, cream))
            root.addView(space(10))
        }
        root.addView(label("للفتوى العملية، ارجع إلى عالم موثوق أو جهة إفتاء رسمية في بلدك، واذكر تفاصيل المسألة كاملة.", 13f, muted))
        root.addView(button("الرئيسية", true) { showHome() })
    }

    private fun showComing(title: String) {
        base(title, "هذا القسم ضمن خطة التطوير")
        root.addView(label("لن نضع محتوى غير موثق أو أزرارًا وهمية. سيُضاف هذا القسم بعد توفير مصادر موثوقة واختبار وظائفه.", 16f, cream))
        root.addView(button("الرئيسية", true) { showHome() })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != 77) return
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "تم السماح بالميكروفون.", Toast.LENGTH_SHORT).show()
            if (pendingRecognitionAfterPermission && recitationStatusView != null && recitationTextView != null) {
                pendingRecognitionAfterPermission = false
                recitationStatusView?.post { beginRecognition(recitationStatusView!!, recitationTextView!!) }
            }
        } else {
            pendingRecognitionAfterPermission = false
            Toast.makeText(this, "لم يُسمح بالميكروفون. يمكنك قراءة المصحف دون التسميع، وتفعيل الإذن لاحقًا من إعدادات التطبيق.", Toast.LENGTH_LONG).show()
        }
    }

}
