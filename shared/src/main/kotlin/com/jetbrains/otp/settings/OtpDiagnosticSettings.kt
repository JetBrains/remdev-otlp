package com.jetbrains.otp.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.jetbrains.otp.exporter.hasMetricsExportOverride
import com.jetbrains.otp.exporter.hasPluginFilterOverride
import com.jetbrains.otp.exporter.readMetricsExportEnabled
import com.jetbrains.otp.exporter.readPluginFilterEnabled

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

    data class State(
        var disabledCategories: MutableSet<String> = mutableSetOf(),
        var frequentSpansEnabled: Boolean = false,
        var pluginSpanFilterEnabled: Boolean = true,
        var metricsExportEnabled: Boolean = true,
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
    ) {
        state.disabledCategories.clear()
        state.disabledCategories.addAll(disabledCategories)
        state.frequentSpansEnabled = frequentSpansEnabled
        state.pluginSpanFilterEnabled = pluginSpanFilterEnabled
        state.metricsExportEnabled = metricsExportEnabled
    }

    fun updateBackendPluginFilterOverride(pluginFilterOverride: Boolean?) {
        backendPluginFilterOverride = pluginFilterOverride
    }

    fun updateBackendMetricsExportOverride(metricsExportOverride: Boolean?) {
        backendMetricsExportOverride = metricsExportOverride
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

    companion object {
        fun getInstance(): OtpDiagnosticSettings = service()
    }
}