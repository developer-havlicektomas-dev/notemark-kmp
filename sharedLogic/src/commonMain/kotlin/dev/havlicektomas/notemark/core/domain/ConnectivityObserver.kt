package dev.havlicektomas.notemark.core.domain

import kotlinx.coroutines.flow.Flow

/** Emits the device's current online state; platform actuals watch the real network. */
interface ConnectivityObserver {
    val isConnected: Flow<Boolean>
}
