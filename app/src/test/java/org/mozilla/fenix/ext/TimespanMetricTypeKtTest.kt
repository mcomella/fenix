/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import mozilla.components.service.glean.private.TimespanMetricType
import org.junit.Before
import org.junit.Test

class TimespanMetricTypeKtTest {

    // We intentionally use a mock so this test doesn't break if we use a probe that is
    // removed: the constructor is also intended to be private.
    @MockK(relaxed = true) private lateinit var typeMock: TimespanMetricType

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `WHEN setRawMillis is called THEN setRawNanos is called with the value converted to nanoseconds`() {
        typeMock.setRawMillis(1)
        verify { typeMock.setRawNanos(1_000_000) }
    }
}
