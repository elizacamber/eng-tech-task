package com.userhub.presentation

import com.userhub.domain.model.User
import com.userhub.domain.time.formatRelativeTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private val dateFormat =
    LocalDate.Format { day(); char(' '); monthName(MonthNames.ENGLISH_ABBREVIATED) }

fun List<User>.toUiModels(createdAtMillis: Map<Long, Long>): List<UserUiModel> =
    map { user ->
        UserUiModel(user, createdAtMillis[user.id]?.let(Instant::fromEpochMilliseconds))
    }

fun User.toNewUiModel(now: Instant): UserUiModel = UserUiModel(this, createdAt = now)

internal data class CreatedLabel(val text: String, val nextChange: Instant?)

// < 1 hour: minutes, updated every minute.
// < 24 hours: hours, updated every hour.
// Older: the local date, which never changes.
internal fun createdLabel(
    createdAt: Instant,
    now: Instant,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): CreatedLabel {
    val elapsed = now - createdAt
    return when {
        elapsed < 1.hours -> {
            val minutes = elapsed.inWholeMinutes.coerceAtLeast(0)
            CreatedLabel(formatRelativeTime(createdAt, now), createdAt + (minutes + 1).minutes)
        }

        elapsed < 24.hours -> {
            // at 23 hours the next whole hour is 24 hours, which is the switch to the date
            CreatedLabel(
                formatRelativeTime(createdAt, now),
                createdAt + (elapsed.inWholeHours + 1).hours
            )
        }

        else -> {
            val createdDay = createdAt.toLocalDateTime(timeZone).date
            CreatedLabel(createdDay.format(dateFormat), nextChange = null)
        }
    }
}
