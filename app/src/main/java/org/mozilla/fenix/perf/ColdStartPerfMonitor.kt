/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.perf

import androidx.core.view.doOnPreDraw
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.home.sessioncontrol.viewholders.topsites.TopSiteItemViewHolder

/**
 * Functionality related to measuring the performance of cold startup. Note: this is cold startup in
 * the context of FNPRMS: we have yet to define cold startup in the context of the production app.
 */
object ColdStartPerfMonitor {

    private var isVisuallyComplete = false

    /**
     * Instruments "visually complete" cold startup time to homescreen for use with our internal
     * measuring system, FNPRMS. This value may also appear in the Google Play Vitals dashboards.
     *
     * For FNPRMS, we define "visually complete" to be when top sites is loaded with placeholders;
     * the animation to display top sites will occur after this point, as will the asynchronous
     * loading of the actual top sites icons. Our focus for visually complete is usability.
     * There are no tabs available in our FNPRMS tests so they are ignored for this instrumentation.
     *
     * This will need to be rewritten if any parts of the UI are changed to be displayed asynchronously.
     */
    fun onTopSitesItemBound(holder: TopSiteItemViewHolder) {
        if (isVisuallyComplete) { return }
        isVisuallyComplete = true

        // For greater accuracy, we could add an onDrawListener instead of a preDrawListener but:
        // - single use onDrawListeners are not built-in and it's non-trivial to write one
        // - the difference in timing is minimal (< 7ms on Pixel 2)
        // - if we compare against another app using a preDrawListener, as we are with Fennec, it
        // should be comparable
        //
        // Ideally we wouldn't cast to HomeActivity but we want to save implementation time.
        holder.itemView.doOnPreDraw { (it.context as HomeActivity).reportFullyDrawn() }
    }
}
