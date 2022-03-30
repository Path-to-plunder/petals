package com.casadetasha.kexp.petals.processor.model

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.petals.annotations.PetalColumn
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import java.util.*

internal class UnprocessedPetalColumn private constructor(
    val isId: Boolean,
    val petalColumn: PetalColumn,
    val previousName: String? = null,
    val isAlteration: Boolean,
    val defaultValue: DefaultPetalValue?,
    val isMutable: Boolean,
    val columnReferenceInfo: ColumnReference? = null,
    val referencedByColumn: ReferencedByColumn?,
    val isRename: Boolean
) : Comparable<UnprocessedPetalColumn> {

    val dataType = petalColumn.dataType
    val name = petalColumn.name
    val isNullable = petalColumn.isNullable

    val isReferencedByColumn: Boolean = referencedByColumn != null
    val isLocalColumn: Boolean = !isReferencedByColumn
    val isReferenceColumn: Boolean = isLocalColumn && columnReferenceInfo != null

    val referencingTableClassName: ClassName? = columnReferenceInfo?.tableClassName
    val referencingEntityClassName: ClassName? = columnReferenceInfo?.entityClassName
    val referencingAccessorClassName: ClassName? = columnReferenceInfo?.accessorClassName
    val referencingIdName: String? = when (columnReferenceInfo) {
        null -> null
        else -> "${name}Id"
    }
    val nestedPetalManagerName: String? = when(isReferenceColumn) {
        true -> "${name}NestedPetalManager"
        false -> null
    }

    val tablePropertyClassName: TypeName by lazy {
        when (isReferenceColumn) {
            true -> Column::class.asClassName()
                .parameterizedBy(
                    EntityID::class.asClassName()
                        .parameterizedBy(kotlinType.copy(nullable = false))
                        .copy(nullable = isNullable)
                )

            false -> Column::class.asClassName()
                .parameterizedBy(kotlinType.copy(nullable = isNullable))
        }
    }

    val entityPropertyClassName: TypeName by lazy {
        when (isReferenceColumn) {
            true -> referencingEntityClassName!!
            else -> kotlinType.copy(nullable = isNullable)
        }
    }

    constructor(
        name: String,
        dataType: String,
        isNullable: Boolean,
        isMutable: Boolean,
        isId: Boolean,
        defaultValue: DefaultPetalValue?,
        previousName: String? = null,
        isAlteration: Boolean,
        columnReference: ColumnReference? = null,
        referencedByColumn: ReferencedByColumn?,
        isRename: Boolean
    ) : this(
        petalColumn = PetalColumn(
            name = name,
            dataType = dataType,
            isNullable = isNullable
        ),
        columnReferenceInfo = columnReference,
        previousName = previousName,
        isAlteration = isAlteration,
        isId = isId,
        defaultValue = defaultValue,
        isMutable = isMutable,
        referencedByColumn = referencedByColumn,
        isRename = isRename
    )

    val kotlinType: ClassName by lazy {
        if (dataType.startsWith("CHARACTER VARYING")) {
            return@lazy String::class.asClassName()
        }

        return@lazy when (val type = dataType) {
            "uuid" -> UUID::class
            "TEXT" -> String::class
            "INT" -> Int::class
            "BIGINT" -> Long::class
            else -> AnnotationParser.printThenThrowError(
                "INTERNAL LIBRARY ERROR: unsupported datatype ($type) found while" +
                        " parsing column for accessor"
            )
        }.asClassName()
    }

    fun process(): PetalColumn = petalColumn

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UnprocessedPetalColumn) return false

        if (petalColumn != other.petalColumn) return false
        if (previousName != other.previousName) return false
        if (isAlteration != other.isAlteration) return false
        if (isId != other.isId) return false
        if (defaultValue != other.defaultValue) return false

        return true
    }

    override fun hashCode(): Int {
        var result = petalColumn.hashCode()
        result = 31 * result + (previousName?.hashCode() ?: 0)
        result = 31 * result + isAlteration.hashCode()
        result = 31 * result + isId.hashCode()
        result = 31 * result + (defaultValue?.hashCode() ?: 0)
        return result
    }

    override fun compareTo(other: UnprocessedPetalColumn): Int {
        return petalColumn.compareTo(other.petalColumn)
    }
}
