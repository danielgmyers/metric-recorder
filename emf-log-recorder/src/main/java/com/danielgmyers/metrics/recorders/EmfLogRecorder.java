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
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.danielgmyers.metrics.MetricRecorder;
import com.danielgmyers.metrics.MetricUnit;
import com.danielgmyers.metrics.recorders.config.DimensionPropertyFilterInput;
import com.danielgmyers.metrics.recorders.config.EmfLogRecorderConfig;
import com.danielgmyers.metrics.recorders.config.HighResolutionMetricFilterInput;

import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.StorageResolution;
import software.amazon.cloudwatchlogs.emf.model.Unit;

/**
 * A MetricRecorder implementation that logs metrics in Embedded Metric Format,
 * using the `aws-embedded-metrics` library.
 */
public class EmfLogRecorder extends MetricRecorder {

    private final MetricsLogger emfLogger;
    private final EmfLogRecorderConfig config;
    private final Map<String, String> dimensionValues;

    // package-private since only the factory should create these
    EmfLogRecorder(EmfLogRecorderConfig config, String operation, Clock clock) {
        this(new MetricsLogger(config.getEmfLoggerEnvironment()), config, operation, clock);
    }

    // package-private for test use
    EmfLogRecorder(MetricsLogger emfLogger, EmfLogRecorderConfig config, String operation, Clock clock) {
        super(operation, clock);
        this.emfLogger = emfLogger;
        this.config = config;
        this.dimensionValues = new HashMap<>();
    }

    @Override
    protected void addPropertyHook(String name, String value) {
        if (propertyFilter(name)) {
            dimensionValues.put(name, value);
        } else {
            emfLogger.putProperty(name, value);
        }
    }

    @Override
    protected void addTimestampHook(String name, Instant time) {
        if (propertyFilter(name)) {
            dimensionValues.put(name, time.toString());
        } else {
            emfLogger.putProperty(name, time.toString());
        }
    }

    @Override
    protected void addCountHook(String name, double value, MetricUnit unit) {
        CwUnitQuantity cwValue = CwUnitQuantity.calculate(value, unit, config);
        emfLogger.putMetric(name, cwValue.getValue(), cwValue.getCwUnit(), getStorageResolution(name));
    }

    @Override
    protected void addDurationHook(String name, Duration duration) {
        emfLogger.putMetric(name, duration.toMillis(), Unit.MILLISECONDS, getStorageResolution(name));
    }

    @Override
    protected void closeHook() {
        DimensionSet dimensions = new DimensionSet();
        dimensionValues.forEach(dimensions::addDimension);
        emfLogger.setDimensions(config.includeDefaultAwsEmbeddedMetricsDimensions(), dimensions);
        emfLogger.setTimestamp(getClock().instant());
        emfLogger.setNamespace(config.getMetricNamespace());
        emfLogger.flush();
    }

    private boolean propertyFilter(String propertyName) {
        return config.getDimensionPropertyFilter().apply(
                new DimensionPropertyFilterInput(config.getMetricNamespace(), getOperation(), propertyName));
    }

    private StorageResolution getStorageResolution(String metricName) {
        if (config.getHighResolutionMetricFilter().apply(
                new HighResolutionMetricFilterInput(config.getMetricNamespace(), getOperation(), metricName))) {
            return StorageResolution.HIGH;
        }
        return StorageResolution.STANDARD;
    }
}
