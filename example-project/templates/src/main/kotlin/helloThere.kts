import com.github.marcoferrer.krotoplus.generators.Generator
import com.github.marcoferrer.krotoplus.utils.addFile
import com.google.protobuf.compiler.PluginProtos


object MyDummyGenerator : Generator {

    override fun invoke(): PluginProtos.CodeGeneratorResponse {

        return PluginProtos.CodeGeneratorResponse.newBuilder()
                .addFile {
                    name = "GeneralKenobi.kt"

                    // language=kotlin
                    content = """
                        object GeneralKenobi{
                            fun greet() = "Hello there"
                        }
                    """.trimIndent()
                }
                .build()
    }
}