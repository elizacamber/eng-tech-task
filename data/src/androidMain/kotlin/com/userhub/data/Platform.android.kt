package com.userhub.data

internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()

internal actual val isDebug: Boolean = BuildConfig.DEBUG
