package com.casadetasha.kexp.petals.annotations;

import kotlinx.serialization.Serializable

@Serializable
data class PetalMigration(
    val tableName: String,
    val className: String,
    val schemaMigrations: Map<Int, PetalSchemaMigration> = HashMap()
)

@Serializable
data class PetalSchemaMigration constructor(
    val primaryKeyType: PetalPrimaryKey,
    val columnMap: Map<String, PetalColumn>,
    val preMigrationSql: String?,
    val migrationSqlRows: List<String>?,
    val migrationAlterationSql: List<String>? = null
) {
    val migrationSql: String? get() { return migrationSqlRows?.joinToString(" ") }
    val columns: List<PetalColumn> by lazy { columnMap.values.toList() }
}

@Serializable
data class PetalColumn constructor(
    val name: String,
    val dataType: String,
    val isNullable: Boolean,
) : Comparable<PetalColumn> {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PetalColumn) return false

        if (name != other.name) return false
        if (dataType != other.dataType) return false
        if (isNullable != other.isNullable) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + dataType.hashCode()
        result = 31 * result + isNullable.hashCode()
        return result
    }

    override fun compareTo(other: PetalColumn) = name.compareTo(other.name)
}
