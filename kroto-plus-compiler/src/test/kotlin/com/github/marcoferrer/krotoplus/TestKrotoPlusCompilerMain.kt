package com.github.marcoferrer.krotoplus

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals

class TestKrotoPlusCompilerMain{

    @[Rule JvmField]
    var folder = TemporaryFolder()

    @Test fun `Test Kroto Plus Cli`() {

        val protoDir = folder.newFolder("proto")
        val outputDir = folder.newFolder("generated")

        File(protoDir, "google/protobuf/empty.proto").apply {
            parentFile.mkdirs()

            writeText("""
                syntax = "proto3";
                package google.protobuf;

                option java_package = "com.google.protobuf";
                option java_outer_classname = "EmptyProto";
                option java_multiple_files = true;

                message Empty{}
            """.trimIndent())
        }

        File(protoDir,"user.proto").writeText("""
            syntax = "proto3";
            package krotoplus.user.test;
            import "google/protobuf/empty.proto";

            option java_package = "krotoplus.test.generator";
            option java_outer_classname = "UserServiceProto";

            message User{
                string name = 1;
                string email = 2;
                int32 age = 3;
            }

            enum AuthStatus {
                PARTIAL_AUTH = 0;
                AUTHENTICATED = 1;
                GUEST = 2;
            }

            message GetUserByNameRequest{
                string name = 1;
            }

            service UserService {
                rpc GetUserByName(GetUserByNameRequest) returns(User);
                rpc GetNewestUser(google.protobuf.Empty) returns(User);
                rpc GetAllUsers(google.protobuf.Empty) returns(stream User);
            }
        """.trimIndent())

        File(protoDir,"address.proto").writeText("""
            syntax = "proto3";
            package krotoplus.address.test;
            import "google/protobuf/empty.proto";

            option java_outer_classname = "AddressProto";

            message Address{
                string street1 = 1;
                string street2 = 2;
                string city = 3;
                string state = 4;
            }
        """.trimIndent())

        main(
                protoDir.absolutePath,
                "-writers","3",
                "-default-out", outputDir.absolutePath,
                "-ProtoTypeBuilder",
                "-StubOverloads",
                "-o|${outputDir.absolutePath}|-coroutines",
                "-MockServices",
                "-o|${outputDir.absolutePath}/test"
        )

        val generatedFiles = outputDir.walkTopDown().filter { it.isFile }.toList()
        val expectedFiles = listOf(
                "AddressProtoBuilders.kt",
                "AddressProtoResponseQueueOverloads.kt",
                "MockUserService.kt",
                "UserServiceRpcOverloads.kt",
                "UserServiceProtoBuilders.kt",
                "UserServiceProtoResponseQueueOverloads.kt"
        )
        assertEquals(expectedFiles.size,generatedFiles.size)
        assert(generatedFiles.all { it.name in expectedFiles }){
            "Not all expected files were generated"
        }
    }
}