package com.labqc

import com.labqc.delta.DeltaCheckRequest
import com.labqc.delta.DeltaCheckServiceImpl
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class DeltaCheckServiceTest {

    private val fixedInstant = Instant.parse("2026-04-10T12:00:00Z")
    private val clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)
    private val service = DeltaCheckServiceImpl(clock)

    private fun request(
        current: Double,
        previous: Double,
        threshold: Double = 20.0,
        windowHours: Int = 24,
        previousOffsetHours: Long = 1
    ) = DeltaCheckRequest(
        currentValue = BigDecimal.valueOf(current),
        previousValue = BigDecimal.valueOf(previous),
        previousRecordedAt = fixedInstant.minus(previousOffsetHours, ChronoUnit.HOURS),
        thresholdPercent = BigDecimal.valueOf(threshold),
        timeWindowHours = windowHours
    )

    @Test
    fun `passes when percentage change is below threshold`() = runTest {
        val result = service.check(request(current = 110.0, previous = 100.0, threshold = 20.0))
        assertTrue(result.passed)
        assertEquals(0, result.percentChange?.compareTo(BigDecimal("10.00")))
    }

    @Test
    fun `fails when percentage change exceeds threshold`() = runTest {
        val result = service.check(request(current = 130.0, previous = 100.0, threshold = 20.0))
        assertFalse(result.passed)
        assertEquals(0, result.percentChange?.compareTo(BigDecimal("30.00")))
        assertTrue(result.reason.contains("30.00%"))
    }

    @Test
    fun `passes when percentage change equals threshold exactly`() = runTest {
        val result = service.check(request(current = 120.0, previous = 100.0, threshold = 20.0))
        assertTrue(result.passed)
    }

    @Test
    fun `passes when previous result is outside time window`() = runTest {
        val result = service.check(
            request(current = 200.0, previous = 100.0, threshold = 20.0, windowHours = 12, previousOffsetHours = 25)
        )
        assertTrue(result.passed)
        assertNull(result.percentChange)
        assertTrue(result.reason.contains("12h time window"))
    }

    @Test
    fun `handles decrease correctly`() = runTest {
        val result = service.check(request(current = 70.0, previous = 100.0, threshold = 20.0))
        assertFalse(result.passed)
        assertEquals(0, result.percentChange?.compareTo(BigDecimal("30.00")))
    }

    @Test
    fun `passes when previous value is zero`() = runTest {
        val result = service.check(request(current = 5.0, previous = 0.0, threshold = 20.0))
        assertTrue(result.passed)
        assertNull(result.percentChange)
    }

    @Test
    fun `absolute change is calculated correctly`() = runTest {
        val result = service.check(request(current = 130.0, previous = 100.0, threshold = 20.0))
        assertEquals(0, result.absoluteChange?.compareTo(BigDecimal("30.0")))
    }

    @Test
    fun `passes with zero change`() = runTest {
        val result = service.check(request(current = 100.0, previous = 100.0, threshold = 20.0))
        assertTrue(result.passed)
        assertEquals(0, result.percentChange?.compareTo(BigDecimal("0.00")))
    }

    @Test
    fun `previous exactly at window boundary is within window`() = runTest {
        val result = service.check(
            request(current = 200.0, previous = 100.0, threshold = 20.0, windowHours = 24, previousOffsetHours = 24)
        )
        assertFalse(result.passed)
        assertNotNull(result.percentChange)
    }

    @Test
    fun `previous one second past window boundary is outside window`() = runTest {
        val req = DeltaCheckRequest(
            currentValue = BigDecimal("200.0"),
            previousValue = BigDecimal("100.0"),
            previousRecordedAt = fixedInstant.minus(24, ChronoUnit.HOURS).minusSeconds(1),
            thresholdPercent = BigDecimal("20.0"),
            timeWindowHours = 24
        )
        val result = service.check(req)
        assertTrue(result.passed)
        assertNull(result.percentChange)
    }

    @Test
    fun `negative threshold does not prevent failure when change exceeds it`() = runTest {
        val result = service.check(request(current = 200.0, previous = 100.0, threshold = -10.0))
        assertFalse(result.passed)
    }

    @Test
    fun `zero window hours means only simultaneous results are within window`() = runTest {
        val result = service.check(
            request(
                current = 200.0,
                previous = 100.0,
                threshold = 20.0,
                windowHours = 0,
                previousOffsetHours = 1
            )
        )
        assertTrue(result.passed)
        assertNull(result.percentChange)
    }
}
