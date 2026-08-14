package com.cyebrcina.pos.data.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The one piece of connectivity-awareness infrastructure in the app — everything reacting to
 * offline/online state (menu caching, the offline order queue in FireHutOrderRepositoryImpl)
 * watches [isOnline] rather than each independently guessing from a failed API call.
 *
 * "Online" here means "the device has *a* validated network path" (Wi-Fi/cellular with real
 * internet access, not just an AP with no upstream) — a fast, local, OS-level signal with no
 * network call of its own. It does NOT prove Fire Hut's own backend is reachable (the device
 * could have working Wi-Fi while this specific server is down) — that's still only proven by a
 * real API call succeeding. This flow exists so a Wi-Fi drop is noticed instantly instead of
 * waiting up to 15s for the next poll to time out.
 */
@Singleton
class ConnectivityObserver @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(currentlyOnline())
    val isOnline: StateFlow<Boolean> = _isOnline

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        // No matching unregisterNetworkCallback — this class is a Hilt SingletonComponent
        // instance, alive for the whole process, same lifetime convention as
        // FireHutRealtimeManager's socket connection elsewhere in the app.
        connectivityManager.registerNetworkCallback(
            request,
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = currentlyOnline()
                }

                override fun onLost(network: Network) {
                    _isOnline.value = currentlyOnline()
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    _isOnline.value = currentlyOnline()
                }
            },
        )
    }

    private fun currentlyOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
