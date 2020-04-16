/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.perf

import android.view.MotionEvent
import android.view.View
import androidx.core.view.doOnPreDraw
import mozilla.telemetry.glean.GleanTimerId
import mozilla.telemetry.glean.private.TimingDistributionMetricType
import org.mozilla.fenix.GleanMetrics.Performance.enterFullscreen
import org.mozilla.fenix.GleanMetrics.Performance.exitFullscreen
import kotlin.reflect.KMutableProperty0

/**
 * Measures the time it takes to enter and exit fullscreen mode: see the metric description for
 * further details.
 */
object FullscreenTransitionMeasurement {

    private var enterFullscreenTimerId: GleanTimerId? = null
    private var exitFullscreenTimerId: GleanTimerId? = null

    fun onHomeActivityDispatchTouchEvent(event: MotionEvent) {
        // To minimize churn, we only handle UP events because those are the only ones that cause
        // fullscreen state to change.
        if (event.action == MotionEvent.ACTION_UP) {
            return
        }

        // todo: explain.
        enterFullscreen.cancelAndStart(::enterFullscreenTimerId)
        exitFullscreen.cancelAndStart(::exitFullscreenTimerId)
    }

    fun onBackPressed() {
        // todo: explain.
        exitFullscreen.cancelAndStart(::exitFullscreenTimerId)
    }

    private fun TimingDistributionMetricType.cancelAndStart(timerIdRef: KMutableProperty0<GleanTimerId?>) {
        timerIdRef.get()?.let { cancel(it) }
        timerIdRef.set(start())
    }

    fun onFullscreenChanged(isFullscreen: Boolean, view: View?) {
        fun TimingDistributionMetricType.stopAccumulateAndNull(timerIdRef: KMutableProperty0<GleanTimerId?>) {
            stopAndAccumulate(timerIdRef.get())
            timerIdRef.set(null)
        }

        // todo: called async so telemetry may have errors if many key presses.
        view?.doOnPreDraw {
            if (isFullscreen) {
                enterFullscreen.stopAccumulateAndNull(::enterFullscreenTimerId)
            } else {
                exitFullscreen.stopAccumulateAndNull(::exitFullscreenTimerId)
            }
        }
    }
}
