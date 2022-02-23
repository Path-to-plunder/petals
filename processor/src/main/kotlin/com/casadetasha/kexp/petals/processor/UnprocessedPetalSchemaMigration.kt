package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey

internal data class UnprocessedPetalSchemaMigration(
    val primaryKeyType: PetalPrimaryKey,
    val columnMigrations:HashMap<String, UnprocessedPetalColumn>) {

    var migrationSql: String? = null
    var migrationAlterationSql: List<String>? = null
    val columnsAsList: List<UnprocessedPetalColumn> by lazy { columnMigrations.values.toList() }
}