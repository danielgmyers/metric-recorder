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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.danielgmyers.metrics.MetricRecorder;

/**
 * For use in validating metrics emitted by methods under test.
 */
public class InMemoryMetricRecorder extends MetricRecorder {

    private final Map<String, String> properties;
    private final Map<String, Instant> dates;
    private final Map<String, Double> counts;
    private final Map<String, Duration> durations;

    public InMemoryMetricRecorder(String operation) {
        this(operation, Clock.systemUTC());
    }

    public InMemoryMetricRecorder(String operation, Clock clock) {
        super(operation, clock);
        this.properties = new HashMap<>();
        this.dates = new HashMap<>();
        this.counts = new HashMap<>();
        this.durations = new HashMap<>();
    }

    @Override
    protected void addPropertyHook(String name, String value) {
        properties.put(name, value);
    }

    @Override
    protected void addTimestampHook(String name, Instant time) {
        dates.put(name, time);
    }

    @Override
    protected void addCountHook(String name, double value) {
        if (!counts.containsKey(name)) {
            counts.put(name, value);
        } else {
            counts.put(name, counts.get(name) + value);
        }
    }

    @Override
    protected void addDurationHook(String name, Duration duration) {
        if (!durations.containsKey(name)) {
            durations.put(name, duration);
        } else {
            durations.put(name, durations.get(name).plus(duration));
        }
    }

    public Map<String, Double> getCounts() {
        verifyClosed();
        return Collections.unmodifiableMap(counts);
    }

    public Double getCount(String metricName) {
        verifyClosed();
        return counts.get(metricName);
    }

    public Map<String, Duration> getDurations() {
        verifyClosed();
        return Collections.unmodifiableMap(durations);
    }

    public Duration getDuration(String metricName) {
        verifyClosed();
        return durations.get(metricName);
    }

    public Map<String, Instant> getTimestamps() {
        verifyClosed();
        return Collections.unmodifiableMap(dates);
    }

    public Instant getTimestamp(String metricName) {
        verifyClosed();
        return dates.get(metricName);
    }

    public Map<String, String> getProperties() {
        verifyClosed();
        return Collections.unmodifiableMap(properties);
    }

    public String getProperty(String metricName) {
        verifyClosed();
        return properties.get(metricName);
    }

    private void verifyClosed() {
        if (!isClosed()) {
            throw new IllegalStateException("Metrics should only be retrieved after the recorder is closed.");
        }
    }
}
