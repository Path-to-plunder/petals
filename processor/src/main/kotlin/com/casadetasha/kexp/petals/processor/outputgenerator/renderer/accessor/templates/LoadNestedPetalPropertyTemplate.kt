package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.templates

import com.casadetasha.kexp.petals.annotations.NestedPetalManager
import com.casadetasha.kexp.petals.annotations.OptionalNestedPetalManager
import com.casadetasha.kexp.petals.processor.model.columns.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.model.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.ExportMethodNames.EXPORT_PETAL_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.generationdsl.dsl.CodeTemplate
import com.casadetasha.kexp.generationdsl.dsl.KotlinTemplate
import com.casadetasha.kexp.generationdsl.dsl.PropertyTemplate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import getNullabilityExtension

internal fun ClassName.toMemberName(): MemberName = MemberName(packageName, simpleName)

internal fun createNestedPetalPropertyTemplates(accessorClassInfo: AccessorClassInfo): List<PropertyTemplate> {
    return accessorClassInfo.petalColumns
        .filterIsInstance<PetalReferenceColumn>()
        .map { it.toNestedPetalPropertyTemplate() }
        .flatten()
}

private fun PetalReferenceColumn.toNestedPetalPropertyTemplate(): List<PropertyTemplate> = listOf(
    PropertyTemplate(name = nestedPetalManagerName, typeName = getNestedPetalManagerClassName()) {
        delegate { petalManagerMethodBody() }
        visibility { KotlinTemplate.Visibility.PRIVATE }
    },
    this.nestedPetalIdPropertyTemplate(),
    this.nestedPetalAccessorPropertyTemplate()
)

private fun PetalReferenceColumn.petalManagerMethodBody(): CodeTemplate =
    CodeTemplate {
        generateControlFlowCode("lazy", endFlowString = "}") {
            generateCodeLine(
                "%L(%L) { dbEntity.%L${getNullabilityExtension()}.%M() }",
                nestedPetalManagerClassName().simpleName,
                referencingIdName,
                name,
                getPetalExportMemberName()
            )
        }
    }

private fun PetalReferenceColumn.getPetalExportMemberName(): MemberName = ClassName(
        "${referencingAccessorClassName.packageName}.${referencingAccessorClassName.simpleName}.Companion",
        EXPORT_PETAL_METHOD_SIMPLE_NAME
    ).toMemberName()

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
