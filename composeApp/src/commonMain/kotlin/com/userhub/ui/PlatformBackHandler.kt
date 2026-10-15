package com.userhub.ui

import androidx.compose.runtime.Composable

// System back on Android. iOS has no system back button, so the on-screen arrow handles it there.
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)