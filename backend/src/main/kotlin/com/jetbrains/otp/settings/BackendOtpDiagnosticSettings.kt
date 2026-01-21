package com.jetbrains.otp.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
@State(
    name = "BackendOtpDiagnosticSettings",
    storages = [Storage("otp-diagnostic-backend.xml")]
)
class BackendOtpDiagnosticSettings : PersistentStateComponent<BackendOtpDiagnosticSettings.State> {

    private var state = State()

    data class State(
        var disabledGroups: MutableSet<String> = mutableSetOf(FrequentSpans.groupName)
    )

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    fun syncDisabledGroups(disabledGroups: Set<String>) {
        state.disabledGroups.clear()
        state.disabledGroups.addAll(disabledGroups)
    }

    fun isGroupEnabled(groupName: String): Boolean {
        return groupName !in state.disabledGroups
    }

    fun isSpanEnabled(spanName: String): Boolean {
        val group = SpanNameRegistry.findGroupForSpan(spanName)
        return group?.let { isGroupEnabled(it.groupName) } ?: true
    }

    companion object {
        fun getInstance(): BackendOtpDiagnosticSettings = service()
    }
}
