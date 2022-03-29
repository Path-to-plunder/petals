package com.casadetasha.kexp.petals.processor.model

import com.casadetasha.kexp.petals.annotations.PetalMigration
import com.squareup.kotlinpoet.MemberName

internal data class UnprocessedPetalMigration(val tableName: String,
                          val className: String,
                          val schemaMigrations: MutableMap<Int, UnprocessedPetalSchemaMigration> = HashMap()) {


    val classMemberName: MemberName by lazy {
        val packageName = "com.casadetasha.kexp.petals.migration"
        val className = "TableMigrations\$${tableName}"
        return@lazy MemberName(packageName, className)
    }

    fun getCurrentSchema(): UnprocessedPetalSchemaMigration? = schemaMigrations.toSortedMap()
        .values
        .lastOrNull()

    fun process(): PetalMigration {
        return PetalMigration(
            tableName = tableName,
            className = className,
            schemaMigrations = schemaMigrations.map { it.key to it.value.process() }.toMap()
        )
    }
}
