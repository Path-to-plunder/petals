package com.casadetasha.kexp.petals.annotations

import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.sql.Connection
import java.sql.SQLException

abstract class BasePetalMigration {

    abstract val petalJson: String

    private var existingTableInfo: DatabaseTableInfo? = null
    private val petalMigration: PetalMigration by lazy { Json.decodeFromString(petalJson) }

    val tableName: String by lazy { petalMigration.tableName }
    private val petalSchemaVersions: Map<Int, PetalSchemaMigration> by lazy { petalMigration.schemaMigrations }
    private lateinit var dataSource: HikariDataSource

    fun migrateToLatest(dataSource: HikariDataSource) {
        this.dataSource = dataSource

        existingTableInfo = MetaTableInfo.loadTableInfo(dataSource, tableName)
        when (existingTableInfo) {
            null -> createNewTable()
            else -> migrateFrom(existingTableInfo!!.version ?: 1)
        }
    }

    private fun createNewTable() {
        checkNotNull(petalSchemaVersions[1]) {
            val firstSchemaVersion = petalSchemaVersions.keys.toSortedSet().first()
            "New petal tables must start with version 1, found $firstSchemaVersion instead"
        }
        performMigrationsStartingWithVersion(1)
    }

    private fun migrateFrom(versionNumber: Int) {
        checkSchemaVersionValidity(versionNumber)
        if (versionNumber == petalSchemaVersions.keys.toSortedSet().last()) return
        checkNextVersionIsIncremental(versionNumber)

        performMigrationsStartingWithVersion(versionNumber + 1)
    }

    private fun checkSchemaVersionValidity(versionNumber: Int) {
        val schemaVersion = petalSchemaVersions[versionNumber]

        checkNotNull(existingTableInfo) {
            "INTERNAL LIBRARY ERROR: should not check version equality when existing" +
                    " table is null"
        }

        checkNotNull(schemaVersion) {
            "Found meta info for table $tableName version $versionNumber but no matching schema was provided to" +
                    " match. A @Petal annotated schema must be provided matching the current table."
        }

        check(schemaVersion.columnsAsList.sortedBy { it.name } == existingTableInfo!!.columns.sortedBy { it.name }) {
            val schemaColumns = schemaVersion.columnsAsList
            val existingColumns = existingTableInfo!!.columns
            val columnDiff = existingColumns.filterNot { schemaColumns.contains(it) }.toMutableSet()
            columnDiff += schemaColumns.filterNot { existingColumns.contains(it) }

            "Table $tableName version $versionNumber does not match the provided schema.\n\n" +
                    "Non-matching columns:\n{\n${
                        columnDiff.sortedBy { it.name }.joinToString("\n")
                    }\n}\n\n" +
                    "Expected Columns:\n{\n${
                        schemaColumns.sortedBy { it.name }.joinToString("\n")
                    }\n}\n\n" +
                    "Actual Columns:\n{\n${
                        existingColumns.sortedBy { it.name }.joinToString("\n")
                    }\n}\n"
        }
    }

    private fun checkNextVersionIsIncremental(versionNumber: Int) {
        val sortedFollowingKeys = petalSchemaVersions.keys.filter { it > versionNumber }.toSortedSet()
        if (sortedFollowingKeys.size == 0) return

        val nextExpectedVersion = versionNumber + 1
        check(sortedFollowingKeys.first() == nextExpectedVersion) {
            "Version changes must be incremental. Expecting next version to be $nextExpectedVersion but instead found" +
                    " ${sortedFollowingKeys.first()} for table $tableName."
        }
    }

    private fun performMigrationsStartingWithVersion(startingMigrationVersionNumber: Int) {
        dataSource.connection.useWithoutAutoCommit { connection ->
            petalSchemaVersions.filterKeys { it >= startingMigrationVersionNumber }
                .toSortedMap()
                .forEach { (version, schema) ->
                    runMigration(connection, schema, version)
                }
        }
    }

    private fun runMigration(connection: Connection, schema: PetalSchemaMigration, version: Int) {
        connection.runTransactionWithRollback {
            migrateSchema(it, schema.migrationSql, schema.migrationAlterationSql, version)
        }
    }

    private fun migrateSchema(
        connection: Connection,
        schemaMigration: String?,
        alterationMigrations: List<String>?,
        tableVersion: Int
    ) {
        if (schemaMigration != null) {
            connection.createStatement().use { statement ->
                statement.execute(schemaMigration)
            }
        }
        (alterationMigrations ?: ArrayList()).forEach { alterationSql ->
            connection.createStatement().use { statement ->
                statement.execute(alterationSql)
            }
        }

        MetaTableInfo.updateTableVersionNumber(connection, tableName, tableVersion)
    }
}

private fun Connection.useWithoutAutoCommit(function: (Connection) -> Unit) {
    use {
        try {
            autoCommit = false
            function.invoke(this)
        } finally {
            autoCommit = true
        }
    }
}

private fun Connection.runTransactionWithRollback(function: (Connection) -> Unit) {
    val savepoint = setSavepoint()
    try {
        function.invoke(this)
        commit()
    } catch (e: SQLException) {
        println(e)
        rollback(savepoint)
    }
}
