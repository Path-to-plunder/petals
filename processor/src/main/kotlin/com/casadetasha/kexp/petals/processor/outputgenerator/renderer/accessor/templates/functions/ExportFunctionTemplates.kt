package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.templates.functions

import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.ExportMethodNames.EXPORT_PETAL_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.generationdsl.dsl.CodeTemplate
import com.casadetasha.kexp.generationdsl.dsl.FunctionTemplate
import getNullabilityExtension

internal fun createExportFunctionTemplate(accessorClassInfo: AccessorClassInfo): FunctionTemplate =
    FunctionTemplate(
        name = EXPORT_PETAL_METHOD_SIMPLE_NAME,
        returnType = accessorClassInfo.className,
        receiverType = accessorClassInfo.entityClassName
    ) {
        this.methodBody {
            createExportFunctionBody(accessorClassInfo)
        }
    }

private fun createExportFunctionBody(accessorClassInfo: AccessorClassInfo): CodeTemplate =
    CodeTemplate {
        controlFlowCode("return ${accessorClassInfo.className.simpleName}",
            beginFlowString = "(",
            endFlowString = ")"
        ) {
            code("dbEntity = this,")
            collectCodeTemplates { accessorClassInfo.createPetalValueColumnTemplates() }
            collectCodeTemplates { accessorClassInfo.createPetalReferenceColumnTemplates() }
            codeTemplate { accessorClassInfo.createPetalIdColumnTemplate() }
        }
    }

private fun AccessorClassInfo.createPetalValueColumnTemplates(): List<CodeTemplate> =
    petalValueColumns.map {
        CodeTemplate("\n${it.name} = ${it.name},")
    }

private fun AccessorClassInfo.createPetalReferenceColumnTemplates(): List<CodeTemplate> =
    petalReferenceColumns.map {
            val codeFormat = "\n${it.name}Id = readValues[%M.${it.name}]${it.getNullabilityExtension()}.value,"
            CodeTemplate(codeFormat, tableMemberName)
        }

private fun AccessorClassInfo.createPetalIdColumnTemplate(): CodeTemplate =
    CodeTemplate("\n${petalIdColumn.name} = ${petalIdColumn.name}.value,")
