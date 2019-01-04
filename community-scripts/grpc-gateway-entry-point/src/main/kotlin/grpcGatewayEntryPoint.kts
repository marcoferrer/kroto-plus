import com.github.marcoferrer.krotoplus.generators.Generator
import com.github.marcoferrer.krotoplus.proto.ProtoService
import com.github.marcoferrer.krotoplus.utils.addFile
import com.google.protobuf.compiler.PluginProtos


object GrpcGatewayEntryPointGenerator : Generator {

    private const val httpOptionId = "72295728"

    override fun invoke(): PluginProtos.CodeGeneratorResponse {

        val services = context.schema.protoServices
            .filter { service ->
                service.methodDefinitions.any { method ->
                    httpOptionId in method.descriptorProto.options.toString()
                }
            }

        return PluginProtos.CodeGeneratorResponse.newBuilder()
            .addFile {
                name = "main.go"
                content = buildMain(services)
            }
            .build()
    }

    // language=go
    private fun buildMain(services: List<ProtoService>) = buildString {
        append(
            """
            package main

            import (
              "flag"
              "net/http"

              "github.com/golang/glog"
              "github.com/grpc-ecosystem/grpc-gateway/runtime"
              "golang.org/x/net/context"
              "google.golang.org/grpc"
            """.trimIndent()
        )

        for (service in services) {
            val goPackage = service.protoFile.protoPackage.replace(".", "/")

            appendln()
            append("  ${service.varName} \"../$goPackage\"")
        }

        appendln()
        append(
            """
            )

            var (
              serviceEndpoint = flag.String("service_endpoint", "localhost:8000", "endpoint of YourService")
            )

            func run() error {
              ctx := context.Background()
              ctx, cancel := context.WithCancel(ctx)
              defer cancel()

              mux := runtime.NewServeMux()
              opts := []grpc.DialOption{grpc.WithInsecure()}
              var err error
            """.trimIndent()
        )

        for (service in services) {
            appendln()
            appendln(
                """
                err = ${service.varName}.Register${service.name}HandlerFromEndpoint(ctx, mux, *serviceEndpoint, opts)
                if err != nil {
                    return err
                }
                """.trimIndent()
            )
        }

        append(
            """
              return http.ListenAndServe(":8080", mux)
            }

            func main() {
              flag.Parse()
              defer glog.Flush()

              if err := run(); err != nil {
                glog.Fatal(err)
              }
            }
            """.trimIndent()
        )
    }

    val ProtoService.varName: String get() = name.decapitalize() + "Gw"
}

