// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.otp.settings

object RemoteDevelopmentSpans : SpanGroup {
  override val groupName = "Remote Development"
  override val description = "RDCT (Remote Development Client-Thin) spans for remote IDE connectivity"
  override val spanNames = listOf(
    "rdct.station.LinkHandler",
    "rdct.station.LinkHandler: started",
    "rdct.station.LinkHandler.send.hello",
    "rdct.station.LinkHandler.received.new.link",
    "rdct.station.LinkHandler.received.new.problem",
    "rdct.station.LinkHandler.received.no.problem",
    "rdct.station.LinkHandler.requested.link",
    "rdct.station.LinkHandler.requested.problem.fix",
    "rdct.reportHostStatus",
    "rdct.UnattendedMode.init",
    "rdct.host.shutdown.notifying.client",
    "rdct.client.connection.project",
    "rdct.client.connection.project.init",
    "rdct.client.connection.project.open",
    "rdct.client.connection.project.is.loaded",
    "rdct.client.connection.InitialConnection.loop",
    "rdct.client.host.shutdown.event",
    "rdct.client.host.shutdown.event.acknowledged",
    "rdct.client.connection.NewConnection",
    "rdct.client.connection.NewConnection.protocol",
    "rdct.gateway.LinkHandler",
    "rdct.gateway.LinkHandler.requested.link",
    "rdct.gateway.LinkHandler.received.new.link",
    "rdct.gateway.LinkHandler.reporting.capabilities",
    "rdct.ThinClientHandle",
    "rdct.ThinClientHandle.client.connection",
    "rdct.ThinClientHandle.client.send.restart.request",
    "rdct.ThinClientHandle.client.received.link.request",
    "rdct.ThinClientHandle.start.client",
    "rdct.ThinClientHandle.send.new.link",
    "rdct.gateway.GatewayConnectionHandle",
    "rdct.gateway.forwardPort",
    "rdct.gateway.ensureHostRunning",
  )
}

object AIAssistantSpans : SpanGroup {
  override val groupName = "AI Assistant"
  override val description = "AI code generation, chat, and context processing"
  override val spanNames = listOf(
    "ai-generate-code",
    "ai-ignore",
    "SimpleCompletableMessage::state",
    "CombinedContextLoader::rankRetrievedContext",
    "CombinedContextLoader::retrieveContextSync",
    "CombinedContextLoader::retrieveContextAsync",
    "ContextRetrieverV2::computeContext",
    "ContextTransformer::transform",
    "ContextPostProcessor::process",
    "agent-metadata",
  )
}

object TerminalSpans : SpanGroup {
  override val groupName = "Terminal"
  override val description = "Terminal language detection and ML models"
  override val spanNames = listOf(
    "terminal-lang-detection-load-model",
    "terminal-lang-detection-root",
    "terminal-lang-detection-predict",
  )
}

object BuildSystemSpans : SpanGroup {
  override val groupName = "Build System"
  override val description = "Compilation, downloading, and build pipeline"
  override val spanNames = listOf(
    "build Professional Edition",
    "build Community Edition",
    "build Toolbox Feed",
    "build pydevd-pycharm package",
    "build DataSpell",
    "build ide",
    "build rider searchable options for i18n plugins",
    "build searchable options for i18n plugins",
    "build provided module list",
    "downloading artifact",
    "download LLDBFrontend",
    "compile all modules and tests",
    "run rider tests",
    "run TestNG command",
    "preparing sources",
    "prepare plugins bundled parts",
    "patch deps.json",
    "setup CIDR dependencies",
    "prepare plugins backend parts"
  )
}

object ObjectiveCSymbolSpans : SpanGroup {
  override val groupName = "Objective-C Symbols"
  override val description = "CIDR symbol building and processing"
  override val spanNames = listOf(
    "OCSymbolLoadingActivity",
    "OCBuildSymbols",
    "OCSymbolBuildingActivity",
    "buildSourceFiles",
    "buildHeaderFiles",
    "OCModuleMapDeserializationActivity",
    "OCClearingSymbolsActivity",
    "OCHeaderMapLoadingActivity",
    "OCFileCollectingActivity",
    "OCModuleMapSerializationActivity",
    "OCSymbolSavingActivity",
    "OCModuleMapBuildingActivity",
  )
}

object BazelBSPSpans : SpanGroup {
  override val groupName = "Bazel/BSP"
  override val description = "Build Server Protocol and Bazel integration"
  override val spanNames = listOf(
    "Resolve project",
    "Get aspect output paths",
    "bsp.sync.project.ms",
    "collect.project.details.ms",
    "apply.changes.on.workspace.model.ms",
    "replaceprojectmodel.in.apply.on.workspace.model.ms",
    "calculate.all.unique.jdk.infos.ms",
    "calculate.all.scala.sdk.infos.ms",
    "create.libraries.ms",
    "create.library.modules.ms",
    "create.module.details.ms",
    "create.target.id.to.module.entities.map.ms",
    "calculate.non.generated.class.files.to.exclude",
    "load.modules.ms",
    "add.bsp.fetched.jdks.ms",
    "global",
    "execute request"
  )
}

object UIActionSpans : SpanGroup {
  override val groupName = "UI & Actions"
  override val description = "Action system and UI interactions"
  override val spanNames = listOf(
    "expandActionGroup",
    "fillMenu",
    "show notification",
  )
}

object ApplicationLifecycleSpans : SpanGroup {
  override val groupName = "Application Lifecycle"
  override val description = "Startup, shutdown, and application state"
  override val spanNames = listOf(
    "application.exit",
    "saveSettingsOnExit",
    "disposeProjects",
  )
}


object JavaScriptSpans : SpanGroup {
  override val groupName = "JavaScript/TypeScript"
  override val description = "Language service and compiler process"
  override val spanNames = listOf(
    "TypeScript compiler process: TS project loading"
  )
}


object ExternalSystemSpans : SpanGroup {
  override val groupName = "External Systems"
  override val description = "Gradle, Maven, and dependency management"
  override val spanNames = listOf(
    "ProjectDataServices",
    "updateDependencySubstitutions",
    "buildLibraryToModuleMap",
    "buildDependencyMap"
  )
}

object TestingSpans : SpanGroup {
  override val groupName = "Testing"
  override val description = "Performance testing and test execution"
  override val spanNames = listOf(
    "testPlan",
    "AllInstancesSummaryStartup",
  )
}

object GoLanguageSpans : SpanGroup {
  override val groupName = "Go Language"
  override val description = "Go module operations and tooling"
  override val spanNames = listOf(
    "Subtask: go list -m -json",
  )
}

object FrequentSpans : SpanGroup {
  override val groupName = "Frequent Spans"
  override val description = "High-frequency spans that may generate significant telemetry volume"
  override val spanNames = listOf("run daemon", "show notification", "backend: getting items for the navigation bar", "backend: apply patch")
}