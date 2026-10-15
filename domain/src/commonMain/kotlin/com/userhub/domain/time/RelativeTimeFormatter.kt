package com.userhub.domain.time

import kotlin.time.Instant

// Shared by Android and iOS so both show the same text.
// Always rounds down: 119 minutes is "1 hour ago", not "2 hours ago".
fun formatRelativeTime(then: Instant, now: Instant): String {
    val minutes = (now - then).inWholeMinutes
    val hours = minutes / 60
    val days = minutes / 1_440
    return when {
        minutes < 1 -> "Just now"   // also covers a timestamp in the future (clock skew)
        minutes < 60 -> plural(minutes, "minute")
        hours < 24 -> plural(hours, "hour")
        else -> plural(days, "day")
    }
}

private fun plural(count: Long, unit: String) =
    if (count == 1L) "1 $unit ago" else "$count ${unit}s ago"
