package com.labqc

import com.labqc.westgard.QcDataPoint
import com.labqc.westgard.WestgardEvaluator
import com.labqc.westgard.WestgardRule
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class WestgardEvaluatorTest {

    private val mean = BigDecimal("100.0")
    private val sd = BigDecimal("5.0")
    private val evaluator = WestgardEvaluator(mean, sd)

    private fun point(value: Double, offsetSeconds: Long = 0): QcDataPoint =
        QcDataPoint(
            value = BigDecimal.valueOf(value),
            timestamp = Instant.now().minusSeconds(offsetSeconds)
        )

    @Test
    fun `no violations when all values within 2SD`() {
        val result = evaluator.evaluate(listOf(point(101.0)))
        assertTrue(result.violations.isEmpty())
        assertTrue(result.accepted)
    }

    @Test
    fun `1-2s warning when value exceeds mean plus 2SD`() {
        val result = evaluator.evaluate(listOf(point(110.1)))
        assertTrue(result.violations.contains(WestgardRule.RULE_1_2S))
        assertFalse(result.violations.contains(WestgardRule.RULE_1_3S))
        assertTrue(result.accepted)
    }

    @Test
    fun `1-3s rejection when value exceeds mean plus 3SD`() {
        val result = evaluator.evaluate(listOf(point(115.1)))
        assertTrue(result.violations.contains(WestgardRule.RULE_1_3S))
        assertTrue(result.violations.contains(WestgardRule.RULE_1_2S))
        assertFalse(result.accepted)
    }

    @Test
    fun `1-3s rejection when value falls below mean minus 3SD`() {
        val result = evaluator.evaluate(listOf(point(84.9)))
        assertTrue(result.violations.contains(WestgardRule.RULE_1_3S))
        assertFalse(result.accepted)
    }

    @Test
    fun `2-2s rejection when two consecutive values exceed plus 2SD`() {
        val points = listOf(point(110.1), point(110.1, 60))
        val result = evaluator.evaluate(points)
        assertTrue(result.violations.contains(WestgardRule.RULE_2_2S))
        assertFalse(result.accepted)
    }

    @Test
    fun `2-2s rejection when two consecutive values below minus 2SD`() {
        val points = listOf(point(89.9), point(89.9, 60))
        val result = evaluator.evaluate(points)
        assertTrue(result.violations.contains(WestgardRule.RULE_2_2S))
        assertFalse(result.accepted)
    }

    @Test
    fun `2-2s no violation when consecutive values on opposite sides of 2SD`() {
        val points = listOf(point(110.1), point(89.9, 60))
        val result = evaluator.evaluate(points)
        assertFalse(result.violations.contains(WestgardRule.RULE_2_2S))
    }

    @Test
    fun `R-4s rejection when range between two consecutive values exceeds 4SD`() {
        val points = listOf(point(115.5), point(93.0, 60))
        val result = evaluator.evaluate(points)
        assertTrue(result.violations.contains(WestgardRule.RULE_R_4S))
        assertFalse(result.accepted)
    }

    @Test
    fun `R-4s no violation when range is exactly 4SD`() {
        val points = listOf(point(110.0), point(90.0, 60))
        val result = evaluator.evaluate(points)
        assertFalse(result.violations.contains(WestgardRule.RULE_R_4S))
    }

    @Test
    fun `4-1s rejection when four consecutive values exceed mean plus 1SD`() {
        val points = (0..3).map { point(106.0, it * 60L) }
        val result = evaluator.evaluate(points)
        assertTrue(result.violations.contains(WestgardRule.RULE_4_1S))
        assertFalse(result.accepted)
    }

    @Test
    fun `4-1s rejection when four consecutive values fall below mean minus 1SD`() {
        val points = (0..3).map { point(94.0, it * 60L) }
        val result = evaluator.evaluate(points)
        assertTrue(result.violations.contains(WestgardRule.RULE_4_1S))
        assertFalse(result.accepted)
    }

    @Test
    fun `4-1s no violation with fewer than 4 data points`() {
        val points = (0..2).map { point(106.0, it * 60L) }
        val result = evaluator.evaluate(points)
        assertFalse(result.violations.contains(WestgardRule.RULE_4_1S))
    }

    @Test
    fun `10x rejection when ten consecutive values on same side of mean`() {
        val points = (0..9).map { point(102.0, it * 60L) }
        val result = evaluator.evaluate(points)
        assertTrue(result.violations.contains(WestgardRule.RULE_10X))
        assertFalse(result.accepted)
    }

    @Test
    fun `10x rejection when ten consecutive values all below mean`() {
        val points = (0..9).map { point(98.0, it * 60L) }
        val result = evaluator.evaluate(points)
        assertTrue(result.violations.contains(WestgardRule.RULE_10X))
        assertFalse(result.accepted)
    }

    @Test
    fun `10x no violation with nine points on same side`() {
        val points = (0..8).map { point(102.0, it * 60L) }
        val result = evaluator.evaluate(points)
        assertFalse(result.violations.contains(WestgardRule.RULE_10X))
    }

    @Test
    fun `z score is zero when sd is zero`() {
        val zeroSdEvaluator = WestgardEvaluator(mean, BigDecimal.ZERO)
        val z = zeroSdEvaluator.calculateZScore(BigDecimal("110.0"))
        assertEquals(0, z.compareTo(BigDecimal.ZERO))
    }

    @Test
    fun `z score calculated correctly`() {
        val z = evaluator.calculateZScore(BigDecimal("110.0"))
        assertEquals(0, z.compareTo(BigDecimal("2.0")))
    }

    @Test
    fun `accepted is false when any rejection rule triggered`() {
        val points = listOf(point(115.1))
        val result = evaluator.evaluate(points)
        assertFalse(result.accepted)
        assertTrue(result.hasRejection)
    }

    @Test
    fun `accepted is true with only warning rule`() {
        val result = evaluator.evaluate(listOf(point(110.1)))
        assertTrue(result.accepted)
        assertTrue(result.hasWarning)
        assertFalse(result.hasRejection)
    }

    @Test
    fun `unsorted input produces same result as sorted`() {
        val sorted = listOf(point(110.1, 0), point(110.1, 60))
        val unsorted = listOf(point(110.1, 60), point(110.1, 0))
        val resultSorted = evaluator.evaluate(sorted)
        val resultUnsorted = evaluator.evaluate(unsorted)
        assertEquals(resultSorted.violations, resultUnsorted.violations)
        assertEquals(resultSorted.accepted, resultUnsorted.accepted)
        assertEquals(0, resultSorted.zScore.compareTo(resultUnsorted.zScore))
    }

    @Test
    fun `R-4s detected regardless of input order`() {
        val chronological = listOf(point(93.0, 60), point(115.5, 0))
        val result = evaluator.evaluate(chronological)
        assertTrue(result.violations.contains(WestgardRule.RULE_R_4S))
    }

    @Test
    fun `empty data points throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            evaluator.evaluate(emptyList())
        }
    }
}
