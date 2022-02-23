package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.petals.annotations.PetalColumn
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import java.util.*

internal class UnprocessedPetalColumn(
    val petalColumn: PetalColumn,
    val previousName: String? = null,
    val isAlteration: Boolean,
    val isId: Boolean,
    val defaultValue: String? = null,
) : Comparable<UnprocessedPetalColumn> {

    val dataType = petalColumn.dataType
    val name = petalColumn.name
    val isNullable = petalColumn.isNullable

    constructor(
        name: String,
        dataType: String,
        isNullable: Boolean,
        previousName: String? = null,
        isAlteration: Boolean,
        isId: Boolean,
        defaultValue: String? = null,
    ) : this(
        petalColumn = PetalColumn(
            name = name,
            dataType = dataType,
            isNullable = isNullable
        ),
        previousName,
        isAlteration,
        isId,
        defaultValue
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
        result = 31 * result + (isAlteration?.hashCode() ?: 0)
        result = 31 * result + (isId?.hashCode() ?: 0)
        result = 31 * result + (defaultValue?.hashCode() ?: 0)
        return result
    }

    override fun compareTo(other: UnprocessedPetalColumn): Int {
        return petalColumn.compareTo(other.petalColumn)
    }
}
