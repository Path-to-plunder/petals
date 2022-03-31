package com.casadetasha.kexp.petals.processor.inputparser

import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.casadetasha.kexp.annotationparser.KotlinValue
import com.casadetasha.kexp.petals.annotations.*
import com.casadetasha.kexp.petals.processor.inputparser.PetalReferenceColumn.Companion.parseReferenceColumn
import com.casadetasha.kexp.petals.processor.inputparser.PetalValueColumn.Companion.parseValueColumn
import com.casadetasha.kexp.petals.processor.inputparser.ReferencedByPetalColumn.Companion.parseReferencedByColumn
import com.casadetasha.kexp.petals.processor.model.ColumnReference
import com.casadetasha.kexp.petals.processor.model.DefaultPetalValue
import com.casadetasha.kexp.petals.processor.model.PetalClasses
import com.casadetasha.kexp.petals.processor.model.ReferencedByColumn
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassFileGenerator
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.exposed.ExposedEntityGenerator
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import java.util.*
import javax.lang.model.element.Element

internal sealed class ParsedPetalColumn(
    val name: String,
    val isNullable: Boolean,
) : Comparable<ParsedPetalColumn> {

    override fun compareTo(other: ParsedPetalColumn): Int {
        return name.compareTo(other.name)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParsedPetalColumn) return false

        if (name != other.name) return false
        if (isNullable != other.isNullable) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + isNullable.hashCode()
        return result
    }

    companion object {
        fun parsePetalColumn(
            petalClasses: Map<ClassName, ParsedSchemalessPetal>,
            parsedPetalSchema: ParsedPetalSchema,
            kotlinProperty: KotlinValue.KotlinProperty
        ): ParsedPetalColumn {
            val referencedBy = kotlinProperty.annotatedElement?.getAnnotation(ReferencedBy::class.java)
            val denulledTypeName = kotlinProperty.typeName.copy(nullable = false)

            return when {
                referencedBy != null -> {
                    parseReferencedByColumn(
                        parsedPetalSchema,
                        kotlinProperty,
                        referencedBy,
                        petalClasses[denulledTypeName]
                    )
                }
                petalClasses.containsKey(denulledTypeName) -> {
                    parseReferenceColumn(petalClasses, kotlinProperty, petalClasses[denulledTypeName]!!)
                }
                PetalClasses.SUPPORTED_TYPES.contains(denulledTypeName) -> {
                    parseValueColumn(petalClasses, kotlinProperty)
                }
                else -> printThenThrowError(
                    "Found invalid type for column ${kotlinProperty.simpleName} for table" +
                            " ${parsedPetalSchema.tableName} schema version ${parsedPetalSchema.schemaVersion}" +
                            " $denulledTypeName is not a valid column type. Only other Petals and the following types" +
                            " are supported: ${PetalClasses.SUPPORTED_TYPES.joinToString()}"
                )
            }
        }
    }
}

internal sealed class LocalPetalColumn private constructor(
    name: String,
    isNullable: Boolean,
    val dataType: String,
    val isMutable: Boolean,
    val alterationInfo: PetalAlteration?,
) : ParsedPetalColumn(
    name = name,
    isNullable = isNullable
) {
    abstract val tablePropertyClassName: TypeName

    val isAlteration = alterationInfo != null
    val previousName by lazy { alterationInfo?.previousName ?: false }
    val isRename by lazy { alterationInfo?.isRename ?: false }

    val kotlinType: ClassName by lazy {
        if (dataType.startsWith("CHARACTER VARYING")) {
            return@lazy String::class.asClassName()
        }

        return@lazy when (val type = dataType) {
            "uuid" -> UUID::class
            "TEXT" -> String::class
            "INT" -> Int::class
            "BIGINT" -> Long::class
            else -> printThenThrowError(
                "INTERNAL LIBRARY ERROR: unsupported datatype ($type) found while" +
                        " parsing column for accessor"
            )
        }.asClassName()
    }

    fun processMigration(): PetalColumn = PetalColumn(
        name = name,
        dataType = dataType,
        isNullable = isNullable,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocalPetalColumn) return false
        if (!super.equals(other)) return false

        if (dataType != other.dataType) return false
        if (isMutable != other.isMutable) return false
        if (alterationInfo != other.alterationInfo) return false
        if (tablePropertyClassName != other.tablePropertyClassName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + dataType.hashCode()
        result = 31 * result + isMutable.hashCode()
        result = 31 * result + (alterationInfo?.hashCode() ?: 0)
        result = 31 * result + tablePropertyClassName.hashCode()
        return result
    }
}

internal class PetalIdColumn private constructor(
    dataType: String,
) : LocalPetalColumn(
    name = "id",
    dataType = dataType,
    alterationInfo = null,
    isNullable = false,
    isMutable = false,
) {

    override val tablePropertyClassName: TypeName = Column::class.asClassName()
        .parameterizedBy(kotlinType.copy(nullable = isNullable))


    companion object {
        fun parseIdColumn(primaryKeyType: PetalPrimaryKey): PetalIdColumn {
            return PetalIdColumn(
                dataType = when (val dataType = primaryKeyType.dataType!!) {
                    "SERIAL" -> "INT"
                    "BIGSERIAL" -> "BIGINT"
                    else -> dataType
                },
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PetalIdColumn) return false
        if (!super.equals(other)) return false

        if (tablePropertyClassName != other.tablePropertyClassName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + tablePropertyClassName.hashCode()
        return result
    }
}

internal class PetalValueColumn private constructor(
    name: String,
    dataType: String,
    isNullable: Boolean,
    isMutable: Boolean,
    alterationInfo: PetalAlteration?,
    val defaultValue: DefaultPetalValue?,
) : LocalPetalColumn(
    name = name,
    dataType = dataType,
    alterationInfo = alterationInfo,
    isNullable = isNullable,
    isMutable = isMutable,
) {

    override val tablePropertyClassName: TypeName = Column::class.asClassName()
        .parameterizedBy(kotlinType.copy(nullable = isNullable))

    companion object {

        fun parseValueColumn(
            petalClasses: Map<ClassName, ParsedSchemalessPetal>,
            kotlinProperty: KotlinValue.KotlinProperty
        ): PetalValueColumn {
            val name = kotlinProperty.simpleName
            val defaultPetalValue = DefaultPetalValue.parseDefaultValueForValueColumn(kotlinProperty)

            val alterColumnAnnotation = kotlinProperty.annotatedElement?.getAnnotation(AlterColumn::class.java)
            val alterationInfo: PetalAlteration? = alterColumnAnnotation?.let {
                val renameFrom = it.renameFrom
                PetalAlteration(
                    previousName = getPreviousName(name, renameFrom),
                    isRename = renameFrom.isNotBlank()
                )
            }

            return PetalValueColumn(
                name = name,
                dataType = getDataType(petalClasses, kotlinProperty),
                isNullable = kotlinProperty.isNullable,
                defaultValue = defaultPetalValue,
                isMutable = kotlinProperty.isMutable,
                alterationInfo = alterationInfo
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PetalValueColumn) return false
        if (!super.equals(other)) return false

        if (defaultValue != other.defaultValue) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (defaultValue?.hashCode() ?: 0)
        return result
    }
}

internal class PetalReferenceColumn private constructor(
    name: String,
    dataType: String,
    isNullable: Boolean,
    isMutable: Boolean,
    alterationInfo: PetalAlteration?,
    val columnReferenceInfo: ColumnReference,
) : LocalPetalColumn(
    name = name,
    dataType = dataType,
    alterationInfo = alterationInfo,
    isNullable = isNullable,
    isMutable = isMutable,
) {

    val referencingTableClassName: ClassName = columnReferenceInfo.tableClassName
    val referencingEntityClassName: ClassName = columnReferenceInfo.entityClassName
    val referencingAccessorClassName: ClassName = columnReferenceInfo.accessorClassName

    val referencingIdName: String = "${name}Id"
    val nestedPetalManagerName: String = "${name}NestedPetalManager"

    override val tablePropertyClassName: TypeName = Column::class.asClassName()
        .parameterizedBy(
            EntityID::class.asClassName()
                .parameterizedBy(kotlinType.copy(nullable = false))
                .copy(nullable = isNullable)
        )

    companion object {

        fun parseReferenceColumn(
            petalClasses: Map<ClassName, ParsedSchemalessPetal>,
            kotlinProperty: KotlinValue.KotlinProperty,
            referencedColumnPetal: ParsedSchemalessPetal
        ): PetalReferenceColumn {
            val name = kotlinProperty.simpleName

            val alterColumnAnnotation = kotlinProperty.annotatedElement?.getAnnotation(AlterColumn::class.java)
            val alterationInfo: PetalAlteration? = alterColumnAnnotation?.let {
                val renameFrom = it.renameFrom
                PetalAlteration(
                    previousName = getPreviousName(name, renameFrom),
                    isRename = renameFrom.isNotBlank()
                )
            }

            return PetalReferenceColumn(
                columnReferenceInfo = referencedColumnPetal.asReference(),
                name = name,
                dataType = getDataType(petalClasses, kotlinProperty),
                isNullable = kotlinProperty.isNullable,
                isMutable = kotlinProperty.isMutable,
                alterationInfo = alterationInfo,
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PetalReferenceColumn) return false
        if (!super.equals(other)) return false

        if (columnReferenceInfo != other.columnReferenceInfo) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + columnReferenceInfo.hashCode()
        return result
    }
}

private fun ParsedSchemalessPetal.asReference(): ColumnReference {
    val accessorName = petalAnnotation.className

    return ColumnReference(
        kotlinTypeName = this.className,
        accessorClassName = ClassName(AccessorClassFileGenerator.PACKAGE_NAME, accessorName),
        tableClassName = ClassName(ExposedEntityGenerator.PACKAGE_NAME, "${accessorName}Table"),
        entityClassName = ClassName(ExposedEntityGenerator.PACKAGE_NAME, "${accessorName}Entity"),
    )
}

internal class ReferencedByPetalColumn private constructor(
    name: String,
    isNullable: Boolean,
    val referencedByColumn: ReferencedByColumn?,
) : ParsedPetalColumn(
    name = name,
    isNullable = isNullable
) {

    companion object {
        fun parseReferencedByColumn(
            parsedPetalSchema: ParsedPetalSchema,
            kotlinProperty: KotlinValue.KotlinProperty,
            referencedBy: ReferencedBy,
            referencedOnPetal: ParsedSchemalessPetal?
        ): ReferencedByPetalColumn {
            checkNotNull(referencedOnPetal) {
                "ReferencedBy annotated column type must be a class or interface annotated with @Petal. Found" +
                        " ${kotlinProperty.typeName} for ReferencedBy column ${kotlinProperty.simpleName} for table" +
                        " ${parsedPetalSchema.tableName}"
            }

            val name = kotlinProperty.simpleName
            val referencedByColumn: ReferencedByColumn =
                referencedOnPetal.asReferencingPetal(referencedBy.referencePropertyName)

            return ReferencedByPetalColumn(
                name = name,
                isNullable = kotlinProperty.isNullable,
                referencedByColumn = referencedByColumn,
            )
        }

        private fun ParsedSchemalessPetal.asReferencingPetal(columnName: String): ReferencedByColumn {
            return ReferencedByColumn(
                columnReference = asReference(),
                columnName = columnName
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReferencedByPetalColumn) return false
        if (!super.equals(other)) return false

        if (referencedByColumn != other.referencedByColumn) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (referencedByColumn?.hashCode() ?: 0)
        return result
    }
}

private fun getDataType(
    petalClasses: Map<ClassName, ParsedSchemalessPetal>,
    kotlinProperty: KotlinValue.KotlinProperty
): String {
    val typeName = kotlinProperty.typeName
    return when (typeName.copy(nullable = false)) {
        String::class.asTypeName() -> getStringTypeName(kotlinProperty.annotatedElement)
        Int::class.asTypeName() -> "INT"
        Long::class.asTypeName() -> "BIGINT"
        UUID::class.asTypeName() -> "uuid"
        else -> getPetalReferenceIdTypeName(petalClasses, typeName)
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

private fun getPetalReferenceIdTypeName(
    petalClasses: Map<ClassName, ParsedSchemalessPetal>,
    typeName: TypeName
): String {
    return petalClasses[typeName.copy(nullable = false)]!!.petalAnnotation.primaryKeyType.dataType
        ?: printThenThrowError(
            "INTERNAL LIBRARY ERROR: Type $typeName was left out of new column sql generation block."
        )
}

private fun getPreviousName(name: String, renameFrom: String?): String? {
    return when {
        renameFrom == null -> null
        renameFrom.isBlank() -> name
        else -> renameFrom
    }
}

data class PetalAlteration(
    val previousName: String? = null,
    val isRename: Boolean,
)
