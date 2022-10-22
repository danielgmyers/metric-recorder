Metric Recorder is an easy interface for recording performance or other measurements from application code, supporting multiple backend reporting mechanisms.

[![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-2.1-4baaaa.svg)](CODE_OF_CONDUCT.md)

[![CodeBuild status badge](TODO -- build status image)](TODO -- build results link)

Prerequisites
-------------

While your code should be written in terms of the base `MetricRecorder` and `MetricRecorderFactory` interfaces, which essentially have no dependencies, you'll need to choose a memory recorder implementation. In the following example, we will use `InMemoryMetricRecorder` which is self-contained, but other recorders will have other dependencies.

Initializing a Factory
----------------------

Initializing an `InMemoryMetricRecorderFactory` is simple:

```java
package example;

import com.danielgmyers.metrics.MetricRecorderFactory;
import com.danielgmyers.metrics.recorders.InMemoryMetricRecorderFactory;

public class Example {
    private MetricRecorderFactory factory;
    
    public Example() {
        factory = new InMemoryMetricRecorderFactory();
    }
}
```

Your application should only need a single `MetricRecorderFactory` instance.

Recording Metrics
-----------------

Whenever your application needs to record a set of metrics for a logical operation, you should call `MetricRecorderFactory.newMetricRecorder()`. For example, suppose we want to record metrics about aspects of a `GetWidget` API call:

```java
package example;

import com.danielgmyers.metrics.MetricRecorderFactory;
import com.danielgmyers.metrics.recorders.InMemoryMetricRecorder;

public class Example {
    private MetricRecorderFactory factory;
    
    public Widget getWidget(String widgetId) {
        try(MetricRecorder metrics = factory.newMetricRecorder("GetWidget")) {
            metrics.addProperty("WidgetId", widgetId);

            metrics.startDuration("DatabaseLookup");
            WidgetInternal widgetInternal = backendDatastore.getWidget(widgetId);
            metrics.endDuration("DatabaseLookup");

            return doSprocketProcessing(widgetInternal);
        }
    }
}
```

Note the use of try-with-resources here; this ensures that the metrics context gets closed when the operation ends. If you do not use try-with-resources, you _must_ directly call `close()` on your `MetricRecorder` objects. Failure to call `close()` will generally result in your metrics not being recorded.

When a `MetricRecorder` is closed (either automatically or by directly calling `close()`), all the recorded metrics are emitted by the backing recorder implementation. Typically, they will be stored as a block of related metric data.

Once a given `MetricRecorder` object has been closed, it cannot be used to generate any more metrics.

Types of Metrics
----------------

Each of the following metrics types are supported by the `MetricRecorder` interface. Each metric is stored alongside a name. 

### Property metrics

`MetricRecorder.addProperty()` allows arbitrary labels to be stored as part of the metric data.

Examples of typical properties:
* Operation - The name of the operation being measured.
* User - The identity of the user who made the request being measured.
* WidgetId - The identifier of the specific widget being operated on.

### Timestamp metrics

`MetricRecorder.addTimestamp()` allows arbitrary timestamps to be stored as part of the metric data.

Examples of typical timestamps:
* StartTime - The time the operation was started.
* EndTime - The time the operation was finished.
* PreviousOperationTime - The previous time this particular widget was operated on.

### Count metrics

`MetricRecorder.addCount()` allows unitless counts to be stored as part of the metric data.

Examples of typical counts:
* WidgetResultCount - The number of widgets being returned by a `ListWidgets` API call.
* FailureCount - How many times this operation has failed (typically 0 or 1).

### Duration metrics

`MetricRecorder.addDuration()` lets you record a `java.time.Duration` as part of the metric data.

Examples of typical durations:
* DatabaseLookupTime - The amount of time taken to look up a record in a database.
* AuthenticationTime - The amount of time taken to perform authentication of an API request.
* WidgetAge - The amount of time a particular Widget has been alive.

Alternatively, you can call `MetricRecorder.startDuration()` just before performing an operation, and then call `MetricRecorder.endDuration()` at the end of the operation. With this approach, `MetricRecorder` automatically calculates the `Duration` and behaves as if `MetricRecorder.addDuration()` had been called with that `Duration`.

When a `MetricRecorder` is closed, `MetricRecorder.endDuration()` is automatically called for any duration timers that were started with `MetricRecorder.startDuration()` but not explicitly ended with a call to `MetricRecorder.endDuration()`.

Automatic Metrics
----------------------

When a `MetricRecorder` is closed, several metrics are added to the context automatically.

The following metrics are automatically added to the set of recorded metrics:

| Type      | Name       | Value                                                                       |
|-----------|------------|-----------------------------------------------------------------------------|
| Property  | Operation  | The operation string passed to `MetricRecorderFactory.newMetricRecorder()`. |
| Property  | ThreadName | The name of the current thread at the time the MetricRecorder is closed.    |
| Timestamp | StartTime  | The time at which `MetricRecorderFactory.newMetricRecorder()` was called.   |
| Timestamp | EndTime    | The time at which `close()` was called.                                     |
| Duration  | Time       | The amount of elapsed time between StartTime to EndTime.                    |

Of particular note, the Operation property is typically used by various backing reporter implementations as a way to namespace or otherwise separate metrics that have the same name. For example, your application will emit the "Time" metric for each API, but generally that metric is most useful when graphed per API, rather than across all APIs.
