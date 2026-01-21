package com.jetbrains.otp.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.otp.connection.getFrontendCoroutineScope
import com.jetbrains.otp.settings.api.OtpDiagnosticSettingsApi
import kotlinx.coroutines.launch
import javax.swing.JComponent

class OtpDiagnosticConfigurable : Configurable {

    private val settings = OtpDiagnosticSettings.getInstance()
    private val groupCheckboxes = mutableMapOf<String, JBCheckBox>()
    private val coroutineScope = getFrontendCoroutineScope()

    override fun getDisplayName(): String = OtpDiagnosticBundle.message("settings.displayName")

    override fun createComponent(): JComponent {
        groupCheckboxes.clear()

        return panel {
            group(OtpDiagnosticBundle.message("settings.group.spanCategories")) {
                row {
                    label(OtpDiagnosticBundle.message("settings.label.description"))
                }

                for (spanGroup in SpanNameRegistry.allGroups) {
                    row {
                        val checkbox = checkBox(spanGroup.groupName)
                            .comment(spanGroup.description)
                            .component
                        checkbox.isSelected = settings.isGroupEnabled(spanGroup.groupName)
                        groupCheckboxes[spanGroup.groupName] = checkbox
                    }
                }
            }
        }
    }

    override fun isModified(): Boolean {
        return groupCheckboxes.any { (groupName, checkbox) ->
            checkbox.isSelected != settings.isGroupEnabled(groupName)
        }
    }

    override fun apply() {
        groupCheckboxes.forEach { (groupName, checkbox) ->
            settings.setGroupEnabled(groupName, checkbox.isSelected)
        }

        val disabledGroups = SpanNameRegistry.allGroups
            .map { it.groupName }
            .filter { !settings.isGroupEnabled(it) }
            .toSet()

        coroutineScope.launch {
            val backendSettings = OtpDiagnosticSettingsApi.getInstance()
            backendSettings.syncDisabledGroups(disabledGroups)
        }
    }

    override fun reset() {
        groupCheckboxes.forEach { (groupName, checkbox) ->
            checkbox.isSelected = settings.isGroupEnabled(groupName)
        }
    }
}
