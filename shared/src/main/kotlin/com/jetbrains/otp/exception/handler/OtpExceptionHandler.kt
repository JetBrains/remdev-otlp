@file:Suppress("UnstableApiUsage")

package com.jetbrains.otp.exception.handler

import com.intellij.ide.plugins.PluginUtil
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ex.ActionContextElement
import com.intellij.openapi.application.Interactive
import com.intellij.openapi.application.impl.CoroutineExceptionHandlerImpl
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class OtpExceptionHandler : AbstractCoroutineContextElement(CoroutineExceptionHandler), CoroutineExceptionHandler {
    override fun handleException(context: CoroutineContext, exception: Throwable) {
        ORIGINAL_HANDLER.handleException(context, exception)
        val action = getCurrentActionName(context)
        val plugin = PluginUtil.getInstance().findPluginId(exception)?.idString
        Span.current().recordException(exception, Attributes.of(
            AttributeKey.stringKey("action"), action,
            AttributeKey.stringKey("plugin"), plugin
        ))
    }

    private fun getCurrentActionName(coroutineContext: CoroutineContext?): String? {
        val action = coroutineContext?.get(ActionContextElement)
        val interactive = coroutineContext?.get(Interactive.Key)

        if (interactive != null) {
            return interactive.action
        } else if (action != null) {
            val text = ActionManager.getInstance().getAction(action.actionId)?.templatePresentation?.text
            return text
        }
        return null
    }
}

private val ORIGINAL_HANDLER = CoroutineExceptionHandlerImpl()