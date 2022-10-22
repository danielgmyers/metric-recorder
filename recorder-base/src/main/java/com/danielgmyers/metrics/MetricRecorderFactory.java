/*
 *   Copyright Flux Contributors
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

/**
 * Generic interface for an object which can create MetricRecorder objects.
 */
public interface MetricRecorderFactory {
    /**
     * Specifies the operation about which metrics will be recorded using the produced MetricRecorder.
     *
     * For example, this could be the name of the API being called, or the name of a discrete sub-operation being performed.
     */
    MetricRecorder newMetricRecorder(String operation);
}
