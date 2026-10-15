package com.userhub.presentation

import com.userhub.domain.model.User
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class UserUiMapperTest {
    private val utc = TimeZone.UTC
    private val user = User(1, "Ada Lovelace", "ada@example.com", "female", "active")

    private fun at(iso: String) = Instant.parse(iso)

    @Test
    fun `under a minute is just now`() {
        val label = createdLabel(at("2026-10-15T14:00:00Z"), at("2026-10-15T14:00:20Z"), utc)
        assertEquals("Just now", label.text)
        assertEquals(at("2026-10-15T14:01:00Z"), label.nextChange)
    }

    @Test
    fun `under an hour shows minutes and changes next minute`() {
        val label = createdLabel(at("2026-10-15T14:00:00Z"), at("2026-10-15T14:05:30Z"), utc)
        assertEquals("5 minutes ago", label.text)
        assertEquals(at("2026-10-15T14:06:00Z"), label.nextChange)
    }

    @Test
    fun `59 minutes changes to hours at the hour mark`() {
        val label = createdLabel(at("2026-10-15T14:00:00Z"), at("2026-10-15T14:59:00Z"), utc)
        assertEquals("59 minutes ago", label.text)
        assertEquals(at("2026-10-15T15:00:00Z"), label.nextChange)
    }

    @Test
    fun `exactly one hour shows hours`() {
        val label = createdLabel(at("2026-10-15T14:00:00Z"), at("2026-10-15T15:00:00Z"), utc)
        assertEquals("1 hour ago", label.text)
        assertEquals(at("2026-10-15T16:00:00Z"), label.nextChange)
    }

    @Test
    fun `under a day shows hours and changes next hour`() {
        val label = createdLabel(at("2026-10-15T09:00:00Z"), at("2026-10-15T12:10:00Z"), utc)
        assertEquals("3 hours ago", label.text)
        assertEquals(at("2026-10-15T13:00:00Z"), label.nextChange)
    }

    @Test
    fun `hours carry on past midnight`() {
        val label = createdLabel(at("2026-10-14T23:00:00Z"), at("2026-10-15T02:00:00Z"), utc)
        assertEquals("3 hours ago", label.text)
        assertEquals(at("2026-10-15T03:00:00Z"), label.nextChange)
    }

    @Test
    fun `23 hours changes to the date at 24 hours`() {
        val label = createdLabel(at("2026-10-14T15:00:00Z"), at("2026-10-15T14:10:00Z"), utc)
        assertEquals("23 hours ago", label.text)
        assertEquals(at("2026-10-15T15:00:00Z"), label.nextChange)
    }

    @Test
    fun `exactly 24 hours shows the date and never changes`() {
        val label = createdLabel(at("2026-10-14T15:00:00Z"), at("2026-10-15T15:00:00Z"), utc)
        assertEquals("14 Oct", label.text)
        assertNull(label.nextChange)
    }

    @Test
    fun `older than a day shows the date`() {
        val label = createdLabel(at("2026-10-12T09:00:00Z"), at("2026-10-15T14:32:00Z"), utc)
        assertEquals("12 Oct", label.text)
        assertNull(label.nextChange)
    }

    @Test
    fun `date uses the local day rather than the UTC day`() {
        // 23:30 UTC on the 12th is already the 13th at UTC+2
        val label = createdLabel(
            at("2026-10-12T23:30:00Z"),
            at("2026-10-15T14:32:00Z"),
            UtcOffset(hours = 2).asTimeZone()
        )
        assertEquals("13 Oct", label.text)
    }

    @Test
    fun `future timestamp is just now`() {
        // device clock moved backwards
        val label = createdLabel(at("2026-10-15T14:05:00Z"), at("2026-10-15T14:00:00Z"), utc)
        assertEquals("Just now", label.text)
        assertNotNull(label.nextChange)
    }

    @Test
    fun `fetched users have no creation time`() {
        assertNull(listOf(user).toUiModels(emptyMap()).single().createdAt)
    }

    @Test
    fun `users created in the app keep their creation time`() {
        val createdAt = at("2026-10-15T14:00:00Z")
        val model =
            listOf(user).toUiModels(mapOf(user.id to createdAt.toEpochMilliseconds())).single()
        assertEquals(createdAt, model.createdAt)
    }
}