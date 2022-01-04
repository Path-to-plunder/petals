package com.casadetasha.kexp.kexportable.annotations;

import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
import kotlinx.serialization.Serializable;
import kotlinx.serialization.Transient;

import java.util.HashMap;

@Serializable
data class PetalMigration(val tableName: String,
                          val schemaMigrations: MutableMap<Int, PetalSchemaMigration> = HashMap())

@Serializable
data class PetalSchemaMigration(val primaryKeyType: PetalPrimaryKey,
                                val columnMigrations:HashMap<String, PetalColumn>) {
    var migrationSql: String? = null
}

@Serializable
data class PetalColumn(@Transient val previousName: String? = null,
                       val name: String,
                       val dataType: String,
                       val isNullable: Boolean,
                       @Transient val isAlteration: Boolean? = null) {

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
}
