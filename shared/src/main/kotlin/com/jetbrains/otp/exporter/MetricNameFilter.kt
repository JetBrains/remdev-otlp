package com.jetbrains.otp.exporter

/**
 * Filters metric names based on denylist patterns.
 * Supports wildcard (*) matching for pattern-based filtering.
 */
object MetricNameFilter {
    private val deniedPatterns: List<Regex> by lazy {
        val providers = DeniedMetricsProvider.EP_NAME.extensionList
        providers.flatMap { it.getDeniedMetrics() }
            .map { pattern -> convertPatternToRegex(pattern) }
    }

    /**
     * Returns true if the metric should be exported (not denied).
     * Note: This method assumes denylist is enabled - caller should check settings first.
     */
    fun shouldExport(metricName: String): Boolean {
        if (deniedPatterns.isEmpty()) return true
        return deniedPatterns.none { it.matches(metricName) }
    }

    /**
     * Converts a simple wildcard pattern to regex.
     * Example: "StreamlinedBlobStorage.*" -> "^StreamlinedBlobStorage\..*$"
     */
    private fun convertPatternToRegex(pattern: String): Regex {
        val regexPattern = pattern
            .replace(".", "\\.")  // Escape dots
            .replace("*", ".*")   // Convert wildcards to regex
        return Regex("^$regexPattern$")
    }
}
