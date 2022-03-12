package com.casadetasha.kexp.petals.processor.classgenerator.accessor

import com.casadetasha.kexp.petals.processor.UnprocessedPetalColumn
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.asClassName
import java.util.*

internal class AccessorClassInfo(
    packageName: String,
    simpleName: String,
    val columns: Set<UnprocessedPetalColumn>,
    val entityClassName: ClassName
    ) {

    val sortedColumns: SortedSet<UnprocessedPetalColumn> by lazy { columns.toSortedSet() }

    val idKotlinType: ClassName = columns.firstOrNull { it.isId }?.kotlinType ?: Int::class.asClassName()
    val className: ClassName = ClassName(packageName, simpleName)

    val entityMemberName by lazy { MemberName(entityClassName.packageName, entityClassName.simpleName) }
}

