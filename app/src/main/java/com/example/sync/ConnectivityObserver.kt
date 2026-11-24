package com.example.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConnectivityObserver(context: Context) {
  private val connectivityManager =
    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

  private val _isOnline = MutableStateFlow(checkCurrentConnectivity())
  val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

  private val networkCallback =
    object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        _isOnline.value = checkCurrentConnectivity()
      }

      override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        _isOnline.value =
          networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
      }

      override fun onLost(network: Network) {
        _isOnline.value = checkCurrentConnectivity()
      }
    }

  init {
    try {
      connectivityManager.registerDefaultNetworkCallback(networkCallback)
    } catch (_: Exception) {
      _isOnline.value = false
    }
  }

  private fun checkCurrentConnectivity(): Boolean {
    val active = connectivityManager.activeNetwork ?: return false
    val caps = connectivityManager.getNetworkCapabilities(active) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
      caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
  }

  fun dispose() {
    try {
      connectivityManager.unregisterNetworkCallback(networkCallback)
    } catch (_: Exception) {
      // Ignore if already unregistered
    }
  }
}
