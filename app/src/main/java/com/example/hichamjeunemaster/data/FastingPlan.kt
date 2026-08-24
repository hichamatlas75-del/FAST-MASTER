package com.example.hichamjeunemaster.data

import com.example.hichamjeunemaster.R

enum class FastingPlan(
    val displayNameRes: Int,
    val emoji: String,
    val fastingHours: Int,
    val eatingHours: Int,
    val difficultyRes: Int,
    val descriptionRes: Int,
    val color: Long
) {
    PLAN_16_8(
        displayNameRes = R.string.plan_16_8_title,
        emoji = "\uD83D\uDFE2",
        fastingHours = 16,
        eatingHours = 8,
        difficultyRes = R.string.difficulty_beginner,
        descriptionRes = R.string.plan_16_8_desc,
        color = 0xFF2ECC71
    ),
    PLAN_18_6(
        displayNameRes = R.string.plan_18_6_title,
        emoji = "\uD83D\uDFE1",
        fastingHours = 18,
        eatingHours = 6,
        difficultyRes = R.string.difficulty_intermediate,
        descriptionRes = R.string.plan_18_6_desc,
        color = 0xFFF39C12
    ),
    PLAN_20_4(
        displayNameRes = R.string.plan_20_4_title,
        emoji = "\uD83D\uDFE0",
        fastingHours = 20,
        eatingHours = 4,
        difficultyRes = R.string.difficulty_advanced,
        descriptionRes = R.string.plan_20_4_desc,
        color = 0xFFE67E22
    ),
    PLAN_OMAD(
        displayNameRes = R.string.plan_omad_title,
        emoji = "\uD83D\uDD34",
        fastingHours = 23,
        eatingHours = 1,
        difficultyRes = R.string.difficulty_expert,
        descriptionRes = R.string.plan_omad_desc,
        color = 0xFFE74C3C
    ),
    PLAN_FLEXIBLE_16_8(
        displayNameRes = R.string.plan_flexible_title,
        emoji = "\uD83D\uDD04",
        fastingHours = 16,
        eatingHours = 8,
        difficultyRes = R.string.difficulty_adaptive,
        descriptionRes = R.string.plan_flexible_desc,
        color = 0xFF9B59B6
    );

    val totalHours: Int get() = fastingHours + eatingHours
    val fastingMillis: Long get() = fastingHours * 3600000L
    val eatingMillis: Long get() = eatingHours * 3600000L
    val totalMillis: Long get() = totalHours * 3600000L
}
