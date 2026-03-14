package com.pula.surveysync.domain.strategy

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Determines sync behavior based on device conditions
 */
interface DeviceAwareSyncStrategy {
    fun shouldSync(): SyncDecision
    fun getMaxBatchSize(): Int
    fun shouldRetryImmediately(): Boolean
}

data class SyncDecision(
    val shouldProceed: Boolean,
    val reason: String
)

class DefaultDeviceAwareSyncStrategy @Inject constructor(
    @ApplicationContext private val context: Context
) : DeviceAwareSyncStrategy {

    companion object {
        private const val MIN_BATTERY_LEVEL = 15 // Don't sync below 15% if not charging
        private const val MIN_STORAGE_MB = 100 // Need at least 100MB free
        private const val BATCH_SIZE_WIFI = 20
        private const val BATCH_SIZE_CELLULAR = 5
        private const val BATCH_SIZE_LOW_BATTERY = 3
    }

    override fun shouldSync(): SyncDecision {
        val batteryStatus = getBatteryStatus()
        val storageStatus = getStorageStatus()
        val networkStatus = getNetworkStatus()

        // Check battery level
        if (!batteryStatus.isCharging && batteryStatus.level < MIN_BATTERY_LEVEL) {
            return SyncDecision(
                shouldProceed = false,
                reason = "Battery level too low (${batteryStatus.level}%)"
            )
        }

        // Check available storage
        if (storageStatus.availableMB < MIN_STORAGE_MB) {
            return SyncDecision(
                shouldProceed = false,
                reason = "Insufficient storage (${storageStatus.availableMB}MB available)"
            )
        }

        // Check network connectivity
        if (!networkStatus.isConnected) {
            return SyncDecision(
                shouldProceed = false,
                reason = "No network connection"
            )
        }

        // Warn about metered connection but allow sync
        if (networkStatus.isMetered && !networkStatus.isWifi) {
            return SyncDecision(
                shouldProceed = true,
                reason = "Syncing on metered connection (will use smaller batches)"
            )
        }

        return SyncDecision(
            shouldProceed = true,
            reason = "All conditions met for sync"
        )
    }

    override fun getMaxBatchSize(): Int {
        val batteryStatus = getBatteryStatus()
        val networkStatus = getNetworkStatus()

        return when {
            // Low battery: smaller batches
            !batteryStatus.isCharging && batteryStatus.level < 30 -> BATCH_SIZE_LOW_BATTERY
            // Cellular/metered: medium batches
            !networkStatus.isWifi -> BATCH_SIZE_CELLULAR
            // WiFi: larger batches
            else -> BATCH_SIZE_WIFI
        }
    }

    override fun shouldRetryImmediately(): Boolean {
        val batteryStatus = getBatteryStatus()
        val networkStatus = getNetworkStatus()

        // Don't retry immediately if battery is low or on metered connection
        return batteryStatus.isCharging ||
               (batteryStatus.level > 50 && networkStatus.isWifi)
    }

    private fun getBatteryStatus(): BatteryStatus {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = batteryManager.isCharging

        return BatteryStatus(level, isCharging)
    }

    private fun getStorageStatus(): StorageStatus {
        val stat = StatFs(context.filesDir.path)
        val availableBytes = stat.availableBytes
        val availableMB = availableBytes / (1024 * 1024)
        val totalBytes = stat.totalBytes
        val totalMB = totalBytes / (1024 * 1024)

        return StorageStatus(availableMB, totalMB)
    }

    private fun getNetworkStatus(): NetworkStatus {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        val isConnected = capabilities != null
        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
        val isMetered = connectivityManager.isActiveNetworkMetered

        return NetworkStatus(isConnected, isWifi, isMetered)
    }
}

data class BatteryStatus(
    val level: Int,
    val isCharging: Boolean
)

data class StorageStatus(
    val availableMB: Long,
    val totalMB: Long
)

data class NetworkStatus(
    val isConnected: Boolean,
    val isWifi: Boolean,
    val isMetered: Boolean
)

