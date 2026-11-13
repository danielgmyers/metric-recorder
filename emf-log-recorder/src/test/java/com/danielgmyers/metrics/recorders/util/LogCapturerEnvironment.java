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

package com.danielgmyers.metrics.recorders.util;

import software.amazon.cloudwatchlogs.emf.environment.Environment;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.cloudwatchlogs.emf.sinks.ISink;

public class LogCapturerEnvironment implements Environment {

    private final LogCapturerSink sink;

    public LogCapturerEnvironment(LogCapturerSink sink) {
        this.sink = sink;
    }


    @Override
    public boolean probe() {
        return true;
    }

    @Override
    public String getName() {
        return "some-name";
    }

    @Override
    public String getType() {
        return "some-type";
    }

    @Override
    public String getLogGroupName() {
        return "some-log-group";
    }

    @Override
    public void configureContext(MetricsContext context) {

    }

    @Override
    public ISink getSink() {
        return sink;
    }
}
