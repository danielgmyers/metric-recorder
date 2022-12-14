/*
 *   Copyright Metric Recorder Contributors
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.danielgmyers.metrics.recorders;

import java.time.Clock;

import com.danielgmyers.metrics.MetricRecorder;

/**
 * The default MetricRecorder hooks do nothing, so the base class itself is already a noop recorder.
 * We can't use a singleton instance because the disallow-writes-after-close behavior needs to be enforced.
 */
public class NoopMetricRecorder extends MetricRecorder {
    public NoopMetricRecorder(String operation, Clock clock) {
        super(operation, clock);
    }
}
