/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.perf

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import mozilla.components.service.glean.private.NoReasonCodes
import mozilla.components.service.glean.private.PingType
import org.junit.Before
import org.junit.Test

class StartupHomeActivityLifecycleObserverTest {

    private lateinit var observer: StartupHomeActivityLifecycleObserver
    @MockK(relaxed = true) private lateinit var startupTimeline: PingType<NoReasonCodes>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        observer = StartupHomeActivityLifecycleObserver(startupTimeline)
    }

    @Test
    fun `WHEN onStop is called THEN the ping is submitted`() {
        observer.onStop()
        verify { startupTimeline.submit() }
    }
}
