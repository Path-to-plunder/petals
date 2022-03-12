package com.casadetasha.kexp.petals.processor.classgenerator.accessor.functions

import com.casadetasha.kexp.petals.processor.UnprocessedPetalColumn
import com.casadetasha.kexp.petals.processor.classgenerator.accessor.AccessorClassInfo
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@OptIn(KotlinPoetMetadataPreview::class)
internal class AccessorStoreFunSpecBuilder(private val accessorClassInfo: AccessorClassInfo) {

    companion object {
        const val STORE_METHOD_SIMPLE_NAME = "store"
        const val SET_VALUES_METHOD_SIMPLE_NAME = "storeValuesInBackend"

        val TRANSACTION_MEMBER_NAME = MemberName("org.jetbrains.exposed.sql.transactions", "transaction")
    }

    fun getFunSpecs(): Iterable<FunSpec> {
        return setOf(
            createStoreFunction(),
            createSetValuesFunction()
        )
    }


    private fun createStoreFunction(): FunSpec {
        return FunSpec.builder(STORE_METHOD_SIMPLE_NAME)
            .addModifiers(KModifier.PRIVATE)
            .returns(accessorClassInfo.className)
            .addCode(StoreFunctionParser().methodBody)
            .build()
    }

    private fun createSetValuesFunction(): FunSpec {
        return FunSpec.builder(SET_VALUES_METHOD_SIMPLE_NAME)
            .addModifiers(KModifier.PRIVATE)
            .receiver(accessorClassInfo.entityClassName)
            .returns(accessorClassInfo.entityClassName)
            .addCode(SetValuesFunctionParser(accessorClassInfo).methodBody)
            .build()
    }

    private class StoreFunctionParser {

        val methodBody: CodeBlock by lazy {
            CodeBlock.builder()
                .beginControlFlow("return %M", TRANSACTION_MEMBER_NAME)
                .addStatement("return@transaction entity.$SET_VALUES_METHOD_SIMPLE_NAME()")
                .unindent()
                .add("}.export()")
                .build()
        }
    }

    private class SetValuesFunctionParser(accessorClassInfo: AccessorClassInfo) {

        val nonIdColumns: Iterable<UnprocessedPetalColumn> by lazy {
            accessorClassInfo.columns.filterNot { it.isId }
        }

        val methodBody: CodeBlock by lazy {
            val codeBlockBuilder = CodeBlock.builder()
            val classSimpleName = accessorClassInfo.className.simpleName
            nonIdColumns.forEach {
                    val name = it.name
                    codeBlockBuilder.addStatement("$name = this@${classSimpleName}.$name")
            }
            codeBlockBuilder.addStatement("return this").build()
        }
    }
}

internal fun TypeSpec.Builder.addStoreMethod(accessorClassInfo: AccessorClassInfo) = apply {
    this.addFunctions(AccessorStoreFunSpecBuilder(accessorClassInfo).getFunSpecs())
}
