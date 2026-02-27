package com.jetbrains.otp.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.jetbrains.otp.api.OtpDiagnosticSettingsApi
import com.jetbrains.otp.connection.getFrontendCoroutineScope
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

class OtpDiagnosticConfigurable : Configurable {

    private val settings = OtpDiagnosticSettings.getInstance()
    private val categoryCheckboxes = mutableMapOf<String, JBCheckBox>()
    private var frequentSpansCheckBox: JBCheckBox? = null
    private var pluginSpanFilterCheckBox: JBCheckBox? = null
    private val coroutineScope = getFrontendCoroutineScope()

    override fun getDisplayName(): String = OtpDiagnosticBundle.message("settings.displayName")

    override fun createComponent(): JComponent {
        categoryCheckboxes.clear()
        val pluginSpanFilterOverridden = settings.isPluginSpanFilterOverriddenByPropertyOrEnv()
        frequentSpansCheckBox = JBCheckBox(OtpDiagnosticBundle.message("settings.checkbox.frequentSpans")).apply {
            isSelected = settings.isFrequentSpansEnabled()
        }
        pluginSpanFilterCheckBox = JBCheckBox(OtpDiagnosticBundle.message("settings.checkbox.pluginSpanFilter")).apply {
            isSelected = settings.isPluginSpanFilterEnabledEffective()
            isEnabled = !pluginSpanFilterOverridden
            if (pluginSpanFilterOverridden) {
                toolTipText = OtpDiagnosticBundle.message("settings.label.pluginSpanFilter.overridden")
            }
        }

        val frequentSpansPanel = JPanel(BorderLayout(JBUI.scale(8), JBUI.scale(8))).apply {
            border = BorderFactory.createTitledBorder(OtpDiagnosticBundle.message("settings.group.frequentSpans"))
            val checksPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                add(frequentSpansCheckBox)
                add(pluginSpanFilterCheckBox)
            }
            add(checksPanel, BorderLayout.NORTH)
            add(JBLabel(OtpDiagnosticBundle.message("settings.label.frequentSpans.description")), BorderLayout.CENTER)
        }

        val categoryListPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(4, 0)
            isOpaque = false
        }

        SpanCategoryRegistry.rootCategories.forEach { category ->
            addCategoryRow(categoryListPanel, category, level = 0)
        }

        updateChildrenEnablement()

        val scrollPane = JBScrollPane(categoryListPanel).apply {
            border = JBUI.Borders.empty()
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }

        val contentPanel = JPanel(BorderLayout(JBUI.scale(8), JBUI.scale(8))).apply {
            border = JBUI.Borders.empty(8)
            add(JBLabel(OtpDiagnosticBundle.message("settings.label.description")), BorderLayout.NORTH)
            add(scrollPane, BorderLayout.CENTER)
        }

        val categorySettingsPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createTitledBorder(OtpDiagnosticBundle.message("settings.group.spanCategories"))
            add(contentPanel, BorderLayout.CENTER)
        }

        return JPanel(BorderLayout(JBUI.scale(8), JBUI.scale(8))).apply {
            border = JBUI.Borders.empty(8)
            add(frequentSpansPanel, BorderLayout.NORTH)
            add(categorySettingsPanel, BorderLayout.CENTER)
        }
    }

    private fun addCategoryRow(container: JPanel, category: SpanCategory, level: Int) {
        val checkbox = JBCheckBox(category.label).apply {
            isSelected = settings.isCategoryEnabled(category.id)
            if (category.label != category.id) {
                toolTipText = category.id
            }
            addActionListener { updateChildrenEnablement() }
        }

        categoryCheckboxes[category.id] = checkbox

        val rowPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(2, JBUI.scale(level * 18), 2, 0)
            isOpaque = false
            add(checkbox, BorderLayout.CENTER)
        }

        container.add(rowPanel)
        category.children.forEach { child ->
            addCategoryRow(container, child, level + 1)
        }
    }

    private fun updateChildrenEnablement() {
        SpanCategoryRegistry.rootCategories.forEach { rootCategory ->
            updateChildrenEnablement(rootCategory, parentEnabled = true)
        }
    }

    private fun updateChildrenEnablement(category: SpanCategory, parentEnabled: Boolean) {
        val checkbox = categoryCheckboxes[category.id] ?: return
        checkbox.isEnabled = parentEnabled
        val childrenParentEnabled = parentEnabled && checkbox.isSelected
        category.children.forEach { child ->
            updateChildrenEnablement(child, childrenParentEnabled)
        }
    }

    private fun collectKnownDisabledCategories(): Set<String> {
        return categoryCheckboxes
            .filterValues { checkbox -> !checkbox.isSelected }
            .keys
            .toSet()
    }

    private fun collectUnknownDisabledCategories(): Set<String> {
        return settings.getDisabledCategories() - SpanCategoryRegistry.allCategories
    }

    override fun isModified(): Boolean {
        val frequentSpansModified = frequentSpansCheckBox?.isSelected != settings.isFrequentSpansEnabled()
        val pluginSpanFilterModified = !settings.isPluginSpanFilterOverriddenByPropertyOrEnv()
            && pluginSpanFilterCheckBox?.isSelected != settings.isPluginSpanFilterEnabled()
        val persistedKnownDisabled = settings.getDisabledCategories().intersect(SpanCategoryRegistry.allCategories)
        return frequentSpansModified || pluginSpanFilterModified || collectKnownDisabledCategories() != persistedKnownDisabled
    }

    override fun apply() {
        val disabledCategories = collectKnownDisabledCategories() + collectUnknownDisabledCategories()
        val frequentSpansEnabled = frequentSpansCheckBox?.isSelected ?: settings.isFrequentSpansEnabled()
        val pluginSpanFilterEnabled = if (settings.isPluginSpanFilterOverriddenByPropertyOrEnv()) {
            settings.isPluginSpanFilterEnabledEffective()
        } else {
            pluginSpanFilterCheckBox?.isSelected ?: settings.isPluginSpanFilterEnabled()
        }
        settings.syncFilteringSettings(
            disabledCategories = disabledCategories,
            frequentSpansEnabled = frequentSpansEnabled,
            pluginSpanFilterEnabled = pluginSpanFilterEnabled,
        )

        coroutineScope.launch {
            val backendSettings = OtpDiagnosticSettingsApi.getInstance()
            backendSettings.syncFilteringSettings(
                disabledCategories = disabledCategories,
                frequentSpansEnabled = frequentSpansEnabled,
                pluginSpanFilterEnabled = pluginSpanFilterEnabled,
            )
        }
    }

    override fun reset() {
        frequentSpansCheckBox?.isSelected = settings.isFrequentSpansEnabled()
        pluginSpanFilterCheckBox?.isSelected = settings.isPluginSpanFilterEnabledEffective()
        categoryCheckboxes.forEach { (categoryId, checkbox) ->
            checkbox.isSelected = settings.isCategoryEnabled(categoryId)
        }
        updateChildrenEnablement()
    }
}
