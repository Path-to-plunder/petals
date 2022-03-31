package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions

import com.casadetasha.kexp.petals.processor.model.columns.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.AccessorExportFunSpecBuilder.Companion.EXPORT_METHOD_SIMPLE_NAME
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@OptIn(KotlinPoetMetadataPreview::class)
internal class AccessorEagerLoadDependenciesFunSpecBuilder(private val accessorClassInfo: com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo) {

    val petalEagerLoadDependenciesFunSpec by lazy {
        FunSpec.builder(PETAL_EAGER_LOAD_DEPENDENCIES_METHOD_SIMPLE_NAME)
            .returns(accessorClassInfo.className)
            .addModifiers(KModifier.OVERRIDE, KModifier.PROTECTED)
            .addCode(petalEagerLoadMethodBody)
            .build()
    }

    val companionEagerLoadDependenciesFunSpec by lazy {
        FunSpec.builder(COMPANION_EAGER_LOAD_DEPENDENCIES_METHOD_SIMPLE_NAME)
            .receiver(accessorClassInfo.entityClassName)
            .returns(accessorClassInfo.className)
            .addCode(companionEagerLoadMethodBody)
            .build()
    }

    private val petalEagerLoadMethodBody: CodeBlock by lazy {
        CodeBlock.builder()
            .beginControlFlow("return apply")
            .apply {
                accessorClassInfo.petalColumns
                    .filterIsInstance<PetalReferenceColumn>()
                    .forEach {
                        addStatement("${it.name}NestedPetalManager.eagerLoadAccessor()")
                    }
            }
            .endControlFlow()
            .build()
    }

    private val companionEagerLoadMethodBody: CodeBlock by lazy {
        CodeBlock.builder()
            .add("return load(")
            .apply {
                accessorClassInfo.petalColumns
                    .filterIsInstance<PetalReferenceColumn>()
                    .forEach {
                        add("\n  %M::${it.name},", accessorClassInfo.entityMemberName)
                    }
            }
            .add("\n).$EXPORT_METHOD_SIMPLE_NAME().eagerLoadDependencies()")
            .build()
    }

    companion object {
        const val COMPANION_EAGER_LOAD_DEPENDENCIES_METHOD_SIMPLE_NAME = "toPetalWithEagerLoadedDependencies"
        const val PETAL_EAGER_LOAD_DEPENDENCIES_METHOD_SIMPLE_NAME = "eagerLoadDependenciesInsideTransaction"
    }

//    override fun eagerLoadDependenciesInsideTransaction(): ParentPetalClass = apply {
//        nestedPetalNestedPetalManager.eagerLoadAccessor()
//    }

//    public fun ParentPetalClassEntity.exportWithEagerLoadedDependencies(): ParentPetalClass =
//        load(
//            com.casadetasha.kexp.petals.ParentPetalClassEntity::nestedPetal,
//        ).export().eagerLoadDependencies()
}
