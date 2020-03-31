/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.perf

import androidx.annotation.UiThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import mozilla.components.service.glean.private.NoReasonCodes
import mozilla.components.service.glean.private.PingType
import org.mozilla.fenix.GleanMetrics.Pings
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.home.sessioncontrol.viewholders.topsites.TopSiteItemViewHolder
import org.mozilla.fenix.perf.StartupTimelineStateMachine.StartupActivity
import org.mozilla.fenix.perf.StartupTimelineStateMachine.StartupDestination
import org.mozilla.fenix.perf.StartupTimelineStateMachine.StartupState

/**
 * A collection of functionality to instrument, measure, and understand startup performance. The
 * responsibilities of this class are to update the internal [StartupState] based on the methods
 * called and to delegate calls to its dependencies, which handle other functionality related to
 * understanding startup.
 *
 * This class, and its dependencies, may need to be modified for any changes in startup.
 *
 * This class is not thread safe and should only be called from the main thread.
 */
@UiThread
object StartupTimeline {

    private var state: StartupState = StartupState.Cold(StartupDestination.UNKNOWN)

    val homeActivityLifecycleObserver = StartupHomeActivityLifecycleObserver()
    private val reportFullyDrawn = StartupReportFullyDrawn()

    fun onActivityCreateEndIntentReceiver() {
        advanceState(StartupActivity.INTENT_RECEIVER)
    }

    fun onActivityCreateEndHome(activity: HomeActivity) {
        advanceState(StartupActivity.HOME)
        reportFullyDrawn.onActivityCreateEndHome(state, activity)
    }

    fun onTopSitesItemBound(holder: TopSiteItemViewHolder) {
        // no advanceState associated with this method.
        reportFullyDrawn.onTopSitesItemBound(state, holder)
    }

    private fun advanceState(startingActivity: StartupActivity) {
        state = StartupTimelineStateMachine.getNextState(state, startingActivity)
    }
}

/**
 * A [LifecycleObserver] for [HomeActivity] focused on startup performance measurement.
 */
class StartupHomeActivityLifecycleObserver(
    private val startupTimeline: PingType<NoReasonCodes> = Pings.startupTimeline
) : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        // Startup metrics placed in the Activity should be re-recorded each time the Activity
        // is started so we need to clear the ping lifetime by submitting once per each startup.
        // It's less complex to add it here rather than the visual completeness task manager.
        //
        // N.B.: this submission location may need to be changed if we add metrics outside of the
        // HomeActivity startup path (e.g. if the user goes directly to a separate activity and
        // closes the app, they will never hit this) to appropriately adjust for the ping lifetimes.
        startupTimeline.submit()
    }
}
