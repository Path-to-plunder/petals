package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@OptIn(KotlinPoetMetadataPreview::class)
internal class AccessorFindBackingEntityFunSpecBuilder(private val accessorClassInfo: com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo) {

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

internal fun TypeSpec.Builder.addEntityDelegate(accessorClassInfo: com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo) = apply {
//    this.addProperty(NestedPetalPropertySpecListBuilder(accessorClassInfo).getPropertySpec())
}
