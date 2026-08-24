package com.example.hichamjeunemaster.data

data class FastingSession(
    val id: Long = System.currentTimeMillis(),
    val plan: FastingPlan,
    val startTimeMillis: Long,
    val endTimeMillis: Long = 0,
    val isCompleted: Boolean = false
) {
    val durationMillis: Long get() = if (endTimeMillis > 0) endTimeMillis - startTimeMillis else 0
}
