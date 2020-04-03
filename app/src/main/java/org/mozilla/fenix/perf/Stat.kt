/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.perf

import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import java.io.File
import kotlin.math.roundToLong

private const val FIELD_POS_STARTTIME = 21 // starttime naming matches field in man page.

private const val MILLIS_IN_SECOND = 1000 // TimeUnit only works with Long so we calculate it manually.

/**
 * Functionality from stat on the proc pseudo-filesystem common to unix systems. /proc contains
 * information related to active processes. /proc/$pid/stat contains information about the status of
 * the process.
 *
 * See the man page - `man 5 proc` - on linux for more information:
 *   http://man7.org/linux/man-pages/man5/proc.5.html
 */
open class Stat {

    @VisibleForTesting(otherwise = PRIVATE)
    open fun getStatText(pid: Int): String = File("/proc/$pid/stat").readText()

    // Values defined by the system configuration. See `man 3 sysconf` for more information:
    @VisibleForTesting(otherwise = PRIVATE)
    open val clockTicksPerSecond: Long get() = Os.sysconf(OsConstants._SC_CLK_TCK)

    /**
     * Gets the process start time since system boot in millis. On the Pixel 2, the true granularity
     * is 10s of milliseconds (centiseconds) but we return milliseconds for convenience. This value
     * can be compared against [SystemClock.elapsedRealtime].
     *
     * Perf note: this call reads from the pseudo-filesystem using the java File APIs, which isn't
     * likely to be a very optimized call path.
     *
     * Implementation inspired by https://stackoverflow.com/a/42195623.
     */
    fun getProcessStartTimeMillis(pid: Int): Long {
        fun parseStartTimeClockTicksSinceSystemBoot(stat: String): Long {
            return stat.split(' ')[FIELD_POS_STARTTIME].toLong()
        }

        val startTimeClockTicks = parseStartTimeClockTicksSinceSystemBoot(getStatText(pid))
        val startTimeSeconds = startTimeClockTicks / clockTicksPerSecond.toDouble()
        val startTimeMillis = startTimeSeconds * MILLIS_IN_SECOND
        return startTimeMillis.roundToLong()
    }
}
