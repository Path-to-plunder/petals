package com.casadetasha.kexp.petals.annotations

import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.sql.Connection

abstract class BasePetalMigration {

    abstract val petalJson: String

    private var existingTableInfo: DatabaseTableInfo? = null
    private val petalMigration: PetalMigration by lazy { Json.decodeFromString(petalJson) }
    private val tableName: String by lazy { petalMigration.tableName }
    private val petalSchemaVersions: MutableMap<Int, PetalSchemaMigration> by lazy { petalMigration.schemaMigrations }
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
        checkVersionEquality(versionNumber)
        if (versionNumber == petalSchemaVersions.keys.toSortedSet().last()) return
        checkNextVersionIsIncremental(versionNumber)

        performMigrationsStartingWithVersion(versionNumber + 1)
    }

    private fun checkVersionEquality(versionNumber: Int) {
        checkNotNull(existingTableInfo) { "INTERNAL LIBRARY ERROR: should not check version equality when existing" +
                " table is null" }

        checkNotNull(petalSchemaVersions[versionNumber]) {
            "Found meta info for table $tableName version $versionNumber but no matching schema was provided to" +
                    " match. A @Petal annotated schema must be provided matching the current table."
        }

        check(petalSchemaVersions[versionNumber]!!.columnMigrations == existingTableInfo!!.columns) {
            "Table $tableName version $versionNumber does not match the provided schema."
        }
    }

    private fun checkNextVersionIsIncremental(versionNumber: Int) {
        val sortedFollowingKeys = petalSchemaVersions.keys.filter { it > versionNumber }.toSortedSet()
        if (sortedFollowingKeys.size == 0) return

        val nextExpectedVersion = versionNumber + 1
        check(sortedFollowingKeys.first() != nextExpectedVersion) {
            "Version changes must be incremental. Expecting next version to be $nextExpectedVersion but instead found" +
                    " ${sortedFollowingKeys.first()} for table $tableName."
        }
    }

    private fun performMigrationsStartingWithVersion(startingMigrationVersionNumber: Int) {
        dataSource.connection.use { connection ->
            petalSchemaVersions.filterKeys { it >= startingMigrationVersionNumber }
                .toSortedMap()
                .forEach { (version, schema) ->
                    migrateSchema(connection, schema.migrationSql!!, version)
                }
        }
    }

    private fun migrateSchema(connection: Connection, schemaMigration: String, tableVersion: Int) {
        MetaTableInfo.createStatementWithVersionUpdateBatchAdded(connection, tableName, tableVersion).use { statement ->
            statement.addBatch(schemaMigration)
            statement.executeBatch()
        }
    }
}
