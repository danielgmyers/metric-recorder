This package provides a `MetricRecorder` implementation that logs in CloudWatch Embedded Metric Format. This allows emitting metrics in a format compatible with the automatic metric publishing feature of CloudWatch Logs.

Prerequisites
-------------

`EmfLogRecorder` uses `software.amazon.cloudwatchlogs:aws-embedded-metrics` to format log content.

Initializing
------------

Initializing an `EmfLogRecorderFactory` is simple:

```java
package example;

import com.danielgmyers.metrics.MetricRecorderFactory;
import com.danielgmyers.metrics.recorders.InMemoryMetricRecorderFactory;
import com.danielgmyers.metrics.recorders.config.EmfLogRecorderConfig;

public class Example {
    private MetricRecorderFactory factory;

    public Example() {
        EmfLogRecorderConfig config = new EmfLogRecorderConfig("MyNamespace");
        factory = new EmfLogRecorderFactory(config);
    }
}
```

Usage of the recorder in code is described in the [main README](../README.md).

The resulting EMF log lines will have their timestamp set to the instant the MetricRecorder is closed. The default set of metrics automatically provided by this library, for a MetricRecorder object open for 100ms, looks like this in EMF (except the real content isn't pretty-printed):

```json
{
  "_aws": {
    "Timestamp": 1794522166983,
    "CloudWatchMetrics": [
      {
        "Namespace": "SomeNamespace",
        "Metrics": [
          {
            "Name": "Time",
            "Unit": "Milliseconds"
          }
        ],
        "Dimensions": [
          []
        ]
      }
    ]
  },
  "ThreadName": "main",
  "StartTime": "2026-11-12T22:22:46.883506Z",
  "Time": 100.0,
  "Operation": "SomeOperation",
  "EndTime": "2026-11-12T22:22:46.983506Z"
}
```

Configuration
-------------

The `EmfLogRecorderFactory` takes an `EmfLogRecorderConfig` object in its constructor. This config object allows some control over how the final EMF logs are written to disk.

See the [Javadocs](emf-log-recorder/src/main/java/com/danielgmyers/metrics/recorders/config/EmfLogRecorderConfig.java) for detailed information about each available configuration.


Validating metrics in Tests
---------------------------

During unit testing, assuming the class under test takes a `MetricRecorderFactory` as one of its constructor arguments, you will likely want to do something like this:

```java
import com.danielgmyers.metrics.recorders.InMemoryMetricRecorder;

public class MyTest {

    @Test
    public void testThing() {
        InMemoryMetricRecorder metricRecorder = new InMemoryMetricRecorder("test");
        
        // We'll use a lambda as a pretend MetricRecorderFactory, so it hands out our metric recorder.
        WidgetManager wm = new WidgetManager((o) -> metricRecorder);
        
        wm.doThingBeingTested();
        
        Assertions.assertTrue(metricRecorder.isClosed());
        Assertions.assertEquals(1, metricRecorder.getCounts("DatabaseLookupCount"));
        // etc
    }
}
```

`InMemoryMetricRecorder` provides lookup methods for each of the supported metric types. It can be found in the `com.danielgmyers.metrics:in-memory-recorder` package, suitable for use as a test dependency.
