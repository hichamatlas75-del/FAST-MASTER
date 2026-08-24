package com.example.hichamjeunemaster.data

/**
 * Les 6 étapes corporelles du jeûne intermittent — inspiré de Fastic.
 * Chaque étape décrit en détail ce qui se passe dans le corps.
 */
import com.example.hichamjeunemaster.R

/**
 * Les 6 étapes corporelles du jeûne intermittent — inspiré de Fastic.
 * Chaque étape décrit en détail ce qui se passe dans le corps.
 */
data class FastingStage(
    val id: Int,
    val titleRes: Int,
    val emoji: String,
    val startHour: Int,
    val endHour: Int,
    val shortDescriptionRes: Int,
    val detailedDescriptionRes: Int,
    val bodyEffectsRes: List<Int>,
    val tipRes: Int,
    val color: Long
)

object FastingStages {

    /**
     * Retourne les 6 étapes détaillées du jeûne.
     * Les heures sont relatives au début du jeûne.
     */
    fun getStages(): List<FastingStage> = listOf(

        // ═══════════════════════════════════════════
        // ÉTAPE 1 : DIGESTION (0-4h)
        // ═══════════════════════════════════════════
        FastingStage(
            id = 1,
            titleRes = R.string.stage1_title,
            emoji = "🍽️",
            startHour = 0,
            endHour = 4,
            shortDescriptionRes = R.string.stage1_short_desc,
            detailedDescriptionRes = R.string.stage1_detailed_desc,
            bodyEffectsRes = listOf(
                R.string.stage1_effect1,
                R.string.stage1_effect2,
                R.string.stage1_effect3,
                R.string.stage1_effect4,
                R.string.stage1_effect5
            ),
            tipRes = R.string.stage1_tip,
            color = 0xFF9E9E9E // Gris — phase neutre
        ),

        // ═══════════════════════════════════════════
        // ÉTAPE 2 : BAISSE DE L'INSULINE (4-8h)
        // ═══════════════════════════════════════════
        FastingStage(
            id = 2,
            titleRes = R.string.stage2_title,
            emoji = "📉",
            startHour = 4,
            endHour = 8,
            shortDescriptionRes = R.string.stage2_short_desc,
            detailedDescriptionRes = R.string.stage2_detailed_desc,
            bodyEffectsRes = listOf(
                R.string.stage2_effect1,
                R.string.stage2_effect2,
                R.string.stage2_effect3,
                R.string.stage2_effect4,
                R.string.stage2_effect5
            ),
            tipRes = R.string.stage2_tip,
            color = 0xFF42A5F5 // Bleu
        ),

        // ═══════════════════════════════════════════
        // ÉTAPE 3 : COMBUSTION DES GRAISSES (8-12h)
        // ═══════════════════════════════════════════
        FastingStage(
            id = 3,
            titleRes = R.string.stage3_title,
            emoji = "🔥",
            startHour = 8,
            endHour = 12,
            shortDescriptionRes = R.string.stage3_short_desc,
            detailedDescriptionRes = R.string.stage3_detailed_desc,
            bodyEffectsRes = listOf(
                R.string.stage3_effect1,
                R.string.stage3_effect2,
                R.string.stage3_effect3,
                R.string.stage3_effect4,
                R.string.stage3_effect5
            ),
            tipRes = R.string.stage3_tip,
            color = 0xFFFF9100 // Orange
        ),

        // ═══════════════════════════════════════════
        // ÉTAPE 4 : CÉTOSE (12-18h)
        // ═══════════════════════════════════════════
        FastingStage(
            id = 4,
            titleRes = R.string.stage4_title,
            emoji = "🧬",
            startHour = 12,
            endHour = 18,
            shortDescriptionRes = R.string.stage4_short_desc,
            detailedDescriptionRes = R.string.stage4_detailed_desc,
            bodyEffectsRes = listOf(
                R.string.stage4_effect1,
                R.string.stage4_effect2,
                R.string.stage4_effect3,
                R.string.stage4_effect4,
                R.string.stage4_effect5,
                R.string.stage4_effect6
            ),
            tipRes = R.string.stage4_tip,
            color = 0xFF7C4DFF // Violet
        ),

        // ═══════════════════════════════════════════
        // ÉTAPE 5 : AUTOPHAGIE (18-24h)
        // ═══════════════════════════════════════════
        FastingStage(
            id = 5,
            titleRes = R.string.stage5_title,
            emoji = "🔄",
            startHour = 18,
            endHour = 24,
            shortDescriptionRes = R.string.stage5_short_desc,
            detailedDescriptionRes = R.string.stage5_detailed_desc,
            bodyEffectsRes = listOf(
                R.string.stage5_effect1,
                R.string.stage5_effect2,
                R.string.stage5_effect3,
                R.string.stage5_effect4,
                R.string.stage5_effect5,
                R.string.stage5_effect6
            ),
            tipRes = R.string.stage5_tip,
            color = 0xFF00BFA5 // Teal
        ),

        // ═══════════════════════════════════════════
        // ÉTAPE 6 : RÉGÉNÉRATION PROFONDE (24h+)
        // ═══════════════════════════════════════════
        FastingStage(
            id = 6,
            titleRes = R.string.stage6_title,
            emoji = "⚡",
            startHour = 24,
            endHour = 72,
            shortDescriptionRes = R.string.stage6_short_desc,
            detailedDescriptionRes = R.string.stage6_detailed_desc,
            bodyEffectsRes = listOf(
                R.string.stage6_effect1,
                R.string.stage6_effect2,
                R.string.stage6_effect3,
                R.string.stage6_effect4,
                R.string.stage6_effect5,
                R.string.stage6_effect6
            ),
            tipRes = R.string.stage6_tip,
            color = 0xFFFF1744 // Rouge vif
        )
    )

    /**
     * Retourne l'étape actuelle basée sur les heures écoulées depuis le début du jeûne.
     */
    fun getCurrentStage(elapsedHours: Float): FastingStage {
        val stages = getStages()
        return stages.lastOrNull { elapsedHours >= it.startHour } ?: stages.first()
    }

    /**
     * Retourne la progression (0f-1f) à l'intérieur de l'étape actuelle.
     */
    fun getStageProgress(elapsedHours: Float, stage: FastingStage): Float {
        val duration = (stage.endHour - stage.startHour).toFloat()
        val elapsed = (elapsedHours - stage.startHour).coerceIn(0f, duration)
        return elapsed / duration
    }
}
