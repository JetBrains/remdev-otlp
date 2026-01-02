package com.jetbrains.otp.connection

import com.intellij.openapi.client.ClientAppSession
import com.jetbrains.otp.span.DefaultRootSpanService
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rdclient.connection.ClientSessionListener
import com.jetbrains.thinclient.ThinClientId

class ClientSessionSpanUpdater : ClientSessionListener {
    @Suppress("UnstableApiUsage")
    override fun appSessionInitialized(
        lifetime: Lifetime,
        session: ClientAppSession
    ) {
        DefaultRootSpanService.getInstance().startSessionSpan(ThinClientId.Instance.value)

        lifetime.onTermination {
            DefaultRootSpanService.getInstance().endSessionSpan()
        }
    }
}