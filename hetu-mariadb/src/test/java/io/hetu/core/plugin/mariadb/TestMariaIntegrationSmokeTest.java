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
package io.hetu.core.plugin.mariadb;

import io.airlift.testing.mysql.TestingMySqlServer;
import io.prestosql.Session;
import io.prestosql.testing.MaterializedResult;
import io.prestosql.testing.MaterializedRow;
import io.prestosql.tests.AbstractTestIntegrationSmokeTest;
import org.intellij.lang.annotations.Language;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static com.google.common.collect.Iterables.getOnlyElement;
import static io.airlift.tpch.TpchTable.ORDERS;
import static io.hetu.core.plugin.mariadb.MariaQueryRunner.createMariaQueryRunner;
import static io.prestosql.spi.type.VarcharType.VARCHAR;
import static io.prestosql.testing.TestingSession.testSessionBuilder;
import static io.prestosql.testing.assertions.Assert.assertEquals;
import static java.lang.String.format;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@Test
public class TestMariaIntegrationSmokeTest
        extends AbstractTestIntegrationSmokeTest
{
    private final TestingMySqlServer mysqlServer;

    public TestMariaIntegrationSmokeTest()
            throws Exception
    {
        this(MariaQueryRunner.createTestingMariaServer("testuser", "testpass", "tpch", "test_database"));
    }

    public TestMariaIntegrationSmokeTest(TestingMySqlServer mysqlServer)
    {
        super(() -> createMariaQueryRunner(mysqlServer, ORDERS));
        this.mysqlServer = mysqlServer;
    }

    @AfterClass(alwaysRun = true)
    public final void destroy()
    {
        mysqlServer.close();
    }

    @Override
    public void testDescribeTable()
    {
        // we need specific implementation of this tests due to specific Presto<->MariaDB varchar length mapping.
        MaterializedResult actualColumns = computeActual("DESC ORDERS").toTestTypes();

        MaterializedResult expectedColumns = MaterializedResult.resultBuilder(getQueryRunner().getDefaultSession(), VARCHAR, VARCHAR, VARCHAR, VARCHAR).row("orderkey", "bigint", "", "").row("custkey", "bigint", "", "").row("orderstatus", "varchar(255)", "", "").row("totalprice", "double", "", "").row("orderdate", "date", "", "").row("orderpriority", "varchar(255)", "", "").row("clerk", "varchar(255)", "", "").row("shippriority", "integer", "", "").row("comment", "varchar(255)", "", "").build();
        assertEquals(actualColumns, expectedColumns);
    }

    @Test
    public void testDropTable()
    {
        assertUpdate("CREATE TABLE test_drop AS SELECT 123 x", 1);
        assertTrue(getQueryRunner().tableExists(getSession(), "test_drop"));

        assertUpdate("DROP TABLE test_drop");
        assertFalse(getQueryRunner().tableExists(getSession(), "test_drop"));
    }

    @Test
    public void testViews()
            throws SQLException
    {
        execute("CREATE OR REPLACE VIEW tpch.test_view AS SELECT * FROM tpch.orders");
        assertQuery("SELECT orderkey FROM test_view", "SELECT orderkey FROM orders");
        execute("DROP VIEW IF EXISTS tpch.test_view");
    }

    @Test
    public void testInsert()
            throws Exception
    {
        execute("CREATE TABLE tpch.test_insert (x bigint, y varchar(100))");
        assertUpdate("INSERT INTO test_insert VALUES (123, 'test')", 1);
        assertQuery("SELECT * FROM test_insert", "SELECT 123 x, 'test' y");
        assertUpdate("DROP TABLE test_insert");
    }

    @Test
    public void testDateTime()
            throws Exception
    {
        Session session = testSessionBuilder().setCatalog("maria").setSchema("test_database").build();

        assertUpdate("CREATE TABLE maria.test_database.testDateTime(a date,b time,c timestamp)");
        assertUpdate("INSERT INTO maria.test_database.testDateTime VALUES (date '2001-08-22', TIME '23:00:01.000',timestamp '2001-08-22 03:04:05')", 1);
        assertQuery("SELECT * FROM maria.test_database.testDateTime", "SELECT '2001-08-22' a,'23:00:01.000' b, '2001-08-22 03:04:05' c");
        assertUpdate("DROP TABLE maria.test_database.testDateTime");
    }

    @Test
    public void integerColumn()
            throws Exception
    {
        Session session = testSessionBuilder().setCatalog("maria").setSchema("test_database").build();

        assertUpdate("CREATE TABLE maria.test_database.testInteger(a tinyint, b smallint, c integer, d bigint)");
        assertUpdate("INSERT INTO maria.test_database.testInteger VALUES (tinyint '-2', smallint '1', 5, 158)", 1);
        assertQuery("SELECT * FROM maria.test_database.testInteger", "SELECT -2 a, 1 b, 5 c, 158 d");
        assertUpdate("DROP TABLE maria.test_database.testInteger");
    }

    @Test
    public void floatingPointColumn()
            throws Exception
    {
        Session session = testSessionBuilder().setCatalog("maria").setSchema("test_database").build();

        assertUpdate("CREATE TABLE maria.test_database.testFloating(a real, b double)");
        assertUpdate("INSERT INTO maria.test_database.testFloating VALUES (1.987, 3.1487596)", 1);
        assertQuery("SELECT * FROM maria.test_database.testFloating", "SELECT 1.987 a, 3.1487596 b");
        assertUpdate("DROP TABLE maria.test_database.testFloating");
    }

    @Test
    public void fixedPrecisionColumn()
            throws Exception
    {
        Session session = testSessionBuilder().setCatalog("maria").setSchema("test_database").build();

        assertUpdate("CREATE TABLE maria.test_database.testFixed(a decimal(10,3))");
        assertUpdate("INSERT INTO maria.test_database.testFixed VALUES (1234567.123)", 1);
        assertQuery("SELECT * FROM maria.test_database.testFixed", "SELECT 1234567.123 a");
        assertUpdate("DROP TABLE maria.test_database.testFixed");
    }

    @Test
    public void stringColumn()
            throws Exception
    {
        Session session = testSessionBuilder().setCatalog("maria").setSchema("test_database").build();

        assertUpdate("CREATE TABLE maria.test_database.testString(a string, b varchar(5), c char(5), d json)");
        assertUpdate("INSERT INTO maria.test_database.testString VALUES ('olk', 'abcd', 'ftr', json '[{\"street\": \"street address\", \"city\": \"Berlin\"}]')", 1);
        assertQuery("SELECT * FROM maria.test_database.testString", "SELECT 'olk' a, 'abcd' b, 'ftr' c , '[{\"street\": \"street address\", \"city\": \"Berlin\"}]' d");
        assertUpdate("DROP TABLE maria.test_database.testString");
    }

    @Test
    public void testNameEscaping()
    {
        Session session = testSessionBuilder().setCatalog("maria").setSchema("test_database").build();

        assertFalse(getQueryRunner().tableExists(session, "test_table"));

        assertUpdate(session, "CREATE TABLE test_table AS SELECT 123 x", 1);
        assertTrue(getQueryRunner().tableExists(session, "test_table"));

        assertQuery(session, "SELECT * FROM test_table", "SELECT 123");

        assertUpdate(session, "DROP TABLE test_table");
        assertFalse(getQueryRunner().tableExists(session, "test_table"));
    }

    @Test
    public void testMySqlTinyint1()
            throws Exception
    {
        execute("CREATE TABLE tpch.mysql_test_tinyint1 (c_tinyint tinyint(1))");

        MaterializedResult actual = computeActual("SHOW COLUMNS FROM mysql_test_tinyint1");
        MaterializedResult expected = MaterializedResult.resultBuilder(getSession(), VARCHAR, VARCHAR, VARCHAR, VARCHAR).row("c_tinyint", "tinyint", "", "").build();

        assertEquals(actual, expected);

        execute("INSERT INTO tpch.mysql_test_tinyint1 VALUES (127), (-128)");
        MaterializedResult materializedRows = computeActual("SELECT * FROM tpch.mysql_test_tinyint1 WHERE c_tinyint = 127");
        assertEquals(materializedRows.getRowCount(), 1);
        MaterializedRow row = getOnlyElement(materializedRows);

        assertEquals(row.getFields().size(), 1);
        assertEquals(row.getField(0), (byte) 127);

        assertUpdate("DROP TABLE mysql_test_tinyint1");
    }

    @Test
    public void testCharTrailingSpace()
            throws Exception
    {
        execute("CREATE TABLE tpch.char_trailing_space (x char(10))");
        assertUpdate("INSERT INTO char_trailing_space VALUES ('test')", 1);

        assertQuery("SELECT * FROM char_trailing_space WHERE x = char 'test'", "VALUES 'test'");
        assertQuery("SELECT * FROM char_trailing_space WHERE x = char 'test  '", "VALUES 'test'");
        assertQuery("SELECT * FROM char_trailing_space WHERE x = char 'test        '", "VALUES 'test'");

        assertEquals(getQueryRunner().execute("SELECT * FROM char_trailing_space WHERE x = char ' test'").getRowCount(), 0);

        assertUpdate("DROP TABLE char_trailing_space");
    }

    @Test
    public void testInsertIntoNotNullColumn()
    {
        @Language("SQL") String createTableSql = format("" + "CREATE TABLE %s.tpch.test_insert_not_null (\n" + "   column_a date,\n" + "   column_b date NOT NULL\n" + ")", getSession().getCatalog().get());
        assertUpdate(createTableSql);
        assertEquals(computeScalar("SHOW CREATE TABLE test_insert_not_null"), createTableSql);

        assertQueryFails("INSERT INTO test_insert_not_null (column_a) VALUES (date '2012-12-31')", "^\\(conn=\\d*\\) Column 'column_b' cannot be null$");
        assertQueryFails("INSERT INTO test_insert_not_null (column_a, column_b) VALUES (date '2012-12-31', null)", "^\\(conn=\\d*\\) Column 'column_b' cannot be null$");

        assertUpdate("ALTER TABLE test_insert_not_null ADD COLUMN column_c BIGINT NOT NULL");

        createTableSql = format("" + "CREATE TABLE %s.tpch.test_insert_not_null (\n" + "   column_a date,\n" + "   column_b date NOT NULL,\n" + "   column_c bigint NOT NULL\n" + ")", getSession().getCatalog().get());
        assertEquals(computeScalar("SHOW CREATE TABLE test_insert_not_null"), createTableSql);

        assertQueryFails("INSERT INTO test_insert_not_null (column_b) VALUES (date '2012-12-31')", "^\\(conn=\\d*\\) Column 'column_c' cannot be null$");
        assertQueryFails("INSERT INTO test_insert_not_null (column_b, column_c) VALUES (date '2012-12-31', null)", "^\\(conn=\\d*\\) Column 'column_c' cannot be null$");

        assertUpdate("INSERT INTO test_insert_not_null (column_b, column_c) VALUES (date '2012-12-31', 1)", 1);
        assertUpdate("INSERT INTO test_insert_not_null (column_a, column_b, column_c) VALUES (date '2013-01-01', date '2013-01-02', 2)", 1);
        assertQuery("SELECT * FROM test_insert_not_null", "VALUES (NULL, CAST('2012-12-31' AS DATE), 1), (CAST('2013-01-01' AS DATE), CAST('2013-01-02' AS DATE), 2)");

        assertUpdate("DROP TABLE test_insert_not_null");
    }

    private void execute(String sql)
            throws SQLException
    {
        try (Connection connection = DriverManager.getConnection(mysqlServer.getJdbcUrl()); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String reformNotNullErrorMsg(String errorMsg)
    {
        return errorMsg.substring(errorMsg.indexOf(")"));
    }
}
