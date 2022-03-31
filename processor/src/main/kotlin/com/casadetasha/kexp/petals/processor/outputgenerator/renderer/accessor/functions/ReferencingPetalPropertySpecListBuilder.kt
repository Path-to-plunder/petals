package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions

import com.casadetasha.kexp.petals.processor.model.columns.ReferencedByPetalColumn
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.AccessorCreateFunSpecBuilder.Companion.TRANSACTION_MEMBER_NAME
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@OptIn(KotlinPoetMetadataPreview::class)
internal class ReferencingPetalPropertySpecListBuilder(
    private val column: ReferencedByPetalColumn
) {

    internal val referencedByFunSpec: FunSpec by lazy {
        FunSpec.builder("load${column.name.uppercaseFirstChar()}")
            .returns(
                List::class.asClassName()
                    .parameterizedBy(column.referencedByColumn!!.columnReference.accessorClassName)
            )
            .addStatement(
                "return %M { dbEntity.${column.name}.map{ it.%M() } }",
                TRANSACTION_MEMBER_NAME,
                MemberName("${column.referencedByColumn.columnReference.accessorClassName}.Companion", "toPetal")
            )
            .build()
    }
}

internal fun TypeSpec.Builder.addReferencingPetalPropertySpec(accessorClassInfo: com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo) = apply {
    addFunctions(
        accessorClassInfo.petalColumns
            .filterIsInstance<ReferencedByPetalColumn>()
            .map { ReferencingPetalPropertySpecListBuilder(it).referencedByFunSpec }
    )
}

private fun String.uppercaseFirstChar(): String = replaceFirstChar { it.uppercase() }
