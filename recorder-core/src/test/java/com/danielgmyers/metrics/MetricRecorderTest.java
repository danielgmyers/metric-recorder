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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MetricRecorderTest {

    @Test
    public void testDefaultMetrics() {
        ManualClock clock = new ManualClock();

        Instant startTime = clock.instant();
        String operation = "test";

        StubMetricRecorder recorder = new StubMetricRecorder(operation, clock);

        Assertions.assertFalse(recorder.isClosed());

        Duration operationDuration = Duration.ofMillis(200);
        Instant endTime = clock.forward(operationDuration);
        recorder.close();

        Assertions.assertTrue(recorder.isClosed());
        Assertions.assertEquals(startTime, recorder.getTimestamps().get(StandardMetricNames.START_TIME.toString()));
        Assertions.assertEquals(endTime, recorder.getTimestamps().get(StandardMetricNames.END_TIME.toString()));
        Assertions.assertEquals(operationDuration, recorder.getDurations().get(StandardMetricNames.TIME.toString()));
        Assertions.assertEquals(operation, recorder.getProperties().get(StandardMetricNames.OPERATION.toString()));
        Assertions.assertEquals(Thread.currentThread().getName(),
                                recorder.getProperties().get(StandardMetricNames.THREAD_NAME.toString()));
    }

    @Test
    public void testCloseCallsCloseHook() {
        ManualClock clock = new ManualClock();
        StubMetricRecorder recorder = new StubMetricRecorder("test", clock);
        Assertions.assertFalse(recorder.isClosed());
        Assertions.assertFalse(recorder.isCloseHookCalled());

        recorder.close();

        Assertions.assertTrue(recorder.isCloseHookCalled());
        Assertions.assertTrue(recorder.isClosed());
    }

    @Test
    public void testCloseRejectedIfAlreadyClosed() {
        ManualClock clock = new ManualClock();
        StubMetricRecorder recorder = new StubMetricRecorder("test", clock);

        recorder.close();

        Assertions.assertThrows(IllegalStateException.class, recorder::close);
    }

    @Test
    public void testAddPropertyCallsAddPropertyHook() {
        ManualClock clock = new ManualClock();
        StubMetricRecorder recorder = new StubMetricRecorder("test", clock);
        String metricName = UUID.randomUUID().toString();
        String metricValue = UUID.randomUUID().toString();
        recorder.addProperty(metricName, metricValue);

        // We know the hook was called if the stub has the metric in its map
        Assertions.assertEquals(metricValue, recorder.getProperties().get(metricName));
    }

    @Test
    public void testAddPropertyRejectedWhenClosed() {
        ManualClock clock = new ManualClock();
        StubMetricRecorder recorder = new StubMetricRecorder("test", clock);
        recorder.close();

        String metricName = UUID.randomUUID().toString();
        String metricValue = UUID.randomUUID().toString();
        Assertions.assertThrows(IllegalStateException.class, () -> recorder.addProperty(metricName, metricValue));
    }

    @Test
    public void testAddTimestampCallsAddTimestampHook() {
        ManualClock clock = new ManualClock();
        StubMetricRecorder recorder = new StubMetricRecorder("test", clock);
        String metricName = UUID.randomUUID().toString();
        Instant metricValue = clock.forward(Duration.ofHours(17));
        recorder.addTimestamp(metricName, metricValue);

        // We know the hook was called if the stub has the metric in its map
        Assertions.assertEquals(metricValue, recorder.getTimestamps().get(metricName));
    }

    @Test
    public void testAddTimestampRejectedWhenClosed() {
        ManualClock clock = new ManualClock();
        StubMetricRecorder recorder = new StubMetricRecorder("test", clock);
        recorder.close();

        String metricName = UUID.randomUUID().toString();
        Instant metricValue = clock.forward(Duration.ofHours(17));
        Assertions.assertThrows(IllegalStateException.class, () -> recorder.addTimestamp(metricName, metricValue));
    }

    @Test
    public void testAddDurationCallsAddTimestampHook() {
        ManualClock clock = new ManualClock();
        StubMetricRecorder recorder = new StubMetricRecorder("test", clock);
        String metricName = UUID.randomUUID().toString();
        Duration metricValue = Duration.ofHours(17);
        recorder.addDuration(metricName, metricValue);

        // We know the hook was called if the stub has the metric in its map
        Assertions.assertEquals(metricValue, recorder.getDurations().get(metricName));
    }

    @Test
    public void testAddDurationRejectedWhenClosed() {
        ManualClock clock = new ManualClock();
        StubMetricRecorder recorder = new StubMetricRecorder("test", clock);
        recorder.close();

        String metricName = UUID.randomUUID().toString();
        Duration metricValue = Duration.ofHours(17);
        Assertions.assertThrows(IllegalStateException.class, () -> recorder.addDuration(metricName, metricValue));
    }

    @Test
    public void testAddCountCallsAddCountHook() {
        ManualClock clock = new ManualClock();
        StubMetricRecorder recorder = new StubMetricRecorder("test", clock);
        String metricName = UUID.randomUUID().toString();
        double metricValue = ThreadLocalRandom.current().nextDouble();
        recorder.addCount(metricName, metricValue);
        // We know the hook was called if the stub has the metric in its map
        Assertions.assertEquals(metricValue, recorder.getCounts().get(metricName));
    }

    @Test
    public void testAddCountRejectedWhenClosed() {
        ManualClock clock = new ManualClock();
        StubMetricRecorder recorder = new StubMetricRecorder("test", clock);
        recorder.close();

        String metricName = UUID.randomUUID().toString();
        double metricValue = ThreadLocalRandom.current().nextDouble();
        Assertions.assertThrows(IllegalStateException.class, () -> recorder.addCount(metricName, metricValue));
    }

    @Test
    public void testStartAndEndDurationCalculateDurationCorrectly() {
        ManualClock clock = new ManualClock();
        StubMetricRecorder recorder = new StubMetricRecorder("test", clock);

        String metricName = UUID.randomUUID().toString();
        recorder.startDuration(metricName);

        Duration expectedDuration = Duration.ofSeconds(21);
        clock.forward(expectedDuration);
        recorder.endDuration(metricName);

        Assertions.assertEquals(expectedDuration, recorder.getDurations().get(metricName));
    }

    @Test
    public void testStartAndEndDurationWithCustomTimestampsCalculateDurationCorrectly() {
        ManualClock clock = new ManualClock();
        StubMetricRecorder recorder = new StubMetricRecorder("test", clock);

        String metricName = UUID.randomUUID().toString();
        Instant startTime = clock.forward(Duration.ofDays(3));
        recorder.startDuration(metricName, startTime);

        Duration expectedDuration = Duration.ofSeconds(17);
        Instant endTime = clock.forward(expectedDuration);
        recorder.endDuration(metricName, endTime);

        Assertions.assertEquals(expectedDuration, recorder.getDurations().get(metricName));
    }

    @Test
    public void testStartAndEndMultipleConcurrentDurationsCalculatesAllDurationsCorrectly() {
        ManualClock clock = new ManualClock();
        StubMetricRecorder recorder = new StubMetricRecorder("test", clock);

        String metricName1 = UUID.randomUUID().toString();
        recorder.startDuration(metricName1);

        clock.forward(Duration.ofMinutes(2));
        String metricName2 = UUID.randomUUID().toString();
        recorder.startDuration(metricName2);

        clock.forward(Duration.ofMinutes(1));
        recorder.endDuration(metricName1);

        clock.forward(Duration.ofMinutes(1));
        recorder.endDuration(metricName2);

        Assertions.assertEquals(Duration.ofMinutes(3), recorder.getDurations().get(metricName1));
        Assertions.assertEquals(Duration.ofMinutes(2), recorder.getDurations().get(metricName2));
    }

    @Test
    public void testCloseEndsAllOpenDurations() {
        ManualClock clock = new ManualClock();
        StubMetricRecorder recorder = new StubMetricRecorder("test", clock);

        String metricName1 = UUID.randomUUID().toString();
        recorder.startDuration(metricName1);

        clock.forward(Duration.ofMinutes(1));
        String metricName2 = UUID.randomUUID().toString();
        recorder.startDuration(metricName2);

        clock.forward(Duration.ofMinutes(3));
        recorder.close();

        Assertions.assertEquals(Duration.ofMinutes(4), recorder.getDurations().get(metricName1));
        Assertions.assertEquals(Duration.ofMinutes(3), recorder.getDurations().get(metricName2));
    }

    @Test
    public void testStartDurationRejectedWhenClosed() {
        ManualClock clock = new ManualClock();
        StubMetricRecorder recorder = new StubMetricRecorder("test", clock);
        recorder.close();

        String metricName = UUID.randomUUID().toString();
        Assertions.assertThrows(IllegalStateException.class, () -> recorder.startDuration(metricName));
        Assertions.assertThrows(IllegalStateException.class, () -> recorder.startDuration(metricName, clock.instant()));
    }

    @Test
    public void testStartDurationRejectedWhenDurationAlreadyStarted() {
        ManualClock clock = new ManualClock();
        StubMetricRecorder recorder = new StubMetricRecorder("test", clock);
        String metricName = UUID.randomUUID().toString();
        recorder.startDuration(metricName);

        Assertions.assertThrows(IllegalStateException.class, () -> recorder.startDuration(metricName));
        Assertions.assertThrows(IllegalStateException.class, () -> recorder.startDuration(metricName, clock.instant()));
    }

    @Test
    public void testEndDurationRejectedWhenClosed() {
        ManualClock clock = new ManualClock();
        StubMetricRecorder recorder = new StubMetricRecorder("test", clock);
        recorder.close();

        String metricName = UUID.randomUUID().toString();
        Assertions.assertThrows(IllegalStateException.class, () -> recorder.endDuration(metricName));
        Assertions.assertThrows(IllegalStateException.class, () -> recorder.endDuration(metricName, clock.instant()));
    }

    @Test
    public void testEndDurationRejectedWhenNoMatchingDurationWasStarted() {
        ManualClock clock = new ManualClock();
        StubMetricRecorder recorder = new StubMetricRecorder("test", clock);

        String metricName = UUID.randomUUID().toString();
        Assertions.assertThrows(IllegalStateException.class, () -> recorder.endDuration(metricName));
        Assertions.assertThrows(IllegalStateException.class, () -> recorder.endDuration(metricName, clock.instant()));
    }

    @Test
    public void testStartAndEndDurationCallsHookEachTimeIfNameReused() {
        // This tests if it's possible for implementations of MetricRecorder
        // are given enough information to aggregate multiple durations
        // if they're recorded with the same name one after the other.

        ManualClock clock = new ManualClock();
        StubMetricRecorder recorder = new StubMetricRecorder("test", clock);

        String metricName = UUID.randomUUID().toString();
        recorder.startDuration(metricName);
        clock.forward(Duration.ofMinutes(2));
        recorder.endDuration(metricName);

        Assertions.assertEquals(Duration.ofMinutes(2), recorder.getDurations().get(metricName));

        recorder.startDuration(metricName);
        clock.forward(Duration.ofMinutes(3));
        recorder.endDuration(metricName);

        // note that our stub does append durations
        Assertions.assertEquals(Duration.ofMinutes(5), recorder.getDurations().get(metricName));
    }

    /**
     * This recorder lets us verify that the base MetricRecorder class calls the base class hooks at the right times.
     * This is basically a trimmed-down version of InMemoryMetricRecorder.
     */
    public static class StubMetricRecorder extends MetricRecorder {
        private final Map<String, String> properties;
        private final Map<String, Instant> dates;
        private final Map<String, Double> counts;
        private final Map<String, Duration> durations;

        private boolean closeHookCalled;

        public StubMetricRecorder(String operation, Clock clock) {
            super(operation, clock);
            this.properties = new HashMap<>();
            this.dates = new HashMap<>();
            this.counts = new HashMap<>();
            this.durations = new HashMap<>();
            this.closeHookCalled = false;
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

        @Override
        protected void closeHook() {
            closeHookCalled = true;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        public Map<String, Instant> getTimestamps() {
            return dates;
        }

        public Map<String, Double> getCounts() {
            return counts;
        }

        public Map<String, Duration> getDurations() {
            return durations;
        }

        public boolean isCloseHookCalled() {
            return closeHookCalled;
        }
    }
}
