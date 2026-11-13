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
import com.danielgmyers.metrics.MetricRecorderFactory;
import com.danielgmyers.metrics.recorders.config.EmfLogRecorderConfig;

public class EmfLogRecorderFactory implements MetricRecorderFactory {

    private EmfLogRecorderConfig config;

    /**
     * Creates a recorder factory for EmfLogRecorders. The inputs to the factory are provided to
     * the individual MetricRecorder objects and applied at that level.
     */
    public EmfLogRecorderFactory(EmfLogRecorderConfig config) {
        this.config = config;
    }

    /**
     * Creates a fresh EmfLogRecorder.
     *
     * @param operation Specifies the operation about which metrics will be recorded using the produced MetricRecorder.
     *                  For example, this could be the name of the API being called, or the name of a discrete
     *                  sub-operation being performed.
     * @param clock When the current time is needed, it is taken from the provided clock.
     */
    @Override
    public MetricRecorder newMetricRecorder(String operation, Clock clock) {
        return new EmfLogRecorder(config, operation, clock);
    }
}
