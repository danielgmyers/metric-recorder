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
 * Information in this structure should be used to determine whether a given property name
 * should be used as a Dimension when generating metrics. Properties or Timestamps that meet this filter
 * are removed from the normal Property list in the EMF output, and included instead as Dimensions.
 * <p/>
 * Fields:
 *     metricNamespace: Matches the namespace provided in the call to the EmfLogRecorderFactory constructor.
 *     operationName: Matches the name provided in the call to EmfLogRecorderFactory.newMetricRecorder().
 *     propertyName: Specifies which Property or Timestamp is being evaluated.
 */
public class DimensionPropertyFilterInput {
    private final String metricNamespace;
    private final String operationName;
    private final String propertyName;

    public DimensionPropertyFilterInput(String metricNamespace, String operationName, String propertyName) {
        this.metricNamespace = metricNamespace;
        this.operationName = operationName;
        this.propertyName = propertyName;
    }

    public String getMetricNamespace() {
        return metricNamespace;
    }

    public String getOperationName() {
        return operationName;
    }

    public String getPropertyName() {
        return propertyName;
    }
}
