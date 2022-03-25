package com.casadetasha.kexp.petals.processor.migration

import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.casadetasha.kexp.annotationparser.KotlinContainer.KotlinClass
import com.casadetasha.kexp.annotationparser.KotlinValue.KotlinProperty
import com.casadetasha.kexp.petals.annotations.*
import com.casadetasha.kexp.petals.processor.DefaultPetalValue
import com.casadetasha.kexp.petals.processor.PetalClasses.PETAL_CLASSES
import com.casadetasha.kexp.petals.processor.UnprocessedPetalColumn
import com.casadetasha.kexp.petals.processor.UnprocessedPetalSchemaMigration
import com.casadetasha.kexp.petals.processor.classgenerator.accessor.AccessorClassFileGenerator
import com.casadetasha.kexp.petals.processor.classgenerator.table.ExposedEntityGenerator
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import java.util.*
import javax.lang.model.element.Element
import kotlin.reflect.KClass

internal object PetalSchemaMigrationParser {
    fun parseFromClass(kotlinClass: KotlinClass, primaryKeyType: PetalPrimaryKey): UnprocessedPetalSchemaMigration {
        kotlinClass.getAnnotation(PetalSchema::class)
            ?: printThenThrowError(
                "INTERNAL LIBRARY ERROR: Cannot parse petal migration from class ${kotlinClass.className}:" +
                        " petal class must contain petal annotation "
            )

        return UnprocessedPetalSchemaMigration(
            primaryKeyType = primaryKeyType,
            columnMigrations = parsePetalColumns(kotlinClass, primaryKeyType)
        )
    }

    private fun parsePetalColumns(
        kotlinClass: KotlinClass,
        primaryKeyType: PetalPrimaryKey
    ): Map<String, UnprocessedPetalColumn> {
        val columns = parsePetalPropertyColumns(kotlinClass.kotlinProperties)
        columns["id"] = PetalMigrationColumnParser.parseIdColumn(primaryKeyType)

        return columns
    }

    private fun parsePetalPropertyColumns(kotlinProperties: List<KotlinProperty>): MutableMap<String, UnprocessedPetalColumn> {
        val columnMap = HashMap<String, UnprocessedPetalColumn>()
        kotlinProperties.forEach {
            columnMap[it.simpleName] = PetalMigrationColumnParser.parseFromKotlinProperty(it)
        }

        return columnMap.filterValues { it.isLocalColumn }
            .toMutableMap()
    }
}

internal object PetalMigrationColumnParser {
    private val SUPPORTED_TYPES = listOf<KClass<*>>(
        String::class,
        Int::class,
        Long::class,
        UUID::class
    ).map { it.asTypeName() }

    fun parseIdColumn(primaryKeyType: PetalPrimaryKey): UnprocessedPetalColumn {
        return UnprocessedPetalColumn(
            previousName = "id",
            name = "id",
            dataType = when (val dataType = primaryKeyType.dataType!!) {
                "SERIAL" -> "INT"
                "BIGSERIAL" -> "BIGINT"
                else -> dataType
            },
            isNullable = false,
            isAlteration = false,
            isId = true,
            defaultValue = null,
            isMutable = false,
            referencedByColumn = null
        )
    }

    fun parseFromKotlinProperty(kotlinProperty: KotlinProperty): UnprocessedPetalColumn {
        val alterColumnAnnotation = kotlinProperty.annotatedElement?.getAnnotation(AlterColumn::class.java)
        val name = kotlinProperty.simpleName
        val defaultPetalValue = DefaultPetalValue(kotlinProperty)
        val reference = getReferencingClassName(kotlinProperty)
        val referencedBy: ReferencedByColumn? = getReferencedByColumn(kotlinProperty)

        return UnprocessedPetalColumn(
            columnReference = reference,
            previousName = getPreviousName(name, alterColumnAnnotation),
            name = name,
            dataType = getDataType(kotlinProperty),
            isNullable = kotlinProperty.isNullable,
            isAlteration = alterColumnAnnotation?.renameFrom != null,
            isId = false,
            defaultValue = defaultPetalValue,
            isMutable = kotlinProperty.isMutable,
            referencedByColumn = referencedBy
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
        return PETAL_CLASSES[typeName.copy(nullable = false)]?.getAnnotation(Petal::class)?.primaryKeyType?.dataType
            ?: printThenThrowError(
                "INTERNAL LIBRARY ERROR: Type $typeName was left out of new column sql generation block."
            )
    }

    private fun getReferencingClassName(kotlinProperty: KotlinProperty): ColumnReference? {
        return when (SUPPORTED_TYPES.contains(kotlinProperty.typeName.copy(nullable = false))) {
            true -> null
            false -> PETAL_CLASSES[kotlinProperty.typeName.copy(nullable = false)] ?: printThenThrowError(
                "Column type must be a base Petal column type or another Petal. Found ${kotlinProperty.typeName}"
            )
        }?.asReference()
    }

    private fun getReferencedByColumn(kotlinProperty: KotlinProperty): ReferencedByColumn? {
        val alterColumnAnnotation = kotlinProperty.annotatedElement?.getAnnotation(ReferencedBy::class.java)
        return when(alterColumnAnnotation) {
            null -> null
            else -> PETAL_CLASSES[kotlinProperty.typeName.copy(nullable = false)] ?: printThenThrowError(
                "Column type must be a base Petal column type or another Petal. Found ${kotlinProperty.typeName}"
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
        val isPetalReference = PETAL_CLASSES[deNulledTypeName] != null
        if (!isBasicPetalType && !isPetalReference) {
            printThenThrowError(
                "$deNulledTypeName is not a valid column type. Only petal reference and the following types" +
                        " are supported: ${SUPPORTED_TYPES.joinToString()}"
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

private fun KotlinClass.asReference(): ColumnReference {
    val accessorName = getAnnotation(Petal::class)!!.className

    return ColumnReference(
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
