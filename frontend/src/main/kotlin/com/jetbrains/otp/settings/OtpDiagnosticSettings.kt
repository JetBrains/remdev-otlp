package com.jetbrains.otp.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
@State(
    name = "OtpDiagnosticSettings",
    storages = [Storage("otp-diagnostic.xml")]
)
class OtpDiagnosticSettings : PersistentStateComponent<OtpDiagnosticSettings.State> {

    private var state = State()

    data class State(
        var disabledGroups: MutableSet<String> = mutableSetOf(FrequentSpans.groupName)
    )

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    fun isGroupEnabled(groupName: String): Boolean {
        return groupName !in state.disabledGroups
    }

    fun setGroupEnabled(groupName: String, enabled: Boolean) {
        if (enabled) {
            state.disabledGroups.remove(groupName)
        } else {
            state.disabledGroups.add(groupName)
        }
    }

    fun isSpanEnabled(spanName: String): Boolean {
        val group = SpanNameRegistry.findGroupForSpan(spanName)
        return group?.let { isGroupEnabled(it.groupName) } ?: true
    }

    companion object {
        fun getInstance(): OtpDiagnosticSettings = service()
    }
}
