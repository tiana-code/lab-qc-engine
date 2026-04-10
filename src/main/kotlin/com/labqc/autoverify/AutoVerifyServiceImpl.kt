package com.labqc.autoverify

import com.labqc.delta.DeltaCheckRequest
import com.labqc.delta.DeltaCheckService
import com.labqc.model.LabResult
import com.labqc.model.ReferenceRange

class AutoVerifyServiceImpl(
    private val deltaCheckService: DeltaCheckService
) : AutoVerifyService {

    override suspend fun verify(
        result: LabResult,
        referenceRange: ReferenceRange?,
        deltaRequest: DeltaCheckRequest?,
        qcPassedToday: Boolean
    ): VerifyResult {
        val rangeResult = checkRange(result, referenceRange)
        if (rangeResult.status == VerifyStatus.FAILED) return rangeResult

        val deltaResult = checkDelta(deltaRequest)
        if (deltaResult.status == VerifyStatus.FAILED) return deltaResult

        val qcResult = checkQc(qcPassedToday)
        if (qcResult.status == VerifyStatus.FAILED) return qcResult

        return VerifyResult.passed("All auto-verify rules passed", deltaResult.deltaResult)
    }

    private fun checkRange(result: LabResult, referenceRange: ReferenceRange?): VerifyResult {
        if (referenceRange == null || result.numericValue == null) {
            return VerifyResult.skipped("Range check skipped — no numeric value or reference range")
        }
        val value = result.numericValue
        val low = referenceRange.low
        val high = referenceRange.high
        if (low != null && value < low) {
            return VerifyResult.failed(VerifyRule.RANGE_CHECK, "Result $value is below reference range low $low")
        }
        if (high != null && value > high) {
            return VerifyResult.failed(VerifyRule.RANGE_CHECK, "Result $value is above reference range high $high")
        }
        return VerifyResult.passed("Range check passed")
    }

    private suspend fun checkDelta(deltaRequest: DeltaCheckRequest?): VerifyResult {
        if (deltaRequest == null) {
            return VerifyResult.skipped("Delta check skipped — no previous result available")
        }
        val deltaResult = deltaCheckService.check(deltaRequest)
        return if (!deltaResult.passed) {
            VerifyResult.failed(VerifyRule.DELTA_CHECK, deltaResult.reason, deltaResult)
        } else {
            VerifyResult.passed("Delta check passed", deltaResult)
        }
    }

    private fun checkQc(qcPassedToday: Boolean): VerifyResult {
        return if (!qcPassedToday) {
            VerifyResult.failed(VerifyRule.QC_CHECK, "No accepted QC result found today for this test")
        } else {
            VerifyResult.passed("QC check passed")
        }
    }
}
