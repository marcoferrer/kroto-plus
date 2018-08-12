import com.github.marcoferrer.krotoplus.proto.ProtoEnum

// language=java
fun enumScope(message: ProtoEnum) = """
    public static final String kroto = "Hello there, from ${message.name}";
"""