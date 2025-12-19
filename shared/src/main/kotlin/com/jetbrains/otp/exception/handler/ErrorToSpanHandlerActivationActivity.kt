@file:Suppress("UnstableApiUsage")

package com.jetbrains.otp.exception.handler

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.util.logging.Logger

class ErrorToSpanHandlerActivationActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val logger = Logger.getLogger("")
        if (logger.handlers.contains(ErrorToSpanHandler)) return
        logger.addHandler(ErrorToSpanHandler)
    }

}