package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.casadetasha.kexp.annotationparser.KotlinContainer.KotlinClass
import com.casadetasha.kexp.annotationparser.KotlinValue.KotlinProperty
import com.casadetasha.kexp.petals.annotations.AlterColumn
import com.casadetasha.kexp.petals.annotations.Petal
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass

data class PetalMigration(val tableName: String,
                          val version: Int,
                          val primaryKeyType: PetalPrimaryKey,
                          val columnMigrations: HashMap<String, PetalMigrationColumn>) {

    companion object {
        fun parseFromClass(kotlinClass: KotlinClass): PetalMigration {
            val petalAnnotation = kotlinClass.getAnnotation(Petal::class)
                ?: printThenThrowError(
                    "INTERNAL LIBRARY ERROR: Cannot parse petal migration from class ${kotlinClass.className}:" +
                            " petal class must contain petal annotation ")

            return PetalMigration(
                tableName = petalAnnotation.tableName,
                version = petalAnnotation.version,
                primaryKeyType = petalAnnotation.primaryKeyType,
                columnMigrations = parsePetalColumns(kotlinClass.kotlinProperties)
            )
        }

        private fun parsePetalColumns(kotlinProperties: List<KotlinProperty>): HashMap<String, PetalMigrationColumn> {
            val columnMap = HashMap<String, PetalMigrationColumn>()
            kotlinProperties.forEach {
                columnMap[it.simpleName] = PetalMigrationColumn.parseFromProperty(it)
            }

            return columnMap
        }
    }
}

class PetalMigrationColumn(val name: String,
                           val typeName: TypeName,
                           kotlinProperty: KotlinProperty) {

    companion object {
        private val SUPPORTED_TYPES = listOf<KClass<*>>(
            String::class,
            Int::class,
            Long::class,
            UUID::class
        ).map { it.asTypeName() }

        fun parseFromProperty(kotlinProperty: KotlinProperty): PetalMigrationColumn {
            return PetalMigrationColumn(
                name = kotlinProperty.simpleName,
                typeName = kotlinProperty.typeName,
                kotlinProperty = kotlinProperty
            ).apply {
                checkTypeValidity(typeName)
            }
        }

        private fun checkTypeValidity(typeName: TypeName) {
            if (!SUPPORTED_TYPES.contains(typeName.copy(nullable = false))) {
                printThenThrowError(
                    "$typeName is not a valid column type. Only the following types are supported:" +
                            " ${SUPPORTED_TYPES.joinToString()}")
            }
        }
    }

    val dataType: String by lazy {
        when (typeName.copy(nullable = false)) {
            String::class.asTypeName() -> "TEXT"
            Int::class.asTypeName() -> "INT"
            Long::class.asTypeName() -> "BIGINT"
            UUID::class.asTypeName() -> "UUID"
            else -> printThenThrowError(
                "INTERNAL LIBRARY ERROR: Type $typeName was left out of new column sql generation block."
            )
        }
    }

    val isNullable: Boolean by lazy { kotlinProperty.isNullable }

    private val alterColumnAnnotation: AlterColumn? by lazy {
        kotlinProperty.annotatedElement?.getAnnotation(AlterColumn::class.java)
    }

    val isAlteration: Boolean by lazy { alterColumnAnnotation != null }

    val previousName: String by lazy {
        if (!isAlteration) printThenThrowError(
            "INTERNAL LIBRARY ERROR: Only AlterColumn annotated properties can have previousName")
        val renameFromColumn = alterColumnAnnotation!!.renameFrom
        return@lazy when (renameFromColumn.isBlank()) {
            true -> name
            false -> alterColumnAnnotation!!.renameFrom
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
