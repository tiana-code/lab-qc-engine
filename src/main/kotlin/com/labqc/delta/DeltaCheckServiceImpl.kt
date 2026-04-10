package com.labqc.delta

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.temporal.ChronoUnit

class DeltaCheckServiceImpl(
    private val clock: Clock = Clock.systemUTC()
) : DeltaCheckService {

    companion object {
        private const val PERCENT_SCALE = 4
        private const val DISPLAY_SCALE = 2
        private val HUNDRED = BigDecimal("100")
    }

    override suspend fun check(request: DeltaCheckRequest): DeltaCheckResult {
        val windowStart = clock.instant().minus(request.timeWindowHours.toLong(), ChronoUnit.HOURS)
        if (request.previousRecordedAt.isBefore(windowStart)) {
            return DeltaCheckResult(
                passed = true,
                percentChange = null,
                absoluteChange = null,
                reason = "Previous result is outside the ${request.timeWindowHours}h time window — delta check skipped"
            )
        }

        val absoluteChange = (request.currentValue - request.previousValue).abs()

        val percentChange = if (request.previousValue.compareTo(BigDecimal.ZERO) != 0) {
            absoluteChange
                .divide(request.previousValue.abs(), PERCENT_SCALE, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
        } else null

        val exceeded = percentChange != null && percentChange > request.thresholdPercent

        return if (exceeded) {
            DeltaCheckResult(
                passed = false,
                percentChange = percentChange,
                absoluteChange = absoluteChange,
                reason = "Percentage change $percentChange% exceeds threshold ${request.thresholdPercent}%"
            )
        } else {
            DeltaCheckResult(
                passed = true,
                percentChange = percentChange,
                absoluteChange = absoluteChange,
                reason = "Delta within acceptable range"
            )
        }
    }
}
