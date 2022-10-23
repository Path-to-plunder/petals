import com.casadetasha.kexp.petals.processor.model.columns.PetalReferenceColumn

internal fun String.uppercaseFirstChar(): String = replaceFirstChar { it.uppercase() }

internal fun PetalReferenceColumn.getNullabilityExtension(): Any {
    return if (isNullable) {
        "?"
    } else {
        ""
    }
}

internal fun <E> MutableList<E>.addIf(condition: Boolean, function: () -> E): MutableList<E> = apply {
    if (condition) {
        add(function())
    }
}
