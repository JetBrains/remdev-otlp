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
    private var metricsExportEnabledCheckBox: JBCheckBox? = null
    private var frequentPerformanceMetricsReportingCheckBox: JBCheckBox? = null
    private val coroutineScope = getFrontendCoroutineScope()

    override fun getDisplayName(): String = OtpDiagnosticBundle.message("settings.displayName")

    override fun createComponent(): JComponent {
        categoryCheckboxes.clear()
        val pluginSpanFilterOverridden = settings.isPluginFilterOverridden()
        val metricsExportEnabledOverridden = settings.isMetricsExportOverridden()
        val frequentPerformanceMetricsReportingOverridden = settings.isFrequentPerformanceMetricsReportingOverridden()
        frequentSpansCheckBox = JBCheckBox(OtpDiagnosticBundle.message("settings.checkbox.frequentSpans")).apply {
            isSelected = settings.isFrequentSpansEnabled()
        }
        pluginSpanFilterCheckBox = JBCheckBox(OtpDiagnosticBundle.message("settings.checkbox.pluginSpanFilter")).apply {
            isSelected = settings.pluginFilterEnabledEffective()
            isEnabled = !pluginSpanFilterOverridden
            if (pluginSpanFilterOverridden) {
                toolTipText = OtpDiagnosticBundle.message("settings.label.pluginSpanFilter.overridden")
            }
        }
        metricsExportEnabledCheckBox = JBCheckBox(OtpDiagnosticBundle.message("settings.checkbox.enableMetricsExport")).apply {
            isSelected = settings.metricsExportEnabledEffective()
            isEnabled = !metricsExportEnabledOverridden
            if (metricsExportEnabledOverridden) {
                toolTipText = OtpDiagnosticBundle.message("settings.label.enableMetricsExport.overridden")
            }
        }
        frequentPerformanceMetricsReportingCheckBox = JBCheckBox(
            OtpDiagnosticBundle.message("settings.checkbox.enableFrequentPerformanceMetricsReporting")
        ).apply {
            isSelected = settings.frequentPerformanceMetricsReportingEnabledEffective()
            isEnabled = !frequentPerformanceMetricsReportingOverridden
            if (frequentPerformanceMetricsReportingOverridden) {
                toolTipText = OtpDiagnosticBundle.message("settings.label.enableFrequentPerformanceMetricsReporting.overridden")
            }
        }

        val frequentSpansPanel = JPanel(BorderLayout(JBUI.scale(8), JBUI.scale(8))).apply {
            border = BorderFactory.createTitledBorder(OtpDiagnosticBundle.message("settings.group.frequentSpans"))
            val checksPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                add(frequentSpansCheckBox)
                add(pluginSpanFilterCheckBox)
                add(metricsExportEnabledCheckBox)
                add(frequentPerformanceMetricsReportingCheckBox)
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
        val pluginSpanFilterModified = !settings.isPluginFilterOverridden()
            && pluginSpanFilterCheckBox?.isSelected != settings.isPluginSpanFilterEnabled()
        val metricsExportEnabledModified = !settings.isMetricsExportOverridden()
            && metricsExportEnabledCheckBox?.isSelected != settings.isMetricsExportEnabled()
        val frequentPerformanceMetricsReportingModified = !settings.isFrequentPerformanceMetricsReportingOverridden()
            && frequentPerformanceMetricsReportingCheckBox?.isSelected != settings.isFrequentPerformanceMetricsReportingEnabled()
        val persistedKnownDisabled = settings.getDisabledCategories().intersect(SpanCategoryRegistry.allCategories)
        return frequentSpansModified
            || pluginSpanFilterModified
            || metricsExportEnabledModified
            || frequentPerformanceMetricsReportingModified
            || collectKnownDisabledCategories() != persistedKnownDisabled
    }

    override fun apply() {
        val disabledCategories = collectKnownDisabledCategories() + collectUnknownDisabledCategories()
        val frequentSpansEnabled = frequentSpansCheckBox?.isSelected ?: settings.isFrequentSpansEnabled()
        val pluginSpanFilterEnabled = if (settings.isPluginFilterOverridden()) {
            settings.pluginFilterEnabledEffective()
        } else {
            pluginSpanFilterCheckBox?.isSelected ?: settings.isPluginSpanFilterEnabled()
        }
        val metricsExportEnabled = if (settings.isMetricsExportOverridden()) {
            settings.metricsExportEnabledEffective()
        } else {
            metricsExportEnabledCheckBox?.isSelected ?: settings.isMetricsExportEnabled()
        }
        val frequentPerformanceMetricsReportingEnabled = if (settings.isFrequentPerformanceMetricsReportingOverridden()) {
            settings.frequentPerformanceMetricsReportingEnabledEffective()
        } else {
            frequentPerformanceMetricsReportingCheckBox?.isSelected ?: settings.isFrequentPerformanceMetricsReportingEnabled()
        }
        settings.syncFilteringSettings(
            disabledCategories = disabledCategories,
            frequentSpansEnabled = frequentSpansEnabled,
            pluginSpanFilterEnabled = pluginSpanFilterEnabled,
            metricsExportEnabled = metricsExportEnabled,
            frequentPerformanceMetricsReportingEnabled = frequentPerformanceMetricsReportingEnabled,
        )

        coroutineScope.launch {
            val backendSettings = OtpDiagnosticSettingsApi.getInstance()
            backendSettings.syncFilteringSettings(
                disabledCategories = disabledCategories,
                frequentSpansEnabled = frequentSpansEnabled,
                pluginSpanFilterEnabled = pluginSpanFilterEnabled,
                metricsExportEnabled = metricsExportEnabled,
                frequentPerformanceMetricsReportingEnabled = frequentPerformanceMetricsReportingEnabled,
            )
        }
    }

    override fun reset() {
        frequentSpansCheckBox?.isSelected = settings.isFrequentSpansEnabled()
        pluginSpanFilterCheckBox?.isSelected = settings.pluginFilterEnabledEffective()
        metricsExportEnabledCheckBox?.isSelected = settings.metricsExportEnabledEffective()
        frequentPerformanceMetricsReportingCheckBox?.isSelected = settings.frequentPerformanceMetricsReportingEnabledEffective()
        categoryCheckboxes.forEach { (categoryId, checkbox) ->
            checkbox.isSelected = settings.isCategoryEnabled(categoryId)
        }
        updateChildrenEnablement()
    }
}
