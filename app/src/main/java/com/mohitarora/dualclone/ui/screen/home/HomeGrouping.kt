package com.mohitarora.dualclone.ui.screen.home

import androidx.annotation.StringRes
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.data.model.AppItem
import com.mohitarora.dualclone.ui.viewmodel.SortOrder

/** A labelled run of apps. Alphabetical sorts group by initial and date sorts group by age. */
data class AppGroup(
    val key: String,
    val label: String?,
    @StringRes val labelRes: Int?,
    val apps: List<AppItem>
)

private const val DAY = 24 * 60 * 60 * 1000L

fun SortOrder.isAlphabetical(): Boolean = this == SortOrder.NAME_ASC || this == SortOrder.NAME_DESC

fun alphabet(sort: SortOrder): List<Char> =
    if (sort == SortOrder.NAME_DESC) ('Z' downTo 'A').toList() + '#' else listOf('#') + ('A'..'Z').toList()

fun groupApps(apps: List<AppItem>, sort: SortOrder, now: Long = System.currentTimeMillis()): List<AppGroup> {
    if (sort.isAlphabetical()) {
        val order = alphabet(sort)
        return apps
            .groupBy { app ->
                val first = app.appName.firstOrNull()?.uppercaseChar() ?: '#'
                if (first in 'A'..'Z') first else '#'
            }
            .toSortedMap(compareBy { order.indexOf(it) })
            .map { (initial, list) -> AppGroup("h_$initial", initial.toString(), null, list) }
    }
    val time: (AppItem) -> Long = if (sort == SortOrder.RECENTLY_UPDATED) { a -> a.lastUpdatedTime } else { a -> a.installTime }
    val buckets = linkedMapOf(
        "today" to (R.string.group_today to mutableListOf<AppItem>()),
        "week" to (R.string.group_week to mutableListOf()),
        "month" to (R.string.group_month to mutableListOf()),
        "earlier" to (R.string.group_earlier to mutableListOf())
    )
    for (app in apps) {
        val diff = now - time(app)
        val bucket = when {
            diff < DAY -> "today"
            diff < 7 * DAY -> "week"
            diff < 30 * DAY -> "month"
            else -> "earlier"
        }
        buckets.getValue(bucket).second.add(app)
    }
    return buckets.filterValues { it.second.isNotEmpty() }
        .map { (key, value) -> AppGroup("h_$key", null, value.first, value.second) }
}

/** Index of the first list item for [letter] or the next group after it. */
fun indexForLetter(groups: List<AppGroup>, sort: SortOrder, letter: Char): Int {
    val order = alphabet(sort)
    val target = order.indexOf(letter)
    var cumulative = 0
    for (group in groups) {
        val initial = group.label?.firstOrNull() ?: continue
        if (initial == letter || order.indexOf(initial) >= target) return cumulative
        cumulative += group.apps.size
    }
    return -1
}
