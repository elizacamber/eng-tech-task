package com.userhub.data

import kotlin.experimental.ExperimentalNativeApi
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

internal actual fun currentTimeMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()

@OptIn(ExperimentalNativeApi::class)
internal actual val isDebug: Boolean = Platform.isDebugBinary
