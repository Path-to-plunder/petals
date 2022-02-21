package com.casadetasha.kexp.petals.processor.classgenerator.accessor

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.petals.annotations.PetalColumn
import com.casadetasha.kexp.petals.processor.ktx.kotlinType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import java.util.*
import kotlin.collections.HashSet
import kotlin.reflect.KClass

class AccessorClassInfo(
    packageName: String,
    simpleName: String,
    val columns: Set<PetalColumn>,
    val sourceClassName: ClassName
    ) {

    val idKotlinType: ClassName = columns.firstOrNull { it.isId!! }?.kotlinType ?: Int::class.asClassName()
    val className: ClassName = ClassName(packageName, simpleName)

    val entityMemberName by lazy { MemberName(sourceClassName.packageName, sourceClassName.simpleName) }
}

