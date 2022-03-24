package com.casadetasha.kexp.petals.processor.classgenerator.accessor.functions

import com.casadetasha.kexp.petals.processor.classgenerator.accessor.AccessorClassInfo
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@OptIn(KotlinPoetMetadataPreview::class)
internal class AccessorCreateFunSpecBuilder(private val accessorClassInfo: AccessorClassInfo) {

    companion object {
        const val CREATE_METHOD_SIMPLE_NAME = "create"

        val TRANSACTION_MEMBER_NAME = MemberName("org.jetbrains.exposed.sql.transactions", "transaction")
    }

    fun getFunSpec(): FunSpec {
        return FunSpec.builder(CREATE_METHOD_SIMPLE_NAME)
            .returns(accessorClassInfo.className)
            .addParameters(AccessorClassInfoCreateFunParameterSpec(accessorClassInfo).parameterSpecs)
            .addCode(CreateFunctionParser(accessorClassInfo).methodBody)
            .build()
    }

    private class CreateFunctionParser(accessorClassInfo: AccessorClassInfo) {

        val methodBody: CodeBlock by lazy {
            val entityMemberName = accessorClassInfo.entityMemberName
            CodeBlock.builder()
                .beginControlFlow("return %M", TRANSACTION_MEMBER_NAME)
                .beginControlFlow("val storeValues: %M.() -> Unit = ", entityMemberName)
                .add(AssignAccessorValuesCodeBlockBuilder(accessorClassInfo).assignValuesCodeBlock)
                .endControlFlow()
                .beginControlFlow("return@transaction when (id) ")
                .addStatement("null -> %M.new { storeValues() }", entityMemberName)
                .addStatement("else -> %M.new(id) { storeValues() }", entityMemberName)
                .addStatement("}")
                .unindent()
                .unindent()
                .add("}.export()")
                .build()
        }
    }
}

private class AssignAccessorValuesCodeBlockBuilder(accessorClassInfo: AccessorClassInfo) {

    val assignValuesCodeBlock: CodeBlock by lazy {
        val nonIdColumns = accessorClassInfo.columns.filterNot { it.isId }
        val valueColumns = nonIdColumns.filter { it.columnReferenceInfo == null }
        val referenceColumns = nonIdColumns.filterNot { it.columnReferenceInfo == null }

        val builder = CodeBlock.builder()

        valueColumns
            .forEach { column ->
                builder.addStatement("this.%L = %L", column.name, column.name)
            }

        referenceColumns
            .forEach { column ->
            builder.addStatement("this.%L = %L", column.name, "${column.name}.dbEntity")
        }

        return@lazy builder.build()
    }
}

internal fun TypeSpec.Builder.addCreateMethod(accessorClassInfo: AccessorClassInfo) = apply {
    this.addFunction(AccessorCreateFunSpecBuilder(accessorClassInfo).getFunSpec())
}
