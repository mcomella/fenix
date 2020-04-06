/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import mozilla.components.service.glean.private.TimespanMetricType
import java.util.concurrent.TimeUnit

/**
 * Explicitly set the timespan value, in milliseconds.
 *
 * This method should generally be avoided: see [TimespanMetricType.setRawNanos] for details.
 */
fun TimespanMetricType.setRawMillis(millis: Long) {
    setRawNanos(TimeUnit.MILLISECONDS.toNanos(millis))
}
