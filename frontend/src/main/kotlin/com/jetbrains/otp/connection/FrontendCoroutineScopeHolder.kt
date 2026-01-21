package com.jetbrains.otp.connection

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import kotlinx.coroutines.CoroutineScope

@Service(Service.Level.APP)
class FrontendCoroutineScopeHolder(val cs: CoroutineScope)

fun getFrontendCoroutineScope(): CoroutineScope = service<FrontendCoroutineScopeHolder>().cs