/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.perf

import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import mozilla.components.service.glean.private.EventMetricType
import mozilla.components.service.glean.private.NoExtraKeys
import mozilla.components.service.glean.private.TimespanMetricType
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import org.mozilla.fenix.GleanMetrics.StartupTimeline as Telemetry

class StartupTimelineAdditionalMetricsTest {

    private lateinit var metrics: StartupTimelineAdditionalMetrics
    @MockK private lateinit var stat: Stat

    // We'd prefer to use the Glean test methods over these mocks but they require us to add
    // Robolectric and it's not worth the impact on test duration.
    @MockK private lateinit var telemetry: Telemetry
    @MockK(relaxed = true) private lateinit var frameworkStart: TimespanMetricType
    @MockK(relaxed = true) private lateinit var frameworkStartError: EventMetricType<NoExtraKeys>

    private var elapsedRealtimeMillis = -1L
    private var processStartTimeMillis = -1L

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        elapsedRealtimeMillis = -1
        processStartTimeMillis = -1

        val getElapsedRealtimeMillis = { elapsedRealtimeMillis }
        every { stat.getProcessStartTimeMillis(any()) } answers { processStartTimeMillis }

        every { telemetry.frameworkStart } returns frameworkStart
        every { telemetry.frameworkStartError } returns frameworkStartError

        metrics = StartupTimelineAdditionalMetrics(stat, telemetry, getElapsedRealtimeMillis)
    }

    @Test
    fun `GIVEN process start is invalid WHEN setMetrics is called THEN frameworkStartError is recorded`() {
        setProcessAppInitAndMetrics(processStart = -1, appInit = 100)
        verifyFrameworkStartError()
    }

    @Test
    fun `GIVEN app init is invalid WHEN setMetrics is called THEN frameworkStartError is recorded`() {
        setProcessAppInitAndMetrics(processStart = 10, appInit = -1)
        verifyFrameworkStartError()
    }

    @Test
    fun `GIVEN process start is valid and app init is not called WHEN setMetrics is called THEN frameworkStartError is recorded`() {
        processStartTimeMillis = 100
        elapsedRealtimeMillis = 100 // unused but we set it just to be safe.
        metrics.setExpensiveMetrics()
        verifyFrameworkStartError()
    }

    @Test
    fun `GIVEN process start and app init are set to valid values WHEN setMetrics is called THEN frameworkStart is set with the correct value`() {
        setProcessAppInitAndMetrics(processStart = 10, appInit = 100)
        verifyFrameworkStartSuccess(90)
    }

    @Test // this overlaps with the success case test.
    fun `GIVEN process and app init have valid values WHEN onAppInit is called twice and setMetrics THEN frameworkStart uses the first appInit value`() {
        processStartTimeMillis = 10

        elapsedRealtimeMillis = 100
        metrics.onApplicationInit()
        elapsedRealtimeMillis = 200
        metrics.onApplicationInit()

        metrics.setExpensiveMetrics()
        verifyFrameworkStartSuccess(90)
    }

    @Test
    fun `GIVEN process and app init have valid values WHEN setMetrics is called twice THEN frameworkStart is only set once`() {
        setProcessAppInitAndMetrics(10, 100)
        metrics.setExpensiveMetrics()
        verify(exactly = 1) { frameworkStart.setRawNanos(any()) }
        verify { frameworkStartError wasNot Called }
    }

    private fun setProcessAppInitAndMetrics(processStart: Long, appInit: Long) {
        processStartTimeMillis = processStart

        elapsedRealtimeMillis = appInit
        metrics.onApplicationInit()

        metrics.setExpensiveMetrics()
    }

    private fun verifyFrameworkStartSuccess(millis: Long) {
        verify { frameworkStart.setRawNanos(TimeUnit.MILLISECONDS.toNanos(millis)) }
        verify { frameworkStartError wasNot Called }
    }

    private fun verifyFrameworkStartError() {
        verify { frameworkStartError.record() }
        verify { frameworkStart wasNot Called }
    }
}
