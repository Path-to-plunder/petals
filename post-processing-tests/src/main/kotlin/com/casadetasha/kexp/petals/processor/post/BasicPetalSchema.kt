package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.Petal

@Petal(tableName = "basic_petal")
abstract class BasicPetalSchema {
    abstract val name: String
    abstract val count: Int
}
