import com.casadetasha.kexp.petals.annotations.Petal
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
import com.casadetasha.kexp.petals.annotations.PetalSchema

@Petal(tableName = "example_table", className = "ExampleClass", primaryKeyType = PetalPrimaryKey.INT)
interface ExamplePetal

@PetalSchema(petal = ExamplePetal::class)
interface ExamplePetalSchema {
    val name: String
}