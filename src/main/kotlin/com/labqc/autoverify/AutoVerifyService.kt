package com.labqc.autoverify

import com.labqc.delta.DeltaCheckRequest
import com.labqc.model.LabResult
import com.labqc.model.ReferenceRange

interface AutoVerifyService {
    suspend fun verify(
        result: LabResult,
        referenceRange: ReferenceRange?,
        deltaRequest: DeltaCheckRequest?,
        qcPassedToday: Boolean
    ): VerifyResult
}
