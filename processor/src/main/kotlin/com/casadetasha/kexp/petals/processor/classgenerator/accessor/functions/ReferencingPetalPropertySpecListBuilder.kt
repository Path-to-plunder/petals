package com.casadetasha.kexp.petals.processor.classgenerator.accessor.functions

import com.casadetasha.kexp.petals.processor.UnprocessedPetalColumn
import com.casadetasha.kexp.petals.processor.classgenerator.accessor.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.classgenerator.accessor.functions.AccessorCreateFunSpecBuilder.Companion.TRANSACTION_MEMBER_NAME
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@OptIn(KotlinPoetMetadataPreview::class)
internal class ReferencingPetalPropertySpecListBuilder(
    private val column: UnprocessedPetalColumn
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

internal fun TypeSpec.Builder.addReferencingPetalPropertySpec(accessorClassInfo: AccessorClassInfo) = apply {
    addFunctions(
        accessorClassInfo.columns
            .filter { it.isReferencedByColumn }
            .map { ReferencingPetalPropertySpecListBuilder(it).referencedByFunSpec }
    )
}

private fun String.uppercaseFirstChar(): String = replaceFirstChar { it.uppercase() }
