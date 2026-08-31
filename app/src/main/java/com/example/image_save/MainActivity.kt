package com.example.image_save

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val engine = EventEngine()
    private var selectedType: CreatureType = CreatureType.BABY_BOY
    private var ageInMonths = 4

    private lateinit var rootLayout: LinearLayout
    private lateinit var titleText: TextView
    private lateinit var amountText: TextView
    private lateinit var summaryText: TextView
    private lateinit var eventListText: TextView
    private lateinit var btnBabyBoy: Button
    private lateinit var btnBabyGirl: Button
    private lateinit var btnCat: Button
    private lateinit var btnDog: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rootLayout = findViewById(R.id.rootLayout)
        titleText = findViewById(R.id.titleText)
        amountText = findViewById(R.id.amountText)
        summaryText = findViewById(R.id.summaryText)
        eventListText = findViewById(R.id.eventListText)
        btnBabyBoy = findViewById(R.id.btnBabyBoy)
        btnBabyGirl = findViewById(R.id.btnBabyGirl)
        btnCat = findViewById(R.id.btnCat)
        btnDog = findViewById(R.id.btnDog)

        btnBabyBoy.setOnClickListener { selectType(CreatureType.BABY_BOY) }
        btnBabyGirl.setOnClickListener { selectType(CreatureType.BABY_GIRL) }
        btnCat.setOnClickListener { selectType(CreatureType.CAT) }
        btnDog.setOnClickListener { selectType(CreatureType.DOG) }

        selectType(selectedType)
    }

    private fun selectType(type: CreatureType) {
        selectedType = type
        val profile = CreatureProfile(
            id = "profile-${type.name.lowercase()}",
            name = when (type) {
                CreatureType.BABY_BOY -> "小宝"
                CreatureType.BABY_GIRL -> "小花"
                CreatureType.CAT -> "小橘"
                CreatureType.DOG -> "小柴"
            },
            type = type,
            birthTimeMillis = System.currentTimeMillis() - (ageInMonths * 30L * 24L * 60L * 60L * 1000L),
            ageInMonths = ageInMonths,
            totalExpense = 0.0
        )

        val events = engine.getTriggeredEvents(profile, ageInMonths)
        val total = engine.totalFor(profile, ageInMonths)

        titleText.text = "${profile.name} · ${ageInMonths}个月"
        amountText.text = "累计花销：¥${String.format("%,.0f", total)}"
        summaryText.text = "当前角色：${type.label} ｜ 待处理事项：${events.size}个"

        val eventText = if (events.isEmpty()) {
            "暂无待处理事件"
        } else {
            events.joinToString("\n") { event ->
                "• ${event.title}：¥${String.format("%.0f", event.amount)}"
            }
        }
        eventListText.text = eventText

        val color = when (type) {
            CreatureType.BABY_BOY -> Color.parseColor("#90CAF9")
            CreatureType.BABY_GIRL -> Color.parseColor("#F48FB1")
            CreatureType.CAT -> Color.parseColor("#A5D6A7")
            CreatureType.DOG -> Color.parseColor("#FFCC80")
        }
        rootLayout.setBackgroundColor(Color.parseColor("#FFF8E1"))
        titleText.setTextColor(Color.parseColor("#333333"))
        amountText.setTextColor(color)

        btnBabyBoy.setBackgroundColor(if (type == CreatureType.BABY_BOY) color else Color.parseColor("#F5F5F5"))
        btnBabyGirl.setBackgroundColor(if (type == CreatureType.BABY_GIRL) color else Color.parseColor("#F5F5F5"))
        btnCat.setBackgroundColor(if (type == CreatureType.CAT) color else Color.parseColor("#F5F5F5"))
        btnDog.setBackgroundColor(if (type == CreatureType.DOG) color else Color.parseColor("#F5F5F5"))
    }
}
