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
import io.prestosql.spi.type.TimeType;
import io.prestosql.spi.type.Timestamps;
import io.prestosql.spi.type.Type;

import static io.prestosql.spi.StandardErrorCode.NOT_SUPPORTED;
import static java.lang.String.format;

public class TimeMicrosColumnReader
        extends PrimitiveColumnReader
{
    public TimeMicrosColumnReader(RichColumnDescriptor field)
    {
        super(field);
    }

    @Override
    protected void readValue(BlockBuilder blockBuilder, Type type)
    {
        long picos = valuesReader.readLong() / Timestamps.PICOSECONDS_PER_NANOSECOND;
        if (type instanceof TimeType) {
            type.writeLong(blockBuilder, picos);
        }
        else {
            throw new TrinoException(NOT_SUPPORTED, format("Unsupported Trino column type (%s) for Parquet column (%s)", type, columnDescriptor));
        }
    }

    @Override
    protected void skipValue()
    {}
}
