package com.casadetasha.kexp.petals.processor.classgenerator.accessor.functions

import com.casadetasha.kexp.petals.processor.classgenerator.accessor.AccessorClassInfo
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@OptIn(KotlinPoetMetadataPreview::class)
internal class AccessorFindBackingEntityFunSpecBuilder(private val accessorClassInfo: AccessorClassInfo) {

    fun getPropertySpec(): PropertySpec {
        return PropertySpec.builder("entity", accessorClassInfo.entityClassName)
            .addModifiers(KModifier.PRIVATE)
            .delegate(FindBackingEntityFunctionParser().methodBody)
            .build()
    }

    private class FindBackingEntityFunctionParser {
        val methodBody: CodeBlock by lazy {
            CodeBlock.builder()
                .beginControlFlow("lazy ")
                .addStatement("checkNotNull(dbEntity) { %S }", "Null petal ID found even though isStored is true")
                .unindent()
                .addStatement("}")
                .build()
        }
    }
}

internal fun TypeSpec.Builder.addEntityDelegate(accessorClassInfo: AccessorClassInfo) = apply {
//    this.addProperty(NestedPetalPropertySpecListBuilder(accessorClassInfo).getPropertySpec())
}
