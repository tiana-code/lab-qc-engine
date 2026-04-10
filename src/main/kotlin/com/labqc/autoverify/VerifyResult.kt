package com.labqc.autoverify

import com.labqc.delta.DeltaCheckResult

enum class VerifyStatus {
    PASSED,
    FAILED,
    SKIPPED
}

data class VerifyResult(
    val status: VerifyStatus,
    val failedRule: VerifyRule?,
    val reason: String,
    val deltaResult: DeltaCheckResult? = null
) {
    val passed: Boolean get() = status != VerifyStatus.FAILED

    companion object {
        fun passed(reason: String, deltaResult: DeltaCheckResult? = null) = VerifyResult(
            status = VerifyStatus.PASSED,
            failedRule = null,
            reason = reason,
            deltaResult = deltaResult
        )

        fun failed(rule: VerifyRule, reason: String, deltaResult: DeltaCheckResult? = null) = VerifyResult(
            status = VerifyStatus.FAILED,
            failedRule = rule,
            reason = reason,
            deltaResult = deltaResult
        )

        fun skipped(reason: String) = VerifyResult(
            status = VerifyStatus.SKIPPED,
            failedRule = null,
            reason = reason
        )
    }
}
