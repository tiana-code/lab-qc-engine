package com.labqc.westgard

import java.math.BigDecimal

data class WestgardResult(
    val violations: List<WestgardRule>,
    val zScore: BigDecimal,
    val accepted: Boolean
) {
    val hasRejection: Boolean get() = violations.any { !it.isWarning }
    val hasWarning: Boolean get() = violations.any { it.isWarning }
}
