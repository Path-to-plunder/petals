import com.casadetasha.kexp.petals.processor.model.columns.PetalReferenceColumn

internal fun String.uppercaseFirstChar(): String = replaceFirstChar { it.uppercase() }

internal fun PetalReferenceColumn.getNullabilityExtension(): Any {
    return if (isNullable) {
        "?"
    } else {
        ""
    }
}
