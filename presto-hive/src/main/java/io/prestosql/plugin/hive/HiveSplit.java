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
package io.prestosql.plugin.hive;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.prestosql.plugin.hive.HiveBucketing.BucketingVersion;
import io.prestosql.spi.HostAddress;
import org.openjdk.jol.info.ClassLayout;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Properties;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static io.prestosql.spi.util.SizeOf.INTEGER_INSTANCE_SIZE;
import static io.prestosql.spi.util.SizeOf.LONG_INSTANCE_SIZE;
import static io.prestosql.spi.util.SizeOf.estimatedSizeOf;
import static io.prestosql.spi.util.SizeOf.sizeOf;
import static java.util.Objects.requireNonNull;

public class HiveSplit
{
    private static final int INSTANCE_SIZE = ClassLayout.parseClass(HiveSplit.class).instanceSize();

    private final String path;
    private final long start;
    private final long length;
    private final long fileSize;
    private final long lastModifiedTime;
    private final Properties schema;
    private final List<HivePartitionKey> partitionKeys;
    private final List<HostAddress> addresses;
    private final String database;
    private final String table;
    private final String partitionName;
    private final OptionalInt bucketNumber;
    private final boolean forceLocalScheduling;
    private final Map<Integer, HiveType> columnCoercions; // key: hiveColumnIndex
    private final Optional<BucketConversion> bucketConversion;
    private final boolean s3SelectPushdownEnabled;
    private final Optional<DeleteDeltaLocations> deleteDeltaLocations;
    private final Optional<Long> startRowOffsetOfFile;
    private final boolean cacheable;
    private final Map<String, String> customSplitInfo;

    @JsonCreator
    public HiveSplit(
            @JsonProperty("database") String database,
            @JsonProperty("table") String table,
            @JsonProperty("partitionName") String partitionName,
            @JsonProperty("path") String path,
            @JsonProperty("start") long start,
            @JsonProperty("length") long length,
            @JsonProperty("fileSize") long fileSize,
            @JsonProperty("lastModifiedTime") long lastModifiedTime,
            @JsonProperty("schema") Properties schema,
            @JsonProperty("partitionKeys") List<HivePartitionKey> partitionKeys,
            @JsonProperty("addresses") List<HostAddress> addresses,
            @JsonProperty("bucketNumber") OptionalInt bucketNumber,
            @JsonProperty("forceLocalScheduling") boolean forceLocalScheduling,
            @JsonProperty("columnCoercions") Map<Integer, HiveType> columnCoercions,
            @JsonProperty("bucketConversion") Optional<BucketConversion> bucketConversion,
            @JsonProperty("s3SelectPushdownEnabled") boolean s3SelectPushdownEnabled,
            @JsonProperty("deleteDeltaLocations") Optional<DeleteDeltaLocations> deleteDeltaLocations,
            @JsonProperty("validWriteIdList") Optional<Long> startRowOffsetOfFile,
            @JsonProperty("cacheable") boolean cacheable,
            @JsonProperty("customSplitInfo") Map<String, String> customSplitInfo)
    {
        checkArgument(start >= 0, "start must be positive");
        checkArgument(length >= 0, "length must be positive");
        checkArgument(fileSize >= 0, "fileSize must be positive");
        checkArgument(lastModifiedTime >= 0, "lastModifiedTime must be positive");
        requireNonNull(database, "database is null");
        requireNonNull(table, "table is null");
        requireNonNull(partitionName, "partitionName is null");
        requireNonNull(path, "path is null");
        requireNonNull(schema, "schema is null");
        requireNonNull(partitionKeys, "partitionKeys is null");
        requireNonNull(addresses, "addresses is null");
        requireNonNull(bucketNumber, "bucketNumber is null");
        requireNonNull(columnCoercions, "columnCoercions is null");
        requireNonNull(bucketConversion, "bucketConversion is null");
        requireNonNull(deleteDeltaLocations, "deleteDeltaLocations is null");

        this.database = database;
        this.table = table;
        this.partitionName = partitionName;
        this.path = path;
        this.start = start;
        this.length = length;
        this.fileSize = fileSize;
        this.lastModifiedTime = lastModifiedTime;
        this.schema = schema;
        this.partitionKeys = ImmutableList.copyOf(partitionKeys);
        this.addresses = ImmutableList.copyOf(addresses);
        this.bucketNumber = bucketNumber;
        this.forceLocalScheduling = forceLocalScheduling;
        this.columnCoercions = columnCoercions;
        this.bucketConversion = bucketConversion;
        this.s3SelectPushdownEnabled = s3SelectPushdownEnabled;
        this.deleteDeltaLocations = deleteDeltaLocations;
        this.startRowOffsetOfFile = startRowOffsetOfFile;
        this.cacheable = cacheable;
        this.customSplitInfo = ImmutableMap.copyOf(requireNonNull(customSplitInfo, "customSplitInfo is null"));
    }

    @JsonProperty
    public String getDatabase()
    {
        return database;
    }

    @JsonProperty
    public String getTable()
    {
        return table;
    }

    @JsonProperty
    public String getPartitionName()
    {
        return partitionName;
    }

    @JsonProperty
    public String getPath()
    {
        return path;
    }

    @JsonProperty
    public long getStart()
    {
        return start;
    }

    @JsonProperty
    public long getLength()
    {
        return length;
    }

    @JsonProperty
    public long getFileSize()
    {
        return fileSize;
    }

    @JsonProperty
    public Properties getSchema()
    {
        return schema;
    }

    @JsonProperty
    public List<HivePartitionKey> getPartitionKeys()
    {
        return partitionKeys;
    }

    @JsonProperty
    public List<HostAddress> getAddresses()
    {
        return addresses;
    }

    @JsonProperty
    public OptionalInt getBucketNumber()
    {
        return bucketNumber;
    }

    @JsonProperty
    public boolean isForceLocalScheduling()
    {
        return forceLocalScheduling;
    }

    @JsonProperty
    public Map<Integer, HiveType> getColumnCoercions()
    {
        return columnCoercions;
    }

    @JsonProperty
    public Optional<BucketConversion> getBucketConversion()
    {
        return bucketConversion;
    }

    public boolean isRemotelyAccessible()
    {
        return !forceLocalScheduling;
    }

    @JsonProperty
    public boolean isS3SelectPushdownEnabled()
    {
        return s3SelectPushdownEnabled;
    }

    @JsonProperty
    public Optional<DeleteDeltaLocations> getDeleteDeltaLocations()
    {
        return deleteDeltaLocations;
    }

    //presto: default method to get split path for Split filter
    public String getFilePath()
    {
        return path;
    }

    public long getStartIndex()
    {
        return start;
    }

    public long getEndIndex()
    {
        return start + length;
    }

    @JsonProperty
    public long getLastModifiedTime()
    {
        return lastModifiedTime;
    }

    @JsonProperty
    public boolean isCacheable()
    {
        return cacheable;
    }

    @JsonProperty
    public Map<String, String> getCustomSplitInfo()
    {
        return customSplitInfo;
    }

    public Object getInfo()
    {
        return ImmutableMap.builder()
                .put("path", path)
                .put("start", start)
                .put("length", length)
                .put("fileSize", fileSize)
                .put("lastModifiedTime", lastModifiedTime)
                .put("hosts", addresses)
                .put("database", database)
                .put("table", table)
                .put("forceLocalScheduling", forceLocalScheduling)
                .put("partitionName", partitionName)
                .put("s3SelectPushdownEnabled", s3SelectPushdownEnabled)
                .put("cacheable", cacheable)
                .build();
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .addValue(path)
                .addValue(start)
                .addValue(length)
                .addValue(fileSize)
                .toString();
    }

    public Optional<Long> getStartRowOffsetOfFile()
    {
        return startRowOffsetOfFile;
    }

    public long getRetainedSizeInBytes()
    {
        return INSTANCE_SIZE
                + estimatedSizeOf(path)
                + estimatedSizeOf(schema, key -> estimatedSizeOf((String) key), value -> estimatedSizeOf((String) value))
                + estimatedSizeOf(partitionKeys, HivePartitionKey::getEstimatedSizeInBytes)
                + estimatedSizeOf(addresses, HostAddress::getRetainedSizeInBytes)
                + estimatedSizeOf(database)
                + estimatedSizeOf(table)
                + estimatedSizeOf(partitionName)
                + sizeOf(bucketNumber)
                + sizeOf(bucketConversion, BucketConversion::getRetainedSizeInBytes)
                + estimatedSizeOf(columnCoercions, (Integer key) -> INTEGER_INSTANCE_SIZE, HiveType::getRetainedSizeInBytes)
                + sizeOf(startRowOffsetOfFile, value -> LONG_INSTANCE_SIZE)
                + sizeOf(deleteDeltaLocations, DeleteDeltaLocations::getRetainedSizeInBytes)
                + estimatedSizeOf(customSplitInfo, key -> estimatedSizeOf(key), value -> estimatedSizeOf(value));
    }

    public static class BucketConversion
    {
        private static final int INSTANCE_SIZE = ClassLayout.parseClass(BucketConversion.class).instanceSize();

        private final BucketingVersion bucketingVersion;
        private final int tableBucketCount;
        private final int partitionBucketCount;
        private final List<HiveColumnHandle> bucketColumnNames;
        // bucketNumber is needed, but can be found in bucketNumber field of HiveSplit.

        @JsonCreator
        public BucketConversion(
                @JsonProperty("bucketingVersion") BucketingVersion bucketingVersion,
                @JsonProperty("tableBucketCount") int tableBucketCount,
                @JsonProperty("partitionBucketCount") int partitionBucketCount,
                @JsonProperty("bucketColumnHandles") List<HiveColumnHandle> bucketColumnHandles)
        {
            this.bucketingVersion = requireNonNull(bucketingVersion, "bucketingVersion is null");
            this.tableBucketCount = tableBucketCount;
            this.partitionBucketCount = partitionBucketCount;
            this.bucketColumnNames = requireNonNull(bucketColumnHandles, "bucketColumnHandles is null");
        }

        @JsonProperty
        public BucketingVersion getBucketingVersion()
        {
            return bucketingVersion;
        }

        @JsonProperty
        public int getTableBucketCount()
        {
            return tableBucketCount;
        }

        @JsonProperty
        public int getPartitionBucketCount()
        {
            return partitionBucketCount;
        }

        @JsonProperty
        public List<HiveColumnHandle> getBucketColumnHandles()
        {
            return bucketColumnNames;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            BucketConversion that = (BucketConversion) o;
            return tableBucketCount == that.tableBucketCount &&
                    partitionBucketCount == that.partitionBucketCount &&
                    Objects.equals(bucketColumnNames, that.bucketColumnNames);
        }

        public long getRetainedSizeInBytes()
        {
            return INSTANCE_SIZE
                    + estimatedSizeOf(bucketColumnNames, HiveColumnHandle::getRetainedSizeInBytes);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(tableBucketCount, partitionBucketCount, bucketColumnNames);
        }
    }
}
