package com.userhub.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.userhub.presentation.createdLabel
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Instant

// Recomputes the label exactly when its text changes, and only while the screen is visible.
@Composable
fun rememberCreatedLabel(createdAt: Instant?, clock: Clock = Clock.System): String? {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    return produceState(createdAt?.let { createdLabel(it, clock.now()).text }, createdAt) {
        if (createdAt == null) return@produceState
        // restarts with a fresh label each time the app comes back to the foreground
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val label = createdLabel(createdAt, clock.now())
                value = label.text
                val next = label.nextChange ?: break   // a date never changes, so stop
                delay(next - clock.now())
            }
        }
    }.value
}