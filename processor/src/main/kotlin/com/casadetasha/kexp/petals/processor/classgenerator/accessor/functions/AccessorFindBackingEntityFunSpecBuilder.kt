package com.casadetasha.kexp.petals.processor.classgenerator.accessor.functions

import com.casadetasha.kexp.petals.processor.classgenerator.accessor.AccessorClassInfo
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@OptIn(KotlinPoetMetadataPreview::class)
internal class AccessorFindBackingEntityFunSpecBuilder(private val accessorClassInfo: AccessorClassInfo) {

    companion object {
        const val METHOD_SIMPLE_NAME = "findBackingEntity"
    }

    fun getFunSpec(): FunSpec {
        return FunSpec.builder(METHOD_SIMPLE_NAME)
            .addModifiers(KModifier.PRIVATE)
            .returns(accessorClassInfo.sourceClassName.copy(nullable = true))
            .addCode(FindBackingEntityFunctionParser(accessorClassInfo).methodBody)
            .build()
    }

    private class FindBackingEntityFunctionParser(accessorClassInfo: AccessorClassInfo) {
        val methodBody: CodeBlock by lazy {
            CodeBlock.builder()
                .addStatement("checkNotNull(id) { %S }", "Null petal ID found even though isStored is true")
                .addStatement("return %M.findById(id)", accessorClassInfo.entityMemberName)
                .build()
        }
    }
}

internal fun TypeSpec.Builder.addFindEntityMethod(accessorClassInfo: AccessorClassInfo) = apply {
    this.addFunction(AccessorFindBackingEntityFunSpecBuilder(accessorClassInfo).getFunSpec())
}
