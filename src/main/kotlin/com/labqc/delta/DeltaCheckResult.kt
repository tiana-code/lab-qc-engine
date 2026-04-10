package com.labqc.delta

import java.math.BigDecimal

data class DeltaCheckResult(
    val passed: Boolean,
    val percentChange: BigDecimal?,
    val absoluteChange: BigDecimal?,
    val reason: String
)
