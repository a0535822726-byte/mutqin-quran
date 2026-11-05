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
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.widget.*
import java.util.Locale

class MainActivity : Activity() {
    private var lightTheme = false
    private var bg = Color.rgb(9, 28, 25)
    private var panel = Color.rgb(18, 48, 42)
    private var panel2 = Color.rgb(25, 61, 53)
    private var gold = Color.rgb(218, 183, 111)
    private var cream = Color.rgb(246, 241, 226)
    private var muted = Color.rgb(174, 194, 184)

    private fun applyTheme() {
        if (lightTheme) {
            bg = Color.rgb(246, 243, 234)
            panel = Color.rgb(255, 255, 250)
            panel2 = Color.rgb(232, 224, 204)
            gold = Color.rgb(126, 91, 35)
            cream = Color.rgb(37, 53, 46)
            muted = Color.rgb(94, 111, 101)
        } else {
            bg = Color.rgb(9, 28, 25)
            panel = Color.rgb(18, 48, 42)
            panel2 = Color.rgb(25, 61, 53)
            gold = Color.rgb(218, 183, 111)
            cream = Color.rgb(246, 241, 226)
            muted = Color.rgb(174, 194, 184)
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
    private var pendingHighlight: Pair<Int, Int>? = null
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
        window.decorView.systemUiVisibility = 0
        lightTheme = getSharedPreferences("deeni", MODE_PRIVATE).getBoolean("light_theme", false)
        applyTheme()
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
        root.addView(button(if (lightTheme) "التبديل إلى الوضع الداكن" else "التبديل إلى الوضع الفاتح", false) {
            lightTheme = !lightTheme
            getSharedPreferences("deeni", MODE_PRIVATE).edit().putBoolean("light_theme", lightTheme).apply()
            applyTheme()
            showHome()
        })
        root.addView(space(10))
        card("التفريغ الصوتي التجريبي", "اعرض الكلمات التي يلتقطها الهاتف دون ادعاء تصحيح قرآني", "🎙") { showQuran(true) }
        card("البحث في القرآن", "ابحث عن كلمة أو عبارة في الآيات", "⌕") { showQuranSearch() }
        card("الآيات المحفوظة", "اجمع الآيات التي تريد الرجوع إليها", "☆") { showSavedAyahs() }
        card("التفسير الميسر", "اقرأ تفسير الآية مع نصها القرآني", "📖") { showTafsir() }
        card("مكتبة القراء", "اختر قارئك المفضل", "♫") { chooseReader() }
        card("الأذكار والتسبيح", "ذكر يومي وعداد محفوظ", "✧") { showTasbeeh() }
        card("الحفظ والمراجعة", "خطة يومية وسجل إنجاز محفوظ", "۞") { showRevision() }
        card("السيرة والحديث", "محطات السيرة وأحاديث مع مراجعها", "☾") { showIslamicLibrary() }
        card("الأدعية القرآنية", "أدعية من القرآن مع مواضعها وإمكانية نسخها", "🤲") { showDuas() }
        card("ملاحظاتي", "احفظ فوائدك وملاحظاتك محليًا", "✎") { showNotes() }
        card("الأذكار", "أذكار مختارة مع عداد لكل ذكر", "۞") { showAdhkar() }
        card("المسابقات الإسلامية", "اختبر معلوماتك في القرآن والسيرة والحديث", "🏆") { showQuiz() }
        card("أسماء الله الحسنى", "معانٍ مختصرة لأسماء الله الحسنى", "✧") { showNames() }
        card("مواقيت الصلاة", "اعرض مواقيت الصلاة حسب مدينتك", "◷") { showPrayerTimes() }
        card("اتجاه القبلة", "بوصلة تتبع اتجاه القبلة بحساس الهاتف", "🧭") { showQibla() }
        card("تعلم التجويد", "دروس تأسيسية وأمثلة للتدرب", "۞") { showTajweed() }
        card("قصص الأنبياء", "مختارات من القصص الواردة في القرآن", "☾") { showProphetStories() }
        card("قصص الصحابة", "لمحات موجزة عن رجال ونساء من الصحابة", "☼") { showCompanionStories() }
        card("مبادئ الفقه", "تعريفات وأحكام عامة للتعلم", "⚖") { showFiqhBasics() }
        root.addView(label("المصحف يعرض نص الصفحات عند الاتصال بالإنترنت. بعض الأدوات المتقدمة مثل التسميع المتخصص ومزامنة القراء ما زالت تحتاج محركًا واختبارات إضافية.", 12f, muted))
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
    }

    private fun highlightMushafAyah(surah: Int, ayah: Int) {
        mushafWebView?.evaluateJavascript("window.highlightAyah && window.highlightAyah($surah,$ayah)", null)
    }

    private fun showSelectedAyah(surah: Int, ayah: Int) {
        ayahStatus?.text = "جارٍ تحميل الآية $surah:$ayah…"
        Thread {
            val result = QuranPageSource.loadAyahDetails(surah, ayah)
            runOnUiThread {
                if (currentPage != "quran") return@runOnUiThread
                if (result.isSuccess) {
                    val (text, page) = result.getOrThrow()
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
        base("المصحف الشريف", "مصحف المدينة النبوية • رواية حفص • صفحات مطبوعة")
        root.addView(label("القارئ المحدد: $selectedReader", 14f, gold, true))
        root.addView(button("تغيير القارئ", false) { chooseReader() })
        root.addView(label("صفحات مصحف المدينة النبوية الصادر عن مجمع الملك فهد لطباعة المصحف الشريف. يلزم الاتصال بالإنترنت لعرض الصفحة.", 12f, muted))
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
        pageStatus = label("جارٍ تحميل صفحة المصحف…", 14f, gold, true)
        root.addView(pageStatus!!)
        mushafWebView = WebView(this).apply {
            setBackgroundColor(Color.rgb(248, 243, 228))
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
                override fun onPageFinished(view: WebView?, url: String?) {
                    pageStatus?.text = "صفحة $mushafPage من 604 • اضغط على الآية لتحديدها"
                }
            }
        }
        root.addView(mushafWebView!!, LinearLayout.LayoutParams(-1, 560))
        ayahStatus = label("اضغط على موضع آية في الصفحة لعرض نصها.", 19f, cream, true)
        ayahStatus!!.background = shape(panel, 16f)
        ayahStatus!!.setPadding(14, 14, 14, 14)
        root.addView(ayahStatus!!)
        root.addView(space(10))
        root.addView(button("حفظ هذه الصفحة كموضع متابعة", true) {
            getSharedPreferences("deeni", MODE_PRIVATE).edit().putInt("mushaf_page", mushafPage).apply()
            Toast.makeText(this, "تم حفظ الصفحة $mushafPage", Toast.LENGTH_SHORT).show()
        })
        root.addView(button("إضافة الصفحة إلى العلامات المرجعية", false) {
            val prefs = getSharedPreferences("deeni", MODE_PRIVATE)
            val saved = prefs.getStringSet("mushaf_bookmarks", emptySet())?.toMutableSet() ?: mutableSetOf()
            saved.add(mushafPage.toString())
            prefs.edit().putStringSet("mushaf_bookmarks", saved).apply()
            Toast.makeText(this, "أضيفت الصفحة $mushafPage إلى العلامات", Toast.LENGTH_SHORT).show()
        })
        root.addView(button("عرض العلامات المرجعية", false) { showMushafBookmarks() })
        root.addView(button("اختيار سورة وتشغيل تلاوة القارئ", true) { showSurahPicker() })
        playbackStatus = label("التلاوة متوقفة", 13f, muted)
        root.addView(playbackStatus!!)
        root.addView(button("إيقاف مؤقت / متابعة", false) { toggleQuranPlayback() })
        root.addView(button("إيقاف التلاوة", false) { stopQuranAudio() })
        root.addView(button("تكرار الآية الحالية: تشغيل / إيقاف", false) {
            val timing = timingRows.firstOrNull { it.ayah == currentTimedAyah }
            if (timing == null) {
                Toast.makeText(this, "ابدأ تلاوة تتوفر لها بيانات توقيت الآيات وانتظر ظهور الآية الحالية.", Toast.LENGTH_LONG).show()
            } else {
                repeatTiming = if (repeatTiming == null) timing else null
                playbackStatus?.text = if (repeatTiming != null) "تكرار الآية ${timing.ayah} مفعّل" else "تم إيقاف تكرار الآية"
            }
        })
        root.addView(button("سرعة التلاوة: 0.8× / 1× / 1.2×", false) {
            playbackSpeed = when (playbackSpeed) { 1.0f -> 0.8f; 0.8f -> 1.2f; else -> 1.0f }
            try {
                quranPlayer?.let { player -> player.playbackParams = player.playbackParams.setSpeed(playbackSpeed) }
                playbackStatus?.text = "سرعة التلاوة ${playbackSpeed}×"
            } catch (_: Exception) {
                Toast.makeText(this, "تعذر تغيير السرعة في هذا الجهاز.", Toast.LENGTH_SHORT).show()
            }
        })
        root.addView(space(14))
        root.addView(label("التسميع الصوتي", 20f, cream, true))
        root.addView(label("التعرف الصوتي العام قد يخطئ في ألفاظ القرآن؛ هذه الأداة ليست محركًا متخصصًا ولا تصحح التجويد.", 13f, muted))
        val status = label("تفريغ صوتي عام — ليس تصحيحًا للقرآن", 14f, gold, true)
        root.addView(status)
        val live = TextView(this).apply { text = "النص الملتقط يظهر هنا"; textSize = 17f; setTextColor(cream); background = shape(panel, 16f); setPadding(14, 14, 14, 14) }
        root.addView(live)
        root.addView(button("ابدأ التفريغ الصوتي", true) { beginRecognition(status, live, mutableListOf()) })
        root.addView(button("إيقاف التسميع", false) { stopRecognition(); status.text = "تم إيقاف التسميع" })
        root.addView(button("العودة للرئيسية", false) { stopRecognition(); showHome() })
        loadMushafPage(mushafPage)
        if (startMic) root.post { beginRecognition(status, live, mutableListOf()) }
    }

    private fun mushafHtml(page: Int): String {
        val safePage = page.coerceIn(1, 604)
        return """
            <!doctype html>
            <html lang="ar" dir="rtl">
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=4">
              <style>
                html,body{margin:0;padding:0;background:#f8f3e4}
                #page{width:100%;margin:0 auto}
                svg{display:block;width:100%;height:auto;max-width:100%}
                .ayahPolygon{cursor:pointer;pointer-events:all}
              </style>
            </head>
            <body>
              <div id="page"></div>
              <script>
                (async function(){
                  const host=document.getElementById('page');
                  const number=String($safePage).padStart(3,'0');
                  const url='https://raw.githubusercontent.com/quran-ws/quran-svg/main/mushafs/hafs/kfqc/svg/'+number+'.svg';
                  try{
                    const response=await fetch(url);
                    if(!response.ok) throw new Error('HTTP '+response.status);
                    const source=await response.text();
                    const parsed=new DOMParser().parseFromString(source,'image/svg+xml');
                    const svg=parsed.documentElement;
                    if(svg.querySelector('parsererror')) throw new Error('SVG parse error');
                    svg.removeAttribute('width');
                    svg.removeAttribute('height');
                    host.replaceChildren(document.importNode(svg,true));
                    const mounted=host.querySelector('svg');
                    mounted.style.width='100%';
                    mounted.style.height='auto';
                    try{
                      const content=mounted.querySelector('#content')||mounted;
                      const box=content.getBBox();
                      if(box.width>0&&box.height>0){
                        const pad=5;
                        mounted.setAttribute('viewBox',(box.x-pad)+' '+(box.y-pad)+' '+(box.width+pad*2)+' '+(box.height+pad*2));
                      }
                    }catch(ignore){}
                    mounted.querySelectorAll('.ayahPolygon').forEach(function(path){
                      path.style.pointerEvents='all';
                      path.addEventListener('click',function(){
                        mounted.querySelectorAll('.ayahPolygon').forEach(function(p){p.setAttribute('fill-opacity','0');});
                        path.setAttribute('fill','#d6b76f');
                        path.setAttribute('fill-opacity','0.38');
                        const s=Number(path.getAttribute('surah'));
                        const a=Number(path.getAttribute('ayah'));
                        if(s>0&&a>0&&window.DeeniMushaf) window.DeeniMushaf.onAyahSelected(s,a);
                      });
                    });
                    window.highlightAyah=function(s,a){
                      const found=Array.from(mounted.querySelectorAll('.ayahPolygon')).find(function(p){
                        return Number(p.getAttribute('surah'))===s && Number(p.getAttribute('ayah'))===a;
                      });
                      if(!found) return false;
                      mounted.querySelectorAll('.ayahPolygon').forEach(function(p){p.setAttribute('fill-opacity','0');});
                      found.setAttribute('fill','#d6b76f');
                      found.setAttribute('fill-opacity','0.38');
                      return true;
                    };
                    if(window.DeeniMushaf) window.DeeniMushaf.onPageReady();
                  }catch(error){
                    host.innerHTML='<p style="font:18px sans-serif;color:#704b20;text-align:center;padding:24px">تعذر تحميل صفحة المصحف. تحقق من الإنترنت ثم أعد المحاولة من التطبيق.</p>';
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
        mushafWebView?.loadDataWithBaseURL("https://appassets.androidplatform.net/", mushafHtml(page), "text/html", "UTF-8", null)
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

    private fun showSurahPicker() {
        val prefs = getSharedPreferences("deeni", MODE_PRIVATE)
        val server = prefs.getString("audio_server", null)
        if (server.isNullOrBlank()) {
            Toast.makeText(this, "اختر القارئ أولًا", Toast.LENGTH_SHORT).show()
            chooseReader()
            return
        }
        val surahs = listOf(
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
        val timingId = prefs.getInt("timing_read_id", -1)
        if (timingId > 0) {
            Thread {
                val timings = ReciterSource.loadTimings(surah, timingId)
                if (timings.isSuccess) {
                    runOnUiThread { timingRows = timings.getOrThrow() }
                }
            }.start()
        }
        val url = server.trimEnd('/') + "/" + surah.toString().padStart(3, '0') + ".mp3"
        val player = MediaPlayer()
        quranPlayer = player
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
        base("الآيات المحفوظة", "آيات أضفتها من نتائج البحث لتعود إليها بسهولة")
        val prefs = getSharedPreferences("deeni", MODE_PRIVATE)
        val saved = prefs.getStringSet("saved_ayahs", emptySet())?.toList()
            ?.mapNotNull { value ->
                val parts = value.split(":")
                val surah = parts.getOrNull(0)?.toIntOrNull()
                val ayah = parts.getOrNull(1)?.toIntOrNull()
                if (surah != null && ayah != null && surah in 1..114 && ayah > 0) surah to ayah else null
            }?.sortedWith(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second }) ?: emptyList()
        if (saved.isEmpty()) {
            root.addView(label("لا توجد آيات محفوظة. ابحث في القرآن ثم اضغط حفظ الآية.", 15f, muted))
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
        root.addView(label("المدينة", 15f, gold, true))
        root.addView(city)
        root.addView(label("اكتب اسم المدينة بالإنجليزية لضمان تعرف خدمة المواقيت عليها. تُستخدم طريقة أم القرى للحساب، وقد تختلف دقائق قليلة عن تقويم المسجد المحلي.", 12f, muted))
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
        val storedCity = prefs.getString("prayer_city", null)
        val city = if (storedCity.isNullOrBlank() || storedCity == "أبو عريش") "Abu Arish" else storedCity
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
        base("تعلم التجويد", "دروس تأسيسية مختصرة — التطبيق للتعلم ولا يصحح مخارج الحروف آليًا")
        val lessons = listOf(
            Triple("معنى التجويد", "إعطاء الحروف حقها ومستحقها عند تلاوة القرآن، مع مراعاة صفاتها وأحكامها.", "ابدأ بالاستماع إلى قارئ متقن ثم اعرض قراءتك على معلم."),
            Triple("النون الساكنة والتنوين", "أحكامها المشهورة: الإظهار، الإدغام، الإقلاب، والإخفاء.", "تعلّم حروف كل حكم من معلم التجويد، ولا تعتمد على التخمين."),
            Triple("أحكام الميم الساكنة", "الإخفاء الشفوي، الإدغام الشفوي، والإظهار الشفوي.", "انتبه إلى موضع الشفتين عند النطق."),
            Triple("المد الطبيعي", "يمد بمقدار حركتين في مواضعه المعروفة، ما لم يأت سبب يغيّر حكمه.", "مقدار المد يضبط بالمشافهة والتلقي."),
            Triple("القلقلة", "صفة تظهر في حروف قطب جد عند سكونها.", "تجنب المبالغة في توليد حركة بعد الحرف."),
            Triple("مخارج الحروف", "تدرس المخارج من الجوف والحلق واللسان والشفتين والخيشوم.", "التصحيح العملي يحتاج سماعًا مباشرًا من معلم متقن.")
        )
        lessons.forEachIndexed { index, item ->
            root.addView(label("الدرس ${index + 1}: ${item.first}", 18f, gold, true))
            root.addView(label(item.second, 16f, cream))
            root.addView(label(item.third, 13f, muted))
            root.addView(space(10))
        }
        root.addView(label("تنبيه: هذه مقدمة تعليمية وليست منهجًا كاملًا، وقد تختلف بعض تفاصيل الأداء بحسب الرواية والطريق. تعلّم التلاوة بالمشافهة من معلم مؤهل.", 13f, muted))
        root.addView(button("فتح المصحف للتطبيق", true) { showQuran() })
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
        if (requestCode == 77 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "تم السماح بالميكروفون. اضغط ابدأ التسميع.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onStop() { stopRecognition(); stopQiblaSensor(); stopQuranAudio(); super.onStop() }
}
