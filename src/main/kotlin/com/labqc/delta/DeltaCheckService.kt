package com.labqc.delta

interface DeltaCheckService {
    suspend fun check(request: DeltaCheckRequest): DeltaCheckResult
}
