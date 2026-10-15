package com.userhub.data

import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
internal actual val isDebug: Boolean = Platform.isDebugBinary
