package com.labqc.westgard

import java.math.BigDecimal
import java.time.Instant

data class QcDataPoint(
    val value: BigDecimal,
    val timestamp: Instant
)
