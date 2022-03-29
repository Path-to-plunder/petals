package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor

import com.casadetasha.kexp.petals.processor.model.UnprocessedPetalColumn
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.asClassName
import java.util.*

internal class AccessorClassInfo constructor(
    packageName: String,
    simpleName: String,
    val columns: Set<UnprocessedPetalColumn>,
    val entityClassName: ClassName,
    val tableClassName: ClassName,
    val dataClassName: ClassName
    ) {

    val sortedColumns: SortedSet<UnprocessedPetalColumn> by lazy { columns.toSortedSet() }

    val idKotlinClassName: ClassName = columns.firstOrNull { it.isId }?.kotlinType ?: Int::class.asClassName()
    val className: ClassName = ClassName(packageName, simpleName)

    val entityMemberName by lazy { MemberName(entityClassName.packageName, entityClassName.simpleName) }
    val tableMemberName by lazy { MemberName(tableClassName.packageName, tableClassName.simpleName) }
}

