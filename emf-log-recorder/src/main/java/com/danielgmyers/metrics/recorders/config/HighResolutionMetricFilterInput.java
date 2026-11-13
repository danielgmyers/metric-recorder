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

package com.danielgmyers.metrics.recorders.config;

/**
 * Information in this structure should be used to determine whether a given metric name
 * should be tracked as a "high resolution" metric. Counts and Durations that meet this filter are written
 * to the EMF log with 1-second resolution, instead of 60-second resolution.
 *
 * Fields:
 *     metricNamespace: Matches the namespace provided in the call to the EmfLogRecorderFactory constructor.
 *     operationName: Matches the name provided in the call to EmfLogRecorderFactory.newMetricRecorder().
 *     metricName: Specifies which Count or Duration is being evaluated.
 */
public class HighResolutionMetricFilterInput {
    private final String metricNamespace;
    private final String operationName;
    private final String metricName;

    public HighResolutionMetricFilterInput(String metricNamespace, String operationName, String metricName) {
        this.metricNamespace = metricNamespace;
        this.operationName = operationName;
        this.metricName = metricName;
    }

    public String getMetricNamespace() {
        return metricNamespace;
    }

    public String getOperationName() {
        return operationName;
    }

    public String getMetricName() {
        return metricName;
    }
}
