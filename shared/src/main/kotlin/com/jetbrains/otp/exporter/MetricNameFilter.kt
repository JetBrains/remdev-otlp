package com.jetbrains.otp.exporter

/**
 * Filters metric names based on allowlist patterns.
 * Supports wildcard (*) matching for pattern-based filtering.
 */
object MetricNameFilter {
    private val allowedPatterns: List<Regex> by lazy {
        val providers = AllowedMetricsProvider.EP_NAME.extensionList
        providers.flatMap { it.getAllowedMetrics() }
            .map { pattern -> convertPatternToRegex(pattern) }
    }

    /**
     * Returns true if the metric should be exported (is allowed).
     */
    fun shouldExport(metricName: String): Boolean {
        if (allowedPatterns.isEmpty()) return false
        return allowedPatterns.any { it.matches(metricName) }
    }

    /**
     * Converts a simple wildcard pattern to regex.
     * Example: "JVM.*" -> "^JVM\..*$"
     */
    private fun convertPatternToRegex(pattern: String): Regex {
        val regexPattern = pattern
            .replace(".", "\\.")  // Escape dots
            .replace("*", ".*")   // Convert wildcards to regex
        return Regex("^$regexPattern$")
    }
}
