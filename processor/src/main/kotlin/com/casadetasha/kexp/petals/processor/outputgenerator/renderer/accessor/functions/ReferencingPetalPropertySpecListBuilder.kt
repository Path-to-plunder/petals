package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions

import com.casadetasha.kexp.petals.processor.model.columns.ReferencedByPetalColumn
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.CreateMethodNames.TRANSACTION_MEMBER_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.FunctionTemplate
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName

internal fun createLoadReferencingPetalFunctionTemplate(accessorClassInfo: AccessorClassInfo): List<FunctionTemplate> =
    accessorClassInfo.petalColumns
        .filterIsInstance<ReferencedByPetalColumn>()
        .map { it.asReferencingPetalFunctionTemplate() }

private fun ReferencedByPetalColumn.asReferencingPetalFunctionTemplate(): FunctionTemplate {
    val returnType = List::class.asClassName()
        .parameterizedBy(referencedByColumn.columnReference.accessorClassName)

    return FunctionTemplate(
        name = "load${name.uppercaseFirstChar()}",
        returnType = returnType
    ) {
        methodBody(
            "return %M { dbEntity.${name}.map{ it.%M() } }",
            TRANSACTION_MEMBER_NAME,
            MemberName("${referencedByColumn.columnReference.accessorClassName}.Companion", "toPetal")
        )
    }
}

private fun String.uppercaseFirstChar(): String = replaceFirstChar { it.uppercase() }
