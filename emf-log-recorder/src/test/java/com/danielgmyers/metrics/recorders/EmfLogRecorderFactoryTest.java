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
import java.time.Instant;
import java.time.ZoneId;

import com.danielgmyers.metrics.recorders.config.EmfLogRecorderConfig;
import com.danielgmyers.metrics.recorders.util.LogCapturerEnvironment;
import com.danielgmyers.metrics.recorders.util.LogCapturerSink;
import org.junit.jupiter.api.BeforeEach;
import software.amazon.cloudwatchlogs.emf.environment.Environment;

import com.danielgmyers.metrics.MetricRecorder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EmfLogRecorderFactoryTest {

    private final LogCapturerSink logCapturerSink = new LogCapturerSink();
    private final Environment logCapturerEnvironment = new LogCapturerEnvironment(logCapturerSink);
    private EmfLogRecorderConfig config;
    private EmfLogRecorderFactory factory;

    @BeforeEach
    public void setup() {
        config = new EmfLogRecorderConfig("MyNamespace", logCapturerEnvironment);
        factory = new EmfLogRecorderFactory(config);
    }

    @Test
    public void testFactoryReturnsEmfLogRecorder() {
        MetricRecorder recorder = factory.newMetricRecorder("test");
        Assertions.assertEquals(EmfLogRecorder.class, recorder.getClass());

        recorder = factory.newMetricRecorder("test", Clock.fixed(Instant.now(), ZoneId.systemDefault()));
        Assertions.assertEquals(EmfLogRecorder.class, recorder.getClass());
    }
}
