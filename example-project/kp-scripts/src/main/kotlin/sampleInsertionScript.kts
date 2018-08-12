import com.github.marcoferrer.krotoplus.proto.ProtoEnum
import com.github.marcoferrer.krotoplus.proto.ProtoFile
import com.github.marcoferrer.krotoplus.proto.ProtoMessage

// language=java
fun interfaceExtends(message: ProtoMessage): String? = ""

fun messageImplements(message: ProtoMessage): String? = ""

fun builderImplements(message: ProtoMessage): String? = ""

fun builderScope(message: ProtoMessage): String? = ""

fun classScope(message: ProtoMessage): String? = ""

fun enumScope(enumMessage: ProtoEnum): String? = ""

fun outerClassScope(file: ProtoFile): String? = ""
