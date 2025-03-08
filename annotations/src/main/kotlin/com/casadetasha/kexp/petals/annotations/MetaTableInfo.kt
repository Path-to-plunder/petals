package com.casadetasha.kexp.petals.annotations

import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.Serializable
import java.sql.Connection
import java.sql.ResultSet


object MetaTableInfo {

    private const val META_TABLE_NAME: String = "kotlin_petal_table_version_info_table"

    private const val CREATE_META_TABLE_SQL: String = "CREATE TABLE $META_TABLE_NAME (" +
            " id BIGSERIAL PRIMARY KEY," +
            " table_name TEXT NOT NULL UNIQUE," +
            " version INT NOT NULL" +
            ")"

    private const val UPDATE_TABLE_VERSION_SQL: String = "INSERT INTO $META_TABLE_NAME (" +
            " table_name," +
            " version" +
            ")" +
            " VALUES(?, ?)" +
            " ON CONFLICT (table_name) DO UPDATE SET table_name=?, version=?"

    private const val UPDATE_TABLE_VERSION_SQL_H2 = """
    MERGE INTO $META_TABLE_NAME 
    USING (SELECT 1) AS dummy  -- Dummy table required for H2
    ON $META_TABLE_NAME.table_name = ?
    WHEN MATCHED THEN 
        UPDATE SET version = ?
    WHEN NOT MATCHED THEN 
        INSERT (table_name, version) VALUES (?, ?)
"""

    private const val LOAD_TABLE_VERSION_SQL: String = "SELECT version" +
            " FROM $META_TABLE_NAME" +
            " WHERE table_name = ?"

    private const val LOAD_TABLE_INFO_SQL: String = "SELECT table_name, column_name, data_type, is_nullable," +
            " character_maximum_length" +
            " FROM information_schema.columns" +
            " WHERE table_name = UPPER(?)"

    fun loadTableInfo(dataSource: HikariDataSource, tableName: String): DatabaseTableInfo? {
        val version: Int? = loadTableVersion(dataSource, tableName)
        val tableInfo: DatabaseTableInfo? = loadDataForDatabaseTable(dataSource, tableName)
            ?.apply { this.version = version }

        return when (tableInfo == null) {
            true -> null
            false -> tableInfo
        }
    }

    fun loadTableVersion(dataSource: HikariDataSource, tableName: String): Int? {
        loadOrCreatePetalVersionTable(dataSource)
        dataSource.connection.use {  connection ->
            connection.prepareStatement(LOAD_TABLE_VERSION_SQL).use { preparedStatement ->
                preparedStatement.setString(1, tableName)
                val rows = preparedStatement.executeQuery()
                return when (rows.next()) {
                    false -> null
                    true -> {
                        rows.getInt("version")
                    }
                }
            }
        }
    }

    fun loadOrCreatePetalVersionTable(dataSource: HikariDataSource): DatabaseTableInfo {
        return loadDataForDatabaseTable(dataSource, META_TABLE_NAME) ?: createPetalVersionTable(dataSource)
    }

    private fun createPetalVersionTable(dataSource: HikariDataSource): DatabaseTableInfo {
        dataSource.connection.use { connection ->
            connection.prepareStatement(CREATE_META_TABLE_SQL).execute()
        }

        return loadDataForDatabaseTable(dataSource, META_TABLE_NAME) ?: throw IllegalStateException(
            "INTERNAL LIBRARY ERROR: Petal version table should have just been created, something went wrong with" +
                    " table creation if it cannot be loaded.")
    }

    fun loadDataForDatabaseTable(dataSource: HikariDataSource, tableName: String): DatabaseTableInfo? {
        dataSource.connection.use { connection ->
            return queryDatabaseTableInfo(connection, tableName)
        }
    }

    private fun queryDatabaseTableInfo(connection: Connection, tableName: String): DatabaseTableInfo? {
        connection.prepareStatement(LOAD_TABLE_INFO_SQL).use { pst ->
            pst.setString(1, tableName)
            val rows: ResultSet = pst.executeQuery()

            return parseDatabaseTableInfoQueryResult(tableName, rows)
        }
    }

    private fun parseDatabaseTableInfoQueryResult(tableName: String, rows: ResultSet): DatabaseTableInfo? {
        val columnList = mutableListOf<PetalColumn>()
        while (rows.next()) {
            columnList += parseColumn(rows)
        }

        return when (columnList.isEmpty()) {
            true -> null
            false -> DatabaseTableInfo(
                name = tableName,
                columns = columnList)
        }
    }

    private fun parseColumn(row: ResultSet): PetalColumn {
        val columnName = row.getString("column_name")
        val dataType = row.getString("data_type")
        val isNullable = row.getString("is_nullable")
        val maxCharacterLength = row.getString("character_maximum_length")

        return PetalColumn(
            name = columnName,
            dataType = dataType.mapFromSchemaColumnType(maxCharacterLength),
            isNullable = when (isNullable) {
                "NO" -> false
                "YES" -> true
                else -> throw IllegalStateException("Illegal value for isNullable column: $isNullable")
            }
        )
    }

    fun updateTableVersionNumber(connection: Connection,
                                          tableName: String,
                                          tableVersion: Int) {
        connection.prepareStatement(getUpdateTableVersionSql(connection)).use { statement ->
            statement.setString(1, tableName)
            statement.setInt(2, tableVersion)
            statement.setString(3, tableName)
            statement.setInt(4, tableVersion)
            statement.executeUpdate()
        }
    }

    private fun getUpdateTableVersionSql(connection: Connection): String {
        val dbProductName = connection.metaData.databaseProductName
        val isH2 = dbProductName.equals("H2", ignoreCase = true)

        return if (isH2) {
            UPDATE_TABLE_VERSION_SQL_H2
        } else {
            UPDATE_TABLE_VERSION_SQL
        }
    }
}

private fun String.mapFromSchemaColumnType(maxCharacterLength: String?): String {
    return when (this) {
        "uuid" -> "uuid"
        "integer" -> "INT"
        "character varying" -> "CHARACTER VARYING(${checkCharLengthNotNull(maxCharacterLength)})"
        else -> this.uppercase()
    }
}

fun checkCharLengthNotNull(maxCharacterLength: String?): String {
    checkNotNull(maxCharacterLength) {
        "CHARACTER VARYING columns without a max character length are not currently supported."
    }
    return maxCharacterLength
}

@Serializable
data class DatabaseTableInfo(var version: Int? = null, val name: String, val columns: List<PetalColumn>)
