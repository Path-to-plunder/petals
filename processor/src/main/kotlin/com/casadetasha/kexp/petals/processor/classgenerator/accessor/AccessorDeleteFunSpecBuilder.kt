package com.casadetasha.kexp.petals.processor.classgenerator.accessor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@OptIn(KotlinPoetMetadataPreview::class)
internal class AccessorDeleteFunSpecBuilder() {

    companion object {
        const val METHOD_SIMPLE_NAME = "delete";

        val TRANSACTION_MEMBER_NAME = MemberName("org.jetbrains.exposed.sql.transactions", "transaction")
    }

    fun getFunSpec(): FunSpec {
        return FunSpec.builder(METHOD_SIMPLE_NAME)
            .addCode(FindBackingEntityFunctionParser().methodBody)
            .build()
    }

    private class FindBackingEntityFunctionParser() {
        val methodBody: CodeBlock by lazy {
            CodeBlock.builder()
                .addStatement("if (!isStored) { return }")
                .addStatement("")
                .addStatement("%M { findBackingEntity()?.delete() }", TRANSACTION_MEMBER_NAME)
                .build()
        }
    }
}
