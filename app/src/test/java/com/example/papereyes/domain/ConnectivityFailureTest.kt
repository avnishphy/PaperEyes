package com.example.papereyes.domain

import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectivityFailureTest {
    @Test
    fun detectsDirectAndWrappedTransportFailures() {
        assertTrue(UnknownHostException().isConnectivityFailure())
        assertTrue(
            PaperResolutionException(
                "Lookup unavailable",
                SocketTimeoutException()
            ).isConnectivityFailure()
        )
    }

    @Test
    fun doesNotMisclassifyNonNetworkFailures() {
        assertFalse(IllegalStateException("bad response").isConnectivityFailure())
    }
}
