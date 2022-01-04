package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.casadetasha.kexp.annotationparser.KotlinContainer.KotlinClass
import com.casadetasha.kexp.annotationparser.KotlinValue.KotlinProperty
import com.casadetasha.kexp.kexportable.annotations.PetalColumn
import com.casadetasha.kexp.kexportable.annotations.PetalSchemaMigration
import com.casadetasha.kexp.petals.annotations.AlterColumn
import com.casadetasha.kexp.petals.annotations.Petal
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import java.util.*
import kotlin.reflect.KClass

object PetalSchemaMigrationParser {
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

    private fun parsePetalColumns(kotlinProperties: List<KotlinProperty>): java.util.HashMap<String, PetalColumn> {
        val columnMap = java.util.HashMap<String, PetalColumn>()
        kotlinProperties.forEach {
            columnMap[it.simpleName] = PetalMigrationColumnParser.parseFromKotlinProperty(it)
        }

        return columnMap
    }
}

object PetalMigrationColumnParser {
    private val SUPPORTED_TYPES = listOf<KClass<*>>(
        String::class,
        Int::class,
        Long::class,
        UUID::class
    ).map { it.asTypeName() }

    fun parseFromKotlinProperty(kotlinProperty: KotlinProperty): PetalColumn {
        val alterColumnAnnotation = kotlinProperty.annotatedElement?.getAnnotation(AlterColumn::class.java)
        val name = kotlinProperty.simpleName
        return PetalColumn(
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

