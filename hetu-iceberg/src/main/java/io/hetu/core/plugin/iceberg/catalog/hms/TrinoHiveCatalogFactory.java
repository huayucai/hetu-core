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
package io.hetu.core.plugin.iceberg.catalog.hms;

import io.hetu.core.plugin.iceberg.IcebergConfig;
import io.hetu.core.plugin.iceberg.IcebergSecurityConfig;
import io.hetu.core.plugin.iceberg.catalog.IcebergTableOperationsProvider;
import io.hetu.core.plugin.iceberg.catalog.TrinoCatalog;
import io.hetu.core.plugin.iceberg.catalog.TrinoCatalogFactory;
import io.prestosql.plugin.hive.HdfsEnvironment;
import io.prestosql.plugin.hive.HiveConfig;
import io.prestosql.plugin.hive.NodeVersion;
import io.prestosql.plugin.hive.metastore.HiveMetastoreFactory;
import io.prestosql.spi.connector.CatalogName;
import io.prestosql.spi.security.ConnectorIdentity;
import io.prestosql.spi.type.TypeManager;

import javax.inject.Inject;

import java.util.Optional;

import static io.hetu.core.plugin.iceberg.IcebergSecurityConfig.IcebergSecurity.SYSTEM;
import static io.prestosql.plugin.hive.metastore.CachingHiveMetastore.memoizeMetastore;
import static java.util.Objects.requireNonNull;

public class TrinoHiveCatalogFactory
        implements TrinoCatalogFactory
{
    private final CatalogName catalogName;
    private final HiveMetastoreFactory metastoreFactory;
    private final HdfsEnvironment hdfsEnvironment;
    private final TypeManager typeManager;
    private final IcebergTableOperationsProvider tableOperationsProvider;
    private final String trinoVersion;
    private final boolean isUniqueTableLocation;
    private final boolean isUsingSystemSecurity;
    private final boolean deleteSchemaLocationsFallback;

    @Inject
    public TrinoHiveCatalogFactory(
            IcebergConfig config,
            CatalogName catalogName,
            HiveMetastoreFactory metastoreFactory,
            HdfsEnvironment hdfsEnvironment,
            TypeManager typeManager,
            IcebergTableOperationsProvider tableOperationsProvider,
            NodeVersion nodeVersion,
            IcebergSecurityConfig securityConfig,
            HiveConfig hiveConfig)
    {
        this.catalogName = requireNonNull(catalogName, "catalogName is null");
        this.metastoreFactory = requireNonNull(metastoreFactory, "metastoreFactory is null");
        this.hdfsEnvironment = requireNonNull(hdfsEnvironment, "hdfsEnvironment is null");
        this.typeManager = requireNonNull(typeManager, "typeManager is null");
        this.tableOperationsProvider = requireNonNull(tableOperationsProvider, "tableOperationProvider is null");
        this.trinoVersion = requireNonNull(nodeVersion, "trinoVersion is null").toString();
        requireNonNull(config, "config is null");
        this.isUniqueTableLocation = config.isUniqueTableLocation();
        requireNonNull(securityConfig, "securityConfig is null");
        this.isUsingSystemSecurity = securityConfig.getSecuritySystem() == SYSTEM;
        requireNonNull(hiveConfig, "hiveConfig is null");
        this.deleteSchemaLocationsFallback = hiveConfig.isDeleteSchemaLocationsFallback();
    }

    @Override
    public TrinoCatalog create(ConnectorIdentity identity)
    {
        return new TrinoHiveCatalog(
                catalogName,
                memoizeMetastore(metastoreFactory.createMetastore(Optional.of(identity)), 1000),
                hdfsEnvironment,
                typeManager,
                tableOperationsProvider,
                trinoVersion,
                isUniqueTableLocation,
                isUsingSystemSecurity,
                deleteSchemaLocationsFallback);
    }
}
