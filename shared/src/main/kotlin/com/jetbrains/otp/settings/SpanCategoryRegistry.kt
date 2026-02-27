package com.jetbrains.otp.settings

data class SpanCategory(
    val id: String,
    val label: String = id,
    val children: List<SpanCategory> = emptyList(),
)

object SpanCategoryRegistry {

    private val parentToChildren: LinkedHashMap<String, List<String>> = linkedMapOf(
        "rdct" to listOf("connection", "gateway.flow", "lux"),
        "platform.metrics" to listOf(
            "ExtractionMetricsScope",
            "compiler",
            "completion.ranking.ml",
            "dependencySubstitution",
            "edt",
            "external.system",
            "findUsages",
            "ijent",
            "indexes",
            "jps",
            "jvm",
            "progressManager",
            "storage",
            "test",
            "ui",
            "vfs",
            "workspaceModel",
        ),
        "AIAssistant" to listOf("Langfuse"),
        "station" to listOf("discovery", "station.flow"),
    )

    private val standaloneCategories = listOf(
        "vcs",
        "daemon",
        "exitApp",
        "projectViewInit",
        "startup",
        "notification",
        "terminal-completion",
        "terminal-lang-detection",
        "CodeVision",
        "HandleSpanCommand",
        "HighlightingPasses",
        "JS Language Service",
        "MoveDeclarations",
        "MoveFiles",
        "PerformanceWatcher",
        "RenameProcessorScope",
        "SimpleCompletableMessage",
        "actionSystem",
        "ai-assistant",
        "benchmarkUnitTests",
        "codeCompletion",
        "codeCompletionContributors",
        "combined-context-loader",
        "globalInspection",
        "highlightVisitor",
        "javaToKotlin",
        "kmm-ide",
        "llm.privacy",
        "mcpServer",
        "moduleMaps",
        "performance-plugin",
        "project generator creation",
        "qodana",
        "semanticSearch",
        "skiko",
        "stopWatch",
        "symbols",
    )

    val rootCategories: List<SpanCategory> = buildRootCategories()

    val allCategories: Set<String> = flattenCategoryIds(rootCategories).toSet()

    fun isScopeEnabled(scopeName: String, disabledCategories: Set<String>): Boolean {
        if (scopeName.isBlank()) return true
        return disabledCategories.none { categoryId -> isScopeInCategory(scopeName, categoryId) }
    }

    fun isScopeInCategory(scopeName: String, categoryId: String): Boolean {
        if (scopeName == categoryId) return true
        return scopeName.startsWith("$categoryId.")
    }

    private fun buildRootCategories(): List<SpanCategory> {
        val roots = mutableListOf<SpanCategory>()
        val rootIds = linkedSetOf<String>()

        for ((parent, children) in parentToChildren) {
            rootIds.add(parent)
            roots += SpanCategory(
                id = parent,
                label = parent,
                children = children.map { child ->
                    SpanCategory(
                        id = childCategoryId(parent, child),
                        label = child,
                    )
                }
            )
        }

        for (categoryId in standaloneCategories) {
            if (rootIds.add(categoryId)) {
                roots += SpanCategory(id = categoryId)
            }
        }

        return roots
    }

    private fun childCategoryId(parent: String, child: String): String {
        return if (child.startsWith("$parent.")) child else "$parent.$child"
    }

    private fun flattenCategoryIds(categories: List<SpanCategory>): List<String> {
        return buildList {
            categories.forEach { category ->
                add(category.id)
                addAll(flattenCategoryIds(category.children))
            }
        }
    }
}