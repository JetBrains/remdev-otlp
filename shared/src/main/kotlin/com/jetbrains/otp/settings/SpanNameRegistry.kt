// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.otp.settings


object SpanNameRegistry {

  val allGroups: List<SpanGroup> = listOf(
    RemoteDevelopmentSpans,
    AIAssistantSpans,
    TerminalSpans,
    BuildSystemSpans,
    ObjectiveCSymbolSpans,
    BazelBSPSpans,
    UIActionSpans,
    ApplicationLifecycleSpans,
    JavaScriptSpans,
    ExternalSystemSpans,
    TestingSpans,
    GoLanguageSpans,
    FrequentSpans,
  )

  val allSpanNames: List<String> = allGroups.flatMap { it.spanNames }

  fun findGroupForSpan(spanName: String): SpanGroup? {
    return allGroups.find { spanName in it.spanNames }
  }


  fun getSpansByGroup(groupName: String): List<String>? {
    return allGroups.find { it.groupName == groupName }?.spanNames
  }

  fun isRegistered(spanName: String): Boolean {
    return spanName in allSpanNames
  }
}
