package com.mohitarora.dualclone.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mohitarora.dualclone.work.RestoreAfterBootWorker

/**
 * Starts the delayed restore of Dual Messenger clones after a boot or a system server restart.
 * The receiver only schedules work. Whether anything is restored is decided later by [RestoreAfterBootWorker].
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        RestoreAfterBootWorker.schedule(context, System.currentTimeMillis())
    }
}
