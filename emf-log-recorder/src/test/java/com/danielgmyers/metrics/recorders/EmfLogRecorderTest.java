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
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import com.danielgmyers.metrics.InvalidMetricConversionException;
import com.danielgmyers.metrics.MetricUnit;
import com.danielgmyers.metrics.StandardMetricNames;
import com.danielgmyers.metrics.recorders.config.EmfLogRecorderConfig;
import com.danielgmyers.metrics.recorders.util.DimensionSetMatcher;
import org.easymock.EasyMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.StorageResolution;
import software.amazon.cloudwatchlogs.emf.model.Unit;

public class EmfLogRecorderTest {

    MetricsLogger mockEmfLogger;
    EmfLogRecorderConfig config;

    private static final String METRIC_NAMESPACE = "myMetricNamespace";
    private static final String OPERATION_NAME = "test";

    @BeforeEach
    public void setup() {
        mockEmfLogger = EasyMock.createMock(MetricsLogger.class);
        config = new EmfLogRecorderConfig(METRIC_NAMESPACE);
    }

    @AfterEach
    public void cleanup() {
        EasyMock.verify(mockEmfLogger);
    }

    private EmfLogRecorder loggerWithDefaultMetrics() {
        return loggerWithDefaultMetrics(Collections.emptyMap());
    }

    private EmfLogRecorder loggerWithDefaultMetrics(Map<String, ?> expectedDimensions) {
        EmfLogRecorder recorder = new EmfLogRecorder(mockEmfLogger, config, OPERATION_NAME, Clock.systemUTC());

        // we don't actually care what timestamp/duration ends up in these three properties
        EasyMock.expect(mockEmfLogger.putProperty(EasyMock.eq(StandardMetricNames.START_TIME.toString()),
                                                  EasyMock.isA(String.class)))
                .andReturn(mockEmfLogger);
        EasyMock.expect(mockEmfLogger.putProperty(EasyMock.eq(StandardMetricNames.END_TIME.toString()),
                                                  EasyMock.isA(String.class)))
                .andReturn(mockEmfLogger);
        EasyMock.expect(mockEmfLogger.putMetric(EasyMock.eq(StandardMetricNames.TIME.toString()), EasyMock.anyLong(),
                                                EasyMock.eq(Unit.MILLISECONDS), EasyMock.eq(StorageResolution.STANDARD)))
                .andReturn(mockEmfLogger);

        EasyMock.expect(mockEmfLogger.putProperty(StandardMetricNames.OPERATION.toString(), OPERATION_NAME)).andReturn(mockEmfLogger);
        EasyMock.expect(mockEmfLogger.putProperty(StandardMetricNames.THREAD_NAME.toString(), Thread.currentThread().getName())).andReturn(mockEmfLogger);

        EasyMock.expect(mockEmfLogger.setDimensions(EasyMock.eq(config.includeDefaultAwsEmbeddedMetricsDimensions()),
                                                    DimensionSetMatcher.eqDimensionSet(expectedDimensions)))
                .andReturn(mockEmfLogger);

        EasyMock.expect(mockEmfLogger.setNamespace(config.getMetricNamespace())).andReturn(mockEmfLogger);
        EasyMock.expect(mockEmfLogger.setTimestamp(EasyMock.isA(Instant.class))).andReturn(mockEmfLogger);

        mockEmfLogger.flush();
        EasyMock.expectLastCall();

        return recorder;
    }

    @Test
    public void testDefaultMetrics() {
        EmfLogRecorder recorder = loggerWithDefaultMetrics();

        EasyMock.replay(mockEmfLogger);

        recorder.close();
    }

    @Test
    public void testAddProperty() {
        EmfLogRecorder recorder = loggerWithDefaultMetrics();
        String metricName = "StarshipName";
        String metricValue = "Enterprise";

        EasyMock.expect(mockEmfLogger.putProperty(metricName, metricValue)).andReturn(mockEmfLogger);

        EasyMock.replay(mockEmfLogger);

        recorder.addProperty(metricName, metricValue);
        recorder.close();
    }

    @Test
    public void testAddLotsOfProperties() {
        EmfLogRecorder recorder = loggerWithDefaultMetrics();

        Map<String, UUID> properties = generateData(10, UUID::randomUUID);

        properties.forEach((metricName, value) -> {
            EasyMock.expect(mockEmfLogger.putProperty(metricName, value.toString())).andReturn(mockEmfLogger);
        });

        EasyMock.replay(mockEmfLogger);

        properties.forEach((key, value) -> recorder.addProperty(key, value.toString()));
        recorder.close();
    }

    @Test
    public void testAddTimestamp() {
        EmfLogRecorder recorder = loggerWithDefaultMetrics();
        String metricName = "TheFuture";
        Instant metricValue = Instant.now().plus(Duration.ofDays(3650));

        EasyMock.expect(mockEmfLogger.putProperty(metricName, metricValue.toString())).andReturn(mockEmfLogger);

        EasyMock.replay(mockEmfLogger);

        recorder.addTimestamp(metricName, metricValue);
        recorder.close();
    }

    @Test
    public void testAddLotsOfTimestamps() {
        EmfLogRecorder recorder = loggerWithDefaultMetrics();

        Instant nextTimestamp = Instant.now(Clock.systemUTC());
        Map<String, Instant> timestamps = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            String metricName = UUID.randomUUID().toString();
            timestamps.put(metricName, nextTimestamp);
            nextTimestamp.plus(Duration.ofMinutes(42));
        }

        timestamps.forEach((metricName, value) -> {
            EasyMock.expect(mockEmfLogger.putProperty(metricName, value.toString())).andReturn(mockEmfLogger);
        });

        EasyMock.replay(mockEmfLogger);

        timestamps.forEach(recorder::addTimestamp);
        recorder.close();
    }

    @Test
    public void testAddDuration() {
        EmfLogRecorder recorder = loggerWithDefaultMetrics();
        String metricName = "OneMonth";
        Duration metricValue = Duration.ofDays(30);

        EasyMock.expect(mockEmfLogger.putMetric(metricName, metricValue.toMillis(),
                        Unit.MILLISECONDS, StorageResolution.STANDARD)).andReturn(mockEmfLogger);

        EasyMock.replay(mockEmfLogger);

        recorder.addDuration(metricName, metricValue);
        recorder.close();
    }

    @Test
    public void testAddLotsOfDurations() {
        EmfLogRecorder recorder = loggerWithDefaultMetrics();

        Duration nextDuration = Duration.ofMinutes(11);
        Map<String, Duration> durations = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            String metricName = UUID.randomUUID().toString();
            durations.put(metricName, nextDuration);
            nextDuration = nextDuration.plusMinutes(11);
        }

        durations.forEach((metricName, value) -> {
            EasyMock.expect(mockEmfLogger.putMetric(metricName, value.toMillis(), Unit.MILLISECONDS, StorageResolution.STANDARD))
                    .andReturn(mockEmfLogger);
        });

        EasyMock.replay(mockEmfLogger);

        durations.forEach(recorder::addDuration);
        recorder.close();
    }

    @Test
    public void testAddCount() {
        EmfLogRecorder recorder = loggerWithDefaultMetrics();
        String metricName = "Rings";
        double metricValue = 1.0;

        EasyMock.expect(mockEmfLogger.putMetric(metricName, metricValue,
                        Unit.NONE, StorageResolution.STANDARD)).andReturn(mockEmfLogger);
        EasyMock.replay(mockEmfLogger);

        recorder.addCount(metricName, metricValue);
        recorder.close();
    }

    @Test
    public void testAddCountWithUnit() {
        EmfLogRecorder recorder = loggerWithDefaultMetrics();
        String metricName = "FileSize";
        double metricValue = 1.0;

        EasyMock.expect(mockEmfLogger.putMetric(metricName, metricValue,
                        Unit.GIGABYTES, StorageResolution.STANDARD)).andReturn(mockEmfLogger);
        EasyMock.replay(mockEmfLogger);

        recorder.addCount(metricName, metricValue, MetricUnit.GB);
        recorder.close();
    }

    @Test
    public void testAddCountWithCompatibleUnitConvertsToOriginalUnit() {
        EmfLogRecorder recorder = loggerWithDefaultMetrics();
        String metricName = "FileSize";

        EasyMock.expect(mockEmfLogger.putMetric(metricName, 1.0,
                        Unit.GIGABYTES, StorageResolution.STANDARD)).andReturn(mockEmfLogger);
        EasyMock.expect(mockEmfLogger.putMetric(metricName, 3.0,
                        Unit.GIGABYTES, StorageResolution.STANDARD)).andReturn(mockEmfLogger);
        EasyMock.expect(mockEmfLogger.putMetric(metricName, 7.0,
                        Unit.GIGABYTES, StorageResolution.STANDARD)).andReturn(mockEmfLogger);
        EasyMock.expect(mockEmfLogger.putMetric(metricName, 9.0,
                        Unit.GIGABYTES, StorageResolution.STANDARD)).andReturn(mockEmfLogger);

        EasyMock.replay(mockEmfLogger);

        recorder.addCount(metricName, 1.0, MetricUnit.GB);
        recorder.addCount(metricName, 3000.0, MetricUnit.MB);
        recorder.addCount(metricName, 7000000.0, MetricUnit.KB);
        recorder.addCount(metricName, 0.009, MetricUnit.TB);
        recorder.close();
    }

    @Test
    public void testAddCountRejectsMismatchedUnits() {
        EmfLogRecorder recorder = loggerWithDefaultMetrics();
        String metricName = UUID.randomUUID().toString();
        double metricValue = ThreadLocalRandom.current().nextDouble();

        EasyMock.expect(mockEmfLogger.putMetric(metricName, metricValue,
                        Unit.NONE, StorageResolution.STANDARD)).andReturn(mockEmfLogger);

        EasyMock.replay(mockEmfLogger);

        recorder.addCount(metricName, metricValue);

        // should be rejected because None != Percent
        double anotherValue = ThreadLocalRandom.current().nextDouble();
        MetricUnit wrongMetricUnit = MetricUnit.Percent;
        Assertions.assertThrows(InvalidMetricConversionException.class,
                                () -> recorder.addCount(metricName, anotherValue, wrongMetricUnit));

        recorder.close();
    }

    @Test
    public void testAddCountWithUnitRejectsMismatchedUnits() {
        EmfLogRecorder recorder = loggerWithDefaultMetrics();
        String metricName = UUID.randomUUID().toString();
        double metricValue = ThreadLocalRandom.current().nextDouble();
        MetricUnit metricUnit = MetricUnit.Gbps;

        EasyMock.expect(mockEmfLogger.putMetric(metricName, metricValue,
                        Unit.GIGABITS_SECOND, StorageResolution.STANDARD)).andReturn(mockEmfLogger);

        EasyMock.replay(mockEmfLogger);

        recorder.addCount(metricName, metricValue, metricUnit);

        // should be rejected because Gbps != Percent
        double anotherValue = ThreadLocalRandom.current().nextDouble();
        MetricUnit wrongMetricUnit = MetricUnit.Percent;
        Assertions.assertThrows(InvalidMetricConversionException.class,
                                () -> recorder.addCount(metricName, anotherValue, wrongMetricUnit));

        recorder.close();
    }

    @Test
    public void testAddLotsOfCounts() {
        EmfLogRecorder recorder = loggerWithDefaultMetrics();
        Map<String, Double> counts = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            String metricName = UUID.randomUUID().toString();
            double metricValue = ThreadLocalRandom.current().nextDouble();
            counts.put(metricName, metricValue);
        }

        counts.forEach((metricName, value) -> {
            EasyMock.expect(mockEmfLogger.putMetric(metricName, value, Unit.NONE, StorageResolution.STANDARD))
                    .andReturn(mockEmfLogger);
        });

        EasyMock.replay(mockEmfLogger);

        counts.forEach(recorder::addCount);
        recorder.close();
    }

    @Test
    public void testAddLotsOfCountsWithUnits() {
        EmfLogRecorder recorder = loggerWithDefaultMetrics();
        Map<String, Double> counts = new HashMap<>();
        Map<String, MetricUnit> units = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            String metricName = UUID.randomUUID().toString();
            double metricValue = ThreadLocalRandom.current().nextDouble();
            counts.put(metricName, metricValue);
            units.put(metricName, MetricUnit.values()[ThreadLocalRandom.current().nextInt(0, MetricUnit.values().length)]);
        }

        counts.forEach((metricName, value) -> {
            MetricUnit expectedUnit = units.get(metricName);
            CwUnitQuantity cwUnit = CwUnitQuantity.calculate(value, expectedUnit, config);
            EasyMock.expect(mockEmfLogger.putMetric(metricName, cwUnit.getValue(), cwUnit.getCwUnit(), StorageResolution.STANDARD))
                    .andReturn(mockEmfLogger);
        });

        EasyMock.replay(mockEmfLogger);

        counts.forEach((metricName, value) -> {
            MetricUnit metricUnit = units.get(metricName);
            recorder.addCount(metricName, value, metricUnit);
        });

        recorder.close();
    }

    @Test
    public void addPropertyRespectsDimensionFilter() {
        Map<String, UUID> dimensions = generateData(3, UUID::randomUUID);
        Map<String, UUID> nonDimensions = generateData(10, UUID::randomUUID);

        EmfLogRecorder recorder = loggerWithDefaultMetrics(dimensions);

        config.setDimensionPropertyFilter((i) -> {
            Assertions.assertEquals(METRIC_NAMESPACE, i.getMetricNamespace());
            Assertions.assertEquals(OPERATION_NAME, i.getOperationName());
            return dimensions.containsKey(i.getPropertyName());
        });

        nonDimensions.forEach((name, value) -> {
            EasyMock.expect(mockEmfLogger.putProperty(name, value.toString())).andReturn(mockEmfLogger);
        });
        EasyMock.replay(mockEmfLogger);

        dimensions.forEach((key, value) -> recorder.addProperty(key, value.toString()));
        nonDimensions.forEach((key, value) -> recorder.addProperty(key, value.toString()));

        recorder.close();
    }

    @Test
    public void addTimestampRespectsDimensionFilter() {
        Map<String, Instant> dimensions = generateData(3, Instant::now);
        Map<String, Instant> nonDimensions = generateData(10, Instant::now);

        EmfLogRecorder recorder = loggerWithDefaultMetrics(dimensions);

        config.setDimensionPropertyFilter((i) -> {
            Assertions.assertEquals(METRIC_NAMESPACE, i.getMetricNamespace());
            Assertions.assertEquals(OPERATION_NAME, i.getOperationName());
            return dimensions.containsKey(i.getPropertyName());
        });

        nonDimensions.forEach((name, value) -> {
            EasyMock.expect(mockEmfLogger.putProperty(name, value.toString())).andReturn(mockEmfLogger);
        });
        EasyMock.replay(mockEmfLogger);

        dimensions.forEach(recorder::addTimestamp);
        nonDimensions.forEach(recorder::addTimestamp);

        recorder.close();
    }

    @Test
    public void addCountRespectsHighResolutionFilter() {
        EmfLogRecorder recorder = loggerWithDefaultMetrics();

        Map<String, Double> highRes = generateData(3, ThreadLocalRandom.current()::nextDouble);
        Map<String, Double> stdRes = generateData(10, ThreadLocalRandom.current()::nextDouble);

        config.setHighResolutionMetricFilter((i) -> {
            Assertions.assertEquals(METRIC_NAMESPACE, i.getMetricNamespace());
            Assertions.assertEquals(OPERATION_NAME, i.getOperationName());
            return highRes.containsKey(i.getMetricName());
        });

        highRes.forEach((name, value) -> {
            EasyMock.expect(mockEmfLogger.putMetric(name, value, Unit.NONE, StorageResolution.HIGH))
                    .andReturn(mockEmfLogger);
        });
        stdRes.forEach((name, value) -> {
            EasyMock.expect(mockEmfLogger.putMetric(name, value, Unit.NONE, StorageResolution.STANDARD))
                    .andReturn(mockEmfLogger);
        });
        EasyMock.replay(mockEmfLogger);

        highRes.forEach(recorder::addCount);
        stdRes.forEach(recorder::addCount);

        recorder.close();
    }

    @Test
    public void addCountWithUnitRespectsHighResolutionFilter() {
        EmfLogRecorder recorder = loggerWithDefaultMetrics();

        Map<String, Double> highRes = generateData(3, ThreadLocalRandom.current()::nextDouble);
        Map<String, Double> stdRes = generateData(10, ThreadLocalRandom.current()::nextDouble);

        config.setHighResolutionMetricFilter((i) -> {
            Assertions.assertEquals(METRIC_NAMESPACE, i.getMetricNamespace());
            Assertions.assertEquals(OPERATION_NAME, i.getOperationName());
            return highRes.containsKey(i.getMetricName());
        });

        highRes.forEach((name, value) -> {
            EasyMock.expect(mockEmfLogger.putMetric(name, value, Unit.COUNT, StorageResolution.HIGH))
                    .andReturn(mockEmfLogger);
        });
        stdRes.forEach((name, value) -> {
            EasyMock.expect(mockEmfLogger.putMetric(name, value, Unit.COUNT, StorageResolution.STANDARD))
                    .andReturn(mockEmfLogger);
        });
        EasyMock.replay(mockEmfLogger);

        highRes.forEach((k, v) -> recorder.addCount(k, v, MetricUnit.Count));
        stdRes.forEach((k, v) -> recorder.addCount(k, v, MetricUnit.Count));

        recorder.close();
    }

    @Test
    public void addDurationRespectsHighResolutionFilter() {
        EmfLogRecorder recorder = loggerWithDefaultMetrics();

        Supplier<Duration> randomDurationSupplier = () -> Duration.ofMillis(ThreadLocalRandom.current().nextInt());

        Map<String, Duration> highRes = generateData(3, randomDurationSupplier);
        Map<String, Duration> stdRes = generateData(10, randomDurationSupplier);

        config.setHighResolutionMetricFilter((i) -> {
            Assertions.assertEquals(METRIC_NAMESPACE, i.getMetricNamespace());
            Assertions.assertEquals(OPERATION_NAME, i.getOperationName());
            return highRes.containsKey(i.getMetricName());
        });

        highRes.forEach((name, value) -> {
            EasyMock.expect(mockEmfLogger.putMetric(name, value.toMillis(), Unit.MILLISECONDS, StorageResolution.HIGH))
                    .andReturn(mockEmfLogger);
        });
        stdRes.forEach((name, value) -> {
            EasyMock.expect(mockEmfLogger.putMetric(name, value.toMillis(), Unit.MILLISECONDS, StorageResolution.STANDARD))
                    .andReturn(mockEmfLogger);
        });
        EasyMock.replay(mockEmfLogger);

        highRes.forEach(recorder::addDuration);
        stdRes.forEach(recorder::addDuration);

        recorder.close();
    }

    private static <T> Map<String, T> generateData(int count, Supplier<T> valueGenerator) {
        Map<String, T> properties = new HashMap<>();
        for (int i = 0; i < count; i++) {
            properties.put(UUID.randomUUID().toString(), valueGenerator.get());
        }
        return properties;
    }
}
