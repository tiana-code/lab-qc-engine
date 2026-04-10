package com.labqc.delta

import java.math.BigDecimal
import java.time.Instant

data class DeltaCheckRequest(
    val currentValue: BigDecimal,
    val previousValue: BigDecimal,
    val previousRecordedAt: Instant,
    val thresholdPercent: BigDecimal,
    val timeWindowHours: Int
)
