package com.labqc.westgard

import java.math.BigDecimal
import java.math.MathContext

class WestgardEvaluator(
    private val mean: BigDecimal,
    private val sd: BigDecimal
) {
    init {
        require(sd >= BigDecimal.ZERO) { "Standard deviation must be >= 0" }
    }

    fun calculateZScore(value: BigDecimal): BigDecimal {
        if (sd.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
        return value.subtract(mean).divide(sd, MathContext.DECIMAL64)
    }

    fun evaluate(dataPoints: List<QcDataPoint>): WestgardResult {
        require(dataPoints.isNotEmpty()) { "At least one QC data point is required" }

        val sorted = dataPoints.sortedByDescending { it.timestamp }
        val zScores = sorted.map { calculateZScore(it.value) }
        val currentZ = zScores.first()

        val violations = listOfNotNull(
            check1_2s(zScores),
            check1_3s(zScores),
            check2_2s(zScores),
            checkR_4s(zScores),
            check4_1s(zScores),
            check10x(zScores)
        )

        val accepted = violations.all { it.isWarning }
        return WestgardResult(violations = violations, zScore = currentZ, accepted = accepted)
    }

    private fun check1_2s(zScores: List<BigDecimal>): WestgardRule? {
        val currentZ = zScores.first()
        return if (currentZ.abs() > TWO_SD) WestgardRule.RULE_1_2S else null
    }

    private fun check1_3s(zScores: List<BigDecimal>): WestgardRule? {
        val currentZ = zScores.first()
        return if (currentZ.abs() > THREE_SD) WestgardRule.RULE_1_3S else null
    }

    private fun check2_2s(zScores: List<BigDecimal>): WestgardRule? {
        if (zScores.size < 2) return null
        val last2 = zScores.take(2)
        return if (last2.all { it > TWO_SD } || last2.all { it < NEG_TWO_SD }) WestgardRule.RULE_2_2S else null
    }

    private fun checkR_4s(zScores: List<BigDecimal>): WestgardRule? {
        if (zScores.size < 2) return null
        val range = zScores[0].subtract(zScores[1]).abs()
        return if (range > FOUR_SD) WestgardRule.RULE_R_4S else null
    }

    private fun check4_1s(zScores: List<BigDecimal>): WestgardRule? {
        if (zScores.size < 4) return null
        val last4 = zScores.take(4)
        return if (last4.all { it > ONE_SD } || last4.all { it < NEG_ONE_SD }) WestgardRule.RULE_4_1S else null
    }

    private fun check10x(zScores: List<BigDecimal>): WestgardRule? {
        if (zScores.size < 10) return null
        val last10 = zScores.take(10)
        return if (last10.all { it > BigDecimal.ZERO } || last10.all { it < BigDecimal.ZERO }) WestgardRule.RULE_10X else null
    }

    companion object {
        private val ONE_SD = BigDecimal("1.0")
        private val NEG_ONE_SD = BigDecimal("-1.0")
        private val TWO_SD = BigDecimal("2.0")
        private val NEG_TWO_SD = BigDecimal("-2.0")
        private val THREE_SD = BigDecimal("3.0")
        private val FOUR_SD = BigDecimal("4.0")
    }
}
