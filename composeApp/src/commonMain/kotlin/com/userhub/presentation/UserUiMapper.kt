package com.userhub.presentation

import com.userhub.data.remote.UserDto
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private val timeFormat = LocalTime.Format { hour(); char(':'); minute() }
private val dateFormat =
    LocalDate.Format { day(); char(' '); monthName(MonthNames.ENGLISH_ABBREVIATED) }

fun List<UserDto>.toUiModels(createdAtMillis: Map<Long, Long>, now: Instant): List<UserUiModel> =
    map { user ->
        val createdAt = createdAtMillis[user.id]?.let(Instant::fromEpochMilliseconds)
        UserUiModel(user = user, createdLabel = createdAt?.let { formatCreatedLabel(it, now) })
    }

fun UserDto.toNewUiModel(now: Instant): UserUiModel =
    UserUiModel(user = this, createdLabel = formatCreatedLabel(now, now))

// "Today, 14:32" or "12 Oct", in the device's current timezone
internal fun formatCreatedLabel(
    createdAt: Instant,
    now: Instant,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val local = createdAt.toLocalDateTime(timeZone)
    val today = now.toLocalDateTime(timeZone).date
    return if (local.date == today) "Today, ${local.time.format(timeFormat)}"
    else local.date.format(dateFormat)
}
