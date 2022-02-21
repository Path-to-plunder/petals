package com.casadetasha.kexp.petals.processor.classgenerator.accessor

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.petals.annotations.PetalColumn
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import java.util.*
import kotlin.collections.HashSet

class AccessorClassInfo(
    packageName: String,
    simpleName: String,
    val columns: Set<PetalColumn>,
    val sourceClassName: ClassName
    ) {

    val className: ClassName = ClassName(packageName, simpleName)
}

