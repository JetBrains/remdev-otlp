package com.jetbrains.otp.connection

import com.intellij.openapi.client.ClientAppSession
import com.jetbrains.otp.span.DefaultRootSpanService
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rdserver.core.RemoteClientSessionListener

class HostSessionSpanUpdater : RemoteClientSessionListener {
    @Suppress("UnstableApiUsage")
    override fun appSessionInitialized(
        lifetime: Lifetime,
        session: ClientAppSession
    ) {
        DefaultRootSpanService.getInstance().startSessionSpan(session.clientId.value)

        lifetime.onTermination {
            DefaultRootSpanService.getInstance().endSessionSpan()
        }
    }
}