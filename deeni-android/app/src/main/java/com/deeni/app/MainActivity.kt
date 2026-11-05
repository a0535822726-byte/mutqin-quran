package com.deeni.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private val ink = Color.rgb(25, 48, 43)
    private val green = Color.rgb(25, 112, 83)
    private val paper = Color.rgb(247, 248, 244)
    private lateinit var page: LinearLayout
    private var tasbeehCount = 0
    private var quizIndex = 0
    private var quizScore = 0

    private data class Question(val prompt: String, val answers: List<String>, val correct: Int, val note: String)
    private val questions = listOf(
        Question("كم عدد سور القرآن الكريم؟", listOf("110", "114", "120", "99"), 1, "عدد سور القرآن الكريم 114 سورة."),
        Question("ما السورة التي تُسمّى أم الكتاب؟", listOf("البقرة", "الإخلاص", "الفاتحة", "يس"), 2, "سورة الفاتحة تُسمّى أم الكتاب."),
        Question("كم عدد أركان الإسلام؟", listOf("4", "5", "6", "7"), 1, "أركان الإسلام خمسة."),
        Question("ما اسم الغار الذي نزل فيه الوحي أول مرة؟", listOf("ثور", "حراء", "أحد", "قباء"), 1, "كان ذلك في غار حراء.")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = paper
        window.navigationBarColor = paper
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        showHome()
    }

    private fun setupPage(): ScrollView {
        page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 26, 22, 28)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(paper)
        }
        return ScrollView(this).apply {
            setBackgroundColor(paper)
            addView(page)
        }
    }

    private fun text(value: String, size: Float = 17f, color: Int = ink, bold: Boolean = false): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(4, 9, 4, 9)
        }
    }

    private fun addButton(label: String, action: () -> Unit) {
        val button = Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(green)
            setOnClickListener { action() }
        }
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, 7, 0, 7)
        page.addView(button, params)
    }

    private fun heading(title: String, subtitle: String) {
        page.addView(text(title, 28f, ink, true))
        page.addView(text(subtitle, 15f, green))
        addButton("العودة للرئيسية", ::showHome)
    }

    private fun showHome() {
        val scroll = setupPage()
        page.addView(text("ديني", 34f, ink, true).apply { gravity = Gravity.CENTER })
        page.addView(text("رفيقك للقرآن والسيرة والعبادة", 16f, green).apply {
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 22)
        })
        page.addView(text("ابدأ يومك بذكر الله", 21f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            setPadding(18, 24, 18, 24)
            setBackgroundColor(green)
        })
        val sections = listOf(
            Triple("القرآن الكريم", "قراءة المصحف والحفظ والمراجعة", "quran"),
            Triple("التسميع الذكي", "التسميع الصوتي وتحليل الأخطاء — قيد التطوير", "recite"),
            Triple("التجويد", "أحكام التلاوة ومخارج الحروف", "tajweed"),
            Triple("السيرة النبوية", "محطات السيرة ومصادرها", "seerah"),
            Triple("الحديث الشريف", "أحاديث ومراجعها", "hadith"),
            Triple("الأذكار والأدعية", "أذكار يومية مختارة", "azkar"),
            Triple("الغزوات والتاريخ", "أحداث السيرة والتاريخ", "history"),
            Triple("الألعاب والمسابقات", "أسئلة إسلامية تفاعلية", "quiz"),
            Triple("التسبيح", "عداد تسبيح يحفظ العدد أثناء فتح الصفحة", "tasbeeh")
        )
        sections.forEach { item ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(18, 13, 18, 13)
                setBackgroundColor(Color.WHITE)
                elevation = 3f
                addView(text(item.first, 19f, ink, true))
                addView(text(item.second, 14f, Color.DKGRAY))
                setOnClickListener { openSection(item.third) }
                isClickable = true
            }
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 10, 0, 0)
            page.addView(card, params)
        }
        page.addView(text("تنبيه: ميزات المصحف الكامل والتسميع الصوتي والتحليل الدقيق لم تُنفّذ بعد، ولن نعرضها على أنها جاهزة قبل اختبارها.", 13f, Color.DKGRAY))
        setContentView(scroll)
    }

    private fun openSection(id: String) {
        when (id) {
            "tasbeeh" -> showTasbeeh()
            "quiz" -> showQuiz()
            "azkar" -> showAzkar()
            "quran" -> showInfo("القرآن الكريم", "هذه مساحة قسم القرآن. النسخة الحالية لا تتضمن بعد نص المصحف الكامل أو صفحات المصحف المعتمدة. يجري تجهيز مصدر قرآني موثوق قبل إدراجه حتى لا تظهر آية بخطأ.")
            "recite" -> showInfo("التسميع الذكي", "التسميع الصوتي الحقيقي يحتاج محرك تعرف على الكلام واختبارًا على قراءات متعددة. زر المايك وتحليل أخطاء الكلمات والتجويد غير جاهز في هذه النسخة.")
            "tajweed" -> showInfo("التجويد", "قسم التجويد قيد الإعداد. ستضاف الأحكام مع أمثلة موثوقة وتوضيح أن التقييم الآلي لا يغني عن شيخ متقن.")
            "seerah" -> showInfo("السيرة النبوية", "سيُبنى هذا القسم على روايات ومراجع معروفة، مع بيان مصدر كل حدث وتجنب القصص غير الثابتة.")
            "hadith" -> showInfo("الحديث الشريف", "ستعرض الأحاديث مع المصدر ودرجة الحديث وشرح مختصر. لم تُدرج بعد قاعدة بيانات حديثية في هذه النسخة.")
            "history" -> showInfo("الغزوات والتاريخ", "سيُرتب المحتوى زمنيًا مع مصادره، والتمييز بين الروايات الصحيحة والضعيفة.")
        }
    }

    private fun showInfo(title: String, body: String) {
        val scroll = setupPage()
        heading(title, "قسم من أقسام تطبيق ديني")
        page.addView(text(body, 17f))
        setContentView(scroll)
    }

    private fun showTasbeeh() {
        val scroll = setupPage()
        heading("التسبيح", "عداد بسيط للتسبيح")
        val countView = text("عدد التسبيحات: $tasbeehCount", 25f, green, true).apply { gravity = Gravity.CENTER }
        page.addView(countView)
        addButton("سبّح", { tasbeehCount++; countView.text = "عدد التسبيحات: $tasbeehCount" })
        addButton("إعادة العداد", { tasbeehCount = 0; countView.text = "عدد التسبيحات: 0" })
        setContentView(scroll)
    }

    private fun showAzkar() {
        val scroll = setupPage()
        heading("الأذكار والأدعية", "أذكار مختارة")
        page.addView(text("أذكار الصباح والمساء", 21f, green, true))
        page.addView(text("سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", 23f, ink, true))
        page.addView(text("لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير.", 19f))
        page.addView(text("أستغفر الله وأتوب إليه.", 21f))
        page.addView(text("تنبيه: هذه قائمة أولية مختصرة، وستُضاف الأذكار مع تخريجها وعدد مرات التكرار ومراجعة النصوص قبل اعتمادها.", 13f, Color.DKGRAY))
        setContentView(scroll)
    }

    private fun showQuiz() {
        val scroll = setupPage()
        heading("المسابقة الإسلامية", "أسئلة قصيرة — اختر إجابة واحدة")
        if (quizIndex >= questions.size) {
            page.addView(text("انتهت الجولة! نتيجتك: $quizScore من ${questions.size}", 23f, green, true))
            addButton("إعادة المسابقة", { quizIndex = 0; quizScore = 0; showQuiz() })
            setContentView(scroll)
            return
        }
        val q = questions[quizIndex]
        page.addView(text("السؤال ${quizIndex + 1} من ${questions.size}", 14f, green))
        page.addView(text(q.prompt, 22f, ink, true))
        q.answers.forEachIndexed { index, answer ->
            addButton(answer, {
                val correct = index == q.correct
                if (correct) quizScore++
                quizIndex++
                showQuizFeedback(correct, q.note)
            })
        }
        setContentView(scroll)
    }

    private fun showQuizFeedback(correct: Boolean, note: String) {
        val scroll = setupPage()
        heading("نتيجة الإجابة", if (correct) "إجابة صحيحة" else "حاول مرة أخرى")
        page.addView(text(note, 18f))
        addButton("السؤال التالي", ::showQuiz)
        setContentView(scroll)
    }
}
