package com.casadetasha.kexp.petals.annotations

import com.casadetasha.kexp.kexportable.annotations.PetalColumn
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.Serializable
import java.sql.Connection
import java.sql.ResultSet


object MetaTableInfo {

    private const val META_TABLE_NAME: String = "kotlin_petal_table_meta_info"

    private const val LOAD_TABLE_INFO_SQL: String = "SELECT table_name, column_name, data_type, is_nullable" +
            " FROM information_schema.columns WHERE table_name = ?"

    fun createTableIfNotExists(dataSource: HikariDataSource) {
    }

    fun loadTableInfo(dataSource: HikariDataSource, tableName: String): ExistingTableInfo? {
        val tableInfo: DatabaseTableInfo? = loadDataForDatabaseTable(dataSource, tableName)
        val version: Int? = loadTableVersion(dataSource, tableName)
        return when (tableInfo == null) {
            true -> null
            false -> ExistingTableInfo(version, tableInfo)
        }
    }

    private fun loadTableVersion(dataSource: HikariDataSource, tableName: String): Int? {
        return null
    }

    fun loadDataForDatabaseTable(dataSource: HikariDataSource, tableName: String): DatabaseTableInfo? {
        dataSource.connection.use { connection ->
            return queryDatabaseTableInfo(connection, tableName)
        }
    }

    private fun queryDatabaseTableInfo(connection: Connection, tableName: String): DatabaseTableInfo? {
        connection.prepareStatement(LOAD_TABLE_INFO_SQL).use { pst ->
            pst.setString(1, tableName)
            val columns: ResultSet = pst.executeQuery()

            return parseDatabaseTableInfoQueryResult(tableName, columns)
        }
    }

    private fun parseDatabaseTableInfoQueryResult(tableName: String, columns: ResultSet): DatabaseTableInfo? {
        val columnList = mutableListOf<PetalColumn>()
        while (columns.next()) {
            columnList += parseColumn(columns)
        }

        return when (columnList.isEmpty()) {
            true -> null
            false -> DatabaseTableInfo(tableName, columnList)
        }
    }

    private fun parseColumn(columns: ResultSet): PetalColumn {
        val columnName = columns.getString("column_name")
        val dataType = columns.getString("data_type")
        val isNullable = columns.getString("is_nullable")

        return PetalColumn(name = columnName,
            dataType = dataType,
            isNullable = when (isNullable) {
                "NO" -> false
                "YES" -> true
                else -> throw IllegalStateException("Illegal value for isNullable column: $isNullable")
            })
    }

    internal fun createUpdateTableVersionSql(tableName: String, tableVersion: Int): String {
        return "INSERT INTO $META_TABLE_NAME (table_name, version) VALUES('$tableName', $tableVersion)" +
                " ON CONFLICT SET table_name='$tableName', tableVersion=$tableVersion"
    }
}

@Serializable
data class DatabaseTableInfo(val name: String, val columns: List<PetalColumn>)

class ExistingTableInfo(val versionNumber: Int?, val tableInfo: DatabaseTableInfo?)
