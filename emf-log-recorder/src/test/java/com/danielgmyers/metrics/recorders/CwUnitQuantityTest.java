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

import com.danielgmyers.metrics.MetricUnit;
import com.danielgmyers.metrics.recorders.config.EmfLogRecorderConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.cloudwatchlogs.emf.model.Unit;

public class CwUnitQuantityTest {

    EmfLogRecorderConfig config;

    @BeforeEach
    public void setup() {
        config = new EmfLogRecorderConfig("metricNamespace");
    }

    @AfterEach
    public void cleanup() {
        CwUnitQuantity.clearMappings();
    }

    @Test
    public void allTransformValuesMustExistInDirectMappings_DataUnitBase10() {
        config.setIsCloudWatchDataUnitBase10(true);
        CwUnitQuantity.populateMappings(config);

        Assertions.assertTrue(CwUnitQuantity.DIRECT_MAPPINGS.keySet().containsAll(CwUnitQuantity.TRANSFORMS.values()));

        CwUnitQuantity.clearMappings();
        config.setIsCloudWatchDataUnitBase10(false);
        CwUnitQuantity.populateMappings(config);

        Assertions.assertTrue(CwUnitQuantity.DIRECT_MAPPINGS.keySet().containsAll(CwUnitQuantity.TRANSFORMS.values()));
    }

    @Test
    public void allTransformValuesMustExistInDirectMappings_DataUnitBase2() {
        config.setIsCloudWatchDataUnitBase10(false);
        CwUnitQuantity.populateMappings(config);

        Assertions.assertTrue(CwUnitQuantity.DIRECT_MAPPINGS.keySet().containsAll(CwUnitQuantity.TRANSFORMS.values()));
    }

    @Test
    public void testCalculateKeepsDirectMappings_Base10DataUnit() {
        config.setIsCloudWatchDataUnitBase10(true);

        // We don't actually care about the calculation, that's tested by the MetricUnit tests.
        // We're just verifying the mapping to CW units.
        verifyUnitMapping(MetricUnit.None, Unit.NONE);
        verifyUnitMapping(MetricUnit.Percent, Unit.PERCENT);
        verifyUnitMapping(MetricUnit.Count, Unit.COUNT);
        verifyUnitMapping(MetricUnit.Count_per_second, Unit.COUNT_SECOND);
        verifyUnitMapping(MetricUnit.Bits, Unit.BITS);
        verifyUnitMapping(MetricUnit.Bits_per_second, Unit.BITS_SECOND);
        verifyUnitMapping(MetricUnit.Bytes, Unit.BYTES);
        verifyUnitMapping(MetricUnit.Bytes_per_second, Unit.BYTES_SECOND);

        // when using base10 data units, base10 data should be passed through as-is, and base2 data should be transformed.
        verifyUnitMapping(MetricUnit.KB, Unit.KILOBYTES);
        verifyUnitMapping(MetricUnit.MB, Unit.MEGABYTES);
        verifyUnitMapping(MetricUnit.GB, Unit.GIGABYTES);
        verifyUnitMapping(MetricUnit.TB, Unit.TERABYTES);

        verifyUnitMapping(MetricUnit.Kbps, Unit.KILOBITS_SECOND);
        verifyUnitMapping(MetricUnit.Mbps, Unit.MEGABITS_SECOND);
        verifyUnitMapping(MetricUnit.Gbps, Unit.GIGABITS_SECOND);
        verifyUnitMapping(MetricUnit.Tbps, Unit.TERABITS_SECOND);

        verifyUnitMapping(MetricUnit.KBps, Unit.KILOBYTES_SECOND);
        verifyUnitMapping(MetricUnit.MBps, Unit.MEGABYTES_SECOND);
        verifyUnitMapping(MetricUnit.GBps, Unit.GIGABYTES_SECOND);
        verifyUnitMapping(MetricUnit.TBps, Unit.TERABYTES_SECOND);

        verifyTransform(MetricUnit.KiB, MetricUnit.KB, Unit.KILOBYTES);
        verifyTransform(MetricUnit.MiB, MetricUnit.MB, Unit.MEGABYTES);
        verifyTransform(MetricUnit.GiB, MetricUnit.GB, Unit.GIGABYTES);
        verifyTransform(MetricUnit.TiB, MetricUnit.TB, Unit.TERABYTES);

        verifyTransform(MetricUnit.Kibps, MetricUnit.Kbps, Unit.KILOBITS_SECOND);
        verifyTransform(MetricUnit.Mibps, MetricUnit.Mbps, Unit.MEGABITS_SECOND);
        verifyTransform(MetricUnit.Gibps, MetricUnit.Gbps, Unit.GIGABITS_SECOND);
        verifyTransform(MetricUnit.Tibps, MetricUnit.Tbps, Unit.TERABITS_SECOND);

        verifyTransform(MetricUnit.KiBps, MetricUnit.KBps, Unit.KILOBYTES_SECOND);
        verifyTransform(MetricUnit.MiBps, MetricUnit.MBps, Unit.MEGABYTES_SECOND);
        verifyTransform(MetricUnit.GiBps, MetricUnit.GBps, Unit.GIGABYTES_SECOND);
        verifyTransform(MetricUnit.TiBps, MetricUnit.TBps, Unit.TERABYTES_SECOND);
    }

    @Test
    public void testCalculateKeepsDirectMappings_Base2DataUnit() {
        config.setIsCloudWatchDataUnitBase10(false);

        // We don't actually care about the calculation, that's tested by the MetricUnit tests.
        // We're just verifying the mapping to CW units.
        verifyUnitMapping(MetricUnit.None, Unit.NONE);
        verifyUnitMapping(MetricUnit.Percent, Unit.PERCENT);
        verifyUnitMapping(MetricUnit.Count, Unit.COUNT);
        verifyUnitMapping(MetricUnit.Count_per_second, Unit.COUNT_SECOND);
        verifyUnitMapping(MetricUnit.Bits, Unit.BITS);
        verifyUnitMapping(MetricUnit.Bits_per_second, Unit.BITS_SECOND);
        verifyUnitMapping(MetricUnit.Bytes, Unit.BYTES);
        verifyUnitMapping(MetricUnit.Bytes_per_second, Unit.BYTES_SECOND);

        // when using base2 data units, base2 data should be passed through as-is, and base10 data should be transformed.
        verifyUnitMapping(MetricUnit.KiB, Unit.KILOBYTES);
        verifyUnitMapping(MetricUnit.MiB, Unit.MEGABYTES);
        verifyUnitMapping(MetricUnit.GiB, Unit.GIGABYTES);
        verifyUnitMapping(MetricUnit.TiB, Unit.TERABYTES);

        verifyUnitMapping(MetricUnit.Kibps, Unit.KILOBITS_SECOND);
        verifyUnitMapping(MetricUnit.Mibps, Unit.MEGABITS_SECOND);
        verifyUnitMapping(MetricUnit.Gibps, Unit.GIGABITS_SECOND);
        verifyUnitMapping(MetricUnit.Tibps, Unit.TERABITS_SECOND);

        verifyUnitMapping(MetricUnit.KiBps, Unit.KILOBYTES_SECOND);
        verifyUnitMapping(MetricUnit.MiBps, Unit.MEGABYTES_SECOND);
        verifyUnitMapping(MetricUnit.GiBps, Unit.GIGABYTES_SECOND);
        verifyUnitMapping(MetricUnit.TiBps, Unit.TERABYTES_SECOND);

        verifyTransform(MetricUnit.KB, MetricUnit.KiB, Unit.KILOBYTES);
        verifyTransform(MetricUnit.MB, MetricUnit.MiB, Unit.MEGABYTES);
        verifyTransform(MetricUnit.GB, MetricUnit.GiB, Unit.GIGABYTES);
        verifyTransform(MetricUnit.TB, MetricUnit.TiB, Unit.TERABYTES);

        verifyTransform(MetricUnit.Kbps, MetricUnit.Kibps, Unit.KILOBITS_SECOND);
        verifyTransform(MetricUnit.Mbps, MetricUnit.Mibps, Unit.MEGABITS_SECOND);
        verifyTransform(MetricUnit.Gbps, MetricUnit.Gibps, Unit.GIGABITS_SECOND);
        verifyTransform(MetricUnit.Tbps, MetricUnit.Tibps, Unit.TERABITS_SECOND);

        verifyTransform(MetricUnit.KBps, MetricUnit.KiBps, Unit.KILOBYTES_SECOND);
        verifyTransform(MetricUnit.MBps, MetricUnit.MiBps, Unit.MEGABYTES_SECOND);
        verifyTransform(MetricUnit.GBps, MetricUnit.GiBps, Unit.GIGABYTES_SECOND);
        verifyTransform(MetricUnit.TBps, MetricUnit.TiBps, Unit.TERABYTES_SECOND);
    }

    private void verifyUnitMapping(MetricUnit sourceUnit, Unit targetCwUnit) {
        CwUnitQuantity q = CwUnitQuantity.calculate(42.0, sourceUnit, config);
        Assertions.assertEquals(42.0, q.getValue());
        Assertions.assertEquals(targetCwUnit, q.getCwUnit());
    }

    private void verifyTransform(MetricUnit sourceUnit, MetricUnit targetUnit, Unit targetCwUnit) {
        CwUnitQuantity q = CwUnitQuantity.calculate(42.0, sourceUnit, config);
        Assertions.assertEquals(sourceUnit.convertTo(42.0, targetUnit), q.getValue());
        Assertions.assertEquals(targetCwUnit, q.getCwUnit());
    }
}
