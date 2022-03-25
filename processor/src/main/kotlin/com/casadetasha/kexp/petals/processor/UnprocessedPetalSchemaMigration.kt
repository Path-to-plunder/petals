package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
import com.casadetasha.kexp.petals.annotations.PetalSchemaMigration

internal data class UnprocessedPetalSchemaMigration(
    val primaryKeyType: PetalPrimaryKey,
    val columnMigrations: Map<String, UnprocessedPetalColumn>
) {

    var migrationSql: String? = null
    var migrationAlterationSql: List<String>? = null
    val columnsAsList: List<UnprocessedPetalColumn> by lazy { columnMigrations.values.toList() }

    fun process(): PetalSchemaMigration {
        return PetalSchemaMigration(
            primaryKeyType = primaryKeyType,
            columnMigrations = columnMigrations.map { it.key to it.value.process() }.toMap()
        ).apply {
            migrationSql = this@UnprocessedPetalSchemaMigration.migrationSql
            migrationAlterationSql = this@UnprocessedPetalSchemaMigration.migrationAlterationSql
        }
    }
}