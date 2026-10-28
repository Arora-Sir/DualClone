package com.mohitarora.dualclone.core.common

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/** Text that view models and domain code can emit without touching a Context. */
sealed interface UiText {
    data class Plain(val value: String) : UiText
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Plural(@PluralsRes val id: Int, val count: Int, val args: List<Any> = emptyList()) : UiText

    fun resolve(context: Context): String = when (this) {
        is Plain -> value
        is Res -> context.getString(id, *args.toTypedArray())
        is Plural -> context.resources.getQuantityString(id, count, *args.toTypedArray())
    }
}

/** One-shot events shown as a snackbar. Replaces the single-slot statusMessage field. */
data class UiEvent(val message: UiText, val actionLabel: UiText? = null, val actionId: String? = null)

class UiEventChannel {
    private val channel = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = channel.receiveAsFlow()
    fun send(event: UiEvent) {
        channel.trySend(event)
    }
}
