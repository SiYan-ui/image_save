package com.example.image_save

object ProductCatalog {
    private const val DAY = 24L * 60L * 60L * 1000L

    val all: List<ProductConfig> = listOf(
        ProductConfig("baby_formula", CareType.BABY, "奶粉", "罐", 30 * DAY, 35_500),
        ProductConfig("baby_diaper", CareType.BABY, "纸尿裤", "包", 14 * DAY, 6_800),
        ProductConfig("cat_food", CareType.CAT, "猫粮", "袋", 30 * DAY, 34_000),
        ProductConfig("cat_litter", CareType.CAT, "猫砂", "包", 30 * DAY, 3_500),
        ProductConfig("dog_food", CareType.DOG, "狗粮", "袋", 30 * DAY, 85_000)
    )

    fun forType(type: CareType): List<ProductConfig> = all.filter { it.careType == type }
    fun get(itemId: String): ProductConfig = all.first { it.id == itemId }
}
