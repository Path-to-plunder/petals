package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.casadetasha.kexp.annotationparser.KotlinContainer.KotlinClass
import com.casadetasha.kexp.annotationparser.KotlinValue.KotlinProperty
import com.casadetasha.kexp.petals.annotations.AlterColumn
import com.casadetasha.kexp.petals.annotations.Petal
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass

@Serializable
data class PetalMigration(val tableName: String,
                          val schemaMigrations: MutableMap<Int, PetalSchemaMigration> = HashMap())

@Serializable
data class PetalSchemaMigration(val primaryKeyType: PetalPrimaryKey,
                                val columnMigrations: HashMap<String, PetalMigrationColumn>) {
    var migrationSql: String? = null

    companion object {
        fun parseFromClass(kotlinClass: KotlinClass): PetalSchemaMigration {
            val petalAnnotation = kotlinClass.getAnnotation(Petal::class)
                ?: printThenThrowError(
                    "INTERNAL LIBRARY ERROR: Cannot parse petal migration from class ${kotlinClass.className}:" +
                            " petal class must contain petal annotation ")

            return PetalSchemaMigration(
                primaryKeyType = petalAnnotation.primaryKeyType,
                columnMigrations = parsePetalColumns(kotlinClass.kotlinProperties)
            )
        }

        private fun parsePetalColumns(kotlinProperties: List<KotlinProperty>): HashMap<String, PetalMigrationColumn> {
            val columnMap = HashMap<String, PetalMigrationColumn>()
            kotlinProperties.forEach {
                columnMap[it.simpleName] = PetalMigrationColumn.parseFromKotlinProperty(it)
            }

            return columnMap
        }
    }
}

@Serializable
data class PetalMigrationColumn(@Transient val previousName: String? = null,
                                val name: String,
                                val dataType: String,
                                val isNullable: Boolean,
                                @Transient val isAlteration: Boolean? = null) {

    companion object {
        private val SUPPORTED_TYPES = listOf<KClass<*>>(
            String::class,
            Int::class,
            Long::class,
            UUID::class
        ).map { it.asTypeName() }

        fun parseFromKotlinProperty(kotlinProperty: KotlinProperty): PetalMigrationColumn {
            val alterColumnAnnotation = kotlinProperty.annotatedElement?.getAnnotation(AlterColumn::class.java)
            val name = kotlinProperty.simpleName
            return PetalMigrationColumn(
                name = name,
                previousName = getPreviousName(name, alterColumnAnnotation),
                dataType = getDataType(kotlinProperty.typeName),
                isNullable = kotlinProperty.isNullable,
                isAlteration = alterColumnAnnotation?.renameFrom != null
            )
        }

        private fun getDataType(typeName: TypeName): String {
            checkTypeValidity(typeName)
            return when (typeName.copy(nullable = false)) {
                String::class.asTypeName() -> "TEXT"
                Int::class.asTypeName() -> "INT"
                Long::class.asTypeName() -> "BIGINT"
                UUID::class.asTypeName() -> "UUID"
                else -> printThenThrowError(
                    "INTERNAL LIBRARY ERROR: Type $typeName was left out of new column sql generation block."
                )
            }
        }

        private fun checkTypeValidity(typeName: TypeName) {
            if (!SUPPORTED_TYPES.contains(typeName.copy(nullable = false))) {
                printThenThrowError(
                    "$typeName is not a valid column type. Only the following types are supported:" +
                            " ${SUPPORTED_TYPES.joinToString()}")
            }
        }

        private fun getPreviousName(name: String, alterColumnAnnotation: AlterColumn?): String? {
            val renameFromColumn = alterColumnAnnotation?.renameFrom
            return when (renameFromColumn?.isBlank()) {
                null -> null
                true -> name
                false -> alterColumnAnnotation.renameFrom
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PetalMigrationColumn) return false

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
