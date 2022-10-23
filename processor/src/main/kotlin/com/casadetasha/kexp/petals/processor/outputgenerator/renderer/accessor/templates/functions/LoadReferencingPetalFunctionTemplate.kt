package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.templates.functions

import com.casadetasha.kexp.petals.processor.model.columns.ReferencedByPetalColumn
import com.casadetasha.kexp.petals.processor.model.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.CreateMethodNames.TRANSACTION_MEMBER_NAME
import com.casadetasha.kexp.generationdsl.dsl.FunctionTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.ExportMethodNames.EXPORT_PETAL_METHOD_SIMPLE_NAME
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import uppercaseFirstChar

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
        generateMethodBody(
            "return %M { dbEntity.${name}.map{ it.${EXPORT_PETAL_METHOD_SIMPLE_NAME}() } }",
            TRANSACTION_MEMBER_NAME,
        )
    }
}
