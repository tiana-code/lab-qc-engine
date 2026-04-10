package com.labqc.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class LabResult(
    val id: UUID,
    val patientId: UUID,
    val labId: UUID,
    val testCode: String,
    val numericValue: BigDecimal?,
    val textValue: String?,
    val unit: String?,
    val recordedAt: Instant
)
