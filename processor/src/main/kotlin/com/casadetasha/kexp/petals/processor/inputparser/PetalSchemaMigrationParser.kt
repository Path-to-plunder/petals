package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration

import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.casadetasha.kexp.annotationparser.KotlinContainer.KotlinClass
import com.casadetasha.kexp.annotationparser.KotlinValue.KotlinProperty
import com.casadetasha.kexp.petals.annotations.*
import com.casadetasha.kexp.petals.processor.model.PetalClasses
import com.casadetasha.kexp.petals.processor.model.*
import com.casadetasha.kexp.petals.processor.model.DefaultPetalValue
import com.casadetasha.kexp.petals.processor.model.PetalClasses.Companion.SUPPORTED_TYPES
import com.casadetasha.kexp.petals.processor.model.ReferencedByColumn
import com.casadetasha.kexp.petals.processor.model.UnprocessedPetalColumn
import com.casadetasha.kexp.petals.processor.model.UnprocessedPetalSchemaMigration
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassFileGenerator
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.exposed.ExposedEntityGenerator
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import java.util.*
import javax.lang.model.element.Element

internal class PetalSchemaMigrationParser(private val petalClasses: PetalClasses) {
    fun parseFromClass(petalClass: TypeName, kotlinClass: KotlinClass, primaryKeyType: PetalPrimaryKey): UnprocessedPetalSchemaMigration {
        kotlinClass.getAnnotation(PetalSchema::class)
            ?: printThenThrowError(
                "INTERNAL LIBRARY ERROR: Cannot parse petal migration from class ${kotlinClass.className}:" +
                        " petal class must contain petal annotation "
            )

        return UnprocessedPetalSchemaMigration(
            petalClass = petalClass,
            primaryKeyType = primaryKeyType,
            columnMigrationMap = parsePetalColumns(kotlinClass, primaryKeyType)
        )
    }

    private fun parsePetalColumns(
        kotlinClass: KotlinClass,
        primaryKeyType: PetalPrimaryKey
    ): Map<String, UnprocessedPetalColumn> {
        val columns = parsePetalPropertyColumns(kotlinClass.kotlinProperties)
        columns["id"] = PetalMigrationColumnParser(petalClasses).parseIdColumn(primaryKeyType)

        return columns
    }

    private fun parsePetalPropertyColumns(kotlinProperties: List<KotlinProperty>): MutableMap<String, UnprocessedPetalColumn> {
        val columnMap = HashMap<String, UnprocessedPetalColumn>()
        kotlinProperties.forEach {
            columnMap[it.simpleName] = PetalMigrationColumnParser(petalClasses).parseFromKotlinProperty(it)
        }

        return columnMap
    }
}

internal class PetalMigrationColumnParser(private val petalClasses: PetalClasses) {

    fun parseIdColumn(primaryKeyType: PetalPrimaryKey): UnprocessedPetalColumn {
        return UnprocessedPetalColumn(
            name = "id",
            dataType = when (val dataType = primaryKeyType.dataType!!) {
                "SERIAL" -> "INT"
                "BIGSERIAL" -> "BIGINT"
                else -> dataType
            },
            isNullable = false,
            previousName = "id",
            isAlteration = false,
            isId = true,
            defaultValue = null,
            isMutable = false,
            referencedByColumn = null,
            isRename = false
        )
    }

    fun parseFromKotlinProperty(kotlinProperty: KotlinProperty): UnprocessedPetalColumn {
        val alterColumnAnnotation = kotlinProperty.annotatedElement?.getAnnotation(AlterColumn::class.java)
        val name = kotlinProperty.simpleName
        val reference = getColumnReference(kotlinProperty)
        val referencedBy: ReferencedByColumn? = getReferencedByColumn(kotlinProperty)
        val defaultPetalValue = when (reference) {
            null -> DefaultPetalValue.parseDefaultValueForValueColumn(kotlinProperty)
            else -> DefaultPetalValue.parseDefaultValueForReferenceColumn(kotlinProperty)
        }

        return UnprocessedPetalColumn(
            columnReference = reference,
            previousName = getPreviousName(name, alterColumnAnnotation),
            name = name,
            dataType = getDataType(kotlinProperty),
            isNullable = kotlinProperty.isNullable,
            isAlteration = alterColumnAnnotation != null,
            isId = false,
            defaultValue = defaultPetalValue,
            isMutable = kotlinProperty.isMutable,
            referencedByColumn = referencedBy,
            isRename = alterColumnAnnotation?.renameFrom.let { !it.isNullOrBlank() }
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
            else -> getPetalReferenceIdTypeName(typeName)
        }
    }

    private fun getPetalReferenceIdTypeName(typeName: TypeName): String {
        return petalClasses.PETAL_CLASSES[typeName.copy(nullable = false)]?.getAnnotation(Petal::class)?.primaryKeyType?.dataType
            ?: printThenThrowError(
                "INTERNAL LIBRARY ERROR: Type $typeName was left out of new column sql generation block."
            )
    }

    private fun getColumnReference(kotlinProperty: KotlinProperty): ColumnReference? {
        return when (SUPPORTED_TYPES.contains(kotlinProperty.typeName.copy(nullable = false))) {
            true -> null
            false -> petalClasses.PETAL_CLASSES[kotlinProperty.typeName.copy(nullable = false)] ?: printThenThrowError(
                "Column type must be a base Petal column type or another Petal. Found" +
                        " ${kotlinProperty.typeName} for column ${kotlinProperty.simpleName}"
            )
        }?.asReference()
    }

    private fun getReferencedByColumn(kotlinProperty: KotlinProperty): ReferencedByColumn? {
        val alterColumnAnnotation = kotlinProperty.annotatedElement?.getAnnotation(ReferencedBy::class.java)
        return when(alterColumnAnnotation) {
            null -> null
            else -> petalClasses.PETAL_CLASSES[kotlinProperty.typeName.copy(nullable = false)] ?: printThenThrowError(
                "ReferencedBy type must be a base Petal column type or another Petal. Found ${kotlinProperty.typeName}" +
                        " for column ${kotlinProperty.simpleName}"
            )
        }?.asReferencedBy(alterColumnAnnotation!!.referencePropertyName)
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
        val deNulledTypeName = typeName.copy(nullable = false)
        val isBasicPetalType = SUPPORTED_TYPES.contains(deNulledTypeName)
        val isPetalReference = petalClasses.PETAL_CLASSES[deNulledTypeName] != null
        if (!isBasicPetalType && !isPetalReference) {
            printThenThrowError(
                "$deNulledTypeName is not a valid column type. Only other Petals and the following types are" +
                        " supported: ${SUPPORTED_TYPES.joinToString()}"
            )
        }
    }

    private fun getPreviousName(name: String, alterColumnAnnotation: AlterColumn?): String? {
        val renameFromColumn = alterColumnAnnotation?.renameFrom
        return when {
            renameFromColumn == null -> null
            renameFromColumn.isBlank() -> name
            else -> alterColumnAnnotation.renameFrom
        }
    }
}

private fun KotlinClass.asReference(): ColumnReference {
    val accessorName = getAnnotation(Petal::class)!!.className

    return ColumnReference(
        kotlinTypeName = this.className,
        accessorClassName = ClassName(AccessorClassFileGenerator.PACKAGE_NAME, accessorName),
        tableClassName = ClassName(ExposedEntityGenerator.PACKAGE_NAME, "${accessorName}Table"),
        entityClassName = ClassName(ExposedEntityGenerator.PACKAGE_NAME, "${accessorName}Entity"),
    )
}

private fun KotlinClass.asReferencedBy(columnName: String): ReferencedByColumn {
    return ReferencedByColumn(
        columnReference = asReference(),
        columnName = columnName
    )
}
