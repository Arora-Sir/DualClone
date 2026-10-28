package com.mohitarora.dualclone.shizuku

import rikka.shizuku.Shizuku
import timber.log.Timber
import java.lang.reflect.Method

object ShizukuProcessHelper {
    private val newProcessMethod: Method by lazy {
        Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        ).apply {
            isAccessible = true
        }
    }

    fun newProcess(cmd: Array<String>, env: Array<String>? = null, dir: String? = null): Process {
        return newProcessMethod.invoke(null, cmd, env, dir) as Process
    }

    /**
     * Resolves the reflected method once and reports a missing member loudly.
     * A minified build that stripped it would otherwise fail every command with an opaque error.
     */
    fun selfTest(): Result<Unit> = runCatching { newProcessMethod; Unit }.onFailure {
        Timber.tag("ShizukuProcess").e(it, "Shizuku.newProcess reflection target is missing, all commands will fail")
    }
}
