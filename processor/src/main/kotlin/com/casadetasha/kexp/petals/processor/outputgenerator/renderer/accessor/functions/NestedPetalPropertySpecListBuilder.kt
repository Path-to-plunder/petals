package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions

import com.casadetasha.kexp.petals.annotations.NestedPetalManager
import com.casadetasha.kexp.petals.annotations.OptionalNestedPetalManager
import com.casadetasha.kexp.petals.processor.model.columns.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.ExportMethodNames.EXPORT_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.CodeTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.CodeTemplate.Companion.codeTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.KotlinTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.PropertyTemplate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

internal fun ClassName.toMemberName(): MemberName = MemberName(packageName, simpleName)

internal fun createNestedPetalPropertyTemplates(accessorClassInfo: AccessorClassInfo): List<PropertyTemplate> {
    return accessorClassInfo.petalColumns
        .filterIsInstance<PetalReferenceColumn>()
        .map { it.toNestedPetalPropertyTemplate() }
        .flatten()
}

private fun PetalReferenceColumn.toNestedPetalPropertyTemplate(): List<PropertyTemplate> = listOf(
    this.asNestedPetalManagerPropertyTemplate(),
    this.nestedPetalIdPropertyTemplate(),
    this.nestedPetalAccessorPropertyTemplate()
)


private fun PetalReferenceColumn.asNestedPetalManagerPropertyTemplate(): PropertyTemplate {
    return PropertyTemplate(name = nestedPetalManagerName, typeName = getNestedPetalManagerClassName()) {
        delegate { petalManagerMethodBody() }
        visibility { KotlinTemplate.Visibility.PRIVATE }
    }
}

private fun PetalReferenceColumn.getNestedPetalManagerClassName(): ParameterizedTypeName {
    return when (this.isNullable) {
        false -> NestedPetalManager::class.asClassName()
        true -> OptionalNestedPetalManager::class.asClassName()
    }.parameterizedBy(
        referencingAccessorClassName,
        referencingEntityClassName,
        kotlinType
    )
}

private fun PetalReferenceColumn.petalManagerMethodBody(): CodeTemplate {
    val nullabilityExtension = if (isNullable) {
        "?"
    } else {
        ""
    }
    return codeTemplate {
        CodeBlock.builder()
            .beginControlFlow("lazy")
            .addStatement(
                "%M(%L) { dbEntity.%L$nullabilityExtension.%M() }",
                nestedPetalManagerClassName().toMemberName(),
                referencingIdName,
                name,
                ClassName(
                    "${referencingAccessorClassName.packageName}.${referencingAccessorClassName.simpleName}.Companion",
                    EXPORT_METHOD_SIMPLE_NAME
                ).toMemberName()
            )
            .endControlFlow()
            .build()
    }
}

private fun PetalReferenceColumn.nestedPetalManagerClassName(): ClassName {
    return when (isNullable) {
        false -> NestedPetalManager::class.asClassName()
        true -> OptionalNestedPetalManager::class.asClassName()
    }
}

private fun PetalReferenceColumn.nestedPetalIdPropertyTemplate(): PropertyTemplate {
    return PropertyTemplate(name = referencingIdName, typeName = kotlinType.copy(nullable = isNullable)) {
        delegate { CodeTemplate("${nestedPetalManagerName}::nestedPetalId") }
    }
}

private fun PetalReferenceColumn.nestedPetalAccessorPropertyTemplate(): PropertyTemplate {
    return PropertyTemplate(
        name = name,
        typeName = referencingAccessorClassName.copy(nullable = isNullable),
        isMutable = true
    )
    {
        delegate {
            CodeTemplate("${nestedPetalManagerName}::nestedPetal")
        }
    }
}

