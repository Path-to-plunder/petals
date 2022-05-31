package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions

import com.casadetasha.kexp.petals.processor.model.columns.*
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.AccessorExportFunSpecBuilder.Companion.EXPORT_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.CreateMethodNames.CREATE_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.CreateMethodNames.TRANSACTION_MEMBER_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.CodeTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.FunctionTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.ParameterTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.ParameterTemplate.Companion.collectParameterTemplates
import com.squareup.kotlinpoet.*

internal fun createCreateFunctionTemplate(accessorClassInfo: AccessorClassInfo) =
    FunctionTemplate(
        name = CREATE_METHOD_SIMPLE_NAME,
        returnType = accessorClassInfo.className
    ) {
        collectParameterTemplates {
            accessorClassInfo.localColumns.map { it.asCreateFunctionParameterTemplate() }
        }

        writeCode {
            createCreateFunctionMethodBodyTemplate(accessorClassInfo)
        }
    }


private fun createCreateFunctionMethodBodyTemplate(accessorClassInfo: AccessorClassInfo): CodeTemplate {
    val entityMemberName = accessorClassInfo.entityMemberName
    return CodeTemplate(
        CodeBlock.builder()
            .beginControlFlow("return %M", TRANSACTION_MEMBER_NAME)
            .beginControlFlow("val storeValues: %M.() -> Unit = ", entityMemberName)
            .add(createAssignAccessorValuesCodeBlock(accessorClassInfo))
            .endControlFlow()
            .beginControlFlow("return@transaction when (id) ")
            .addStatement("null -> %M.new { storeValues() }", entityMemberName)
            .addStatement("else -> %M.new(id) { storeValues() }", entityMemberName)
            .addStatement("}")
            .unindent()
            .unindent()
            .add("}.$EXPORT_METHOD_SIMPLE_NAME()")
            .build()
    )
}

private fun createAssignAccessorValuesCodeBlock(accessorClassInfo: AccessorClassInfo): CodeBlock {
    val entityColumns = accessorClassInfo.localColumns
        .filterNot { it is PetalIdColumn }

    val valueColumns = entityColumns.filterIsInstance<PetalValueColumn>()
    val referenceColumns = entityColumns.filterIsInstance<PetalReferenceColumn>()

    val builder = CodeBlock.builder()

    valueColumns
        .forEach { column ->
            builder.addStatement("this.%L = %L", column.name, column.name)
        }

    referenceColumns
        .forEach { column ->
            val name = column.name + if (column.isNullable) {
                "?"
            } else {
                ""
            }
            builder.addStatement("this.%L = %L", column.name, "$name.dbEntity")
        }

    return builder.build()
}

internal object CreateMethodNames {
    const val CREATE_METHOD_SIMPLE_NAME = "create"

    val TRANSACTION_MEMBER_NAME = MemberName("org.jetbrains.exposed.sql.transactions", "transaction")
}
