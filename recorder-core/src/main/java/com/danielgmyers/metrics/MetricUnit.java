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

import javax.measure.BinaryPrefix;
import javax.measure.MetricPrefix;
import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Dimensionless;

import tech.units.indriya.AbstractUnit;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

/**
 * Defines a common set of units that would typically be associated with metrics.
 * Some backends may not support all types or granularities, but where possible,
 * MetricRecorder implementations should convert the provided metric unit into
 * a supported metric, and MUST document where it deviates.
 * <p/>
 * The initial reference list is based on those supported by Amazon CloudWatch
 * as well as those given as example base units by the Prometheus documentation.
 * <p/>
 * For convenience in implementing recorders, this class provides utility methods
 * for converting values between compatible units.
 * <p/>
 * Note that durations are explicitly not supported here, since MetricRecorder
 * models durations directly with the native Java Duration type.
 */
public enum MetricUnit {

    // For unitless metrics
    None("None", null),
    Count("Count", null),
    Percent("percent", CustomUnits.PERCENT),

    // For unitless rates
    Count_per_second("per second", Units.HERTZ),

    // For data sizes
    Bits("bits", CustomUnits.BIT),
    Bytes("bytes", CustomUnits.BYTE),

    // For data sizes: base 10
    KB("kilobytes", CustomUnits.BYTE.prefix(MetricPrefix.KILO)),
    MB("megabytes", CustomUnits.BYTE.prefix(MetricPrefix.MEGA)),
    GB("gigabytes", CustomUnits.BYTE.prefix(MetricPrefix.GIGA)),
    TB("terabytes", CustomUnits.BYTE.prefix(MetricPrefix.TERA)),

    // For data sizes: base 2
    KiB("kibibytes", CustomUnits.BYTE.prefix(BinaryPrefix.KIBI)),
    MiB("mebibytes", CustomUnits.BYTE.prefix(BinaryPrefix.MEBI)),
    GiB("gibibytes", CustomUnits.BYTE.prefix(BinaryPrefix.GIBI)),
    TiB("tebibytes", CustomUnits.BYTE.prefix(BinaryPrefix.TEBI)),

    // For data transfer rates
    Bits_per_second("bits/second", CustomUnits.BITS_PER_SECOND),
    Bytes_per_second("bytes/second", CustomUnits.BYTES_PER_SECOND),

    // For data transfer rates: base 10
    Kbps("kilobits/second", CustomUnits.BITS_PER_SECOND.prefix(MetricPrefix.KILO)),
    Mbps("megabits/second", CustomUnits.BITS_PER_SECOND.prefix(MetricPrefix.MEGA)),
    Gbps("gigabits/second", CustomUnits.BITS_PER_SECOND.prefix(MetricPrefix.GIGA)),
    Tbps("terabits/second", CustomUnits.BITS_PER_SECOND.prefix(MetricPrefix.TERA)),

    KBps("kilobytes/second", CustomUnits.BYTES_PER_SECOND.prefix(MetricPrefix.KILO)),
    MBps("megabytes/second", CustomUnits.BYTES_PER_SECOND.prefix(MetricPrefix.MEGA)),
    GBps("gigabytes/second", CustomUnits.BYTES_PER_SECOND.prefix(MetricPrefix.GIGA)),
    TBps("terabytes/second", CustomUnits.BYTES_PER_SECOND.prefix(MetricPrefix.TERA)),

    // For data transfer rates: base 2
    Kibps("kibibits/second", CustomUnits.BITS_PER_SECOND.prefix(BinaryPrefix.KIBI)),
    Mibps("mebibits/second", CustomUnits.BITS_PER_SECOND.prefix(BinaryPrefix.MEBI)),
    Gibps("gibibits/second", CustomUnits.BITS_PER_SECOND.prefix(BinaryPrefix.GIBI)),
    Tibps("tebibits/second", CustomUnits.BITS_PER_SECOND.prefix(BinaryPrefix.TEBI)),

    KiBps("kibibytes/second", CustomUnits.BYTES_PER_SECOND.prefix(BinaryPrefix.KIBI)),
    MiBps("mebibytes/second", CustomUnits.BYTES_PER_SECOND.prefix(BinaryPrefix.MEBI)),
    GiBps("gibibytes/second", CustomUnits.BYTES_PER_SECOND.prefix(BinaryPrefix.GIBI)),
    TiBps("tebibytes/second", CustomUnits.BYTES_PER_SECOND.prefix(BinaryPrefix.TEBI)),

    ;

    private final String description;
    private final Unit<?> unit;

    MetricUnit(String description, Unit<?> unit) {
        this.description = description;
        this.unit = unit;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCompatible(MetricUnit targetUnit) {
        if (this == targetUnit) {
            return true;
        }
        if (this == None || this == Count || this == Percent || this == Count_per_second) {
            return false;
        }
        if (targetUnit == None || targetUnit == Count || targetUnit == Percent || targetUnit == Count_per_second) {
            return false;
        }
        return this.unit.isCompatible(targetUnit.unit);
    }

    /**
     * Converts the `value` quantity of this unit to the target unit, if compatible.
     * @throws InvalidMetricConversionException if this and the target unit are not compatible.
     */
    public Number convertTo(Number value, MetricUnit targetUnit) {
        return convert(value, this, targetUnit);
    }

    /**
     * Converts the `value` quantity of this unit to the target unit, if compatible.
     * @throws InvalidMetricConversionException if this and the target unit are not compatible.
     */
    public long convertTo(long value, MetricUnit targetUnit) {
        return convert(value, this, targetUnit).longValue();
    }

    /**
     * Converts the `value` quantity of this unit to the target unit, if compatible.
     * @throws InvalidMetricConversionException if this and the target unit are not compatible.
     */
    public double convertTo(double value, MetricUnit targetUnit) {
        return convert(value, this, targetUnit).doubleValue();
    }

    private static <T extends Quantity<T>> Number convert(Number sourceAmount, MetricUnit sourceUnit, MetricUnit targetUnit) {
        if (sourceUnit == targetUnit) {
            return sourceAmount;
        }
        if (sourceUnit.unit == null || targetUnit.unit == null || !sourceUnit.unit.isCompatible(targetUnit.unit)) {
            throw new InvalidMetricConversionException(sourceUnit, targetUnit);
        }

        // the null checks and `isCompatible` check above guarantee that these casts result in a valid conversion.
        @SuppressWarnings("unchecked")
        Unit<T> castedTarget = (Unit<T>)(targetUnit.unit);

        @SuppressWarnings("unchecked")
        Quantity<T> castedSource = (Quantity<T>)Quantities.getQuantity(sourceAmount, sourceUnit.unit);

        return castedSource.to(castedTarget).getValue();
    }

    static final class CustomUnits {
        public static final Unit<Dimensionless> PERCENT = AbstractUnit.ONE.divide(100.0);

        public static final Unit<Dimensionless> BYTE = AbstractUnit.ONE;
        public static final Unit<Dimensionless> BIT = BYTE.divide(8);

        public interface DataTransferRate extends Quantity<DataTransferRate> {
        }

        public static final Unit<DataTransferRate> BYTES_PER_SECOND = BYTE.divide(Units.SECOND).asType(DataTransferRate.class);
        public static final Unit<DataTransferRate> BITS_PER_SECOND = BIT.divide(Units.SECOND).asType(DataTransferRate.class);
    }
}
