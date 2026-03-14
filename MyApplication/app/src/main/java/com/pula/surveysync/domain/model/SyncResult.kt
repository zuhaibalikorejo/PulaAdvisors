package com.pula.surveysync.domain.model

/**
 * Represents the result of a sync operation with detailed information
 */
data class SyncResult(
    val totalItems: Int,
    val successfulItems: List<String>,
    val failedItems: List<FailedItem>,
    val skippedItems: List<String>,
    val terminationReason: TerminationReason
) {
    val successCount: Int get() = successfulItems.size
    val failureCount: Int get() = failedItems.size
    val skippedCount: Int get() = skippedItems.size
    val isFullySuccessful: Boolean get() = failureCount == 0 && terminationReason == TerminationReason.COMPLETED
    val wasTerminatedEarly: Boolean get() = terminationReason != TerminationReason.COMPLETED
}

data class FailedItem(
    val itemId: String,
    val error: SyncError
)

enum class TerminationReason {
    COMPLETED,           // All items processed
    NETWORK_DEGRADED,    // Network issues detected, stopping to conserve resources
    CONCURRENT_SYNC,     // Another sync already running
    EMPTY_QUEUE,         // No items to sync
    CANCELLED            // Sync was explicitly cancelled
}

