package com.casadetasha.kexp.petals.processor.classgenerator.accessor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@OptIn(KotlinPoetMetadataPreview::class)
internal class AccessorLoadFunSpecBuilder {

    companion object {
        const val METHOD_SIMPLE_NAME = "load";
    }

    fun getFunSpec(accessorClassInfo: AccessorClassInfo): FunSpec {
        val statementParser = AccessorKtxFunctionParser(accessorClassInfo)
        return FunSpec.builder(METHOD_SIMPLE_NAME)
            .addParameter(ParameterSpec.builder("id", accessorClassInfo.idKotlinType).build())
            .returns(accessorClassInfo.className.copy(nullable = true))
            .addCode(statementParser.methodStatement)
            .build()
    }

    private class AccessorKtxFunctionParser(private val accessorClassInfo: AccessorClassInfo) {

        val methodStatement: CodeBlock by lazy {
            CodeBlock.builder()
                .beginControlFlow("return %M", TRANSACTION_MEMBER_NAME)
                .addStatement("%M.findById(id)", accessorClassInfo.entityMemberName)
                .unindent()
                .add("}?.export()")
                .build()
        }

        companion object {
            val TRANSACTION_MEMBER_NAME = MemberName("org.jetbrains.exposed.sql.transactions", "transaction")
        }
    }
}
