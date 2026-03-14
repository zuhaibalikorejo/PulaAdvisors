package com.pula.surveysync.domain.model

/**
 * Progress information for ongoing sync operations
 */
data class SyncProgress(
    val current: Int,
    val total: Int,
    val currentItemId: String? = null,
    val phase: SyncPhase = SyncPhase.UPLOADING
) {
    val percentage: Float
        get() = if (total > 0) (current.toFloat() / total.toFloat()) * 100f else 0f

    val displayText: String
        get() = "${phase.displayName} $current of $total..."
}

enum class SyncPhase(val displayName: String) {
    PREPARING("Preparing"),
    UPLOADING("Uploading"),
    FINALIZING("Finalizing"),
    COMPLETED("Completed")
}

