package com.casadetasha.kexp.petals.processor.classgenerator.accessor

import com.casadetasha.kexp.petals.annotations.PetalColumn
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@OptIn(KotlinPoetMetadataPreview::class)
internal class AccessorStoreFunSpecBuilder(private val accessorClassInfo: AccessorClassInfo) {

    companion object {
        const val STORE_METHOD_SIMPLE_NAME = "store";
        const val CREATE_METHOD_SIMPLE_NAME = "create";
        const val UPDATE_METHOD_SIMPLE_NAME = "update";
        const val SET_VALUES_METHOD_SIMPLE_NAME = "setValues";

        val TRANSACTION_MEMBER_NAME = MemberName("org.jetbrains.exposed.sql.transactions", "transaction")
    }

    fun getFunSpecs(): Iterable<FunSpec> {
        return setOf(
            createStoreFunction(),
            createCreateFunction(),
            createUpdateFunction(),
            createSetValuesFunction()
        )
    }

    private fun createStoreFunction(): FunSpec {
        return FunSpec.builder(STORE_METHOD_SIMPLE_NAME)
            .returns(accessorClassInfo.className.copy(nullable = true))
            .addCode(StoreFunctionParser().methodBody)
            .build()
    }

    private fun createCreateFunction(): FunSpec {
        return FunSpec.builder(CREATE_METHOD_SIMPLE_NAME)
            .addModifiers(KModifier.PRIVATE)
            .returns(accessorClassInfo.className)
            .addCode(CreateFunctionParser(accessorClassInfo).methodBody)
            .build()
    }

    private fun createUpdateFunction(): FunSpec {
        return FunSpec.builder(UPDATE_METHOD_SIMPLE_NAME)
            .addModifiers(KModifier.PRIVATE)
            .returns(accessorClassInfo.className)
            .addCode(UpdateFunctionParser().methodBody)
            .build()
    }

    private fun createSetValuesFunction(): FunSpec {
        return FunSpec.builder(SET_VALUES_METHOD_SIMPLE_NAME)
            .addModifiers(KModifier.PRIVATE)
            .receiver(accessorClassInfo.sourceClassName)
            .returns(accessorClassInfo.sourceClassName)
            .addCode(SetValuesFunctionParser(accessorClassInfo).methodBody)
            .build()
    }

    private class StoreFunctionParser {
        val methodBody: CodeBlock by lazy {
            CodeBlock.builder()
                .add("val entity = when (isStored)")
                .beginControlFlow("")
                .addStatement("false -> create()")
                .addStatement("true -> update()")
                .endControlFlow()
                .add("return entity")
                .build()
        }
    }

    private class CreateFunctionParser(accessorClassInfo: AccessorClassInfo) {

        val methodBody: CodeBlock by lazy {
            val petalSimpleName = accessorClassInfo.className.simpleName
            val entityName = accessorClassInfo.entityMemberName
            CodeBlock.builder()
                .beginControlFlow("return %M", TRANSACTION_MEMBER_NAME)
                .beginControlFlow("when (val petalId = this@${petalSimpleName}.id) ")
                .addStatement("null -> %M.new { this.setValues() }", entityName)
                .addStatement("else -> ${entityName.simpleName}.new(petalId) { this.setValues() }")
                .addStatement("}")
                .unindent()
                .unindent()
                .add("}.export()")
                .build()
        }
    }

    private class UpdateFunctionParser {

        val methodBody: CodeBlock by lazy {
            CodeBlock.builder()
                .beginControlFlow("return %M", TRANSACTION_MEMBER_NAME)
                .beginControlFlow("val entity = checkNotNull(findBackingEntity())")
                .addStatement("%S", "Could not update petal, no ID match found in DB.")
                .endControlFlow()
                .addStatement("entity.setValues()")
                .unindent()
                .add("}.export()")
                .build()
        }
    }

    private class SetValuesFunctionParser(accessorClassInfo: AccessorClassInfo) {

        val nonIdColumns: Iterable<PetalColumn> by lazy {
            accessorClassInfo.columns.filterNot { it.isId!! }
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
