/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.perf

import android.util.Log
import androidx.annotation.UiThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.EnumMap
import java.util.concurrent.TimeUnit

private const val TAG = Performance.TAG

/**
 * Instruments and logs metrics related to startup.
 *
 * Currently, logged values are only guaranteed to be valid during cold startup to homescreen.
 */
@UiThread // to minimize overhead and simplify call patterns.
object StartupTime {

    enum class Method {
        APP_ON_CREATE,
        HOME_ACTIVITY_ON_CREATE,
        HOME_ACTIVITY_ON_RESUME,
        HOME_FRAGMENT_ON_CREATE,
        HOME_FRAGMENT_ON_CREATE_VIEW,
        HOME_FRAGMENT_ON_VIEW_CREATED,
        HOME_FRAGMENT_ON_START,
        HOME_FRAGMENT_ON_RESUME
    }

    private val methodStart = EnumMap<Method, Long>(Method::class.java)
    private val methodEnd = EnumMap<Method, Long>(Method::class.java)

    fun instrumentStart(method: Method, time: Long = System.nanoTime()) {
        methodStart[method] = time
    }

    fun instrumentEnd(method: Method, time: Long = System.nanoTime()) {
        methodEnd[method] = time
    }

    /**
     * Logs the instrumented values; expected to be called after startup instrumentation is complete.
     *
     * Ensure VERBOSE debugging is enabled for [TAG] to see results.
     */
    fun dumpInstrumentation() {
        fun getDurationNanos(method: Method): Long? {
            return methodStart[method]?.let { methodEnd[method]?.minus(it) }
        }

        fun getMillisDump(method: Method): String {
            val durationNanos = getDurationNanos(method)
            return if (durationNanos == null) {
                "$method: UNKNOWN - INVALID INSTRUMENTATION"
            } else {
                val timeMillis = TimeUnit.NANOSECONDS.toMillis(durationNanos)
                "$method: ${timeMillis}ms"
            }
        }

        // To minimize possible overhead of creating many strings, throw on a background thread.
        GlobalScope.launch(Dispatchers.Default) {
            val methods = enumValues<Method>()

            // This is fragile because it's hard-coding the first and last methods but, since this
            // class is intended to be used for specific tests, it's fine.
            //
            // Here and below: methodStart/End values may be null in startup scenarios that aren't
            // cold startup to homescreen so we don't assert them to be non-null.
            val totalElapsedMillis = methodStart[Method.APP_ON_CREATE]?.let { first ->
                methodEnd[Method.HOME_FRAGMENT_ON_RESUME]?.minus(first)
            }?.let { TimeUnit.NANOSECONDS.toMillis(it) }

            val totalRecordedMillis = methods.fold(0L) { acc, method ->
                val durationMillis = getDurationNanos(method)?.let { TimeUnit.NANOSECONDS.toMillis(it) }
                acc + (durationMillis ?: 0L)
            }

            val methodLines = enumValues<Method>().joinToString(
                separator = "\n",
                prefix = "",
                transform = ::getMillisDump
            )

            val finalLines = """Cold startup dump:
                |Total Elapsed: ${totalElapsedMillis}ms
                |Total Recorded: ${totalRecordedMillis}ms
            """.trimMargin() + "\n" + methodLines

            Log.v(TAG, finalLines)
        }
    }
}
