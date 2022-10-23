package com.danielgmyers.metrics.recorders;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.danielgmyers.metrics.StandardMetricNames;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InMemoryMetricRecorderTest {

    @Test
    public void testDefaultMetrics() {
        ManualClock clock = new ManualClock();

        Instant startTime = clock.instant();
        String operation = "test";

        InMemoryMetricRecorder recorder = new InMemoryMetricRecorder(operation, clock);
        Duration operationDuration = Duration.ofMillis(200);
        Instant endTime = clock.forward(operationDuration);
        recorder.close();

        Assertions.assertTrue(recorder.isClosed());
        Assertions.assertEquals(startTime, recorder.getTimestamp(StandardMetricNames.START_TIME.toString()));
        Assertions.assertEquals(endTime, recorder.getTimestamp(StandardMetricNames.END_TIME.toString()));
        Assertions.assertEquals(operationDuration, recorder.getDuration(StandardMetricNames.TIME.toString()));
        Assertions.assertEquals(operation, recorder.getProperty(StandardMetricNames.OPERATION.toString()));
        Assertions.assertEquals(Thread.currentThread().getName(), recorder.getProperty(StandardMetricNames.THREAD_NAME.toString()));
    }

    @Test
    public void testAddAndGetProperty() {
        InMemoryMetricRecorder recorder = new InMemoryMetricRecorder("test");
        String metricName = "StarshipName";
        String metricValue = "Enterprise";
        recorder.addProperty(metricName, metricValue);
        recorder.close();
        Assertions.assertEquals(metricValue, recorder.getProperty(metricName));
        Assertions.assertEquals(metricValue, recorder.getProperties().get(metricName));
    }

    @Test
    public void testAddPropertyOnlyRecordsLastValueForMetric() {
        InMemoryMetricRecorder recorder = new InMemoryMetricRecorder("test");
        String metricName = "StarshipName";
        String enterprise = "Enterprise";
        recorder.addProperty(metricName, enterprise);
        String voyager = "Voyager";
        recorder.addProperty(metricName, voyager);
        recorder.close();
        Assertions.assertEquals(voyager, recorder.getProperty(metricName));
        Assertions.assertEquals(voyager, recorder.getProperties().get(metricName));
    }

    @Test
    public void testAddLotsOfProperties() {
        InMemoryMetricRecorder recorder = new InMemoryMetricRecorder("test");
        Map<String, String> properties = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            String propertyName = UUID.randomUUID().toString();
            String propertyValue = UUID.randomUUID().toString();
            properties.put(propertyName, propertyValue);
            recorder.addProperty(propertyName, propertyValue);
        }

        recorder.close();

        properties.forEach((k, v) -> {
            Assertions.assertEquals(v, recorder.getProperty(k));
            Assertions.assertEquals(v, recorder.getProperties().get(k));
        });
    }

    @Test
    public void testGetPropertyRejectedUntilClosed() {
        InMemoryMetricRecorder recorder = new InMemoryMetricRecorder("test");
        String metricName = UUID.randomUUID().toString();
        String metricValue = UUID.randomUUID().toString();
        recorder.addProperty(metricName, metricValue);
        Assertions.assertThrows(IllegalStateException.class, () -> recorder.getProperty(metricName));
        Assertions.assertThrows(IllegalStateException.class, recorder::getProperties);
        recorder.close();
        Assertions.assertEquals(metricValue, recorder.getProperty(metricName));
        Assertions.assertEquals(metricValue, recorder.getProperties().get(metricName));
    }

    @Test
    public void testAddAndGetTimestamp() {
        InMemoryMetricRecorder recorder = new InMemoryMetricRecorder("test");
        String metricName = "TheFuture";
        Instant metricValue = Instant.now().plus(Duration.ofDays(3650));
        recorder.addTimestamp(metricName, metricValue);
        recorder.close();
        Assertions.assertEquals(metricValue, recorder.getTimestamp(metricName));
        Assertions.assertEquals(metricValue, recorder.getTimestamps().get(metricName));
    }

    @Test
    public void testAddTimestampOnlyRecordsLastValueForMetric() {
        InMemoryMetricRecorder recorder = new InMemoryMetricRecorder("test");
        String metricName = "TheFuture";
        Instant tenYears = Instant.now().plus(Duration.ofDays(3650));
        recorder.addTimestamp(metricName, tenYears);
        Instant hundredYears = Instant.now().plus(Duration.ofDays(36500));
        recorder.addTimestamp(metricName, hundredYears);
        recorder.close();
        Assertions.assertEquals(hundredYears, recorder.getTimestamp(metricName));
        Assertions.assertEquals(hundredYears, recorder.getTimestamps().get(metricName));
    }

    @Test
    public void testAddLotsOfTimestamps() {
        ManualClock clock = new ManualClock();
        InMemoryMetricRecorder recorder = new InMemoryMetricRecorder("test", clock);
        Map<String, Instant> timestamps = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            String metricName = UUID.randomUUID().toString();
            Instant metricValue = clock.forward(Duration.ofMinutes(42));
            timestamps.put(metricName, metricValue);
            recorder.addTimestamp(metricName, metricValue);
        }

        recorder.close();

        timestamps.forEach((k, v) -> {
            Assertions.assertEquals(v, recorder.getTimestamp(k));
            Assertions.assertEquals(v, recorder.getTimestamps().get(k));
        });
    }

    @Test
    public void testGetTimestampRejectedUntilClosed() {
        InMemoryMetricRecorder recorder = new InMemoryMetricRecorder("test");
        String metricName = UUID.randomUUID().toString();
        Instant metricValue = Instant.now().plus(Duration.ofDays(7));
        recorder.addTimestamp(metricName, metricValue);
        Assertions.assertThrows(IllegalStateException.class, () -> recorder.getTimestamp(metricName));
        Assertions.assertThrows(IllegalStateException.class, recorder::getTimestamps);
        recorder.close();
        Assertions.assertEquals(metricValue, recorder.getTimestamp(metricName));
        Assertions.assertEquals(metricValue, recorder.getTimestamps().get(metricName));
    }

    @Test
    public void testAddAndGetDuration() {
        InMemoryMetricRecorder recorder = new InMemoryMetricRecorder("test");
        String metricName = "OneMonth";
        Duration metricValue = Duration.ofDays(30);
        recorder.addDuration(metricName, metricValue);
        recorder.close();
        Assertions.assertEquals(metricValue, recorder.getDuration(metricName));
        Assertions.assertEquals(metricValue, recorder.getDurations().get(metricName));
    }

    @Test
    public void testAddDurationAggregates() {
        InMemoryMetricRecorder recorder = new InMemoryMetricRecorder("test");
        String metricName = "FourWeeks";
        Duration metricValue = Duration.ofDays(7);
        recorder.addDuration(metricName, metricValue);
        recorder.addDuration(metricName, metricValue);
        recorder.addDuration(metricName, metricValue);
        recorder.addDuration(metricName, metricValue);
        recorder.close();
        Assertions.assertEquals(Duration.ofDays(28), recorder.getDuration(metricName));
        Assertions.assertEquals(Duration.ofDays(28), recorder.getDurations().get(metricName));
    }

    @Test
    public void testAddLotsOfDurations() {
        InMemoryMetricRecorder recorder = new InMemoryMetricRecorder("test");
        Duration nextDuration = Duration.ofMinutes(11);
        Map<String, Duration> durations = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            String metricName = UUID.randomUUID().toString();
            durations.put(metricName, nextDuration);
            recorder.addDuration(metricName, nextDuration);
            nextDuration = nextDuration.plusMinutes(11);
        }

        recorder.close();

        durations.forEach((k, v) -> {
            Assertions.assertEquals(v, recorder.getDuration(k));
            Assertions.assertEquals(v, recorder.getDurations().get(k));
        });
    }

    @Test
    public void testGetDurationRejectedUntilClosed() {
        InMemoryMetricRecorder recorder = new InMemoryMetricRecorder("test");
        String metricName = UUID.randomUUID().toString();
        Duration metricValue = Duration.ofDays(7);
        recorder.addDuration(metricName, metricValue);
        Assertions.assertThrows(IllegalStateException.class, () -> recorder.getDuration(metricName));
        Assertions.assertThrows(IllegalStateException.class, recorder::getDurations);
        recorder.close();
        Assertions.assertEquals(metricValue, recorder.getDuration(metricName));
        Assertions.assertEquals(metricValue, recorder.getDurations().get(metricName));
    }

    @Test
    public void testAddAndGetCount() {
        InMemoryMetricRecorder recorder = new InMemoryMetricRecorder("test");
        String metricName = "Rings";
        Double metricValue = 1.0;
        recorder.addCount(metricName, metricValue);
        recorder.close();
        Assertions.assertEquals(metricValue, recorder.getCount(metricName));
        Assertions.assertEquals(metricValue, recorder.getCounts().get(metricName));
    }

    @Test
    public void testAddCountAggregates() {
        InMemoryMetricRecorder recorder = new InMemoryMetricRecorder("test");
        String metricName = "Rings";
        recorder.addCount(metricName, 1.0);
        recorder.addCount(metricName, 3.0);
        recorder.addCount(metricName, 7.0);
        recorder.addCount(metricName, 9.0);
        recorder.close();
        Assertions.assertEquals(20, recorder.getCount(metricName));
        Assertions.assertEquals(20, recorder.getCounts().get(metricName));
    }

    @Test
    public void testAddLotsOfCounts() {
        InMemoryMetricRecorder recorder = new InMemoryMetricRecorder("test");
        Map<String, Double> counts = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            String metricName = UUID.randomUUID().toString();
            double metricValue = ThreadLocalRandom.current().nextDouble();
            counts.put(metricName, metricValue);
            recorder.addCount(metricName, metricValue);
        }

        recorder.close();

        counts.forEach((k, v) -> {
            Assertions.assertEquals(v, recorder.getCount(k));
            Assertions.assertEquals(v, recorder.getCounts().get(k));
        });
    }

    @Test
    public void testGetCountRejectedUntilClosed() {
        InMemoryMetricRecorder recorder = new InMemoryMetricRecorder("test");
        String metricName = UUID.randomUUID().toString();
        Double metricValue = ThreadLocalRandom.current().nextDouble();
        recorder.addCount(metricName, metricValue);
        Assertions.assertThrows(IllegalStateException.class, () -> recorder.getCount(metricName));
        Assertions.assertThrows(IllegalStateException.class, recorder::getCounts);
        recorder.close();
        Assertions.assertEquals(metricValue, recorder.getCount(metricName));
        Assertions.assertEquals(metricValue, recorder.getCounts().get(metricName));
    }
}
