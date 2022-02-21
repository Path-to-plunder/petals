package com.casadetasha.kexp.petals.processor.ktx

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.petals.annotations.PetalColumn
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import java.util.*

internal val PetalColumn.kotlinType: ClassName
    get() {
        if (dataType.startsWith("CHARACTER VARYING")) {
            return String::class.asClassName()
        }

        return when (val type = dataType) {
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