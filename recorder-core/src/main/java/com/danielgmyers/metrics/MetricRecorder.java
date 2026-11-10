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

package com.danielgmyers.metrics;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A generic interface for emitting metrics to an arbitrary recording mechanism.
 *
 * This class implements AutoCloseable to encourage use with try-with-resources blocks.
 */
public abstract class MetricRecorder implements AutoCloseable {

    private boolean closed;
    private final Clock clock;
    private final Instant startTime;
    private final String operation;

    private final Map<String, Instant> timers = new HashMap<>();
    private final Map<String, MetricUnit> countUnits = new HashMap<>();

    /**
     * Initializes the MetricRecorder.
     */
    protected MetricRecorder(String operation, Clock clock) {
        this.clock = clock;
        this.startTime = clock.instant();
        this.operation = operation;
    }

    /**
     * Closes this set of metrics. After close() is called, new metrics may no longer be recorded with this object.
     * Implementations should store, publish, or otherwise record the metrics at this time.
     * This method closes any open durations, and emits the standard metrics listed in StandardMetricNames.
     * Throws an IllegalStateException if the MetricRecorder is already closed.
     */
    public final void close() {
        verifyNotClosed();

        Set<String> openTimers = new HashSet<>(timers.keySet());
        for (String openTimer : openTimers) {
            endDuration(openTimer);
        }

        addProperty(StandardMetricNames.OPERATION.toString(), operation);
        addProperty(StandardMetricNames.THREAD_NAME.toString(), Thread.currentThread().getName());
        addTimestamp(StandardMetricNames.START_TIME.toString(), startTime);

        Instant endTime = clock.instant();
        Duration time = Duration.between(startTime, endTime);
        addTimestamp(StandardMetricNames.END_TIME.toString(), endTime);
        addDuration(StandardMetricNames.TIME.toString(), time);

        closeHook();

        closed = true;
    }

    /**
     * Indicates whether the MetricRecorder is closed. If closed, it will reject recording more metrics.
     */
    public final boolean isClosed() {
        return closed;
    }

    /**
     * Records an arbitrary String value alongside the other metrics. This is useful to label this group of metrics.
     * If multiple properties are recorded with the same name, implementations *may* choose to throw an exception,
     * but *must* only honor one of them.
     * Throws an IllegalStateException if the MetricRecorder is already closed.
     */
    public final void addProperty(String name, String value) {
        verifyNotClosed();
        addPropertyHook(name, value);
    }

    /**
     * Records a timestamp.
     * If multiple timestamps are recorded with the same name, implementations *may* choose to throw an exception,
     * but *must* only honor one of them.
     * Throws an IllegalStateException if the MetricRecorder is already closed.
     */
    public final void addTimestamp(String name, Instant time) {
        verifyNotClosed();
        addTimestampHook(name, time);
    }

    /**
     * Records a count metric using a dimensionless unit.
     * If multiple counts are recorded with the same name, implementations should aggregate them.
     * <p/>
     * Records MetricUnit.None for each metric name. If some counts are reported with a different unit,
     * this method will throw InvalidMetricConversionException.
     * <p/>
     * Throws an IllegalStateException if the MetricRecorder is already closed.
     * <p/>
     * In a future version, this method will always delegate to the other addCount with MetricUnit.None.
     */
    public final void addCount(String name, double count) {
        verifyNotClosed();
        MetricUnit existingUnit = countUnits.get(name);
        if (existingUnit == null) {
            countUnits.put(name, MetricUnit.None);
        } else if (existingUnit != MetricUnit.None) {
            throw new InvalidMetricConversionException(existingUnit, MetricUnit.None);
        }
        addCountHook(name, count);
    }

    /**
     * Records a count metric using a specific unit.
     * If multiple counts are recorded with the same name but different units:
     *  1) if the units are compatible, the new value is converted to the existing unit.
     *  2) if the units are incompatible, an InvalidMetricConversionException is thrown.
     * <p/>
     * If multiple counts are recorded with the same name, implementations should aggregate them. Note that the new count
     * will already be converted to the existing metric entry's unit when addCountHook is called.
     * For example, if the existing count is in MetricUnit.GiB, and the application calls addCount with 1024 MetricUnit.MiB,
     * then the addCountHook method will be called as if the application had called addCount with 1 MetricUnit.GiB.
     * <p/>
     * Throws an IllegalStateException if the MetricRecorder is already closed.
     */
    public final void addCount(String name, double count, MetricUnit unit) {
        verifyNotClosed();
        MetricUnit existingUnit = countUnits.get(name);
        if (existingUnit != null) {
            double convertedCount = unit.convertTo(count, existingUnit);
            addCountHook(name, convertedCount, existingUnit);
        } else {
            countUnits.put(name, unit);
            addCountHook(name, count, unit);
        }
    }

    /**
     * Records a specific duration metric.
     * If multiple durations are recorded with the same name, implementations should aggregate them.
     * Throws an IllegalStateException if the MetricRecorder is already closed.
     */
    public final void addDuration(String name, Duration duration) {
        verifyNotClosed();
        addDurationHook(name, duration);
    }

    /**
     * Records the current time as a timestamp with the specified name.
     * Call endDuration() with the same name or close() to record the duration.
     *
     * Returns the stored start time.
     * Throws an IllegalStateException if the MetricRecorder is already closed.
     */
    public final Instant startDuration(String name) {
        return startDuration(name, clock.instant());
    }

    /**
     * Records the specified start time as a timestamp with the specified name.
     * Call endDuration() with the same name or close() to record the duration.
     *
     * Returns the stored start time.
     * Throws an IllegalStateException if the MetricRecorder is already closed.
     */
    public final Instant startDuration(String name, Instant startTime) {
        verifyNotClosed();
        if (timers.containsKey(name)) {
            throw new IllegalStateException("A timer named " + name + " is already open.");
        }
        timers.put(name, startTime);
        return startTime;
    }

    /**
     * When startDuration() was called with the same name, this method calls
     * addDuration() using the duration between the recorded start time and the current time.
     * Returns the measured duration.
     * Throws an IllegalStateException if the MetricRecorder is already closed.
     */
    public final Duration endDuration(String name) {
        return endDuration(name, clock.instant());
    }

    /**
     * When startDuration() was called with the same name, this method calls
     * addDuration() using the duration between the recorded start time and the specified end time.
     * Returns the measured duration.
     * Throws an IllegalStateException if the MetricRecorder is already closed.
     */
    public final Duration endDuration(String name, Instant endTime) {
        verifyNotClosed();
        if (!timers.containsKey(name)) {
            throw new IllegalStateException("No active timer named " + name);
        }
        Instant startTime = timers.remove(name);
        Duration duration = Duration.between(startTime, endTime);
        addDuration(name, duration);
        return duration;
    }

    /**
     * Hook method for implementation-specific behavior.
     *
     * Default is to do nothing.
     */
    protected void closeHook() {}

    /**
     * Hook method for implementation-specific behavior.
     *
     * Default is to do nothing.
     */
    protected void addPropertyHook(String name, String value) {}

    /**
     * Hook method for implementation-specific behavior.
     *
     * Default is to do nothing.
     */
    protected void addTimestampHook(String name, Instant time) {}

    /**
     * Hook method for implementation-specific behavior.
     *
     * Default is to delegate to the other addCountHook with MetricUnit.None.
     *
     * Implementations should switch to implementing the newer addCountHook with the MetricUnit parameter instead.
     */
    @Deprecated
    protected void addCountHook(String name, double count) {
        this.addCountHook(name, count, com.danielgmyers.metrics.MetricUnit.None);
    }

    /**
     * Hook method for implementation-specific behavior.
     *
     * Note that implementations can call getCountUnits() to look up the units; they do not need to store their own copy.
     *
     * Default is to do nothing.
     */
    protected void addCountHook(String name, double count, MetricUnit unit) {}

    /**
     * Hook method for implementation-specific behavior.
     *
     * Default is to do nothing.
     */
    protected void addDurationHook(String name, Duration duration) {}

    /**
     * Allows child classes to retrieve the operation name if needed.
     */
    protected final String getOperation() {
        return operation;
    }

    /**
     * Allows child classes to retrieve the clock if needed.
     */
    protected final Clock getClock() {
        return clock;
    }

    /**
     * Allows child classes to retrieve the units tied to each count.
     */
    protected Map<String, MetricUnit> getCountUnits() {
        return Collections.unmodifiableMap(countUnits);
    }

    private void verifyNotClosed() {
        if (closed) {
            throw new IllegalStateException("MetricRecorder is already closed.");
        }
    }
}
