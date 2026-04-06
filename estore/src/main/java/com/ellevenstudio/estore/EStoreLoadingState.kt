package com.ellevenstudio.estore

/**
 * Represents the loading state of EStore products.
 * Observe via `EStore.loadingState` to show appropriate UI.
 *
 * Usage:
 *     val state by EStore.loadingState.collectAsState()
 *     when (state) {
 *         is EStoreLoadingState.Idle -> { }
 *         is EStoreLoadingState.Loading -> { /* show spinner */ }
 *         is EStoreLoadingState.Loaded -> { /* show products */ }
 *         is EStoreLoadingState.Failed -> { /* show error: state.message */ }
 *     }
 */
sealed class EStoreLoadingState {
    data object Idle : EStoreLoadingState()
    data object Loading : EStoreLoadingState()
    data object Loaded : EStoreLoadingState()
    data class Failed(
        val message: String,
        val billingResponseCode: Int? = null
    ) : EStoreLoadingState()
}
