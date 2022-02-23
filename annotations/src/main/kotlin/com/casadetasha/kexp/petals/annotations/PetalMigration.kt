package com.casadetasha.kexp.petals.annotations;

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class PetalMigration(val tableName: String,
                          val className: String,
                          val schemaMigrations: MutableMap<Int, PetalSchemaMigration> = HashMap()) {

    fun getCurrentSchema(): PetalSchemaMigration? = schemaMigrations.toSortedMap()
        .values
        .lastOrNull()
}

@Serializable
data class PetalSchemaMigration(val primaryKeyType: PetalPrimaryKey,
                                val columnMigrations:HashMap<String, PetalColumn>) {
    var migrationSql: String? = null
    var migrationAlterationSql: List<String>? = null

    val columnsAsList: List<PetalColumn> by lazy { columnMigrations.values.toList() }
}

@Serializable
data class PetalColumn constructor(
    val name: String,
    val dataType: String,
    val isNullable: Boolean,
): Comparable<PetalColumn> {

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
