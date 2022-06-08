package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration

import com.casadetasha.kexp.petals.annotations.BasePetalMigration
import com.casadetasha.kexp.petals.processor.model.ParsedPetal
import com.casadetasha.kexp.generationdsl.dsl.CodeTemplate
import com.casadetasha.kexp.generationdsl.dsl.FileTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration.ConfiguredJson.json
import com.squareup.kotlinpoet.ClassName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


internal fun FileTemplate.createDbMigrationClassTemplate(petal: ParsedPetal, className: ClassName) =
    generateClass(className = className) {
        generateSuperClass(BasePetalMigration::class)

        generateProperty(name = "petalJson", type = String::class) {
            override()
            initializer {
                val initializerJson = json.encodeToString(parsePetalMigration(petal))
                CodeTemplate("%S", initializerJson)
            }
        }
    }

internal object ConfiguredJson {
    val json = Json { prettyPrint = true }
}
