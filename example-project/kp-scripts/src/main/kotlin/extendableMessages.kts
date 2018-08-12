import com.github.marcoferrer.krotoplus.proto.ProtoMessage

// language=java
fun messageImplements(message: ProtoMessage): String =
        "com.github.marcoferrer.krotoplus.message.KpMessage<${message.canonicalJavaName},${message.canonicalJavaName}.Builder>,"

// language=java
fun builderImplements(message: ProtoMessage): String =
        "com.github.marcoferrer.krotoplus.message.KpBuilder<${message.canonicalJavaName}>,"

// language=java
fun classScope(message: ProtoMessage) = """

    @javax.annotation.Nonnull
    public static final Companion Kroto =
            com.github.marcoferrer.krotoplus.message.KpCompanion.Registry
                    .initializeCompanion(${message.canonicalJavaName}.class, new Companion());

    @javax.annotation.Nonnull
    @Override
    public ${message.canonicalJavaName}.Companion getCompanion() {
      return ${message.canonicalJavaName}.Kroto;
    }

    public static final class Companion implements
        com.github.marcoferrer.krotoplus.message.KpCompanion<${message.canonicalJavaName},${message.canonicalJavaName}.Builder> {

      private Companion(){}

      @javax.annotation.Nonnull
      @Override
      public ${message.canonicalJavaName} getDefaultInstance() {
        return ${message.canonicalJavaName}.getDefaultInstance();
      }

      @javax.annotation.Nonnull
      @Override
      public ${message.canonicalJavaName}.Builder newBuilder() {
        return ${message.canonicalJavaName}.newBuilder();
      }
    }
"""
