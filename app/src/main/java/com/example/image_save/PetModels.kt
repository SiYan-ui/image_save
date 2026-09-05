package com.example.image_save

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.Period
import java.util.UUID

/** 性别使用类型化枚举，避免在业务代码中传递未约束字符串。 */
enum class Gender {
    MALE,
    FEMALE,
    UNKNOWN
}

/** 养育对象的领域层契约，供不同角色类型复用。 */
interface PetInterface {
    val id: String
    var name: String
    val gender: Gender
    val createdAt: Long
    val avatarEmoji: String?
    val imageRes: Int?

    fun getAgeInMonths(): Int
    fun getAgeDisplay(): String
    fun getDefaultName(): String
    val defaultAvatar: String
    fun getAvatar(): Any?
}

/**
 * 养育对象抽象模板。
 * 年龄只由 createdAt 动态计算，不保存易过期的年龄字段。
 */
abstract class PetBase(
    override val id: String = UUID.randomUUID().toString(),
    override var name: String = "",
    override val gender: Gender = Gender.UNKNOWN,
    override val createdAt: Long = System.currentTimeMillis(),
    override val avatarEmoji: String? = null,
    override val imageRes: Int? = null,
    var health: Int = 100,
    var hunger: Int = 100,
    var happiness: Int = 100,
    var lastInteractAt: Long? = null
) : PetInterface {

    init {
        require(health in 0..100) { "health must be between 0 and 100" }
        require(hunger in 0..100) { "hunger must be between 0 and 100" }
        require(happiness in 0..100) { "happiness must be between 0 and 100" }
    }

    final override fun getAgeInMonths(): Int {
        val period = agePeriod()
        return period.years * 12 + period.months
    }

    final override fun getAgeDisplay(): String {
        val period = agePeriod()
        if (period.isZero) return "刚刚"

        val totalMonths = period.years * 12 + period.months
        return if (totalMonths >= 12) {
            "${period.years}岁${period.months}月"
        } else {
            "${totalMonths}月${period.days}天"
        }
    }

    override fun getAvatar(): Any? = imageRes ?: avatarEmoji ?: defaultAvatar

    private fun agePeriod(): Period {
        val zone = ZoneId.systemDefault()
        val createdDate = Instant.ofEpochMilli(createdAt).atZone(zone).toLocalDate()
        val currentDate = LocalDate.now(zone)
        return if (createdDate.isAfter(currentDate)) {
            Period.ZERO
        } else {
            Period.between(createdDate, currentDate)
        }
    }
}

/** 婴儿角色，使用 gender 区分男婴和女婴。 */
class Baby(
    override val gender: Gender,
    id: String = UUID.randomUUID().toString(),
    name: String = "",
    createdAt: Long = System.currentTimeMillis(),
    imageRes: Int? = if (gender == Gender.MALE) R.drawable.baby_m1 else null,
    avatarEmoji: String? = if (gender == Gender.FEMALE) "👧" else "👶"
) : PetBase(
    id = id,
    name = name,
    gender = gender,
    createdAt = createdAt,
    avatarEmoji = avatarEmoji,
    imageRes = imageRes
) {
    override fun getDefaultName(): String =
        if (gender == Gender.FEMALE) "女婴" else "男婴"

    override val defaultAvatar: String
        get() = if (gender == Gender.FEMALE) "👧" else "👶"

    init {
        if (this.name.isBlank()) this.name = getDefaultName()
    }
}

/** 猫猫角色；breed 预留给后续品种扩展。 */
class Cat(
    id: String = UUID.randomUUID().toString(),
    name: String = "",
    override val gender: Gender = Gender.UNKNOWN,
    createdAt: Long = System.currentTimeMillis(),
    val breed: String? = null
) : PetBase(
    id = id,
    name = name,
    gender = gender,
    createdAt = createdAt,
    avatarEmoji = "🐱"
) {
    override fun getDefaultName(): String = "猫猫"
    override val defaultAvatar: String = "🐱"

    init {
        if (this.name.isBlank()) this.name = getDefaultName()
    }
}

/** 狗狗角色；breed 预留给后续品种扩展。 */
class Dog(
    id: String = UUID.randomUUID().toString(),
    name: String = "",
    override val gender: Gender = Gender.UNKNOWN,
    createdAt: Long = System.currentTimeMillis(),
    val breed: String? = null
) : PetBase(
    id = id,
    name = name,
    gender = gender,
    createdAt = createdAt,
    avatarEmoji = "🐶"
) {
    override fun getDefaultName(): String = "狗狗"
    override val defaultAvatar: String = "🐶"

    init {
        if (this.name.isBlank()) this.name = getDefaultName()
    }
}
