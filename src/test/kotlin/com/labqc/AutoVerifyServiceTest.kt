package com.labqc

import com.labqc.autoverify.AutoVerifyServiceImpl
import com.labqc.autoverify.VerifyRule
import com.labqc.autoverify.VerifyStatus
import com.labqc.delta.DeltaCheckRequest
import com.labqc.delta.DeltaCheckResult
import com.labqc.delta.DeltaCheckService
import com.labqc.model.LabResult
import com.labqc.model.ReferenceRange
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class AutoVerifyServiceTest {

    private val deltaCheckService: DeltaCheckService = mockk()
    private val service = AutoVerifyServiceImpl(deltaCheckService)

    private fun result(value: Double? = 5.0) = LabResult(
        id = UUID.randomUUID(),
        patientId = UUID.randomUUID(),
        labId = UUID.randomUUID(),
        testCode = "GLU",
        numericValue = value?.let { BigDecimal.valueOf(it) },
        textValue = null,
        unit = "mmol/L",
        recordedAt = Instant.now()
    )

    private fun range(low: Double = 3.9, high: Double = 6.1) = ReferenceRange(
        id = UUID.randomUUID(),
        labId = UUID.randomUUID(),
        testCode = "GLU",
        low = BigDecimal.valueOf(low),
        high = BigDecimal.valueOf(high),
        unit = "mmol/L"
    )

    private fun deltaRequest(current: Double = 5.0, previous: Double = 4.8) = DeltaCheckRequest(
        currentValue = BigDecimal.valueOf(current),
        previousValue = BigDecimal.valueOf(previous),
        previousRecordedAt = Instant.now().minusSeconds(3600),
        thresholdPercent = BigDecimal("20.0"),
        timeWindowHours = 24
    )

    private val deltaPass = DeltaCheckResult(
        passed = true,
        percentChange = BigDecimal("4.17"),
        absoluteChange = BigDecimal("0.2"),
        reason = "Delta within acceptable range"
    )
    private val deltaFail = DeltaCheckResult(
        passed = false,
        percentChange = BigDecimal("50.00"),
        absoluteChange = BigDecimal("2.4"),
        reason = "Percentage change 50.00% exceeds threshold 20.0%"
    )

    @Test
    fun `all rules pass returns success`() = runTest {
        coEvery { deltaCheckService.check(any()) } returns deltaPass
        val result = service.verify(result(), range(), deltaRequest(), qcPassedToday = true)
        assertTrue(result.passed)
        assertEquals(VerifyStatus.PASSED, result.status)
        assertNull(result.failedRule)
    }

    @Test
    fun `range check fails when value below lower bound`() = runTest {
        val result = service.verify(result(value = 3.0), range(), null, qcPassedToday = true)
        assertFalse(result.passed)
        assertEquals(VerifyStatus.FAILED, result.status)
        assertEquals(VerifyRule.RANGE_CHECK, result.failedRule)
        assertTrue(result.reason.contains("below"))
    }

    @Test
    fun `range check fails when value above upper bound`() = runTest {
        val result = service.verify(result(value = 9.0), range(), null, qcPassedToday = true)
        assertFalse(result.passed)
        assertEquals(VerifyStatus.FAILED, result.status)
        assertEquals(VerifyRule.RANGE_CHECK, result.failedRule)
        assertTrue(result.reason.contains("above"))
    }

    @Test
    fun `early exit on range failure does not call delta check`() = runTest {
        service.verify(result(value = 3.0), range(), deltaRequest(), qcPassedToday = true)
        coVerify(exactly = 0) { deltaCheckService.check(any()) }
    }

    @Test
    fun `delta check fails stops evaluation before qc check`() = runTest {
        coEvery { deltaCheckService.check(any()) } returns deltaFail
        val result = service.verify(result(), range(), deltaRequest(), qcPassedToday = false)
        assertFalse(result.passed)
        assertEquals(VerifyStatus.FAILED, result.status)
        assertEquals(VerifyRule.DELTA_CHECK, result.failedRule)
    }

    @Test
    fun `qc check fails when no accepted qc today`() = runTest {
        coEvery { deltaCheckService.check(any()) } returns deltaPass
        val result = service.verify(result(), range(), deltaRequest(), qcPassedToday = false)
        assertFalse(result.passed)
        assertEquals(VerifyStatus.FAILED, result.status)
        assertEquals(VerifyRule.QC_CHECK, result.failedRule)
        assertTrue(result.reason.contains("No accepted QC"))
    }

    @Test
    fun `range check skipped when no reference range provided`() = runTest {
        coEvery { deltaCheckService.check(any()) } returns deltaPass
        val result = service.verify(result(), null, deltaRequest(), qcPassedToday = true)
        assertTrue(result.passed)
        assertEquals(VerifyStatus.PASSED, result.status)
    }

    @Test
    fun `range check skipped when result has no numeric value`() = runTest {
        coEvery { deltaCheckService.check(any()) } returns deltaPass
        val result = service.verify(result(value = null), range(), deltaRequest(), qcPassedToday = true)
        assertTrue(result.passed)
    }

    @Test
    fun `delta check skipped when no delta request provided`() = runTest {
        val result = service.verify(result(), range(), null, qcPassedToday = true)
        assertTrue(result.passed)
        coVerify(exactly = 0) { deltaCheckService.check(any()) }
    }

    @Test
    fun `range check passes when value equals lower bound exactly`() = runTest {
        coEvery { deltaCheckService.check(any()) } returns deltaPass
        val result = service.verify(result(value = 3.9), range(), deltaRequest(), qcPassedToday = true)
        assertTrue(result.passed)
    }

    @Test
    fun `range check passes when value equals upper bound exactly`() = runTest {
        coEvery { deltaCheckService.check(any()) } returns deltaPass
        val result = service.verify(result(value = 6.1), range(), deltaRequest(), qcPassedToday = true)
        assertTrue(result.passed)
    }

    @Test
    fun `delta result is propagated when all rules pass`() = runTest {
        coEvery { deltaCheckService.check(any()) } returns deltaPass
        val result = service.verify(result(), range(), deltaRequest(), qcPassedToday = true)
        assertNotNull(result.deltaResult)
        assertEquals(0, result.deltaResult?.percentChange?.compareTo(BigDecimal("4.17")))
    }

    @Test
    fun `delta result is propagated on delta failure`() = runTest {
        coEvery { deltaCheckService.check(any()) } returns deltaFail
        val result = service.verify(result(), range(), deltaRequest(), qcPassedToday = true)
        assertNotNull(result.deltaResult)
        assertFalse(result.deltaResult!!.passed)
    }
}
