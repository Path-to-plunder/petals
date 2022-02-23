package com.casadetasha.kexp.petals.processor.classgenerator.accessor.functions

import com.casadetasha.kexp.petals.processor.UnprocessedPetalColumn
import com.casadetasha.kexp.petals.processor.classgenerator.accessor.AccessorClassInfo
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import kotlinx.serialization.Transient

@OptIn(KotlinPoetMetadataPreview::class)
internal class AccessorStoreFunSpecBuilder(private val accessorClassInfo: AccessorClassInfo) {

    companion object {
        const val STORE_METHOD_SIMPLE_NAME = "store"
        const val CREATE_METHOD_SIMPLE_NAME = "create"
        const val UPDATE_METHOD_SIMPLE_NAME = "update"
        const val SET_VALUES_METHOD_SIMPLE_NAME = "storeValuesInBackend"

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
            .returns(accessorClassInfo.className)
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
                .addStatement("null -> %M.new { this.$SET_VALUES_METHOD_SIMPLE_NAME() }", entityName)
                .addStatement("else -> ${entityName.simpleName}.new(petalId) { this.$SET_VALUES_METHOD_SIMPLE_NAME() }")
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
                .addStatement("entity.$SET_VALUES_METHOD_SIMPLE_NAME()")
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
    this.addIsStoredParam()
        .addFunctions(AccessorStoreFunSpecBuilder(accessorClassInfo).getFunSpecs())
}

private fun TypeSpec.Builder.addIsStoredParam() = apply {
    addProperty(
        PropertySpec.builder("_isStored", Boolean::class.asClassName(), KModifier.PRIVATE)
            .addAnnotation(Transient::class)
            .initializer("false")
            .mutable()
            .build())
    addProperty(
        PropertySpec.builder("isStored", Boolean::class.asClassName(), KModifier.PUBLIC)
            .getter(FunSpec.getterBuilder()
                .addAnnotation(Synchronized::class)
                .addStatement("return _isStored")
                .build()
            )
            .mutable()
            .setter(FunSpec.setterBuilder()
                .addParameter("value", Boolean::class.asClassName())
                .addModifiers(KModifier.PRIVATE)
                .addAnnotation(Synchronized::class)
                .addStatement("_isStored = value")
                .build()
            )
            .build())
}
