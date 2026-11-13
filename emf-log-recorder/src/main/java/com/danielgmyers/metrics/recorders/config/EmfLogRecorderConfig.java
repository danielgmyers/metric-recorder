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

import java.util.function.Function;

import software.amazon.cloudwatchlogs.emf.environment.Environment;
import software.amazon.cloudwatchlogs.emf.environment.EnvironmentProvider;

/**
 * Specifies configuration for the EmfLogRecorder and underlying MetricsLogger runtime.
 */
public class EmfLogRecorderConfig {

    private String metricNamespace;
    private Environment emfLoggerEnvironment;
    private Function<DimensionPropertyFilterInput, Boolean> dimensionPropertyFilter;
    private Function<HighResolutionMetricFilterInput, Boolean> highResolutionMetricFilter;
    private boolean isCloudWatchDataUnitBase10;
    private boolean includeDefaultAwsEmbeddedMetricsDimensions;

    /**
     * Metrics Namespace is a required input for the configuration. This controls which CloudWatch
     * namespace the metrics emitted by this recorder are stored in.
     *
     * Note that this does mean if you want to emit metrics in multiple namespaces, you will need
     * multiple config objects, one per namespace, and one EmfLogRecorderFactory per configuration.
     */
    public EmfLogRecorderConfig(String metricNamespace) {
        this(metricNamespace, new EnvironmentProvider().getDefaultEnvironment());
    }

    /**
     * The emfLoggerEnvironment parameter controls how the underlying MetricsLogger library behaves
     * when writing out the actual log files. In particular, it controls what data is emitted as
     * additional properties and/or dimensions.
     *
     * By default, EmfLogRecorder will use `EnvironmentProvidier.getDefaultEnvironment()` from
     * the `aws-embedded-metrics` library, which is usually what you'll want when running in
     * an AWS environment like Lambda, ECS, or EC2.
     */
    public EmfLogRecorderConfig(String metricNamespace, Environment emfLoggerEnvironment) {
        this.metricNamespace = metricNamespace;
        this.emfLoggerEnvironment = emfLoggerEnvironment;
        this.dimensionPropertyFilter = (x) -> false;
        this.highResolutionMetricFilter = (x) -> false;
        this.isCloudWatchDataUnitBase10 = true;
        this.includeDefaultAwsEmbeddedMetricsDimensions = false;
    }

    public String getMetricNamespace() {
        return metricNamespace;
    }

    public Environment getEmfLoggerEnvironment() {
        return emfLoggerEnvironment;
    }

    public Function<DimensionPropertyFilterInput, Boolean> getDimensionPropertyFilter() {
        return dimensionPropertyFilter;
    }

    /**
     * The common MetricRecorder interface does not define "Dimension" as a first-class concept.
     * Consequently, this filter allows you to control whether values added as properties
     * (through either `addTimestamp` or `addProperty`) are treated instead as CloudWatch Dimensions.
     *
     * Either way, the value will be emitted in the top-level list of properties in the EMF record,
     * but if this filter returns `true`, then the property name will also be included in the list of
     * Dimensions for all data points in the EMF record.
     *
     * See the documentation for `DimensionPropertyFilterInput` for details about what data
     * is provided to the filter to use when making the decision.
     *
     * The default behavior evaluates to `false` for all queries.
     */
    public void setDimensionPropertyFilter(Function<DimensionPropertyFilterInput, Boolean> dimensionPropertyFilter) {
        this.dimensionPropertyFilter = dimensionPropertyFilter;
    }

    public Function<HighResolutionMetricFilterInput, Boolean> getHighResolutionMetricFilter() {
        return highResolutionMetricFilter;
    }

    /**
     * The common MetricRecorder interface does not define metric resolution as a first-class concept,
     * it just emits raw timestamps and defers to the backend to decide how to resolve the timestamps
     * reported to the destination service. In the case of CloudWatch, by default metric data is recorded
     * with a timestamp at 1-minute resolution, so any metrics emitted during that minute on the application
     * side end up recorded in CloudWatch under the same timestamp.
     *
     * However, CloudWatch does support higher resolution metrics. This filter allows you to opt in on a
     * per-metric basis (for metrics added via `addCount` or `addDuration`) to enable the higher-resolution
     * setting on that data point.
     *
     * See the documentation for `HighResolutionMetricFilterInput` for details about what data
     * is provided to the filter to use when making the decision.
     *
     * The default behavior evaluates to `false` for all queries.
     */
    public void setHighResolutionMetricFilter(Function<HighResolutionMetricFilterInput, Boolean> highResolutionMetricFilter) {
        this.highResolutionMetricFilter = highResolutionMetricFilter;
    }

    public boolean isCloudWatchDataUnitBase10() {
        return isCloudWatchDataUnitBase10;
    }

    /**
     * CloudWatch itself does not actually support units, it just treats them as arbitrary labels.
     * Specifically, if you submit metrics as "Megabytes/second", you must also retrieve the data points as
     * "Megabytes/second"; it will not automatically convert them to "Kilobytes/second" on demand.
     * (Instead you get no metric data back at all.)
     *
     * Users can specify units as a way of providing additional context without needing to embed
     * the unit in the metric name itself.
     *
     * We want EmfLogRecorder to preserve the user's intent when we actually publish to CloudWatch.
     * In support of that, we need to do two things:
     * 1) Map our MetricUnits directly to the corresponding CloudWatch units, where possible.
     * 2) Map our MetricUnits to a _semantically similar_ CloudWatch unit, where there is no direct match.
     *
     * For example, CloudWatch doesn't support an "Exabytes/second" unit, so if we had an equivalent
     * in our MetricUnit list, we would want to convert the raw data and the unit reported to CloudWatch
     * such that it behaves as if the customer had reported the original data with MetricUnit.TBps.
     *
     * For historical reasons, there is a complication we should account for:
     *
     * CloudWatch's supported unit list includes e.g. "Megabytes/second" but not "Mebibytes/second".
     * Many people/systems/contexts still casually use MBps to refer to the base 2 measurement,
     * and we don't want to inadvertently cause confusion for users by publishing the data with the value
     * converted to an unexpected unit.
     *
     * For example, if they report MetricUnit.MiBps, we have a few options:
     * a) we can report the value as-is, but pick the "Megabytes/second" unit in CloudWatch. This works
     *    but produces confusing results if the user _also_ reports data in MBps.
     * b) we can assume CloudWatch's unit is base 10, and transparently convert base 2 values to base 10
     *    when recording the data.
     * c) we can assume CloudWatch's unit is base 2, and report the user's base 2 value as-is.
     *
     * Conversely, we have the same dilemma in reverse if the user reports MetricUnit.MBps, and if
     * they report both, then a hardcoded assumption either way would be wrong.
     *
     * To address this ambiguity, EmfLogReporterConfig includes a flag for toggling whether CloudWatch's
     * data transfer and data storage units are treated as base 2 or base 10.
     *
     * Whenever the user reports a data transfer or data storage metric using the MetricUnit that is
     * *not* configured to map to CloudWatch's data transfer unit, that value will be converted to the
     * other prior to publishing (so, by default, 42 MiBps will be reported as 44.0402 "Megabytes/second").
     *
     * Note that this behavior applies to both the data storage (MiB) and data transfer (MiBps) units.
     *
     * By default, we treat MetricUnit.MBps the same as CloudWatch's "Megabytes/second" unit, but the
     * toggle allows the user to tell us to treat MetricUnit.MiBps as "Megabytes/second" instead.
     */
    public void setIsCloudWatchDataUnitBase10(boolean isCloudWatchDataUnitBase10) {
        this.isCloudWatchDataUnitBase10 = isCloudWatchDataUnitBase10;
    }

    public boolean includeDefaultAwsEmbeddedMetricsDimensions() {
        return includeDefaultAwsEmbeddedMetricsDimensions;
    }

    /**
     * Determines whether the EMF log files are written with the default Dimensions included by the
     * `aws-embedded-metrics` library used under the covers to write the log files.
     * Generally this includes the `LogGroup`, `ServiceName`, and `ServiceType` properties,
     * but may include others depending on the environment.
     *
     * The default is `false`.
     */
    public void setIncludeDefaultAwsEmbeddedMetricsDimensions(boolean includeDefaultAwsEmbeddedMetricsDimensions) {
        this.includeDefaultAwsEmbeddedMetricsDimensions = includeDefaultAwsEmbeddedMetricsDimensions;
    }
}
