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

import java.util.HashMap;
import java.util.Map;

import com.danielgmyers.metrics.MetricUnit;
import com.danielgmyers.metrics.recorders.config.EmfLogRecorderConfig;

import software.amazon.cloudwatchlogs.emf.model.Unit;

/**
 * An internal utility class to handle mapping between MetricRecorder's MetricUnit
 * and the available CloudWatch units, including performing the relevant
 * metric math where necessary.
 */
final class CwUnitQuantity {

    private Unit cwUnit;
    private double value;

    private CwUnitQuantity(Unit cwUnit, double value) {
        this.cwUnit = cwUnit;
        this.value = value;
    }

    public Unit getCwUnit() {
        return cwUnit;
    }

    public double getValue() {
        return value;
    }

    // package-private for test access
    static final Map<MetricUnit, Unit> DIRECT_MAPPINGS = new HashMap<>();
    static final Map<MetricUnit, MetricUnit> TRANSFORMS = new HashMap<>();

    public static CwUnitQuantity calculate(double value, MetricUnit metricUnit, EmfLogRecorderConfig config) {
        if (DIRECT_MAPPINGS.isEmpty()) {
            populateMappings(config);
        }

        double resolvedValue = value;
        Unit cwUnit = DIRECT_MAPPINGS.get(metricUnit);
        if (cwUnit == null) {
            // If we get here, we have to check for known transforms, or throw an exception.
            MetricUnit transformedUnit = TRANSFORMS.get(metricUnit);
            cwUnit = DIRECT_MAPPINGS.get(transformedUnit);
            if (cwUnit == null) {
                throw new IllegalArgumentException("Could not map MetricUnit " + metricUnit.name()
                        + " to a CloudWatch unit type.");
            }
            resolvedValue = metricUnit.convertTo(value, transformedUnit);
        }

        return new CwUnitQuantity(cwUnit, resolvedValue);
    }

    /**
     * We calculate the mappings and transforms only once, the first time a count is recorded.
     * This is primarily to simplify the conversion logic by doing the if/else check only once.
     * This should be fine since we don't support changing config after startup.
     *
     * This method is synchronized to avoid two threads populating the maps at the same time.
     * It's immutable data, so it wouldn't break anything to let them all do it.
     *
     * Package-private so tests can initialize it directly.
     */
    static synchronized void populateMappings(EmfLogRecorderConfig config) {
        // Only populate the maps if we're the first to get here.
        if (DIRECT_MAPPINGS.isEmpty()) {
            DIRECT_MAPPINGS.put(MetricUnit.None, Unit.NONE);
            DIRECT_MAPPINGS.put(MetricUnit.Percent, Unit.PERCENT);
            DIRECT_MAPPINGS.put(MetricUnit.Count, Unit.COUNT);
            DIRECT_MAPPINGS.put(MetricUnit.Count_per_second, Unit.COUNT_SECOND);
            DIRECT_MAPPINGS.put(MetricUnit.Bits, Unit.BITS);
            DIRECT_MAPPINGS.put(MetricUnit.Bits_per_second, Unit.BITS_SECOND);
            DIRECT_MAPPINGS.put(MetricUnit.Bytes, Unit.BYTES);
            DIRECT_MAPPINGS.put(MetricUnit.Bytes_per_second, Unit.BYTES_SECOND);

            if (config.isCloudWatchDataUnitBase10()) {
                DIRECT_MAPPINGS.put(MetricUnit.KB, Unit.KILOBYTES);
                DIRECT_MAPPINGS.put(MetricUnit.MB, Unit.MEGABYTES);
                DIRECT_MAPPINGS.put(MetricUnit.GB, Unit.GIGABYTES);
                DIRECT_MAPPINGS.put(MetricUnit.TB, Unit.TERABYTES);

                DIRECT_MAPPINGS.put(MetricUnit.Kbps, Unit.KILOBITS_SECOND);
                DIRECT_MAPPINGS.put(MetricUnit.Mbps, Unit.MEGABITS_SECOND);
                DIRECT_MAPPINGS.put(MetricUnit.Gbps, Unit.GIGABITS_SECOND);
                DIRECT_MAPPINGS.put(MetricUnit.Tbps, Unit.TERABITS_SECOND);

                DIRECT_MAPPINGS.put(MetricUnit.KBps, Unit.KILOBYTES_SECOND);
                DIRECT_MAPPINGS.put(MetricUnit.MBps, Unit.MEGABYTES_SECOND);
                DIRECT_MAPPINGS.put(MetricUnit.GBps, Unit.GIGABYTES_SECOND);
                DIRECT_MAPPINGS.put(MetricUnit.TBps, Unit.TERABYTES_SECOND);

                TRANSFORMS.put(MetricUnit.KiB, MetricUnit.KB);
                TRANSFORMS.put(MetricUnit.MiB, MetricUnit.MB);
                TRANSFORMS.put(MetricUnit.GiB, MetricUnit.GB);
                TRANSFORMS.put(MetricUnit.TiB, MetricUnit.TB);

                TRANSFORMS.put(MetricUnit.Kibps, MetricUnit.Kbps);
                TRANSFORMS.put(MetricUnit.Mibps, MetricUnit.Mbps);
                TRANSFORMS.put(MetricUnit.Gibps, MetricUnit.Gbps);
                TRANSFORMS.put(MetricUnit.Tibps, MetricUnit.Tbps);

                TRANSFORMS.put(MetricUnit.KiBps, MetricUnit.KBps);
                TRANSFORMS.put(MetricUnit.MiBps, MetricUnit.MBps);
                TRANSFORMS.put(MetricUnit.GiBps, MetricUnit.GBps);
                TRANSFORMS.put(MetricUnit.TiBps, MetricUnit.TBps);
            } else { // cloudwatch data transfer unit is base 2
                DIRECT_MAPPINGS.put(MetricUnit.KiB, Unit.KILOBYTES);
                DIRECT_MAPPINGS.put(MetricUnit.MiB, Unit.MEGABYTES);
                DIRECT_MAPPINGS.put(MetricUnit.GiB, Unit.GIGABYTES);
                DIRECT_MAPPINGS.put(MetricUnit.TiB, Unit.TERABYTES);

                DIRECT_MAPPINGS.put(MetricUnit.Kibps, Unit.KILOBITS_SECOND);
                DIRECT_MAPPINGS.put(MetricUnit.Mibps, Unit.MEGABITS_SECOND);
                DIRECT_MAPPINGS.put(MetricUnit.Gibps, Unit.GIGABITS_SECOND);
                DIRECT_MAPPINGS.put(MetricUnit.Tibps, Unit.TERABITS_SECOND);

                DIRECT_MAPPINGS.put(MetricUnit.KiBps, Unit.KILOBYTES_SECOND);
                DIRECT_MAPPINGS.put(MetricUnit.MiBps, Unit.MEGABYTES_SECOND);
                DIRECT_MAPPINGS.put(MetricUnit.GiBps, Unit.GIGABYTES_SECOND);
                DIRECT_MAPPINGS.put(MetricUnit.TiBps, Unit.TERABYTES_SECOND);

                TRANSFORMS.put(MetricUnit.KB, MetricUnit.KiB);
                TRANSFORMS.put(MetricUnit.MB, MetricUnit.MiB);
                TRANSFORMS.put(MetricUnit.GB, MetricUnit.GiB);
                TRANSFORMS.put(MetricUnit.TB, MetricUnit.TiB);

                TRANSFORMS.put(MetricUnit.Kbps, MetricUnit.Kibps);
                TRANSFORMS.put(MetricUnit.Mbps, MetricUnit.Mibps);
                TRANSFORMS.put(MetricUnit.Gbps, MetricUnit.Gibps);
                TRANSFORMS.put(MetricUnit.Tbps, MetricUnit.Tibps);

                TRANSFORMS.put(MetricUnit.KBps, MetricUnit.KiBps);
                TRANSFORMS.put(MetricUnit.MBps, MetricUnit.MiBps);
                TRANSFORMS.put(MetricUnit.GBps, MetricUnit.GiBps);
                TRANSFORMS.put(MetricUnit.TBps, MetricUnit.TiBps);
            }
        }
    }

    /**
     * Package-private test helper to clear the mappings.
     */
    static void clearMappings() {
        DIRECT_MAPPINGS.clear();
        TRANSFORMS.clear();
    }
}
