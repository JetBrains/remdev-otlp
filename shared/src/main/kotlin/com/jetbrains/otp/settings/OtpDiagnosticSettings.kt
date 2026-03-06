package com.jetbrains.otp.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.jetbrains.otp.exporter.hasFrequentPerformanceMetricsReportingOverride
import com.jetbrains.otp.exporter.hasMetricsExportOverride
import com.jetbrains.otp.exporter.hasPluginFilterOverride
import com.jetbrains.otp.exporter.hasMetricsDenylistOverride
import com.jetbrains.otp.exporter.hasMetricsExportIntervalOverride
import com.jetbrains.otp.exporter.readFrequentPerformanceMetricsReportingEnabled
import com.jetbrains.otp.exporter.readMetricsExportEnabled
import com.jetbrains.otp.exporter.readPluginFilterEnabled
import com.jetbrains.otp.exporter.readMetricsDenylistEnabled
import com.jetbrains.otp.exporter.readMetricsExportIntervalMinutes

@Service(Service.Level.APP)
@State(
    name = "OtpDiagnosticSettings",
    storages = [Storage("otp-diagnostic.xml")]
)
class OtpDiagnosticSettings : PersistentStateComponent<OtpDiagnosticSettings.State> {

    private var state = State()
    @Volatile
    private var backendPluginFilterOverride: Boolean? = null
    @Volatile
    private var backendMetricsExportOverride: Boolean? = null
    @Volatile
    private var backendFrequentPerformanceMetricsReportingOverride: Boolean? = null

    data class State(
        var disabledCategories: MutableSet<String> = mutableSetOf(),
        var frequentSpansEnabled: Boolean = false,
        var pluginSpanFilterEnabled: Boolean = true,
        var metricsExportEnabled: Boolean = true,
        var frequentPerformanceMetricsReportingEnabled: Boolean = false,
        var metricsDenylistEnabled: Boolean = true,
        var metricsExportIntervalMinutes: Int = 5,
    )

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    fun syncFilteringSettings(
        disabledCategories: Set<String>,
        frequentSpansEnabled: Boolean,
        pluginSpanFilterEnabled: Boolean,
        metricsExportEnabled: Boolean,
        frequentPerformanceMetricsReportingEnabled: Boolean,
    ) {
        state.disabledCategories.clear()
        state.disabledCategories.addAll(disabledCategories)
        state.frequentSpansEnabled = frequentSpansEnabled
        state.pluginSpanFilterEnabled = pluginSpanFilterEnabled
        state.metricsExportEnabled = metricsExportEnabled
        state.frequentPerformanceMetricsReportingEnabled = frequentPerformanceMetricsReportingEnabled
    }

    fun updateBackendPluginFilterOverride(pluginFilterOverride: Boolean?) {
        backendPluginFilterOverride = pluginFilterOverride
    }

    fun updateBackendMetricsExportOverride(metricsExportOverride: Boolean?) {
        backendMetricsExportOverride = metricsExportOverride
    }

    fun updateBackendFrequentPerformanceMetricsReportingOverride(frequentPerformanceMetricsReportingOverride: Boolean?) {
        backendFrequentPerformanceMetricsReportingOverride = frequentPerformanceMetricsReportingOverride
    }

    fun isCategoryEnabled(categoryId: String): Boolean {
        return categoryId !in state.disabledCategories
    }

    fun getDisabledCategories(): Set<String> {
        return state.disabledCategories.toSet()
    }

    fun isScopeEnabled(scopeName: String): Boolean {
        return SpanCategoryRegistry.isScopeEnabled(scopeName, state.disabledCategories)
    }

    fun isFrequentSpansEnabled(): Boolean {
        return state.frequentSpansEnabled
    }

    fun isPluginSpanFilterEnabled(): Boolean {
        return state.pluginSpanFilterEnabled
    }

    fun pluginFilterEnabledEffective(): Boolean {
        backendPluginFilterOverride?.let { return it }
        return readPluginFilterEnabled(state.pluginSpanFilterEnabled)
    }

    fun isPluginFilterOverridden(): Boolean {
        return backendPluginFilterOverride != null || hasPluginFilterOverride()
    }

    fun isMetricsExportEnabled(): Boolean {
        return state.metricsExportEnabled
    }

    fun metricsExportEnabledEffective(): Boolean {
        backendMetricsExportOverride?.let { return it }
        return readMetricsExportEnabled(state.metricsExportEnabled)
    }

    fun isMetricsExportOverridden(): Boolean {
        return backendMetricsExportOverride != null || hasMetricsExportOverride()
    }

    fun isFrequentPerformanceMetricsReportingEnabled(): Boolean {
        return state.frequentPerformanceMetricsReportingEnabled
    }

    fun frequentPerformanceMetricsReportingEnabledEffective(): Boolean {
        backendFrequentPerformanceMetricsReportingOverride?.let { return it }
        return readFrequentPerformanceMetricsReportingEnabled(state.frequentPerformanceMetricsReportingEnabled)
    }

    fun isFrequentPerformanceMetricsReportingOverridden(): Boolean {
        return backendFrequentPerformanceMetricsReportingOverride != null || hasFrequentPerformanceMetricsReportingOverride()
    }

    fun isMetricsDenylistEnabled(): Boolean {
        return state.metricsDenylistEnabled
    }

    fun metricsDenylistEnabledEffective(): Boolean {
        return readMetricsDenylistEnabled(state.metricsDenylistEnabled)
    }

    fun isMetricsDenylistOverridden(): Boolean {
        return hasMetricsDenylistOverride()
    }

    fun getMetricsExportIntervalMinutes(): Int {
        return state.metricsExportIntervalMinutes
    }

    fun metricsExportIntervalMinutesEffective(): Int {
        return readMetricsExportIntervalMinutes(state.metricsExportIntervalMinutes)
    }

    fun isMetricsExportIntervalOverridden(): Boolean {
        return hasMetricsExportIntervalOverride()
    }

    companion object {
        fun getInstance(): OtpDiagnosticSettings = service()
    }
}
