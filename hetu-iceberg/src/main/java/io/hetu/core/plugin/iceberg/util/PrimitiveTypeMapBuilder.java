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
package io.hetu.core.plugin.iceberg.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.prestosql.spi.type.ArrayType;
import io.prestosql.spi.type.MapType;
import io.prestosql.spi.type.RowType;
import io.prestosql.spi.type.Type;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static io.prestosql.spi.type.StandardTypes.ARRAY;
import static io.prestosql.spi.type.StandardTypes.MAP;
import static io.prestosql.spi.type.StandardTypes.ROW;
import static org.apache.iceberg.avro.AvroSchemaUtil.makeCompatibleName;

public class PrimitiveTypeMapBuilder
{
    private final ImmutableMap.Builder<List<String>, Type> builder = ImmutableMap.builder();

    private PrimitiveTypeMapBuilder() {}

    public static Map<List<String>, Type> makeTypeMap(List<Type> types, List<String> columnNames)
    {
        return new PrimitiveTypeMapBuilder().buildTypeMap(types, columnNames);
    }

    private Map<List<String>, Type> buildTypeMap(List<Type> types, List<String> columnNames)
    {
        for (int i = 0; i < types.size(); i++) {
            visitType(types.get(i), makeCompatibleName(columnNames.get(i)), ImmutableList.of());
        }
        return builder.build();
    }

    private void visitType(Type type, String name, List<String> parent)
    {
        if (ROW.equals(type.getTypeSignature().getBase())) {
            visitRowType((RowType) type, name, parent);
        }
        else if (MAP.equals(type.getTypeSignature().getBase())) {
            visitMapType((MapType) type, name, parent);
        }
        else if (ARRAY.equals(type.getTypeSignature().getBase())) {
            visitArrayType((ArrayType) type, name, parent);
        }
        else {
            builder.put(ImmutableList.<String>builder().addAll(parent).add(name).build(), type);
        }
    }

    private void visitArrayType(ArrayType type, String name, List<String> parent)
    {
        List<String> listParent = parent;
        listParent = ImmutableList.<String>builder().addAll(listParent).add(name).add("list").build();
        visitType(type.getElementType(), "element", listParent);
    }

    private void visitMapType(MapType type, String name, List<String> parent)
    {
        List<String> listParent = parent;
        listParent = ImmutableList.<String>builder().addAll(listParent).add(name).add("key_value").build();
        visitType(type.getKeyType(), "key", listParent);
        visitType(type.getValueType(), "value", listParent);
    }

    private void visitRowType(RowType type, String name, List<String> parent)
    {
        List<String> listParent = parent;
        listParent = ImmutableList.<String>builder().addAll(listParent).add(name).build();
        for (RowType.Field field : type.getFields()) {
            checkArgument(field.getName().isPresent(), "field in struct type doesn't have name");
            visitType(field.getType(), field.getName().get(), listParent);
        }
    }
}
