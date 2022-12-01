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
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.danielgmyers.metrics.recorders.NoopMetricRecorder;
import com.danielgmyers.metrics.recorders.NoopMetricRecorderFactory;

public class NoopMetricRecorderFactoryTest {

    private final NoopMetricRecorderFactory factory = new NoopMetricRecorderFactory();

    @Test
    public void testFactoryReturnsBaseMetricRecorder() {
        MetricRecorder recorder = factory.newMetricRecorder("test");
        Assertions.assertEquals(NoopMetricRecorder.class, recorder.getClass());

        recorder = factory.newMetricRecorder("test", Clock.fixed(Instant.now(), ZoneId.systemDefault()));
        Assertions.assertEquals(NoopMetricRecorder.class, recorder.getClass());
    }
}
