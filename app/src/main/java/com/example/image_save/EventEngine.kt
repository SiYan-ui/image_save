package com.example.image_save

import kotlin.math.max

enum class CreatureType(val label: String) {
    BABY_BOY("男婴"),
    BABY_GIRL("女婴"),
    CAT("猫咪"),
    DOG("狗狗");

    fun key(): String = when (this) {
        BABY_BOY -> "baby_boy"
        BABY_GIRL -> "baby_girl"
        CAT -> "cat"
        DOG -> "dog"
    }
}

data class CreatureProfile(
    val id: String,
    val name: String,
    val type: CreatureType,
    val birthTimeMillis: Long,
    val ageInMonths: Int = 0,
    val totalExpense: Double = 0.0
)

data class LifeEvent(
    val id: String,
    val title: String,
    val description: String,
    val amount: Double,
    val kind: String,
    val monthAge: Int,
    val dueDay: Int
)

class EventEngine {
    private val templates = listOf(
        Template("E_BABY_INIT_0", "baby_boy", "初生儿必备物品", "婴儿诞生后需要购置的基础用品", 1060.0, "一次性", 0, 0, 0),
        Template("E_BABY_FORMULA_REC", "baby_boy", "购买婴儿奶粉1段", "1段配方奶粉需要定期补充", 355.0, "周期性", 0, 6, 0),
        Template("E_BABY_DIAPER_REC", "baby_boy", "购买纸尿裤", "纸尿裤按周期补充", 68.0, "周期性", 0, 4, 2),
        Template("E_BABY_VITAMIN_REC", "baby_boy", "购买维生素D3滴剂", "维生素D3按月定期补充", 25.0, "周期性", 0, 30, 7),
        Template("E_BABY_VACCINE_1", "baby_boy", "第一批疫苗接种", "13价肺炎球菌+五联疫苗第1针", 1110.0, "一次性", 2, 0, 2),
        Template("E_BABY_VACCINE_2", "baby_boy", "第二批疫苗接种", "五联疫苗第2针", 1195.0, "一次性", 3, 0, 4),
        Template("E_BABY_VACCINE_3", "baby_boy", "第三批疫苗接种", "五联疫苗第3针", 640.0, "一次性", 4, 0, 8),
        Template("E_BABY_RICE_CEREAL_REC", "baby_boy", "购买婴儿米粉", "辅食添加阶段的米粉补充", 58.0, "周期性", 6, 12, 3),

        Template("E_CAT_INIT_0", "cat", "初生幼猫必备物品", "猫窝、食盆、猫抓板、玩具等基础需要", 1116.5, "一次性", 0, 0, 0),
        Template("E_CAT_FOOD_REC", "cat", "购买猫粮", "猫粮按月定期补充", 340.0, "周期性", 0, 30, 0),
        Template("E_CAT_LITTER_REC", "cat", "购买猫砂", "猫砂定期补充", 35.0, "周期性", 0, 30, 8),
        Template("E_CAT_VACCINE_1", "cat", "第一批疫苗接种", "猫三联疫苗第1针+驱虫", 252.0, "一次性", 2, 0, 3),
        Template("E_CAT_DEWORMING_REC", "cat", "定期驱虫", "体内外驱虫药按季补充", 129.0, "周期性", 2, 90, 15),
        Template("E_CAT_NEUTER_SPAY", "cat", "绝育手术", "猫咪绝育手术建议安排", 1400.0, "一次性", 8, 0, 5),

        Template("E_DOG_INIT_0", "dog", "初生幼犬必备物品", "狗笼、牵引绳、食盆、狗窝等基础需要", 2656.5, "一次性", 0, 0, 0),
        Template("E_DOG_FOOD_REC", "dog", "购买幼犬粮", "幼犬粮按月定期补充", 850.0, "周期性", 0, 30, 0),
        Template("E_DOG_DEWORMING_REC", "dog", "定期驱虫", "体内外驱虫药按月补充", 105.0, "周期性", 0, 30, 8),
        Template("E_DOG_MILK_REC", "dog", "购买羊奶粉", "断奶过渡用品", 123.5, "周期性", 0, 30, 2),
        Template("E_DOG_VACCINE_1", "dog", "第一批疫苗接种", "犬联苗第1针", 225.0, "一次性", 1, 0, 15),
        Template("E_DOG_VACCINE_2", "dog", "第二批疫苗接种", "犬联苗第2针", 225.0, "一次性", 2, 0, 10),
        Template("E_DOG_VACCINE_RABIES", "dog", "狂犬病疫苗接种", "狂犬病疫苗接种", 225.0, "一次性", 4, 0, 8),
        Template("E_DOG_NEUTER_SPAY", "dog", "绝育手术", "狗狗绝育手术建议安排", 2200.0, "一次性", 8, 0, 5)
    )

    fun getTriggeredEvents(profile: CreatureProfile, monthAge: Int): List<LifeEvent> {
        val initialAge = if (monthAge >= 0) monthAge else profile.ageInMonths
        val filtered = templates.filter { it.creatureType == profile.type.key() }

        return filtered.mapNotNull { template ->
            val triggered = when (template.kind) {
                "一次性" -> initialAge == template.triggerMonth
                "周期性" -> {
                    val startDay = (template.triggerMonth * 30) + template.offsetDay
                    val totalDays = initialAge * 30
                    totalDays >= startDay && ((totalDays - startDay) % template.intervalDays == 0)
                }
                else -> false
            }

            if (!triggered) null else LifeEvent(
                id = template.id,
                title = template.title,
                description = template.description,
                amount = template.amount,
                kind = template.kind,
                monthAge = initialAge,
                dueDay = template.offsetDay
            )
        }.sortedBy { it.monthAge }
    }

    fun totalFor(profile: CreatureProfile, monthAge: Int): Double {
        return getTriggeredEvents(profile, monthAge).sumOf { it.amount.toDouble() }
    }
}

private data class Template(
    val id: String,
    val creatureType: String,
    val title: String,
    val description: String,
    val amount: Double,
    val kind: String,
    val triggerMonth: Int,
    val intervalDays: Int,
    val offsetDay: Int
)
