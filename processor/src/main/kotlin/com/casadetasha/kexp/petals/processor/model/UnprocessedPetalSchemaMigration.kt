package com.casadetasha.kexp.petals.processor.model

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

    fun process(): PetalSchemaMigration {
        return PetalSchemaMigration(
            primaryKeyType = primaryKeyType,
            columnMap = localColumnMigrations.map { it.key to it.value.process() }.toMap()
        ).apply {
            migrationSqlRows = this@UnprocessedPetalSchemaMigration.migrationSql?.let { listOf(it) }
            migrationAlterationSql = this@UnprocessedPetalSchemaMigration.migrationAlterationSql
        }
    }
}
