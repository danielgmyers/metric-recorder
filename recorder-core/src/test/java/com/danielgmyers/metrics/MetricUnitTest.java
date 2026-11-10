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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * We have comprehensive testing for `isCompatible` because that logic is only
 * partially delegated to Indriya. `convert`, on the other hand,
 * checks our `isCompatible`, and then delegates the actual math to Indriya,
 * so in our tests here we just need to spot-check a few conversions
 * so that we know a) the plumbing to Indriya is correct, and b) it's actually
 * checking `isCompatible`.
 */
public class MetricUnitTest {

    private static final Set<MetricUnit> unconvertableUnits = Set.of(
        MetricUnit.None,
        MetricUnit.Count,
        MetricUnit.Percent,
        MetricUnit.Count_per_second
    );

    private static final Set<MetricUnit> dataSizes = Set.of(
            MetricUnit.Bits,
            MetricUnit.Bytes,
            MetricUnit.KB,
            MetricUnit.MB,
            MetricUnit.GB,
            MetricUnit.TB,
            MetricUnit.KiB,
            MetricUnit.MiB,
            MetricUnit.GiB,
            MetricUnit.TiB
    );

    private static final Set<MetricUnit> dataTransferRates = Set.of(
            MetricUnit.Bits_per_second,
            MetricUnit.Kbps,
            MetricUnit.Mbps,
            MetricUnit.Gbps,
            MetricUnit.Tbps,
            MetricUnit.Kibps,
            MetricUnit.Mibps,
            MetricUnit.Gibps,
            MetricUnit.Tibps,
            MetricUnit.Bytes_per_second,
            MetricUnit.KBps,
            MetricUnit.MBps,
            MetricUnit.GBps,
            MetricUnit.TBps,
            MetricUnit.KiBps,
            MetricUnit.MiBps,
            MetricUnit.GiBps,
            MetricUnit.TiBps
    );

    /**
     * This test exists to ensure that if a new category or value of metric
     * is added, the tests here are also modified appropriately.
     */
    @Test
    public void verifyAllUnitsAccountedForInListsAbove() {
        Set<MetricUnit> allUnits = new HashSet<>(Arrays.asList(MetricUnit.values()));

        allUnits.removeAll(unconvertableUnits);
        allUnits.removeAll(dataSizes);
        allUnits.removeAll(dataTransferRates);

        Assertions.assertTrue(allUnits.isEmpty());
    }

    private static Stream<Arguments> listUnconvertableUnits() {
        return unconvertableUnits.stream().map(Arguments::of);
    }

    private static Stream<Arguments> listDataSizes() {
        return dataSizes.stream().map(Arguments::of);
    }

    private static Stream<Arguments> listDataTransferRates() {
        return dataTransferRates.stream().map(Arguments::of);
    }

    @Test
    public void testAllUnitsCompatibleWithThemselves() {
        for (MetricUnit unit : MetricUnit.values()) {
            Assertions.assertTrue(unit.isCompatible(unit),
                                  unit.name() + " should have reported as compatible with itself");
        }
    }

    @Test
    public void testIsCompatibleAlwaysSymmetrical() {
        MetricUnit[] units = MetricUnit.values();
        for (int i = 0; i < units.length; i++) {
            for (int j = i + 1; j < units.length; j++) {
                Assertions.assertEquals(units[i].isCompatible(units[j]), units[j].isCompatible(units[i]),
                                        units[i].name() + ".isCompatible(" + units[j].name() + ") was not symmetrical");
            }
        }
    }

    @ParameterizedTest
    @MethodSource("listUnconvertableUnits")
    public void testIsCompatibleRejectsAllExceptItselfForUnconvertableUnits(MetricUnit targetUnit) {
        for (MetricUnit unit : MetricUnit.values()) {
            if (unit == targetUnit) {
                continue;
            }
            Assertions.assertFalse(unit.isCompatible(targetUnit),
                                   unit.name() + " should have reported as incompatible with " + targetUnit.name());
        }
    }

    @ParameterizedTest
    @MethodSource("listDataSizes")
    public void testDataSizesOnlyCompatibleWithDurations(MetricUnit targetUnit) {
        for (MetricUnit unit : MetricUnit.values()) {
            if (unit == targetUnit) {
                continue;
            }
            boolean shouldBeCompatible = dataSizes.contains(unit);
            String messageStr = (shouldBeCompatible ? "compatible" : "incompatible");
            Assertions.assertEquals(shouldBeCompatible, unit.isCompatible(targetUnit),
                                    unit.name() + " should have reported as " + messageStr + " with " + targetUnit.name());
        }
    }

    @ParameterizedTest
    @MethodSource("listDataTransferRates")
    public void testDataTransferRatesOnlyCompatibleWithDurations(MetricUnit targetUnit) {
        for (MetricUnit unit : MetricUnit.values()) {
            if (unit == targetUnit) {
                continue;
            }
            boolean shouldBeCompatible = dataTransferRates.contains(unit);
            String messageStr = (shouldBeCompatible ? "compatible" : "incompatible");
            Assertions.assertEquals(shouldBeCompatible, unit.isCompatible(targetUnit),
                                    unit.name() + " should have reported as " + messageStr + " with " + targetUnit.name());
        }
    }

    @Test
    public void testConvertToNumber() {
        Number vInt = 1;
        Assertions.assertEquals(1024, MetricUnit.KiB.convertTo(vInt, MetricUnit.Bytes));

        final Number vDouble = 16.0;
        Assertions.assertEquals(2L, MetricUnit.Bits.convertTo(vDouble, MetricUnit.Bytes).longValue());

        Assertions.assertThrows(InvalidMetricConversionException.class,
                                () -> MetricUnit.Gbps.convertTo(vDouble, MetricUnit.Bits));

        Assertions.assertThrows(InvalidMetricConversionException.class,
                                () -> MetricUnit.None.convertTo(vDouble, MetricUnit.Percent));
    }

    @Test
    public void testConvertToLong() {
        Assertions.assertEquals(1024L, MetricUnit.KiB.convertTo(1L, MetricUnit.Bytes));
        Assertions.assertEquals(2L, MetricUnit.Bits.convertTo(16L, MetricUnit.Bytes));

        Assertions.assertThrows(InvalidMetricConversionException.class,
                                () -> MetricUnit.Gbps.convertTo(1L, MetricUnit.Bits));

        Assertions.assertThrows(InvalidMetricConversionException.class,
                                () -> MetricUnit.None.convertTo(1L, MetricUnit.Percent));
    }

    @Test
    public void testConvertToDouble() {
        Assertions.assertEquals(1024.0, MetricUnit.KiB.convertTo(1.0, MetricUnit.Bytes));
        Assertions.assertEquals(2.0, MetricUnit.Bits.convertTo(16.0, MetricUnit.Bytes));

        Assertions.assertThrows(InvalidMetricConversionException.class,
                                () -> MetricUnit.Gbps.convertTo(1.0, MetricUnit.Bits));

        Assertions.assertThrows(InvalidMetricConversionException.class,
                                () -> MetricUnit.None.convertTo(1.0, MetricUnit.Percent));
    }

    /**
     * Just as another spot check, this verifies that the multiple conversion steps
     * from Megabytes Per Second to Kibibits Per Second are handled correctly.
     */
    @Test
    public void testConvertHandlesMegabytesPerSecondToKibibitsPerSecond() {
        // here's the expected conversion math, keeping in mind that MB is powers of 10 and Kib is powers of 2.
        // 2 MBps -> 2000000 bytes per second -> 16000000 bits per second -> 15625 Kibps
        Assertions.assertEquals(15625L, MetricUnit.MBps.convertTo(2L, MetricUnit.Kibps));
    }
}
