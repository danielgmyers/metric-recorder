package com.danielgmyers.metrics.recorders;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import com.danielgmyers.metrics.MetricRecorder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InMemoryMetricRecorderFactoryTest {

    private InMemoryMetricRecorderFactory factory = new InMemoryMetricRecorderFactory();

    @Test
    public void testFactoryReturnsInMemoryMetricRecorder() {
        MetricRecorder recorder = factory.newMetricRecorder("test");
        Assertions.assertEquals(InMemoryMetricRecorder.class, recorder.getClass());

        recorder = factory.newMetricRecorder("test", Clock.fixed(Instant.now(), ZoneId.systemDefault()));
        Assertions.assertEquals(InMemoryMetricRecorder.class, recorder.getClass());
    }
}
