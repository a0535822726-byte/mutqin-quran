package com.deeni.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private val ink = Color.rgb(25, 48, 43)
    private val green = Color.rgb(25, 112, 83)
    private val paper = Color.rgb(247, 248, 244)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = paper
        window.navigationBarColor = paper
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        showHome()
    }

    private fun showHome() {
        val scroll = ScrollView(this).apply { setBackgroundColor(paper) }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 30, 22, 28)
            layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
        }
        val title = TextView(this).apply {
            text = "ديني"
            textSize = 32f
            setTextColor(ink)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "رفيقك للقرآن والسيرة والعبادة"
            textSize = 16f
            setTextColor(green)
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 22)
        }
        page.addView(title)
        page.addView(subtitle)
        val welcome = TextView(this).apply {
            text = "ابدأ يومك بذكر الله"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(18, 28, 18, 28)
            setBackgroundColor(green)
        }
        page.addView(welcome)
        val sections = listOf(
            "القرآن الكريم" to "مصحف وقراءة وحفظ ومراجعة",
            "التسميع" to "مساحة مخصصة للتسميع الصوتي (قيد التطوير)",
            "التجويد" to "تعلم أحكام التلاوة",
            "السيرة النبوية" to "محطات من السيرة وأحداثها",
            "الحديث الشريف" to "أحاديث مع مصادرها ودرجاتها",
            "الأذكار والأدعية" to "أذكار يومية وأدعية مأثورة",
            "الغزوات والتاريخ" to "أحداث تاريخية موثقة",
            "الألعاب والمسابقات" to "اختبر معلوماتك الإسلامية",
            "التسبيح" to "عداد تسبيح بسيط"
        )
        sections.forEach { (name, detail) ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(18, 15, 18, 15)
                setBackgroundColor(Color.WHITE)
                elevation = 3f
                val heading = TextView(this@MainActivity).apply {
                    text = name
                    textSize = 19f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(ink)
                }
                val description = TextView(this@MainActivity).apply {
                    text = detail
                    textSize = 14f
                    setTextColor(Color.DKGRAY)
                    setPadding(0, 5, 0, 0)
                }
                addView(heading)
                addView(description)
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 12, 0, 0)
            page.addView(card, params)
        }
        val note = TextView(this).apply {
            text = "هذه نسخة تأسيسية. ستُضاف الوظائف والمحتويات بعد اختبار البناء، ولن تُعرض المزايا غير المنفذة على أنها تعمل."
            textSize = 13f
            setTextColor(Color.DKGRAY)
            setPadding(4, 20, 4, 4)
        }
        page.addView(note)
        scroll.addView(page)
        setContentView(scroll)
    }
}
