/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.prestosql.parquet.reader;

import io.prestosql.parquet.RichColumnDescriptor;
import io.prestosql.spi.TrinoException;
import io.prestosql.spi.block.BlockBuilder;
import io.prestosql.spi.type.LongTimestamp;
import io.prestosql.spi.type.LongTimestampWithTimeZone;
import io.prestosql.spi.type.Timestamps;
import io.prestosql.spi.type.Type;

import static io.prestosql.spi.StandardErrorCode.NOT_SUPPORTED;
import static io.prestosql.spi.type.BigintType.BIGINT;
import static io.prestosql.spi.type.DateTimeEncoding.packDateTimeWithZone;
import static io.prestosql.spi.type.TimeZoneKey.UTC_KEY;
import static io.prestosql.spi.type.TimestampType.TIMESTAMP_MICROS;
import static io.prestosql.spi.type.TimestampType.TIMESTAMP_MILLIS;
import static io.prestosql.spi.type.TimestampType.TIMESTAMP_NANOS;
import static io.prestosql.spi.type.TimestampWithTimeZoneType.TIMESTAMP_TZ_MICROS;
import static io.prestosql.spi.type.TimestampWithTimeZoneType.TIMESTAMP_TZ_MILLIS;
import static io.prestosql.spi.type.TimestampWithTimeZoneType.TIMESTAMP_TZ_NANOS;
import static io.prestosql.spi.type.Timestamps.MICROSECONDS_PER_MILLISECOND;
import static io.prestosql.spi.type.Timestamps.PICOSECONDS_PER_MICROSECOND;
import static java.lang.Math.floorDiv;
import static java.lang.Math.floorMod;
import static java.lang.Math.toIntExact;
import static java.lang.String.format;

public class TimestampMicrosColumnReader
        extends PrimitiveColumnReader
{
    public TimestampMicrosColumnReader(RichColumnDescriptor field)
    {
        super(field);
    }

    @Override
    protected void readValue(BlockBuilder blockBuilder, Type type)
    {
        long epochMicros = valuesReader.readLong();
        // TODO: specialize the class at creation time
        if (type == TIMESTAMP_MILLIS || type.getTypeId().equals(TIMESTAMP_MILLIS.getTypeId())) {
            type.writeLong(blockBuilder, epochMicros / MICROSECONDS_PER_MILLISECOND);
        }
        else if (type == TIMESTAMP_MICROS || type.getTypeId().equals(TIMESTAMP_MICROS.getTypeId())) {
            type.writeLong(blockBuilder, epochMicros);
        }
        else if (type == TIMESTAMP_NANOS || type.getTypeId().equals(TIMESTAMP_NANOS.getTypeId())) {
            type.writeObject(blockBuilder, new LongTimestamp(epochMicros, 0));
        }
        else if (type == TIMESTAMP_TZ_MILLIS || type.getTypeId().equals(TIMESTAMP_TZ_MILLIS.getTypeId())) {
            long epochMillis = Timestamps.round(epochMicros, 3) / MICROSECONDS_PER_MILLISECOND;
            type.writeLong(blockBuilder, packDateTimeWithZone(epochMillis, UTC_KEY));
        }
        else if (type == TIMESTAMP_TZ_MICROS || type == TIMESTAMP_TZ_NANOS || type.getTypeId().equals(TIMESTAMP_TZ_NANOS.getTypeId())) {
            long epochMillis = floorDiv(epochMicros, MICROSECONDS_PER_MILLISECOND);
            int picosOfMillis = toIntExact(floorMod(epochMicros, MICROSECONDS_PER_MILLISECOND)) * PICOSECONDS_PER_MICROSECOND;
            type.writeObject(blockBuilder, LongTimestampWithTimeZone.fromEpochMillisAndFraction(epochMillis, picosOfMillis, UTC_KEY));
        }
        else if (type == BIGINT || type.getTypeId().equals(BIGINT.getTypeId())) {
            type.writeLong(blockBuilder, epochMicros);
        }
        else {
            throw new TrinoException(NOT_SUPPORTED, format("Unsupported Trino column type (%s) for Parquet column (%s)", type, columnDescriptor));
        }
    }

    @Override
    protected void skipValue()
    {}
}
