package com.labqc.autoverify

enum class VerifyRule(val priority: Int, val description: String) {
    RANGE_CHECK(1, "Numeric result must fall within configured reference range"),
    DELTA_CHECK(2, "Change from previous patient result must not exceed configured threshold"),
    QC_CHECK(3, "Accepted QC result must exist for this test within the current day")
}
