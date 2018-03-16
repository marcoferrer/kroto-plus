import org.junit.Test

class TestSchemaUtils{

    @Test
    fun `Test ProtoType to ClassName`(){
        val coffeeSchema = RepoBuilder()
                .add("coffee.proto", """

                """.trimIndent()
                                     + "message CafeDrink {\n"
                                     + "  optional string customer_name = 1;\n"
                                     + "  repeated EspressoShot shots = 2;\n"
                                     + "  optional Foam foam = 3;\n"
                                     + "  optional int32 size_ounces = 14;\n"
                                     + "  optional Dairy dairy = 15;\n"
                                     + "\n"
                                     + "  enum Foam {\n"
                                     + "    NOT_FOAMY_AND_QUITE_BORING = 1;\n"
                                     + "    ZOMG_SO_FOAMY = 3;\n"
                                     + "  }\n"
                                     + "}\n"
                                     + "\n"
                                     + "message Dairy {\n"
                                     + "  optional int32 count = 2;\n"
                                     + "  optional string type = 1;\n"
                                     + "}\n"
                                     + "\n"
                                     + "message EspressoShot {\n"
                                     + "  optional string bean_type = 1;\n"
                                     + "  optional double caffeine_level = 2;\n"
                                     + "}\n")
                .schema()
    }

}