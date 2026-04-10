package com.labqc.westgard

enum class WestgardRule(val code: String, val description: String, val isWarning: Boolean) {
    RULE_1_2S("1-2s", "Warning: 1 control exceeds mean ± 2SD", true),
    RULE_1_3S("1-3s", "Reject: 1 control exceeds mean ± 3SD", false),
    RULE_2_2S("2-2s", "Reject: 2 consecutive controls exceed mean ± 2SD on same side", false),
    RULE_R_4S("R-4s", "Reject: Range between 2 consecutive controls exceeds 4SD", false),
    RULE_4_1S("4-1s", "Reject: 4 consecutive controls exceed mean ± 1SD on same side", false),
    RULE_10X("10x", "Reject: 10 consecutive controls fall on same side of mean", false)
}
