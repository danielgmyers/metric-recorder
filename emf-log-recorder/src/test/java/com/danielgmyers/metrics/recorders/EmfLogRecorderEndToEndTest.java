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

import com.danielgmyers.metrics.MetricRecorder;
import com.danielgmyers.metrics.StandardMetricNames;
import com.danielgmyers.metrics.recorders.config.EmfLogRecorderConfig;
import com.danielgmyers.metrics.recorders.util.LogCapturerEnvironment;
import com.danielgmyers.metrics.recorders.util.LogCapturerSink;
import com.danielgmyers.metrics.recorders.util.ManualClock;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.cloudwatchlogs.emf.environment.Environment;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EmfLogRecorderEndToEndTest {
    private final LogCapturerSink logCapturerSink = new LogCapturerSink();
    private final Environment logCapturerEnvironment = new LogCapturerEnvironment(logCapturerSink);
    private EmfLogRecorderConfig config;
    private EmfLogRecorderFactory factory;

    private static final String OPERATION_NAME = "testOperation";
    private static final String NAMESPACE = "MyMetricNamespace";

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        logCapturerSink.reset();
        config = new EmfLogRecorderConfig(NAMESPACE, logCapturerEnvironment);
        factory = new EmfLogRecorderFactory(config);

    }

    /**
     * We're generally trusting the MetricsLogger to write EMF correctly, but
     * here we're doing a spot check of a known set of default metrics to make sure
     * the EMF format doesn't change unexpectedly.
     */
    @Test
    public void testDefaultMetricsEmfOutput() throws JsonProcessingException {
        validateEmfOutput(Collections.emptyMap());
    }

    /**
     * Here we're spot-checking that the default dimensions are included when the config flag is enabled.
     * @throws JsonProcessingException
     */
    @Test
    public void testRespectsIncludeDefaultAwsEmbeddedMetricsDimensionsConfig() throws JsonProcessingException {
        config.setIncludeDefaultAwsEmbeddedMetricsDimensions(true);
        config.setHighResolutionMetricFilter((x) -> true);
        validateEmfOutput(Map.of("LogGroup", logCapturerEnvironment.getLogGroupName(),
                             "ServiceName", logCapturerEnvironment.getName(),
                             "ServiceType", logCapturerEnvironment.getType()));
    }

    private void validateEmfOutput(Map<String, ?> expectedDimensions) throws JsonProcessingException {
        Instant startTime = Instant.now();
        ManualClock clock = new ManualClock(startTime);

        Assertions.assertTrue(logCapturerSink.getCapturedLines().isEmpty());

        MetricRecorder recorder = factory.newMetricRecorder(OPERATION_NAME, clock);

        // move the clock forward 100 millis so we can see that the default EndTime and Time metrics
        // are calculated and written correctly in the EMF log.
        clock.forward(Duration.ofMillis(100));

        Instant endTime = clock.instant();

        recorder.close();

        Assertions.assertEquals(1, logCapturerSink.getCapturedLines().size());

        JsonNode root = mapper.readTree(logCapturerSink.getCapturedLines().get(0));

        // these are our defaults, always emitted.
        validateChildString(root, StandardMetricNames.START_TIME.toString(), startTime.toString());
        validateChildString(root, StandardMetricNames.END_TIME.toString(), endTime.toString());
        validateChildString(root, StandardMetricNames.OPERATION.toString(), OPERATION_NAME);
        validateChildDouble(root, StandardMetricNames.TIME.toString(), 100.0);
        validateChildString(root, StandardMetricNames.THREAD_NAME.toString(), Thread.currentThread().getName());

        // ensure all expected dimension values are present
        expectedDimensions.forEach((k, v) -> {
            validateChildString(root, k, v.toString());
        });

        // we check the _aws entries because that's where the namespace and metric units are stored.
        Assertions.assertTrue(root.has("_aws"));
        JsonNode aws = root.get("_aws");
        validateChildLong(aws, "Timestamp", endTime.toEpochMilli());

        Assertions.assertTrue(aws.has("CloudWatchMetrics"));
        ArrayNode cwm = (ArrayNode)(aws.get("CloudWatchMetrics"));
        Assertions.assertEquals(1, cwm.size());
        validateChildString(cwm.get(0), "Namespace", NAMESPACE);

        // there's only one actual count metric in the default set
        Assertions.assertTrue(cwm.get(0).has("Metrics"));
        ArrayNode metricDefs = (ArrayNode)(cwm.get(0).get("Metrics"));
        Assertions.assertEquals(1, metricDefs.size());

        JsonNode unitEntry = metricDefs.get(0);
        validateChildString(unitEntry, "Name", StandardMetricNames.TIME.toString());
        validateChildString(unitEntry, "Unit", "Milliseconds");

        // it should be a list of one item, containing an array matching expectedDimensions.
        Assertions.assertTrue(cwm.get(0).has("Dimensions"));
        ArrayNode dimensionArray = (ArrayNode)(cwm.get(0).get("Dimensions"));
        Assertions.assertEquals(1, dimensionArray.size());
        ArrayNode dimensionsItem = (ArrayNode)(dimensionArray.get(0));
        Set<String> foundDimensions = new HashSet<>();
        for (int i = 0; i < dimensionsItem.size(); i++) {
            foundDimensions.add(dimensionsItem.get(i).asText());
        }
        Assertions.assertEquals(expectedDimensions.keySet(), foundDimensions);
    }

    private void validateChildLong(JsonNode parent, String childName, Long expectedValue) {
        Assertions.assertTrue(parent.has(childName));
        JsonNode child = parent.get(childName);
        Assertions.assertTrue(child.isNumber());
        Assertions.assertEquals(expectedValue, child.longValue());
    }

    private void validateChildDouble(JsonNode parent, String childName, Double expectedValue) {
        Assertions.assertTrue(parent.has(childName));
        JsonNode child = parent.get(childName);
        Assertions.assertTrue(child.isFloatingPointNumber());
        Assertions.assertEquals(expectedValue, child.doubleValue());
    }

    private void validateChildString(JsonNode parent, String childName, String expectedValue) {
        Assertions.assertTrue(parent.has(childName));
        JsonNode child = parent.get(childName);
        Assertions.assertTrue(child.isTextual());
        Assertions.assertEquals(expectedValue, child.textValue());
    }


}
