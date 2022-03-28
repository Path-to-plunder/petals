package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
import com.casadetasha.kexp.petals.annotations.PetalSchemaMigration
import com.squareup.kotlinpoet.TypeName

internal class UnprocessedPetalSchemaMigration constructor(
    val columnMigrationMap: Map<String, UnprocessedPetalColumn>,
    val petalClass: TypeName,
    val primaryKeyType: PetalPrimaryKey,
) {

    val localColumnMigrations by lazy {
        columnMigrationMap.filterValues { it.isLocalColumn }
    }
    var migrationSql: String? = null
    var migrationAlterationSql: List<String>? = null
    val columnsAsList: List<UnprocessedPetalColumn> by lazy { columnMigrationMap.values.toList() }

    fun process(): PetalSchemaMigration {
        return PetalSchemaMigration(
            primaryKeyType = primaryKeyType,
            columnMigrations = localColumnMigrations.map { it.key to it.value.process() }.toMap()
        ).apply {
            migrationSql = this@UnprocessedPetalSchemaMigration.migrationSql
            migrationAlterationSql = this@UnprocessedPetalSchemaMigration.migrationAlterationSql
        }
    }
}
