package com.mohitarora.dualclone.ui.external

import android.content.Context
import android.widget.Toast

/** Single-instance toast for places that have no snackbar host. Cancels the visible toast first. */
object AppToast {
    private var current: Toast? = null

    fun show(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        current?.cancel()
        current = Toast.makeText(context.applicationContext, message, duration).also { it.show() }
    }
}
