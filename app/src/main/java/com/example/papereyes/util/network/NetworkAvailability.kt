package com.example.papereyes.util.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkAvailability {
    fun hasValidatedInternet(context: Context): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java)
            ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

const val OFFLINE_MESSAGE =
    "No working internet connection. Reconnect, then try again."
