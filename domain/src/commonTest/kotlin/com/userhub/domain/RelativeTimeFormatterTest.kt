package com.userhub.domain

import com.userhub.domain.time.formatRelativeTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class RelativeTimeFormatterTest {

    private val now = Instant.fromEpochMilliseconds(1_700_000_000_000L)

    private fun ago(elapsed: Duration) = formatRelativeTime(now - elapsed, now)

    @Test
    fun `under a minute is just now`() = assertEquals("Just now", ago(30.seconds))

    @Test
    fun `future timestamp is just now`() = assertEquals("Just now", ago((-5).minutes))

    @Test
    fun `one minute is singular`() = assertEquals("1 minute ago", ago(1.minutes))

    @Test
    fun `59 minutes stays in minutes`() = assertEquals("59 minutes ago", ago(59.minutes))

    @Test
    fun `60 minutes is one hour`() = assertEquals("1 hour ago", ago(60.minutes))

    @Test
    fun `119 minutes rounds down to one hour`() = assertEquals("1 hour ago", ago(119.minutes))

    @Test
    fun `120 minutes is two hours`() = assertEquals("2 hours ago", ago(120.minutes))

    @Test
    fun `1439 minutes is 23 hours`() = assertEquals("23 hours ago", ago(1_439.minutes))

    @Test
    fun `1440 minutes is one day`() = assertEquals("1 day ago", ago(1_440.minutes))

    @Test
    fun `2880 minutes is two days`() = assertEquals("2 days ago", ago(2_880.minutes))
}
