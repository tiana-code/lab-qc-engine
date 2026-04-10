package com.labqc.model

import java.math.BigDecimal
import java.util.UUID

data class ReferenceRange(
    val id: UUID,
    val labId: UUID,
    val testCode: String,
    val low: BigDecimal?,
    val high: BigDecimal?,
    val unit: String?
)
