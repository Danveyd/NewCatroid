package org.catrobat.catroid.codeanalysis

enum class Severity {
    WARNING,
    ERROR
}

data class AnalysisResult(
    val severity: Severity,
    val message: String
)