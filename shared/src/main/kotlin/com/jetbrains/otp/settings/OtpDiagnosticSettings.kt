package com.jetbrains.otp.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.jetbrains.otp.exporter.isPluginSpanFilterDefinedInPropertyOrEnv
import com.jetbrains.otp.exporter.readPluginSpanFilterEnabledFromPropertyOrEnv

@Service(Service.Level.APP)
@State(
    name = "OtpDiagnosticSettings",
    storages = [Storage("otp-diagnostic.xml")]
)
class OtpDiagnosticSettings : PersistentStateComponent<OtpDiagnosticSettings.State> {

    private var state = State()

    data class State(
        var disabledCategories: MutableSet<String> = mutableSetOf(),
        var frequentSpansEnabled: Boolean = false,
        var pluginSpanFilterEnabled: Boolean = true,
    )

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    fun syncFilteringSettings(
        disabledCategories: Set<String>,
        frequentSpansEnabled: Boolean,
        pluginSpanFilterEnabled: Boolean,
    ) {
        state.disabledCategories.clear()
        state.disabledCategories.addAll(disabledCategories)
        state.frequentSpansEnabled = frequentSpansEnabled
        state.pluginSpanFilterEnabled = pluginSpanFilterEnabled
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

    fun isPluginSpanFilterEnabledEffective(): Boolean {
        return readPluginSpanFilterEnabledFromPropertyOrEnv(state.pluginSpanFilterEnabled)
    }

    fun isPluginSpanFilterOverriddenByPropertyOrEnv(): Boolean {
        return isPluginSpanFilterDefinedInPropertyOrEnv()
    }

    companion object {
        fun getInstance(): OtpDiagnosticSettings = service()
    }
}