package com.casadetasha.kexp.petals.annotations

import com.casadetasha.kexp.kexportable.annotations.PetalMigration
import com.casadetasha.kexp.kexportable.annotations.PetalSchemaMigration
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

abstract class BasePetalMigration {

    abstract val petalJson: String

    private var existingTableInfo: ExistingTableInfo? = null
    private val petalMigration: PetalMigration by lazy { Json.decodeFromString(petalJson) }
    private val tableName: String by lazy { petalMigration.tableName }
    private val petalSchemaVersions: MutableMap<Int, PetalSchemaMigration> by lazy { petalMigration.schemaMigrations }

    init {
        loadTableInfo()
    }

    private fun loadTableInfo(): ExistingTableInfo? {
        return null
    }

    fun migrateToLatest() {
        when (existingTableInfo) {
            null -> createNewTable()
            else -> migrateFrom(existingTableInfo!!.versionNumber)
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

        check(petalSchemaVersions[versionNumber] == existingTableInfo!!.schemaMigration) {
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
        petalSchemaVersions.filterKeys { it >= startingMigrationVersionNumber }
            .toSortedMap()
            .forEach {
                migrateSchema(it.value)
            }
    }

    fun migrateSchema(schemaMigration: PetalSchemaMigration) {
        println(schemaMigration.migrationSql)
    }
}

class ExistingTableInfo(val versionNumber: Int, val schemaMigration: PetalSchemaMigration)
