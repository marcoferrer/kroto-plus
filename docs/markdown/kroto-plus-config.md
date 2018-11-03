# Protocol Documentation
<a name="top"></a>

## Table of Contents

- [krotoplus/compiler/config.proto](#krotoplus/compiler/config.proto)
    - [CompilerConfig](#krotoplus.compiler.CompilerConfig)
    - [ExtenableMessagesGenOptions](#krotoplus.compiler.ExtenableMessagesGenOptions)
    - [FileFilter](#krotoplus.compiler.FileFilter)
    - [GeneratorScriptsGenOptions](#krotoplus.compiler.GeneratorScriptsGenOptions)
    - [GrpcStubExtsGenOptions](#krotoplus.compiler.GrpcStubExtsGenOptions)
    - [InsertionsGenOptions](#krotoplus.compiler.InsertionsGenOptions)
    - [InsertionsGenOptions.Entry](#krotoplus.compiler.InsertionsGenOptions.Entry)
    - [MockServicesGenOptions](#krotoplus.compiler.MockServicesGenOptions)
    - [ProtoBuildersGenOptions](#krotoplus.compiler.ProtoBuildersGenOptions)
  
    - [InsertionPoint](#krotoplus.compiler.InsertionPoint)
  
  
  

- [Scalar Value Types](#scalar-value-types)



<a name="krotoplus/compiler/config.proto"></a>
<p align="right"><a href="#top">Top</a></p>

## krotoplus/compiler/config.proto



<a name="krotoplus.compiler.CompilerConfig"></a>

### CompilerConfig
Message backing the root of a Kroto&#43; configuration file.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| grpc_stub_exts | [GrpcStubExtsGenOptions](#krotoplus.compiler.GrpcStubExtsGenOptions) | repeated | Configuration entries for the &#39;gRPC Stub Extensions&#39; code generator. |
| mock_services | [MockServicesGenOptions](#krotoplus.compiler.MockServicesGenOptions) | repeated | Configuration entries for the &#39;Mock Service&#39; code generator. |
| proto_builders | [ProtoBuildersGenOptions](#krotoplus.compiler.ProtoBuildersGenOptions) | repeated | Configuration entries for the &#39;Proto Builders&#39; code generator. |
| extendable_messages | [ExtenableMessagesGenOptions](#krotoplus.compiler.ExtenableMessagesGenOptions) | repeated | Configuration entries for the &#39;Extendable Messages&#39; code generator. |
| insertions | [InsertionsGenOptions](#krotoplus.compiler.InsertionsGenOptions) | repeated | Configuration entries for the &#39;Protoc Insertions&#39; code generator. |
| generator_scripts | [GeneratorScriptsGenOptions](#krotoplus.compiler.GeneratorScriptsGenOptions) | repeated | Configuration entries for the &#39;Generator Scripts&#39; code generator. |






<a name="krotoplus.compiler.ExtenableMessagesGenOptions"></a>

### ExtenableMessagesGenOptions
Configuration used by the &#39;Extendable Messages&#39; code generator.
Since this code generator relies on the protoc insertion point API,
its outputDir must match that of the protoc java plugin.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| filter | [FileFilter](#krotoplus.compiler.FileFilter) |  | Filter used for limiting the input files that are processed by the code generator The default filter will match true against all input files. |
| companion_field_name | [string](#string) |  | The name of the field that will hold a reference to the pseudo companion object |
| companion_class_name | [string](#string) |  | The name to use for the class declaration of the pseudo companion object |
| companion_extends | [string](#string) |  | The FQ name of the class that the pseudo companion object should extend. Currently limited to classes with at least one no-args contructor. Referencing the current message type, use the value &#39;{{message_type}}&#39;. This is useful when you want to use the current message as a generic type param. ie. com.krotoplus.example.MyCompanionInterface&lt;{{message_type}}, {{message_type}}.Builder&gt; |
| companion_implements | [string](#string) |  | The FQ name of an interface the pseudo companion object should implement. Referencing the current message type, use the value &#39;{{message_type}}&#39;. This is useful when you want to use the current message as a generic type param. ie. com.krotoplus.example.MyCompanionInterface&lt;{{message_type}}, {{message_type}}.Builder&gt; |






<a name="krotoplus.compiler.FileFilter"></a>

### FileFilter
Represent a filter used for including and excluding source files from
being processed by a code generator. It is inclusive by default, so
all paths compared against its default instance will be included as
input to a generator and processed.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| include_path | [string](#string) | repeated | List of file paths to include as inputs for a code generator. A valid value starts from the root package directory of the source file. Globs are supported ie. krotoplus/compiler/config.proto krotoplus/** **/compiler/con*.proto |
| exclude_path | [string](#string) | repeated | List of file paths to exclude as inputs for a code generator. a valid value start from the root package directory of the source file. Globs are supported ie. google/* |






<a name="krotoplus.compiler.GeneratorScriptsGenOptions"></a>

### GeneratorScriptsGenOptions
Configuration used by the &#39;Generator Scripts&#39; code generator.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| filter | [FileFilter](#krotoplus.compiler.FileFilter) |  | Filter used for limiting the input files that are processed by the code generator The default filter will match true against all input files. |
| script_path | [string](#string) | repeated | List of paths to kotlin script files to execute for this configuration. The scripts are compile at runtime by an embedded kotlin compiler. This comes at the cost of performance. Paths for scripts compiled at run time must be relative to the path of the configuration file. ie. &#39;kp-scripts/src/main/kotlin/sampleInsertionScript.kts&#39; For a more performant option for script execution, precompiled scripts are supported. Paths for precompile scripts need to match their location in the supplied jar. ie. &#39;sampleInsertionScript.kts&#39; |
| script_bundle | [string](#string) |  | Path to the jar containing precompile scripts. |






<a name="krotoplus.compiler.GrpcStubExtsGenOptions"></a>

### GrpcStubExtsGenOptions
Configuration used by the &#39;gRPC Stub Extensions&#39; code generator.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| filter | [FileFilter](#krotoplus.compiler.FileFilter) |  | Filter used for limiting the input files that are processed by the code generator The default filter will match true against all input files. |
| support_coroutines | [bool](#bool) |  | Enable code generation for coroutine supporting service stub extensions. This options generates code that relies on the artifact &#39;kroto-plus-coroutines&#39; |






<a name="krotoplus.compiler.InsertionsGenOptions"></a>

### InsertionsGenOptions
Configuration used by the &#39;Protoc Insertions&#39; code generator.
Since this code generator relies on the protoc insertion point API,
its outputDir must match that of the protoc java plugin.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| filter | [FileFilter](#krotoplus.compiler.FileFilter) |  | Filter used for limiting the input files that are processed by the code generator The default filter will match true against all input files. |
| entry | [InsertionsGenOptions.Entry](#krotoplus.compiler.InsertionsGenOptions.Entry) | repeated | List of configurations to be applied to the file filter. |






<a name="krotoplus.compiler.InsertionsGenOptions.Entry"></a>

### InsertionsGenOptions.Entry
Configuration to apply to the files matched by the file filter.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| point | [InsertionPoint](#krotoplus.compiler.InsertionPoint) |  | The protoc insertion point at which the generated code will be inserted. |
| content | [string](#string) | repeated | String literal of content to be set at the insertion point. Referencing the current message type, use the value &#39;{{message_type}}&#39;. This is useful when you want to use the current message as a generic type param. ie. com.krotoplus.example.MyCompanionInterface&lt;{{message_type}}, {{message_type}}.Builder&gt; |
| script_path | [string](#string) | repeated | List of paths to kotlin script files to execute for this configuration. The scripts are compile at runtime by an embedded kotlin compiler. This comes at the cost of performance.Paths for scripts compiled at run time must be relative to the path of the configuration file. ie. &#39;kp-scripts/src/main/kotlin/sampleInsertionScript.kts&#39; For a more performant option for script execution, precompiled scripts are supported. Paths for precompile scripts need to match their location in the supplied jar. ie. &#39;sampleInsertionScript.kts&#39; |
| script_bundle | [string](#string) |  | Path to the jar containing precompile scripts. |






<a name="krotoplus.compiler.MockServicesGenOptions"></a>

### MockServicesGenOptions
Configuration used by the &#39;Mock Services&#39; code generator.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| filter | [FileFilter](#krotoplus.compiler.FileFilter) |  | Filter used for limiting the input files that are processed by the code generator The default filter will match true against all input files. |
| implement_as_object | [bool](#bool) |  | By default, mock services are generated as an open class but an object can be generated instead. |
| generate_service_list | [bool](#bool) |  | Flag for generating a static collection of the Mock Services created. Useful when registering mock services to a GrpcServerRule during unit tests. |
| service_list_package | [string](#string) |  | The java package at which the mock server list should reside. |
| service_list_name | [string](#string) |  | The name of the property at which the mock server list will be initialized at. |






<a name="krotoplus.compiler.ProtoBuildersGenOptions"></a>

### ProtoBuildersGenOptions
Configuration used by the &#39;Proto Builders&#39; code generator.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| filter | [FileFilter](#krotoplus.compiler.FileFilter) |  | Filter used for limiting the input files that are processed by the code generator The default filter will match true against all input files. |
| unwrap_builders | [bool](#bool) |  | By default the generated utility methods for building messages are wrapped in an object similiar to a proto outer class. For better ergonomics with code generated using &#39;java_multiple_files&#39; the builders can be unwrapped and generated at the root scope of the output file. |
| use_dsl_markers | [bool](#bool) |  | Tag java builder classes with a kotlin interface annotated with @DslMarker. This requires the kroto-plus output directory to match the generated java classes directory. Using @DslMarker provides safer and predictable dsl usage. |





 


<a name="krotoplus.compiler.InsertionPoint"></a>

### InsertionPoint


| Name | Number | Description |
| ---- | ------ | ----------- |
| UNKNOWN | 0 |  |
| INTERFACE_EXTENDS | 1 |  |
| MESSAGE_IMPLEMENTS | 2 |  |
| BUILDER_IMPLEMENTS | 3 |  |
| BUILDER_SCOPE | 4 |  |
| CLASS_SCOPE | 5 |  |
| ENUM_SCOPE | 6 |  |
| OUTER_CLASS_SCOPE | 7 |  |


 

 

 



## Scalar Value Types

| .proto Type | Notes | C++ Type | Java Type | Python Type |
| ----------- | ----- | -------- | --------- | ----------- |
| <a name="double" /> double |  | double | double | float |
| <a name="float" /> float |  | float | float | float |
| <a name="int32" /> int32 | Uses variable-length encoding. Inefficient for encoding negative numbers – if your field is likely to have negative values, use sint32 instead. | int32 | int | int |
| <a name="int64" /> int64 | Uses variable-length encoding. Inefficient for encoding negative numbers – if your field is likely to have negative values, use sint64 instead. | int64 | long | int/long |
| <a name="uint32" /> uint32 | Uses variable-length encoding. | uint32 | int | int/long |
| <a name="uint64" /> uint64 | Uses variable-length encoding. | uint64 | long | int/long |
| <a name="sint32" /> sint32 | Uses variable-length encoding. Signed int value. These more efficiently encode negative numbers than regular int32s. | int32 | int | int |
| <a name="sint64" /> sint64 | Uses variable-length encoding. Signed int value. These more efficiently encode negative numbers than regular int64s. | int64 | long | int/long |
| <a name="fixed32" /> fixed32 | Always four bytes. More efficient than uint32 if values are often greater than 2^28. | uint32 | int | int |
| <a name="fixed64" /> fixed64 | Always eight bytes. More efficient than uint64 if values are often greater than 2^56. | uint64 | long | int/long |
| <a name="sfixed32" /> sfixed32 | Always four bytes. | int32 | int | int |
| <a name="sfixed64" /> sfixed64 | Always eight bytes. | int64 | long | int/long |
| <a name="bool" /> bool |  | bool | boolean | boolean |
| <a name="string" /> string | A string must always contain UTF-8 encoded or 7-bit ASCII text. | string | String | str/unicode |
| <a name="bytes" /> bytes | May contain any arbitrary sequence of bytes. | string | ByteString | str |

