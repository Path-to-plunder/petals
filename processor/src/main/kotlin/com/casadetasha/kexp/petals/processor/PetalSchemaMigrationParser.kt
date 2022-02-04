package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.casadetasha.kexp.annotationparser.KotlinContainer.KotlinClass
import com.casadetasha.kexp.annotationparser.KotlinValue.KotlinProperty
import com.casadetasha.kexp.petals.annotations.PetalColumn
import com.casadetasha.kexp.petals.annotations.PetalSchemaMigration
import com.casadetasha.kexp.petals.annotations.AlterColumn
import com.casadetasha.kexp.petals.annotations.Petal
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
import com.casadetasha.kexp.petals.annotations.VarChar
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import java.util.*
import javax.lang.model.element.Element
import kotlin.reflect.KClass

object PetalSchemaMigrationParser {
    fun parseFromClass(kotlinClass: KotlinClass): PetalSchemaMigration {
        val petalAnnotation = kotlinClass.getAnnotation(Petal::class)
            ?: printThenThrowError(
                "INTERNAL LIBRARY ERROR: Cannot parse petal migration from class ${kotlinClass.className}:" +
                        " petal class must contain petal annotation "
            )

        return PetalSchemaMigration(
            primaryKeyType = petalAnnotation.primaryKeyType,
            columnMigrations = parsePetalColumns(kotlinClass, petalAnnotation.primaryKeyType)
        )
    }

    private fun parsePetalColumns(
        kotlinClass: KotlinClass,
        primaryKeyType: PetalPrimaryKey
    ): HashMap<String, PetalColumn> {
        val columns = parsePetalPropertyColumns(kotlinClass.kotlinProperties)
        if (primaryKeyType != PetalPrimaryKey.NONE) {
            columns["id"] = PetalMigrationColumnParser.parseIdColumn(primaryKeyType)
        }

        return columns
    }

    private fun parsePetalPropertyColumns(kotlinProperties: List<KotlinProperty>): HashMap<String, PetalColumn> {
        val columnMap = HashMap<String, PetalColumn>()
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

    fun parseIdColumn(primaryKeyType: PetalPrimaryKey): PetalColumn {
        if (primaryKeyType == PetalPrimaryKey.NONE) {
            printThenThrowError("INTERNAL LIBRARY ERROR: cannot parse id column for type NONE")
        }

        return PetalColumn(
            previousName = "id",
            name = "id",
            isNullable = false,
            isAlteration = false,
            isId = true,
            dataType = when (val dataType = primaryKeyType.dataType!!) {
                "SERIAL" -> "INT"
                "BIGSERIAL" -> "BIGINT"
                else -> dataType
            }
        )
    }

    fun parseFromKotlinProperty(kotlinProperty: KotlinProperty): PetalColumn {
        val alterColumnAnnotation = kotlinProperty.annotatedElement?.getAnnotation(AlterColumn::class.java)
        val name = kotlinProperty.simpleName
        return PetalColumn(
            previousName = getPreviousName(name, alterColumnAnnotation),
            name = name,
            dataType = getDataType(kotlinProperty),
            isNullable = kotlinProperty.isNullable,
            isAlteration = alterColumnAnnotation?.renameFrom != null,
            isId = false
        )
    }

    private fun getDataType(kotlinProperty: KotlinProperty): String {
        val typeName = kotlinProperty.typeName
        checkTypeValidity(typeName)
        return when (typeName.copy(nullable = false)) {
            String::class.asTypeName() -> getStringTypeName(kotlinProperty.annotatedElement)
            Int::class.asTypeName() -> "INT"
            Long::class.asTypeName() -> "BIGINT"
            UUID::class.asTypeName() -> "uuid"
            else -> printThenThrowError(
                "INTERNAL LIBRARY ERROR: Type $typeName was left out of new column sql generation block."
            )
        }
    }

    private fun getStringTypeName(annotatedElement: Element?): String {
        return when (val varCharAnnotation = annotatedElement?.getAnnotation(VarChar::class.java)) {
            null -> "TEXT"
            else -> {
                if (varCharAnnotation.charLimit < 1) "CHARACTER VARYING"
                else "CHARACTER VARYING(${varCharAnnotation.charLimit})"
            }
        }
    }

    private fun checkTypeValidity(typeName: TypeName) {
        if (!SUPPORTED_TYPES.contains(typeName.copy(nullable = false))) {
            printThenThrowError(
                "$typeName is not a valid column type. Only the following types are supported:" +
                        " ${SUPPORTED_TYPES.joinToString()}"
            )
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
