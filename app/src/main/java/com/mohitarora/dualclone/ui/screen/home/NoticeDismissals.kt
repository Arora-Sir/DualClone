package com.mohitarora.dualclone.ui.screen.home

/**
 * Which profile notices the user has dismissed, kept as one string so it survives rotation with rememberSaveable.
 * The helpers are pure so the rules can be unit tested without a device.
 */
internal object NoticeDismissals {
    private const val SEPARATOR = "|"

    fun pausedKey(userId: Int) = "paused:$userId"

    fun keys(raw: String): List<String> = raw.split(SEPARATOR).filter { it.isNotEmpty() }

    fun isDismissed(raw: String, key: String): Boolean = key in keys(raw)

    fun dismiss(raw: String, key: String): String = (keys(raw) + key).distinct().joinToString(SEPARATOR)

    /** A notice whose problem is gone is forgotten, so it shows again when the same problem comes back. */
    fun keepActive(raw: String, activeKeys: List<String>): String = keys(raw).filter { it in activeKeys }.joinToString(SEPARATOR)

    /** Tapping a profile chip again reopens that profile's paused notice even after it was dismissed. */
    fun reopenPaused(raw: String, userId: Int): String = keys(raw).filter { it != pausedKey(userId) }.joinToString(SEPARATOR)
}
