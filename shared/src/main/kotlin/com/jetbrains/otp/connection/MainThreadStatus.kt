package com.jetbrains.otp.connection

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import java.awt.Dialog
import java.awt.GraphicsEnvironment
import java.awt.Window
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

fun checkMainThreadStatus(): Pair<Boolean, Boolean> {
    val application = ApplicationManager.getApplication()
    if (application.isDispatchThread) {
        return isModalDialogOpenedImpl() to false
    }

    val doneFuture = CompletableFuture<Boolean>()
    application.invokeLater({
        doneFuture.complete(isModalDialogOpenedImpl())
    }, ModalityState.any())

    return try {
        doneFuture.get(5, TimeUnit.SECONDS) to false
    } catch (ex: TimeoutException) {
        false to true
    }
}

private fun isModalDialogOpenedImpl(): Boolean {
    if (GraphicsEnvironment.isHeadless()) return false

    return Window.getWindows().any { window ->
        window is Dialog && window.isShowing && window.isModal
    }
}
