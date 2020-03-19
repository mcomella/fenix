/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.perf

import android.os.Process
import android.os.SystemClock
import org.mozilla.fenix.ext.setRawMillis
import org.mozilla.fenix.GleanMetrics.StartupTimeline as Telemetry

/**
 * Our main startup metrics are measured directly with Glean in [StartupTimeline]: this class
 * captures anything that doesn't fit within the Glean programming model.
 */
internal class StartupTimelineAdditionalMetrics(
    stat: Stat = Stat(),
    private val telemetry: Telemetry = Telemetry,
    private val getElapsedRealtimeMillis: () -> Long = SystemClock::elapsedRealtime
) {

    // Timestamps should be sorted from earliest to latest.
    //
    // This value may be slow to fetch initially: see method kdoc for details.
    private val processStartTimeMillis by lazy { stat.getProcessStartTimeMillis(Process.myPid()) }

    private var applicationInitTimeMillis = -1L
    private var isApplicationInitCalled = false
    private var isApplicationInitMetricSet = false

    fun onApplicationInit() {
        // This gets called from multiple processes: don't do anything expensive. See call site for details.
        //
        // In the main process, there are multiple Application impl so we ensure it's only set by
        // the first one.
        if (!isApplicationInitCalled) {
            isApplicationInitCalled = true
            applicationInitTimeMillis = getElapsedRealtimeMillis()
        }
    }

    /**
     * Sets the values for metrics to record in glean.
     *
     * We defer these metrics, rather than setting them as soon as the values are available,
     * because they are slow to fetch and we don't want to impact startup.
     */
    fun setExpensiveMetrics() { with(telemetry) {
        fun setFrameworkStartOrError() {
            // We cannot use glean start/stop for this probe because we cannot call start
            // when the process is starting.
            //
            // The application is only init once per process lifetime so we only set this value once.
            if (isApplicationInitMetricSet) return
            isApplicationInitMetricSet = true

            if (applicationInitTimeMillis < 0 || processStartTimeMillis < 0) {
                frameworkStartError.record()
            } else {
                frameworkStart.setRawMillis(applicationInitTimeMillis - processStartTimeMillis)
            }
        }

        setFrameworkStartOrError()
    } }
}
