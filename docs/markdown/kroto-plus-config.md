# Protocol Documentation
<a name="top"></a>

## Table of Contents

- [google/protobuf/any.proto](#google/protobuf/any.proto)
    - [Any](#google.protobuf.Any)
  
  
  
  

- [google/protobuf/api.proto](#google/protobuf/api.proto)
    - [Api](#google.protobuf.Api)
    - [Method](#google.protobuf.Method)
    - [Mixin](#google.protobuf.Mixin)
  
  
  
  

- [google/protobuf/compiler/plugin.proto](#google/protobuf/compiler/plugin.proto)
    - [CodeGeneratorRequest](#google.protobuf.compiler.CodeGeneratorRequest)
    - [CodeGeneratorResponse](#google.protobuf.compiler.CodeGeneratorResponse)
    - [CodeGeneratorResponse.File](#google.protobuf.compiler.CodeGeneratorResponse.File)
    - [Version](#google.protobuf.compiler.Version)
  
  
  
  

- [google/protobuf/descriptor.proto](#google/protobuf/descriptor.proto)
    - [DescriptorProto](#google.protobuf.DescriptorProto)
    - [DescriptorProto.ExtensionRange](#google.protobuf.DescriptorProto.ExtensionRange)
    - [DescriptorProto.ReservedRange](#google.protobuf.DescriptorProto.ReservedRange)
    - [EnumDescriptorProto](#google.protobuf.EnumDescriptorProto)
    - [EnumDescriptorProto.EnumReservedRange](#google.protobuf.EnumDescriptorProto.EnumReservedRange)
    - [EnumOptions](#google.protobuf.EnumOptions)
    - [EnumValueDescriptorProto](#google.protobuf.EnumValueDescriptorProto)
    - [EnumValueOptions](#google.protobuf.EnumValueOptions)
    - [ExtensionRangeOptions](#google.protobuf.ExtensionRangeOptions)
    - [FieldDescriptorProto](#google.protobuf.FieldDescriptorProto)
    - [FieldOptions](#google.protobuf.FieldOptions)
    - [FileDescriptorProto](#google.protobuf.FileDescriptorProto)
    - [FileDescriptorSet](#google.protobuf.FileDescriptorSet)
    - [FileOptions](#google.protobuf.FileOptions)
    - [GeneratedCodeInfo](#google.protobuf.GeneratedCodeInfo)
    - [GeneratedCodeInfo.Annotation](#google.protobuf.GeneratedCodeInfo.Annotation)
    - [MessageOptions](#google.protobuf.MessageOptions)
    - [MethodDescriptorProto](#google.protobuf.MethodDescriptorProto)
    - [MethodOptions](#google.protobuf.MethodOptions)
    - [OneofDescriptorProto](#google.protobuf.OneofDescriptorProto)
    - [OneofOptions](#google.protobuf.OneofOptions)
    - [ServiceDescriptorProto](#google.protobuf.ServiceDescriptorProto)
    - [ServiceOptions](#google.protobuf.ServiceOptions)
    - [SourceCodeInfo](#google.protobuf.SourceCodeInfo)
    - [SourceCodeInfo.Location](#google.protobuf.SourceCodeInfo.Location)
    - [UninterpretedOption](#google.protobuf.UninterpretedOption)
    - [UninterpretedOption.NamePart](#google.protobuf.UninterpretedOption.NamePart)
  
    - [FieldDescriptorProto.Label](#google.protobuf.FieldDescriptorProto.Label)
    - [FieldDescriptorProto.Type](#google.protobuf.FieldDescriptorProto.Type)
    - [FieldOptions.CType](#google.protobuf.FieldOptions.CType)
    - [FieldOptions.JSType](#google.protobuf.FieldOptions.JSType)
    - [FileOptions.OptimizeMode](#google.protobuf.FileOptions.OptimizeMode)
    - [MethodOptions.IdempotencyLevel](#google.protobuf.MethodOptions.IdempotencyLevel)
  
  
  

- [google/protobuf/duration.proto](#google/protobuf/duration.proto)
    - [Duration](#google.protobuf.Duration)
  
  
  
  

- [google/protobuf/empty.proto](#google/protobuf/empty.proto)
    - [Empty](#google.protobuf.Empty)
  
  
  
  

- [google/protobuf/field_mask.proto](#google/protobuf/field_mask.proto)
    - [FieldMask](#google.protobuf.FieldMask)
  
  
  
  

- [google/protobuf/source_context.proto](#google/protobuf/source_context.proto)
    - [SourceContext](#google.protobuf.SourceContext)
  
  
  
  

- [google/protobuf/struct.proto](#google/protobuf/struct.proto)
    - [ListValue](#google.protobuf.ListValue)
    - [Struct](#google.protobuf.Struct)
    - [Struct.FieldsEntry](#google.protobuf.Struct.FieldsEntry)
    - [Value](#google.protobuf.Value)
  
    - [NullValue](#google.protobuf.NullValue)
  
  
  

- [google/protobuf/timestamp.proto](#google/protobuf/timestamp.proto)
    - [Timestamp](#google.protobuf.Timestamp)
  
  
  
  

- [google/protobuf/type.proto](#google/protobuf/type.proto)
    - [Enum](#google.protobuf.Enum)
    - [EnumValue](#google.protobuf.EnumValue)
    - [Field](#google.protobuf.Field)
    - [Option](#google.protobuf.Option)
    - [Type](#google.protobuf.Type)
  
    - [Field.Cardinality](#google.protobuf.Field.Cardinality)
    - [Field.Kind](#google.protobuf.Field.Kind)
    - [Syntax](#google.protobuf.Syntax)
  
  
  

- [google/protobuf/wrappers.proto](#google/protobuf/wrappers.proto)
    - [BoolValue](#google.protobuf.BoolValue)
    - [BytesValue](#google.protobuf.BytesValue)
    - [DoubleValue](#google.protobuf.DoubleValue)
    - [FloatValue](#google.protobuf.FloatValue)
    - [Int32Value](#google.protobuf.Int32Value)
    - [Int64Value](#google.protobuf.Int64Value)
    - [StringValue](#google.protobuf.StringValue)
    - [UInt32Value](#google.protobuf.UInt32Value)
    - [UInt64Value](#google.protobuf.UInt64Value)
  
  
  
  

- [krotoplus/compiler/config.proto](#krotoplus/compiler/config.proto)
    - [CompilerConfig](#krotoplus.compiler.CompilerConfig)
    - [ExtenableMessagesGenOptions](#krotoplus.compiler.ExtenableMessagesGenOptions)
    - [FileFilter](#krotoplus.compiler.FileFilter)
    - [GeneratorScriptsGenOptions](#krotoplus.compiler.GeneratorScriptsGenOptions)
    - [GrpcCoroutinesGenOptions](#krotoplus.compiler.GrpcCoroutinesGenOptions)
    - [GrpcStubExtsGenOptions](#krotoplus.compiler.GrpcStubExtsGenOptions)
    - [InsertionsGenOptions](#krotoplus.compiler.InsertionsGenOptions)
    - [InsertionsGenOptions.Entry](#krotoplus.compiler.InsertionsGenOptions.Entry)
    - [MockServicesGenOptions](#krotoplus.compiler.MockServicesGenOptions)
    - [ProtoBuildersGenOptions](#krotoplus.compiler.ProtoBuildersGenOptions)
  
    - [InsertionPoint](#krotoplus.compiler.InsertionPoint)
  
  
  

- [Scalar Value Types](#scalar-value-types)



<a name="google/protobuf/any.proto"></a>
<p align="right"><a href="#top">Top</a></p>

## google/protobuf/any.proto



<a name="google.protobuf.Any"></a>

### Any
`Any` contains an arbitrary serialized protocol buffer message along with a
URL that describes the type of the serialized message.

Protobuf library provides support to pack/unpack Any values in the form
of utility functions or additional generated methods of the Any type.

Example 1: Pack and unpack a message in C&#43;&#43;.

    Foo foo = ...;
    Any any;
    any.PackFrom(foo);
    ...
    if (any.UnpackTo(&amp;foo)) {
      ...
    }

Example 2: Pack and unpack a message in Java.

    Foo foo = ...;
    Any any = Any.pack(foo);
    ...
    if (any.is(Foo.class)) {
      foo = any.unpack(Foo.class);
    }

 Example 3: Pack and unpack a message in Python.

    foo = Foo(...)
    any = Any()
    any.Pack(foo)
    ...
    if any.Is(Foo.DESCRIPTOR):
      any.Unpack(foo)
      ...

 Example 4: Pack and unpack a message in Go

     foo := &amp;pb.Foo{...}
     any, err := ptypes.MarshalAny(foo)
     ...
     foo := &amp;pb.Foo{}
     if err := ptypes.UnmarshalAny(any, foo); err != nil {
       ...
     }

The pack methods provided by protobuf library will by default use
&#39;type.googleapis.com/full.type.name&#39; as the type URL and the unpack
methods only use the fully qualified type name after the last &#39;/&#39;
in the type URL, for example &#34;foo.bar.com/x/y.z&#34; will yield type
name &#34;y.z&#34;.


JSON
====
The JSON representation of an `Any` value uses the regular
representation of the deserialized, embedded message, with an
additional field `@type` which contains the type URL. Example:

    package google.profile;
    message Person {
      string first_name = 1;
      string last_name = 2;
    }

    {
      &#34;@type&#34;: &#34;type.googleapis.com/google.profile.Person&#34;,
      &#34;firstName&#34;: &lt;string&gt;,
      &#34;lastName&#34;: &lt;string&gt;
    }

If the embedded message type is well-known and has a custom JSON
representation, that representation will be embedded adding a field
`value` which holds the custom JSON in addition to the `@type`
field. Example (for message [google.protobuf.Duration][]):

    {
      &#34;@type&#34;: &#34;type.googleapis.com/google.protobuf.Duration&#34;,
      &#34;value&#34;: &#34;1.212s&#34;
    }


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| type_url | [string](#string) |  | A URL/resource name that uniquely identifies the type of the serialized protocol buffer message. The last segment of the URL&#39;s path must represent the fully qualified name of the type (as in `path/google.protobuf.Duration`). The name should be in a canonical form (e.g., leading &#34;.&#34; is not accepted).

In practice, teams usually precompile into the binary all types that they expect it to use in the context of Any. However, for URLs which use the scheme `http`, `https`, or no scheme, one can optionally set up a type server that maps type URLs to message definitions as follows:

* If no scheme is provided, `https` is assumed. * An HTTP GET on the URL must yield a [google.protobuf.Type][] value in binary format, or produce an error. * Applications are allowed to cache lookup results based on the URL, or have them precompiled into a binary to avoid any lookup. Therefore, binary compatibility needs to be preserved on changes to types. (Use versioned type names to manage breaking changes.)

Note: this functionality is not currently available in the official protobuf release, and it is not used for type URLs beginning with type.googleapis.com.

Schemes other than `http`, `https` (or the empty scheme) might be used with implementation specific semantics. |
| value | [bytes](#bytes) |  | Must be a valid serialized protocol buffer of the above specified type. |





 

 

 

 



<a name="google/protobuf/api.proto"></a>
<p align="right"><a href="#top">Top</a></p>

## google/protobuf/api.proto



<a name="google.protobuf.Api"></a>

### Api
Api is a light-weight descriptor for an API Interface.

Interfaces are also described as &#34;protocol buffer services&#34; in some contexts,
such as by the &#34;service&#34; keyword in a .proto file, but they are different
from API Services, which represent a concrete implementation of an interface
as opposed to simply a description of methods and bindings. They are also
sometimes simply referred to as &#34;APIs&#34; in other contexts, such as the name of
this message itself. See https://cloud.google.com/apis/design/glossary for
detailed terminology.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| name | [string](#string) |  | The fully qualified name of this interface, including package name followed by the interface&#39;s simple name. |
| methods | [Method](#google.protobuf.Method) | repeated | The methods of this interface, in unspecified order. |
| options | [Option](#google.protobuf.Option) | repeated | Any metadata attached to the interface. |
| version | [string](#string) |  | A version string for this interface. If specified, must have the form `major-version.minor-version`, as in `1.10`. If the minor version is omitted, it defaults to zero. If the entire version field is empty, the major version is derived from the package name, as outlined below. If the field is not empty, the version in the package name will be verified to be consistent with what is provided here.

The versioning schema uses [semantic versioning](http://semver.org) where the major version number indicates a breaking change and the minor version an additive, non-breaking change. Both version numbers are signals to users what to expect from different versions, and should be carefully chosen based on the product plan.

The major version is also reflected in the package name of the interface, which must end in `v&lt;major-version&gt;`, as in `google.feature.v1`. For major versions 0 and 1, the suffix can be omitted. Zero major versions must only be used for experimental, non-GA interfaces. |
| source_context | [SourceContext](#google.protobuf.SourceContext) |  | Source context for the protocol buffer service represented by this message. |
| mixins | [Mixin](#google.protobuf.Mixin) | repeated | Included interfaces. See [Mixin][]. |
| syntax | [Syntax](#google.protobuf.Syntax) |  | The source syntax of the service. |






<a name="google.protobuf.Method"></a>

### Method
Method represents a method of an API interface.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| name | [string](#string) |  | The simple name of this method. |
| request_type_url | [string](#string) |  | A URL of the input message type. |
| request_streaming | [bool](#bool) |  | If true, the request is streamed. |
| response_type_url | [string](#string) |  | The URL of the output message type. |
| response_streaming | [bool](#bool) |  | If true, the response is streamed. |
| options | [Option](#google.protobuf.Option) | repeated | Any metadata attached to the method. |
| syntax | [Syntax](#google.protobuf.Syntax) |  | The source syntax of this method. |






<a name="google.protobuf.Mixin"></a>

### Mixin
Declares an API Interface to be included in this interface. The including
interface must redeclare all the methods from the included interface, but
documentation and options are inherited as follows:

- If after comment and whitespace stripping, the documentation
  string of the redeclared method is empty, it will be inherited
  from the original method.

- Each annotation belonging to the service config (http,
  visibility) which is not set in the redeclared method will be
  inherited.

- If an http annotation is inherited, the path pattern will be
  modified as follows. Any version prefix will be replaced by the
  version of the including interface plus the [root][] path if
  specified.

Example of a simple mixin:

    package google.acl.v1;
    service AccessControl {
      // Get the underlying ACL object.
      rpc GetAcl(GetAclRequest) returns (Acl) {
        option (google.api.http).get = &#34;/v1/{resource=**}:getAcl&#34;;
      }
    }

    package google.storage.v2;
    service Storage {
      rpc GetAcl(GetAclRequest) returns (Acl);

      // Get a data record.
      rpc GetData(GetDataRequest) returns (Data) {
        option (google.api.http).get = &#34;/v2/{resource=**}&#34;;
      }
    }

Example of a mixin configuration:

    apis:
    - name: google.storage.v2.Storage
      mixins:
      - name: google.acl.v1.AccessControl

The mixin construct implies that all methods in `AccessControl` are
also declared with same name and request/response types in
`Storage`. A documentation generator or annotation processor will
see the effective `Storage.GetAcl` method after inherting
documentation and annotations as follows:

    service Storage {
      // Get the underlying ACL object.
      rpc GetAcl(GetAclRequest) returns (Acl) {
        option (google.api.http).get = &#34;/v2/{resource=**}:getAcl&#34;;
      }
      ...
    }

Note how the version in the path pattern changed from `v1` to `v2`.

If the `root` field in the mixin is specified, it should be a
relative path under which inherited HTTP paths are placed. Example:

    apis:
    - name: google.storage.v2.Storage
      mixins:
      - name: google.acl.v1.AccessControl
        root: acls

This implies the following inherited HTTP annotation:

    service Storage {
      // Get the underlying ACL object.
      rpc GetAcl(GetAclRequest) returns (Acl) {
        option (google.api.http).get = &#34;/v2/acls/{resource=**}:getAcl&#34;;
      }
      ...
    }


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| name | [string](#string) |  | The fully qualified name of the interface which is included. |
| root | [string](#string) |  | If non-empty specifies a path under which inherited HTTP paths are rooted. |





 

 

 

 



<a name="google/protobuf/compiler/plugin.proto"></a>
<p align="right"><a href="#top">Top</a></p>

## google/protobuf/compiler/plugin.proto



<a name="google.protobuf.compiler.CodeGeneratorRequest"></a>

### CodeGeneratorRequest
An encoded CodeGeneratorRequest is written to the plugin&#39;s stdin.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| file_to_generate | [string](#string) | repeated | The .proto files that were explicitly listed on the command-line. The code generator should generate code only for these files. Each file&#39;s descriptor will be included in proto_file, below. |
| parameter | [string](#string) | optional | The generator parameter passed on the command-line. |
| proto_file | [google.protobuf.FileDescriptorProto](#google.protobuf.FileDescriptorProto) | repeated | FileDescriptorProtos for all files in files_to_generate and everything they import. The files will appear in topological order, so each file appears before any file that imports it.

protoc guarantees that all proto_files will be written after the fields above, even though this is not technically guaranteed by the protobuf wire format. This theoretically could allow a plugin to stream in the FileDescriptorProtos and handle them one by one rather than read the entire set into memory at once. However, as of this writing, this is not similarly optimized on protoc&#39;s end -- it will store all fields in memory at once before sending them to the plugin.

Type names of fields and extensions in the FileDescriptorProto are always fully qualified. |
| compiler_version | [Version](#google.protobuf.compiler.Version) | optional | The version number of protocol compiler. |






<a name="google.protobuf.compiler.CodeGeneratorResponse"></a>

### CodeGeneratorResponse
The plugin writes an encoded CodeGeneratorResponse to stdout.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| error | [string](#string) | optional | Error message. If non-empty, code generation failed. The plugin process should exit with status code zero even if it reports an error in this way.

This should be used to indicate errors in .proto files which prevent the code generator from generating correct code. Errors which indicate a problem in protoc itself -- such as the input CodeGeneratorRequest being unparseable -- should be reported by writing a message to stderr and exiting with a non-zero status code. |
| file | [CodeGeneratorResponse.File](#google.protobuf.compiler.CodeGeneratorResponse.File) | repeated |  |






<a name="google.protobuf.compiler.CodeGeneratorResponse.File"></a>

### CodeGeneratorResponse.File
Represents a single generated file.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| name | [string](#string) | optional | The file name, relative to the output directory. The name must not contain &#34;.&#34; or &#34;..&#34; components and must be relative, not be absolute (so, the file cannot lie outside the output directory). &#34;/&#34; must be used as the path separator, not &#34;\&#34;.

If the name is omitted, the content will be appended to the previous file. This allows the generator to break large files into small chunks, and allows the generated text to be streamed back to protoc so that large files need not reside completely in memory at one time. Note that as of this writing protoc does not optimize for this -- it will read the entire CodeGeneratorResponse before writing files to disk. |
| insertion_point | [string](#string) | optional | If non-empty, indicates that the named file should already exist, and the content here is to be inserted into that file at a defined insertion point. This feature allows a code generator to extend the output produced by another code generator. The original generator may provide insertion points by placing special annotations in the file that look like: @@protoc_insertion_point(NAME) The annotation can have arbitrary text before and after it on the line, which allows it to be placed in a comment. NAME should be replaced with an identifier naming the point -- this is what other generators will use as the insertion_point. Code inserted at this point will be placed immediately above the line containing the insertion point (thus multiple insertions to the same point will come out in the order they were added). The double-@ is intended to make it unlikely that the generated code could contain things that look like insertion points by accident.

For example, the C&#43;&#43; code generator places the following line in the .pb.h files that it generates: // @@protoc_insertion_point(namespace_scope) This line appears within the scope of the file&#39;s package namespace, but outside of any particular class. Another plugin can then specify the insertion_point &#34;namespace_scope&#34; to generate additional classes or other declarations that should be placed in this scope.

Note that if the line containing the insertion point begins with whitespace, the same whitespace will be added to every line of the inserted text. This is useful for languages like Python, where indentation matters. In these languages, the insertion point comment should be indented the same amount as any inserted code will need to be in order to work correctly in that context.

The code generator that generates the initial file and the one which inserts into it must both run as part of a single invocation of protoc. Code generators are executed in the order in which they appear on the command line.

If |insertion_point| is present, |name| must also be present. |
| content | [string](#string) | optional | The file contents. |






<a name="google.protobuf.compiler.Version"></a>

### Version
The version number of protocol compiler.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| major | [int32](#int32) | optional |  |
| minor | [int32](#int32) | optional |  |
| patch | [int32](#int32) | optional |  |
| suffix | [string](#string) | optional | A suffix for alpha, beta or rc release, e.g., &#34;alpha-1&#34;, &#34;rc2&#34;. It should be empty for mainline stable releases. |





 

 

 

 



<a name="google/protobuf/descriptor.proto"></a>
<p align="right"><a href="#top">Top</a></p>

## google/protobuf/descriptor.proto



<a name="google.protobuf.DescriptorProto"></a>

### DescriptorProto
Describes a message type.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| name | [string](#string) | optional |  |
| field | [FieldDescriptorProto](#google.protobuf.FieldDescriptorProto) | repeated |  |
| extension | [FieldDescriptorProto](#google.protobuf.FieldDescriptorProto) | repeated |  |
| nested_type | [DescriptorProto](#google.protobuf.DescriptorProto) | repeated |  |
| enum_type | [EnumDescriptorProto](#google.protobuf.EnumDescriptorProto) | repeated |  |
| extension_range | [DescriptorProto.ExtensionRange](#google.protobuf.DescriptorProto.ExtensionRange) | repeated |  |
| oneof_decl | [OneofDescriptorProto](#google.protobuf.OneofDescriptorProto) | repeated |  |
| options | [MessageOptions](#google.protobuf.MessageOptions) | optional |  |
| reserved_range | [DescriptorProto.ReservedRange](#google.protobuf.DescriptorProto.ReservedRange) | repeated |  |
| reserved_name | [string](#string) | repeated | Reserved field names, which may not be used by fields in the same message. A given name may only be reserved once. |






<a name="google.protobuf.DescriptorProto.ExtensionRange"></a>

### DescriptorProto.ExtensionRange



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| start | [int32](#int32) | optional |  |
| end | [int32](#int32) | optional |  |
| options | [ExtensionRangeOptions](#google.protobuf.ExtensionRangeOptions) | optional |  |






<a name="google.protobuf.DescriptorProto.ReservedRange"></a>

### DescriptorProto.ReservedRange
Range of reserved tag numbers. Reserved tag numbers may not be used by
fields or extension ranges in the same message. Reserved ranges may
not overlap.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| start | [int32](#int32) | optional | Inclusive. |
| end | [int32](#int32) | optional | Exclusive. |






<a name="google.protobuf.EnumDescriptorProto"></a>

### EnumDescriptorProto
Describes an enum type.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| name | [string](#string) | optional |  |
| value | [EnumValueDescriptorProto](#google.protobuf.EnumValueDescriptorProto) | repeated |  |
| options | [EnumOptions](#google.protobuf.EnumOptions) | optional |  |
| reserved_range | [EnumDescriptorProto.EnumReservedRange](#google.protobuf.EnumDescriptorProto.EnumReservedRange) | repeated | Range of reserved numeric values. Reserved numeric values may not be used by enum values in the same enum declaration. Reserved ranges may not overlap. |
| reserved_name | [string](#string) | repeated | Reserved enum value names, which may not be reused. A given name may only be reserved once. |






<a name="google.protobuf.EnumDescriptorProto.EnumReservedRange"></a>

### EnumDescriptorProto.EnumReservedRange
Range of reserved numeric values. Reserved values may not be used by
entries in the same enum. Reserved ranges may not overlap.

Note that this is distinct from DescriptorProto.ReservedRange in that it
is inclusive such that it can appropriately represent the entire int32
domain.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| start | [int32](#int32) | optional | Inclusive. |
| end | [int32](#int32) | optional | Inclusive. |






<a name="google.protobuf.EnumOptions"></a>

### EnumOptions



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| allow_alias | [bool](#bool) | optional | Set this option to true to allow mapping different tag names to the same value. |
| deprecated | [bool](#bool) | optional | Is this enum deprecated? Depending on the target platform, this can emit Deprecated annotations for the enum, or it will be completely ignored; in the very least, this is a formalization for deprecating enums. Default: false |
| uninterpreted_option | [UninterpretedOption](#google.protobuf.UninterpretedOption) | repeated | The parser stores options it doesn&#39;t recognize here. See above. |






<a name="google.protobuf.EnumValueDescriptorProto"></a>

### EnumValueDescriptorProto
Describes a value within an enum.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| name | [string](#string) | optional |  |
| number | [int32](#int32) | optional |  |
| options | [EnumValueOptions](#google.protobuf.EnumValueOptions) | optional |  |






<a name="google.protobuf.EnumValueOptions"></a>

### EnumValueOptions



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| deprecated | [bool](#bool) | optional | Is this enum value deprecated? Depending on the target platform, this can emit Deprecated annotations for the enum value, or it will be completely ignored; in the very least, this is a formalization for deprecating enum values. Default: false |
| uninterpreted_option | [UninterpretedOption](#google.protobuf.UninterpretedOption) | repeated | The parser stores options it doesn&#39;t recognize here. See above. |






<a name="google.protobuf.ExtensionRangeOptions"></a>

### ExtensionRangeOptions



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| uninterpreted_option | [UninterpretedOption](#google.protobuf.UninterpretedOption) | repeated | The parser stores options it doesn&#39;t recognize here. See above. |






<a name="google.protobuf.FieldDescriptorProto"></a>

### FieldDescriptorProto
Describes a field within a message.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| name | [string](#string) | optional |  |
| number | [int32](#int32) | optional |  |
| label | [FieldDescriptorProto.Label](#google.protobuf.FieldDescriptorProto.Label) | optional |  |
| type | [FieldDescriptorProto.Type](#google.protobuf.FieldDescriptorProto.Type) | optional | If type_name is set, this need not be set. If both this and type_name are set, this must be one of TYPE_ENUM, TYPE_MESSAGE or TYPE_GROUP. |
| type_name | [string](#string) | optional | For message and enum types, this is the name of the type. If the name starts with a &#39;.&#39;, it is fully-qualified. Otherwise, C&#43;&#43;-like scoping rules are used to find the type (i.e. first the nested types within this message are searched, then within the parent, on up to the root namespace). |
| extendee | [string](#string) | optional | For extensions, this is the name of the type being extended. It is resolved in the same manner as type_name. |
| default_value | [string](#string) | optional | For numeric types, contains the original text representation of the value. For booleans, &#34;true&#34; or &#34;false&#34;. For strings, contains the default text contents (not escaped in any way). For bytes, contains the C escaped value. All bytes &gt;= 128 are escaped. TODO(kenton): Base-64 encode? |
| oneof_index | [int32](#int32) | optional | If set, gives the index of a oneof in the containing type&#39;s oneof_decl list. This field is a member of that oneof. |
| json_name | [string](#string) | optional | JSON name of this field. The value is set by protocol compiler. If the user has set a &#34;json_name&#34; option on this field, that option&#39;s value will be used. Otherwise, it&#39;s deduced from the field&#39;s name by converting it to camelCase. |
| options | [FieldOptions](#google.protobuf.FieldOptions) | optional |  |






<a name="google.protobuf.FieldOptions"></a>

### FieldOptions



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| ctype | [FieldOptions.CType](#google.protobuf.FieldOptions.CType) | optional | The ctype option instructs the C&#43;&#43; code generator to use a different representation of the field than it normally would. See the specific options below. This option is not yet implemented in the open source release -- sorry, we&#39;ll try to include it in a future version! Default: STRING |
| packed | [bool](#bool) | optional | The packed option can be enabled for repeated primitive fields to enable a more efficient representation on the wire. Rather than repeatedly writing the tag and type for each element, the entire array is encoded as a single length-delimited blob. In proto3, only explicit setting it to false will avoid using packed encoding. |
| jstype | [FieldOptions.JSType](#google.protobuf.FieldOptions.JSType) | optional | The jstype option determines the JavaScript type used for values of the field. The option is permitted only for 64 bit integral and fixed types (int64, uint64, sint64, fixed64, sfixed64). A field with jstype JS_STRING is represented as JavaScript string, which avoids loss of precision that can happen when a large value is converted to a floating point JavaScript. Specifying JS_NUMBER for the jstype causes the generated JavaScript code to use the JavaScript &#34;number&#34; type. The behavior of the default option JS_NORMAL is implementation dependent.

This option is an enum to permit additional types to be added, e.g. goog.math.Integer. Default: JS_NORMAL |
| lazy | [bool](#bool) | optional | Should this field be parsed lazily? Lazy applies only to message-type fields. It means that when the outer message is initially parsed, the inner message&#39;s contents will not be parsed but instead stored in encoded form. The inner message will actually be parsed when it is first accessed.

This is only a hint. Implementations are free to choose whether to use eager or lazy parsing regardless of the value of this option. However, setting this option true suggests that the protocol author believes that using lazy parsing on this field is worth the additional bookkeeping overhead typically needed to implement it.

This option does not affect the public interface of any generated code; all method signatures remain the same. Furthermore, thread-safety of the interface is not affected by this option; const methods remain safe to call from multiple threads concurrently, while non-const methods continue to require exclusive access.

Note that implementations may choose not to check required fields within a lazy sub-message. That is, calling IsInitialized() on the outer message may return true even if the inner message has missing required fields. This is necessary because otherwise the inner message would have to be parsed in order to perform the check, defeating the purpose of lazy parsing. An implementation which chooses not to check required fields must be consistent about it. That is, for any particular sub-message, the implementation must either *always* check its required fields, or *never* check its required fields, regardless of whether or not the message has been parsed. Default: false |
| deprecated | [bool](#bool) | optional | Is this field deprecated? Depending on the target platform, this can emit Deprecated annotations for accessors, or it will be completely ignored; in the very least, this is a formalization for deprecating fields. Default: false |
| weak | [bool](#bool) | optional | For Google-internal migration only. Do not use. Default: false |
| uninterpreted_option | [UninterpretedOption](#google.protobuf.UninterpretedOption) | repeated | The parser stores options it doesn&#39;t recognize here. See above. |






<a name="google.protobuf.FileDescriptorProto"></a>

### FileDescriptorProto
Describes a complete .proto file.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| name | [string](#string) | optional | file name, relative to root of source tree |
| package | [string](#string) | optional | e.g. &#34;foo&#34;, &#34;foo.bar&#34;, etc. |
| dependency | [string](#string) | repeated | Names of files imported by this file. |
| public_dependency | [int32](#int32) | repeated | Indexes of the public imported files in the dependency list above. |
| weak_dependency | [int32](#int32) | repeated | Indexes of the weak imported files in the dependency list. For Google-internal migration only. Do not use. |
| message_type | [DescriptorProto](#google.protobuf.DescriptorProto) | repeated | All top-level definitions in this file. |
| enum_type | [EnumDescriptorProto](#google.protobuf.EnumDescriptorProto) | repeated |  |
| service | [ServiceDescriptorProto](#google.protobuf.ServiceDescriptorProto) | repeated |  |
| extension | [FieldDescriptorProto](#google.protobuf.FieldDescriptorProto) | repeated |  |
| options | [FileOptions](#google.protobuf.FileOptions) | optional |  |
| source_code_info | [SourceCodeInfo](#google.protobuf.SourceCodeInfo) | optional | This field contains optional information about the original source code. You may safely remove this entire field without harming runtime functionality of the descriptors -- the information is needed only by development tools. |
| syntax | [string](#string) | optional | The syntax of the proto file. The supported values are &#34;proto2&#34; and &#34;proto3&#34;. |






<a name="google.protobuf.FileDescriptorSet"></a>

### FileDescriptorSet
The protocol compiler can output a FileDescriptorSet containing the .proto
files it parses.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| file | [FileDescriptorProto](#google.protobuf.FileDescriptorProto) | repeated |  |






<a name="google.protobuf.FileOptions"></a>

### FileOptions



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| java_package | [string](#string) | optional | Sets the Java package where classes generated from this .proto will be placed. By default, the proto package is used, but this is often inappropriate because proto packages do not normally start with backwards domain names. |
| java_outer_classname | [string](#string) | optional | If set, all the classes from the .proto file are wrapped in a single outer class with the given name. This applies to both Proto1 (equivalent to the old &#34;--one_java_file&#34; option) and Proto2 (where a .proto always translates to a single class, but you may want to explicitly choose the class name). |
| java_multiple_files | [bool](#bool) | optional | If set true, then the Java code generator will generate a separate .java file for each top-level message, enum, and service defined in the .proto file. Thus, these types will *not* be nested inside the outer class named by java_outer_classname. However, the outer class will still be generated to contain the file&#39;s getDescriptor() method as well as any top-level extensions defined in the file. Default: false |
| java_generate_equals_and_hash | [bool](#bool) | optional | This option does nothing. |
| java_string_check_utf8 | [bool](#bool) | optional | If set true, then the Java2 code generator will generate code that throws an exception whenever an attempt is made to assign a non-UTF-8 byte sequence to a string field. Message reflection will do the same. However, an extension field still accepts non-UTF-8 byte sequences. This option has no effect on when used with the lite runtime. Default: false |
| optimize_for | [FileOptions.OptimizeMode](#google.protobuf.FileOptions.OptimizeMode) | optional |  Default: SPEED |
| go_package | [string](#string) | optional | Sets the Go package where structs generated from this .proto will be placed. If omitted, the Go package will be derived from the following: - The basename of the package import path, if provided. - Otherwise, the package statement in the .proto file, if present. - Otherwise, the basename of the .proto file, without extension. |
| cc_generic_services | [bool](#bool) | optional | Should generic services be generated in each language? &#34;Generic&#34; services are not specific to any particular RPC system. They are generated by the main code generators in each language (without additional plugins). Generic services were the only kind of service generation supported by early versions of google.protobuf.

Generic services are now considered deprecated in favor of using plugins that generate code specific to your particular RPC system. Therefore, these default to false. Old code which depends on generic services should explicitly set them to true. Default: false |
| java_generic_services | [bool](#bool) | optional |  Default: false |
| py_generic_services | [bool](#bool) | optional |  Default: false |
| php_generic_services | [bool](#bool) | optional |  Default: false |
| deprecated | [bool](#bool) | optional | Is this file deprecated? Depending on the target platform, this can emit Deprecated annotations for everything in the file, or it will be completely ignored; in the very least, this is a formalization for deprecating files. Default: false |
| cc_enable_arenas | [bool](#bool) | optional | Enables the use of arenas for the proto messages in this file. This applies only to generated classes for C&#43;&#43;. Default: false |
| objc_class_prefix | [string](#string) | optional | Sets the objective c class prefix which is prepended to all objective c generated classes from this .proto. There is no default. |
| csharp_namespace | [string](#string) | optional | Namespace for generated classes; defaults to the package. |
| swift_prefix | [string](#string) | optional | By default Swift generators will take the proto package and CamelCase it replacing &#39;.&#39; with underscore and use that to prefix the types/symbols defined. When this options is provided, they will use this value instead to prefix the types/symbols defined. |
| php_class_prefix | [string](#string) | optional | Sets the php class prefix which is prepended to all php generated classes from this .proto. Default is empty. |
| php_namespace | [string](#string) | optional | Use this option to change the namespace of php generated classes. Default is empty. When this option is empty, the package name will be used for determining the namespace. |
| php_metadata_namespace | [string](#string) | optional | Use this option to change the namespace of php generated metadata classes. Default is empty. When this option is empty, the proto file name will be used for determining the namespace. |
| ruby_package | [string](#string) | optional | Use this option to change the package of ruby generated classes. Default is empty. When this option is not set, the package name will be used for determining the ruby package. |
| uninterpreted_option | [UninterpretedOption](#google.protobuf.UninterpretedOption) | repeated | The parser stores options it doesn&#39;t recognize here. See the documentation for the &#34;Options&#34; section above. |






<a name="google.protobuf.GeneratedCodeInfo"></a>

### GeneratedCodeInfo
Describes the relationship between generated code and its original source
file. A GeneratedCodeInfo message is associated with only one generated
source file, but may contain references to different source .proto files.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| annotation | [GeneratedCodeInfo.Annotation](#google.protobuf.GeneratedCodeInfo.Annotation) | repeated | An Annotation connects some span of text in generated code to an element of its generating .proto file. |






<a name="google.protobuf.GeneratedCodeInfo.Annotation"></a>

### GeneratedCodeInfo.Annotation



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| path | [int32](#int32) | repeated | Identifies the element in the original source .proto file. This field is formatted the same as SourceCodeInfo.Location.path. |
| source_file | [string](#string) | optional | Identifies the filesystem path to the original source .proto. |
| begin | [int32](#int32) | optional | Identifies the starting offset in bytes in the generated code that relates to the identified object. |
| end | [int32](#int32) | optional | Identifies the ending offset in bytes in the generated code that relates to the identified offset. The end offset should be one past the last relevant byte (so the length of the text = end - begin). |






<a name="google.protobuf.MessageOptions"></a>

### MessageOptions



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| message_set_wire_format | [bool](#bool) | optional | Set true to use the old proto1 MessageSet wire format for extensions. This is provided for backwards-compatibility with the MessageSet wire format. You should not use this for any other reason: It&#39;s less efficient, has fewer features, and is more complicated.

The message must be defined exactly as follows: message Foo { option message_set_wire_format = true; extensions 4 to max; } Note that the message cannot have any defined fields; MessageSets only have extensions.

All extensions of your type must be singular messages; e.g. they cannot be int32s, enums, or repeated messages.

Because this is an option, the above two restrictions are not enforced by the protocol compiler. Default: false |
| no_standard_descriptor_accessor | [bool](#bool) | optional | Disables the generation of the standard &#34;descriptor()&#34; accessor, which can conflict with a field of the same name. This is meant to make migration from proto1 easier; new code should avoid fields named &#34;descriptor&#34;. Default: false |
| deprecated | [bool](#bool) | optional | Is this message deprecated? Depending on the target platform, this can emit Deprecated annotations for the message, or it will be completely ignored; in the very least, this is a formalization for deprecating messages. Default: false |
| map_entry | [bool](#bool) | optional | Whether the message is an automatically generated map entry type for the maps field.

For maps fields: map&lt;KeyType, ValueType&gt; map_field = 1; The parsed descriptor looks like: message MapFieldEntry { option map_entry = true; optional KeyType key = 1; optional ValueType value = 2; } repeated MapFieldEntry map_field = 1;

Implementations may choose not to generate the map_entry=true message, but use a native map in the target language to hold the keys and values. The reflection APIs in such implementions still need to work as if the field is a repeated message field.

NOTE: Do not set the option in .proto files. Always use the maps syntax instead. The option should only be implicitly set by the proto compiler parser. |
| uninterpreted_option | [UninterpretedOption](#google.protobuf.UninterpretedOption) | repeated | The parser stores options it doesn&#39;t recognize here. See above. |






<a name="google.protobuf.MethodDescriptorProto"></a>

### MethodDescriptorProto
Describes a method of a service.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| name | [string](#string) | optional |  |
| input_type | [string](#string) | optional | Input and output type names. These are resolved in the same way as FieldDescriptorProto.type_name, but must refer to a message type. |
| output_type | [string](#string) | optional |  |
| options | [MethodOptions](#google.protobuf.MethodOptions) | optional |  |
| client_streaming | [bool](#bool) | optional | Identifies if client streams multiple client messages Default: false |
| server_streaming | [bool](#bool) | optional | Identifies if server streams multiple server messages Default: false |






<a name="google.protobuf.MethodOptions"></a>

### MethodOptions



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| deprecated | [bool](#bool) | optional | Is this method deprecated? Depending on the target platform, this can emit Deprecated annotations for the method, or it will be completely ignored; in the very least, this is a formalization for deprecating methods. Default: false |
| idempotency_level | [MethodOptions.IdempotencyLevel](#google.protobuf.MethodOptions.IdempotencyLevel) | optional |  Default: IDEMPOTENCY_UNKNOWN |
| uninterpreted_option | [UninterpretedOption](#google.protobuf.UninterpretedOption) | repeated | The parser stores options it doesn&#39;t recognize here. See above. |






<a name="google.protobuf.OneofDescriptorProto"></a>

### OneofDescriptorProto
Describes a oneof.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| name | [string](#string) | optional |  |
| options | [OneofOptions](#google.protobuf.OneofOptions) | optional |  |






<a name="google.protobuf.OneofOptions"></a>

### OneofOptions



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| uninterpreted_option | [UninterpretedOption](#google.protobuf.UninterpretedOption) | repeated | The parser stores options it doesn&#39;t recognize here. See above. |






<a name="google.protobuf.ServiceDescriptorProto"></a>

### ServiceDescriptorProto
Describes a service.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| name | [string](#string) | optional |  |
| method | [MethodDescriptorProto](#google.protobuf.MethodDescriptorProto) | repeated |  |
| options | [ServiceOptions](#google.protobuf.ServiceOptions) | optional |  |






<a name="google.protobuf.ServiceOptions"></a>

### ServiceOptions



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| deprecated | [bool](#bool) | optional | Is this service deprecated? Depending on the target platform, this can emit Deprecated annotations for the service, or it will be completely ignored; in the very least, this is a formalization for deprecating services. Default: false |
| uninterpreted_option | [UninterpretedOption](#google.protobuf.UninterpretedOption) | repeated | The parser stores options it doesn&#39;t recognize here. See above. |






<a name="google.protobuf.SourceCodeInfo"></a>

### SourceCodeInfo
Encapsulates information about the original source file from which a
FileDescriptorProto was generated.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| location | [SourceCodeInfo.Location](#google.protobuf.SourceCodeInfo.Location) | repeated | A Location identifies a piece of source code in a .proto file which corresponds to a particular definition. This information is intended to be useful to IDEs, code indexers, documentation generators, and similar tools.

For example, say we have a file like: message Foo { optional string foo = 1; } Let&#39;s look at just the field definition: optional string foo = 1; ^ ^^ ^^ ^ ^^^ a bc de f ghi We have the following locations: span path represents [a,i) [ 4, 0, 2, 0 ] The whole field definition. [a,b) [ 4, 0, 2, 0, 4 ] The label (optional). [c,d) [ 4, 0, 2, 0, 5 ] The type (string). [e,f) [ 4, 0, 2, 0, 1 ] The name (foo). [g,h) [ 4, 0, 2, 0, 3 ] The number (1).

Notes: - A location may refer to a repeated field itself (i.e. not to any particular index within it). This is used whenever a set of elements are logically enclosed in a single code segment. For example, an entire extend block (possibly containing multiple extension definitions) will have an outer location whose path refers to the &#34;extensions&#34; repeated field without an index. - Multiple locations may have the same path. This happens when a single logical declaration is spread out across multiple places. The most obvious example is the &#34;extend&#34; block again -- there may be multiple extend blocks in the same scope, each of which will have the same path. - A location&#39;s span is not always a subset of its parent&#39;s span. For example, the &#34;extendee&#34; of an extension declaration appears at the beginning of the &#34;extend&#34; block and is shared by all extensions within the block. - Just because a location&#39;s span is a subset of some other location&#39;s span does not mean that it is a descendent. For example, a &#34;group&#34; defines both a type and a field in a single declaration. Thus, the locations corresponding to the type and field and their components will overlap. - Code which tries to interpret locations should probably be designed to ignore those that it doesn&#39;t understand, as more types of locations could be recorded in the future. |






<a name="google.protobuf.SourceCodeInfo.Location"></a>

### SourceCodeInfo.Location



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| path | [int32](#int32) | repeated | Identifies which part of the FileDescriptorProto was defined at this location.

Each element is a field number or an index. They form a path from the root FileDescriptorProto to the place where the definition. For example, this path: [ 4, 3, 2, 7, 1 ] refers to: file.message_type(3) // 4, 3 .field(7) // 2, 7 .name() // 1 This is because FileDescriptorProto.message_type has field number 4: repeated DescriptorProto message_type = 4; and DescriptorProto.field has field number 2: repeated FieldDescriptorProto field = 2; and FieldDescriptorProto.name has field number 1: optional string name = 1;

Thus, the above path gives the location of a field name. If we removed the last element: [ 4, 3, 2, 7 ] this path refers to the whole field declaration (from the beginning of the label to the terminating semicolon). |
| span | [int32](#int32) | repeated | Always has exactly three or four elements: start line, start column, end line (optional, otherwise assumed same as start line), end column. These are packed into a single field for efficiency. Note that line and column numbers are zero-based -- typically you will want to add 1 to each before displaying to a user. |
| leading_comments | [string](#string) | optional | If this SourceCodeInfo represents a complete declaration, these are any comments appearing before and after the declaration which appear to be attached to the declaration.

A series of line comments appearing on consecutive lines, with no other tokens appearing on those lines, will be treated as a single comment.

leading_detached_comments will keep paragraphs of comments that appear before (but not connected to) the current element. Each paragraph, separated by empty lines, will be one comment element in the repeated field.

Only the comment content is provided; comment markers (e.g. //) are stripped out. For block comments, leading whitespace and an asterisk will be stripped from the beginning of each line other than the first. Newlines are included in the output.

Examples:

 optional int32 foo = 1; // Comment attached to foo. // Comment attached to bar. optional int32 bar = 2;

 optional string baz = 3; // Comment attached to baz. // Another line attached to baz.

 // Comment attached to qux. // // Another line attached to qux. optional double qux = 4;

 // Detached comment for corge. This is not leading or trailing comments // to qux or corge because there are blank lines separating it from // both.

 // Detached comment for corge paragraph 2.

 optional string corge = 5; /* Block comment attached * to corge. Leading asterisks * will be removed. */ /* Block comment attached to * grault. */ optional int32 grault = 6;

 // ignored detached comments. |
| trailing_comments | [string](#string) | optional |  |
| leading_detached_comments | [string](#string) | repeated |  |






<a name="google.protobuf.UninterpretedOption"></a>

### UninterpretedOption
A message representing a option the parser does not recognize. This only
appears in options protos created by the compiler::Parser class.
DescriptorPool resolves these when building Descriptor objects. Therefore,
options protos in descriptor objects (e.g. returned by Descriptor::options(),
or produced by Descriptor::CopyTo()) will never have UninterpretedOptions
in them.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| name | [UninterpretedOption.NamePart](#google.protobuf.UninterpretedOption.NamePart) | repeated |  |
| identifier_value | [string](#string) | optional | The value of the uninterpreted option, in whatever type the tokenizer identified it as during parsing. Exactly one of these should be set. |
| positive_int_value | [uint64](#uint64) | optional |  |
| negative_int_value | [int64](#int64) | optional |  |
| double_value | [double](#double) | optional |  |
| string_value | [bytes](#bytes) | optional |  |
| aggregate_value | [string](#string) | optional |  |






<a name="google.protobuf.UninterpretedOption.NamePart"></a>

### UninterpretedOption.NamePart
The name of the uninterpreted option.  Each string represents a segment in
a dot-separated name.  is_extension is true iff a segment represents an
extension (denoted with parentheses in options specs in .proto files).
E.g.,{ [&#34;foo&#34;, false], [&#34;bar.baz&#34;, true], [&#34;qux&#34;, false] } represents
&#34;foo.(bar.baz).qux&#34;.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| name_part | [string](#string) | required |  |
| is_extension | [bool](#bool) | required |  |





 


<a name="google.protobuf.FieldDescriptorProto.Label"></a>

### FieldDescriptorProto.Label


| Name | Number | Description |
| ---- | ------ | ----------- |
| LABEL_OPTIONAL | 1 | 0 is reserved for errors |
| LABEL_REQUIRED | 2 |  |
| LABEL_REPEATED | 3 |  |



<a name="google.protobuf.FieldDescriptorProto.Type"></a>

### FieldDescriptorProto.Type


| Name | Number | Description |
| ---- | ------ | ----------- |
| TYPE_DOUBLE | 1 | 0 is reserved for errors. Order is weird for historical reasons. |
| TYPE_FLOAT | 2 |  |
| TYPE_INT64 | 3 | Not ZigZag encoded. Negative numbers take 10 bytes. Use TYPE_SINT64 if negative values are likely. |
| TYPE_UINT64 | 4 |  |
| TYPE_INT32 | 5 | Not ZigZag encoded. Negative numbers take 10 bytes. Use TYPE_SINT32 if negative values are likely. |
| TYPE_FIXED64 | 6 |  |
| TYPE_FIXED32 | 7 |  |
| TYPE_BOOL | 8 |  |
| TYPE_STRING | 9 |  |
| TYPE_GROUP | 10 | Tag-delimited aggregate. Group type is deprecated and not supported in proto3. However, Proto3 implementations should still be able to parse the group wire format and treat group fields as unknown fields. |
| TYPE_MESSAGE | 11 | Length-delimited aggregate. |
| TYPE_BYTES | 12 | New in version 2. |
| TYPE_UINT32 | 13 |  |
| TYPE_ENUM | 14 |  |
| TYPE_SFIXED32 | 15 |  |
| TYPE_SFIXED64 | 16 |  |
| TYPE_SINT32 | 17 | Uses ZigZag encoding. |
| TYPE_SINT64 | 18 | Uses ZigZag encoding. |



<a name="google.protobuf.FieldOptions.CType"></a>

### FieldOptions.CType


| Name | Number | Description |
| ---- | ------ | ----------- |
| STRING | 0 | Default mode. |
| CORD | 1 |  |
| STRING_PIECE | 2 |  |



<a name="google.protobuf.FieldOptions.JSType"></a>

### FieldOptions.JSType


| Name | Number | Description |
| ---- | ------ | ----------- |
| JS_NORMAL | 0 | Use the default type. |
| JS_STRING | 1 | Use JavaScript strings. |
| JS_NUMBER | 2 | Use JavaScript numbers. |



<a name="google.protobuf.FileOptions.OptimizeMode"></a>

### FileOptions.OptimizeMode
Generated classes can be optimized for speed or code size.

| Name | Number | Description |
| ---- | ------ | ----------- |
| SPEED | 1 | Generate complete code for parsing, serialization, |
| CODE_SIZE | 2 | etc.

Use ReflectionOps to implement these methods. |
| LITE_RUNTIME | 3 | Generate code using MessageLite and the lite runtime. |



<a name="google.protobuf.MethodOptions.IdempotencyLevel"></a>

### MethodOptions.IdempotencyLevel
Is this method side-effect-free (or safe in HTTP parlance), or idempotent,
or neither? HTTP based RPC implementation may choose GET verb for safe
methods, and PUT verb for idempotent methods instead of the default POST.

| Name | Number | Description |
| ---- | ------ | ----------- |
| IDEMPOTENCY_UNKNOWN | 0 |  |
| NO_SIDE_EFFECTS | 1 | implies idempotent |
| IDEMPOTENT | 2 | idempotent, but may have side effects |


 

 

 



<a name="google/protobuf/duration.proto"></a>
<p align="right"><a href="#top">Top</a></p>

## google/protobuf/duration.proto



<a name="google.protobuf.Duration"></a>

### Duration
A Duration represents a signed, fixed-length span of time represented
as a count of seconds and fractions of seconds at nanosecond
resolution. It is independent of any calendar and concepts like &#34;day&#34;
or &#34;month&#34;. It is related to Timestamp in that the difference between
two Timestamp values is a Duration and it can be added or subtracted
from a Timestamp. Range is approximately &#43;-10,000 years.

# Examples

Example 1: Compute Duration from two Timestamps in pseudo code.

    Timestamp start = ...;
    Timestamp end = ...;
    Duration duration = ...;

    duration.seconds = end.seconds - start.seconds;
    duration.nanos = end.nanos - start.nanos;

    if (duration.seconds &lt; 0 &amp;&amp; duration.nanos &gt; 0) {
      duration.seconds &#43;= 1;
      duration.nanos -= 1000000000;
    } else if (durations.seconds &gt; 0 &amp;&amp; duration.nanos &lt; 0) {
      duration.seconds -= 1;
      duration.nanos &#43;= 1000000000;
    }

Example 2: Compute Timestamp from Timestamp &#43; Duration in pseudo code.

    Timestamp start = ...;
    Duration duration = ...;
    Timestamp end = ...;

    end.seconds = start.seconds &#43; duration.seconds;
    end.nanos = start.nanos &#43; duration.nanos;

    if (end.nanos &lt; 0) {
      end.seconds -= 1;
      end.nanos &#43;= 1000000000;
    } else if (end.nanos &gt;= 1000000000) {
      end.seconds &#43;= 1;
      end.nanos -= 1000000000;
    }

Example 3: Compute Duration from datetime.timedelta in Python.

    td = datetime.timedelta(days=3, minutes=10)
    duration = Duration()
    duration.FromTimedelta(td)

# JSON Mapping

In JSON format, the Duration type is encoded as a string rather than an
object, where the string ends in the suffix &#34;s&#34; (indicating seconds) and
is preceded by the number of seconds, with nanoseconds expressed as
fractional seconds. For example, 3 seconds with 0 nanoseconds should be
encoded in JSON format as &#34;3s&#34;, while 3 seconds and 1 nanosecond should
be expressed in JSON format as &#34;3.000000001s&#34;, and 3 seconds and 1
microsecond should be expressed in JSON format as &#34;3.000001s&#34;.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| seconds | [int64](#int64) |  | Signed seconds of the span of time. Must be from -315,576,000,000 to &#43;315,576,000,000 inclusive. Note: these bounds are computed from: 60 sec/min * 60 min/hr * 24 hr/day * 365.25 days/year * 10000 years |
| nanos | [int32](#int32) |  | Signed fractions of a second at nanosecond resolution of the span of time. Durations less than one second are represented with a 0 `seconds` field and a positive or negative `nanos` field. For durations of one second or more, a non-zero value for the `nanos` field must be of the same sign as the `seconds` field. Must be from -999,999,999 to &#43;999,999,999 inclusive. |





 

 

 

 



<a name="google/protobuf/empty.proto"></a>
<p align="right"><a href="#top">Top</a></p>

## google/protobuf/empty.proto



<a name="google.protobuf.Empty"></a>

### Empty
A generic empty message that you can re-use to avoid defining duplicated
empty messages in your APIs. A typical example is to use it as the request
or the response type of an API method. For instance:

    service Foo {
      rpc Bar(google.protobuf.Empty) returns (google.protobuf.Empty);
    }

The JSON representation for `Empty` is empty JSON object `{}`.





 

 

 

 



<a name="google/protobuf/field_mask.proto"></a>
<p align="right"><a href="#top">Top</a></p>

## google/protobuf/field_mask.proto



<a name="google.protobuf.FieldMask"></a>

### FieldMask
`FieldMask` represents a set of symbolic field paths, for example:

    paths: &#34;f.a&#34;
    paths: &#34;f.b.d&#34;

Here `f` represents a field in some root message, `a` and `b`
fields in the message found in `f`, and `d` a field found in the
message in `f.b`.

Field masks are used to specify a subset of fields that should be
returned by a get operation or modified by an update operation.
Field masks also have a custom JSON encoding (see below).

# Field Masks in Projections

When used in the context of a projection, a response message or
sub-message is filtered by the API to only contain those fields as
specified in the mask. For example, if the mask in the previous
example is applied to a response message as follows:

    f {
      a : 22
      b {
        d : 1
        x : 2
      }
      y : 13
    }
    z: 8

The result will not contain specific values for fields x,y and z
(their value will be set to the default, and omitted in proto text
output):


    f {
      a : 22
      b {
        d : 1
      }
    }

A repeated field is not allowed except at the last position of a
paths string.

If a FieldMask object is not present in a get operation, the
operation applies to all fields (as if a FieldMask of all fields
had been specified).

Note that a field mask does not necessarily apply to the
top-level response message. In case of a REST get operation, the
field mask applies directly to the response, but in case of a REST
list operation, the mask instead applies to each individual message
in the returned resource list. In case of a REST custom method,
other definitions may be used. Where the mask applies will be
clearly documented together with its declaration in the API.  In
any case, the effect on the returned resource/resources is required
behavior for APIs.

# Field Masks in Update Operations

A field mask in update operations specifies which fields of the
targeted resource are going to be updated. The API is required
to only change the values of the fields as specified in the mask
and leave the others untouched. If a resource is passed in to
describe the updated values, the API ignores the values of all
fields not covered by the mask.

If a repeated field is specified for an update operation, the existing
repeated values in the target resource will be overwritten by the new values.
Note that a repeated field is only allowed in the last position of a `paths`
string.

If a sub-message is specified in the last position of the field mask for an
update operation, then the existing sub-message in the target resource is
overwritten. Given the target message:

    f {
      b {
        d : 1
        x : 2
      }
      c : 1
    }

And an update message:

    f {
      b {
        d : 10
      }
    }

then if the field mask is:

 paths: &#34;f.b&#34;

then the result will be:

    f {
      b {
        d : 10
      }
      c : 1
    }

However, if the update mask was:

 paths: &#34;f.b.d&#34;

then the result would be:

    f {
      b {
        d : 10
        x : 2
      }
      c : 1
    }

In order to reset a field&#39;s value to the default, the field must
be in the mask and set to the default value in the provided resource.
Hence, in order to reset all fields of a resource, provide a default
instance of the resource and set all fields in the mask, or do
not provide a mask as described below.

If a field mask is not present on update, the operation applies to
all fields (as if a field mask of all fields has been specified).
Note that in the presence of schema evolution, this may mean that
fields the client does not know and has therefore not filled into
the request will be reset to their default. If this is unwanted
behavior, a specific service may require a client to always specify
a field mask, producing an error if not.

As with get operations, the location of the resource which
describes the updated values in the request message depends on the
operation kind. In any case, the effect of the field mask is
required to be honored by the API.

## Considerations for HTTP REST

The HTTP kind of an update operation which uses a field mask must
be set to PATCH instead of PUT in order to satisfy HTTP semantics
(PUT must only be used for full updates).

# JSON Encoding of Field Masks

In JSON, a field mask is encoded as a single string where paths are
separated by a comma. Fields name in each path are converted
to/from lower-camel naming conventions.

As an example, consider the following message declarations:

    message Profile {
      User user = 1;
      Photo photo = 2;
    }
    message User {
      string display_name = 1;
      string address = 2;
    }

In proto a field mask for `Profile` may look as such:

    mask {
      paths: &#34;user.display_name&#34;
      paths: &#34;photo&#34;
    }

In JSON, the same mask is represented as below:

    {
      mask: &#34;user.displayName,photo&#34;
    }

# Field Masks and Oneof Fields

Field masks treat fields in oneofs just as regular fields. Consider the
following message:

    message SampleMessage {
      oneof test_oneof {
        string name = 4;
        SubMessage sub_message = 9;
      }
    }

The field mask can be:

    mask {
      paths: &#34;name&#34;
    }

Or:

    mask {
      paths: &#34;sub_message&#34;
    }

Note that oneof type names (&#34;test_oneof&#34; in this case) cannot be used in
paths.

## Field Mask Verification

The implementation of any API method which has a FieldMask type field in the
request should verify the included field paths, and return an
`INVALID_ARGUMENT` error if any path is duplicated or unmappable.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| paths | [string](#string) | repeated | The set of field mask paths. |





 

 

 

 



<a name="google/protobuf/source_context.proto"></a>
<p align="right"><a href="#top">Top</a></p>

## google/protobuf/source_context.proto



<a name="google.protobuf.SourceContext"></a>

### SourceContext
`SourceContext` represents information about the source of a
protobuf element, like the file in which it is defined.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| file_name | [string](#string) |  | The path-qualified name of the .proto file that contained the associated protobuf element. For example: `&#34;google/protobuf/source_context.proto&#34;`. |





 

 

 

 



<a name="google/protobuf/struct.proto"></a>
<p align="right"><a href="#top">Top</a></p>

## google/protobuf/struct.proto



<a name="google.protobuf.ListValue"></a>

### ListValue
`ListValue` is a wrapper around a repeated field of values.

The JSON representation for `ListValue` is JSON array.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| values | [Value](#google.protobuf.Value) | repeated | Repeated field of dynamically typed values. |






<a name="google.protobuf.Struct"></a>

### Struct
`Struct` represents a structured data value, consisting of fields
which map to dynamically typed values. In some languages, `Struct`
might be supported by a native representation. For example, in
scripting languages like JS a struct is represented as an
object. The details of that representation are described together
with the proto support for the language.

The JSON representation for `Struct` is JSON object.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| fields | [Struct.FieldsEntry](#google.protobuf.Struct.FieldsEntry) | repeated | Unordered map of dynamically typed values. |






<a name="google.protobuf.Struct.FieldsEntry"></a>

### Struct.FieldsEntry



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| key | [string](#string) |  |  |
| value | [Value](#google.protobuf.Value) |  |  |






<a name="google.protobuf.Value"></a>

### Value
`Value` represents a dynamically typed value which can be either
null, a number, a string, a boolean, a recursive struct value, or a
list of values. A producer of value is expected to set one of that
variants, absence of any variant indicates an error.

The JSON representation for `Value` is JSON value.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| null_value | [NullValue](#google.protobuf.NullValue) |  | Represents a null value. |
| number_value | [double](#double) |  | Represents a double value. |
| string_value | [string](#string) |  | Represents a string value. |
| bool_value | [bool](#bool) |  | Represents a boolean value. |
| struct_value | [Struct](#google.protobuf.Struct) |  | Represents a structured value. |
| list_value | [ListValue](#google.protobuf.ListValue) |  | Represents a repeated `Value`. |





 


<a name="google.protobuf.NullValue"></a>

### NullValue
`NullValue` is a singleton enumeration to represent the null value for the
`Value` type union.

 The JSON representation for `NullValue` is JSON `null`.

| Name | Number | Description |
| ---- | ------ | ----------- |
| NULL_VALUE | 0 | Null value. |


 

 

 



<a name="google/protobuf/timestamp.proto"></a>
<p align="right"><a href="#top">Top</a></p>

## google/protobuf/timestamp.proto



<a name="google.protobuf.Timestamp"></a>

### Timestamp
A Timestamp represents a point in time independent of any time zone
or calendar, represented as seconds and fractions of seconds at
nanosecond resolution in UTC Epoch time. It is encoded using the
Proleptic Gregorian Calendar which extends the Gregorian calendar
backwards to year one. It is encoded assuming all minutes are 60
seconds long, i.e. leap seconds are &#34;smeared&#34; so that no leap second
table is needed for interpretation. Range is from
0001-01-01T00:00:00Z to 9999-12-31T23:59:59.999999999Z.
By restricting to that range, we ensure that we can convert to
and from  RFC 3339 date strings.
See [https://www.ietf.org/rfc/rfc3339.txt](https://www.ietf.org/rfc/rfc3339.txt).

# Examples

Example 1: Compute Timestamp from POSIX `time()`.

    Timestamp timestamp;
    timestamp.set_seconds(time(NULL));
    timestamp.set_nanos(0);

Example 2: Compute Timestamp from POSIX `gettimeofday()`.

    struct timeval tv;
    gettimeofday(&amp;tv, NULL);

    Timestamp timestamp;
    timestamp.set_seconds(tv.tv_sec);
    timestamp.set_nanos(tv.tv_usec * 1000);

Example 3: Compute Timestamp from Win32 `GetSystemTimeAsFileTime()`.

    FILETIME ft;
    GetSystemTimeAsFileTime(&amp;ft);
    UINT64 ticks = (((UINT64)ft.dwHighDateTime) &lt;&lt; 32) | ft.dwLowDateTime;

    // A Windows tick is 100 nanoseconds. Windows epoch 1601-01-01T00:00:00Z
    // is 11644473600 seconds before Unix epoch 1970-01-01T00:00:00Z.
    Timestamp timestamp;
    timestamp.set_seconds((INT64) ((ticks / 10000000) - 11644473600LL));
    timestamp.set_nanos((INT32) ((ticks % 10000000) * 100));

Example 4: Compute Timestamp from Java `System.currentTimeMillis()`.

    long millis = System.currentTimeMillis();

    Timestamp timestamp = Timestamp.newBuilder().setSeconds(millis / 1000)
        .setNanos((int) ((millis % 1000) * 1000000)).build();


Example 5: Compute Timestamp from current time in Python.

    timestamp = Timestamp()
    timestamp.GetCurrentTime()

# JSON Mapping

In JSON format, the Timestamp type is encoded as a string in the
[RFC 3339](https://www.ietf.org/rfc/rfc3339.txt) format. That is, the
format is &#34;{year}-{month}-{day}T{hour}:{min}:{sec}[.{frac_sec}]Z&#34;
where {year} is always expressed using four digits while {month}, {day},
{hour}, {min}, and {sec} are zero-padded to two digits each. The fractional
seconds, which can go up to 9 digits (i.e. up to 1 nanosecond resolution),
are optional. The &#34;Z&#34; suffix indicates the timezone (&#34;UTC&#34;); the timezone
is required. A proto3 JSON serializer should always use UTC (as indicated by
&#34;Z&#34;) when printing the Timestamp type and a proto3 JSON parser should be
able to accept both UTC and other timezones (as indicated by an offset).

For example, &#34;2017-01-15T01:30:15.01Z&#34; encodes 15.01 seconds past
01:30 UTC on January 15, 2017.

In JavaScript, one can convert a Date object to this format using the
standard [toISOString()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/toISOString]
method. In Python, a standard `datetime.datetime` object can be converted
to this format using [`strftime`](https://docs.python.org/2/library/time.html#time.strftime)
with the time format spec &#39;%Y-%m-%dT%H:%M:%S.%fZ&#39;. Likewise, in Java, one
can use the Joda Time&#39;s [`ISODateTimeFormat.dateTime()`](
http://www.joda.org/joda-time/apidocs/org/joda/time/format/ISODateTimeFormat.html#dateTime--
) to obtain a formatter capable of generating timestamps in this format.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| seconds | [int64](#int64) |  | Represents seconds of UTC time since Unix epoch 1970-01-01T00:00:00Z. Must be from 0001-01-01T00:00:00Z to 9999-12-31T23:59:59Z inclusive. |
| nanos | [int32](#int32) |  | Non-negative fractions of a second at nanosecond resolution. Negative second values with fractions must still have non-negative nanos values that count forward in time. Must be from 0 to 999,999,999 inclusive. |





 

 

 

 



<a name="google/protobuf/type.proto"></a>
<p align="right"><a href="#top">Top</a></p>

## google/protobuf/type.proto



<a name="google.protobuf.Enum"></a>

### Enum
Enum type definition.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| name | [string](#string) |  | Enum type name. |
| enumvalue | [EnumValue](#google.protobuf.EnumValue) | repeated | Enum value definitions. |
| options | [Option](#google.protobuf.Option) | repeated | Protocol buffer options. |
| source_context | [SourceContext](#google.protobuf.SourceContext) |  | The source context. |
| syntax | [Syntax](#google.protobuf.Syntax) |  | The source syntax. |






<a name="google.protobuf.EnumValue"></a>

### EnumValue
Enum value definition.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| name | [string](#string) |  | Enum value name. |
| number | [int32](#int32) |  | Enum value number. |
| options | [Option](#google.protobuf.Option) | repeated | Protocol buffer options. |






<a name="google.protobuf.Field"></a>

### Field
A single field of a message type.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| kind | [Field.Kind](#google.protobuf.Field.Kind) |  | The field type. |
| cardinality | [Field.Cardinality](#google.protobuf.Field.Cardinality) |  | The field cardinality. |
| number | [int32](#int32) |  | The field number. |
| name | [string](#string) |  | The field name. |
| type_url | [string](#string) |  | The field type URL, without the scheme, for message or enumeration types. Example: `&#34;type.googleapis.com/google.protobuf.Timestamp&#34;`. |
| oneof_index | [int32](#int32) |  | The index of the field type in `Type.oneofs`, for message or enumeration types. The first type has index 1; zero means the type is not in the list. |
| packed | [bool](#bool) |  | Whether to use alternative packed wire representation. |
| options | [Option](#google.protobuf.Option) | repeated | The protocol buffer options. |
| json_name | [string](#string) |  | The field JSON name. |
| default_value | [string](#string) |  | The string value of the default value of this field. Proto2 syntax only. |






<a name="google.protobuf.Option"></a>

### Option
A protocol buffer option, which can be attached to a message, field,
enumeration, etc.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| name | [string](#string) |  | The option&#39;s name. For protobuf built-in options (options defined in descriptor.proto), this is the short name. For example, `&#34;map_entry&#34;`. For custom options, it should be the fully-qualified name. For example, `&#34;google.api.http&#34;`. |
| value | [Any](#google.protobuf.Any) |  | The option&#39;s value packed in an Any message. If the value is a primitive, the corresponding wrapper type defined in google/protobuf/wrappers.proto should be used. If the value is an enum, it should be stored as an int32 value using the google.protobuf.Int32Value type. |






<a name="google.protobuf.Type"></a>

### Type
A protocol buffer message type.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| name | [string](#string) |  | The fully qualified message name. |
| fields | [Field](#google.protobuf.Field) | repeated | The list of fields. |
| oneofs | [string](#string) | repeated | The list of types appearing in `oneof` definitions in this type. |
| options | [Option](#google.protobuf.Option) | repeated | The protocol buffer options. |
| source_context | [SourceContext](#google.protobuf.SourceContext) |  | The source context. |
| syntax | [Syntax](#google.protobuf.Syntax) |  | The source syntax. |





 


<a name="google.protobuf.Field.Cardinality"></a>

### Field.Cardinality
Whether a field is optional, required, or repeated.

| Name | Number | Description |
| ---- | ------ | ----------- |
| CARDINALITY_UNKNOWN | 0 | For fields with unknown cardinality. |
| CARDINALITY_OPTIONAL | 1 | For optional fields. |
| CARDINALITY_REQUIRED | 2 | For required fields. Proto2 syntax only. |
| CARDINALITY_REPEATED | 3 | For repeated fields. |



<a name="google.protobuf.Field.Kind"></a>

### Field.Kind
Basic field types.

| Name | Number | Description |
| ---- | ------ | ----------- |
| TYPE_UNKNOWN | 0 | Field type unknown. |
| TYPE_DOUBLE | 1 | Field type double. |
| TYPE_FLOAT | 2 | Field type float. |
| TYPE_INT64 | 3 | Field type int64. |
| TYPE_UINT64 | 4 | Field type uint64. |
| TYPE_INT32 | 5 | Field type int32. |
| TYPE_FIXED64 | 6 | Field type fixed64. |
| TYPE_FIXED32 | 7 | Field type fixed32. |
| TYPE_BOOL | 8 | Field type bool. |
| TYPE_STRING | 9 | Field type string. |
| TYPE_GROUP | 10 | Field type group. Proto2 syntax only, and deprecated. |
| TYPE_MESSAGE | 11 | Field type message. |
| TYPE_BYTES | 12 | Field type bytes. |
| TYPE_UINT32 | 13 | Field type uint32. |
| TYPE_ENUM | 14 | Field type enum. |
| TYPE_SFIXED32 | 15 | Field type sfixed32. |
| TYPE_SFIXED64 | 16 | Field type sfixed64. |
| TYPE_SINT32 | 17 | Field type sint32. |
| TYPE_SINT64 | 18 | Field type sint64. |



<a name="google.protobuf.Syntax"></a>

### Syntax
The syntax in which a protocol buffer element is defined.

| Name | Number | Description |
| ---- | ------ | ----------- |
| SYNTAX_PROTO2 | 0 | Syntax `proto2`. |
| SYNTAX_PROTO3 | 1 | Syntax `proto3`. |


 

 

 



<a name="google/protobuf/wrappers.proto"></a>
<p align="right"><a href="#top">Top</a></p>

## google/protobuf/wrappers.proto



<a name="google.protobuf.BoolValue"></a>

### BoolValue
Wrapper message for `bool`.

The JSON representation for `BoolValue` is JSON `true` and `false`.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| value | [bool](#bool) |  | The bool value. |






<a name="google.protobuf.BytesValue"></a>

### BytesValue
Wrapper message for `bytes`.

The JSON representation for `BytesValue` is JSON string.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| value | [bytes](#bytes) |  | The bytes value. |






<a name="google.protobuf.DoubleValue"></a>

### DoubleValue
Wrapper message for `double`.

The JSON representation for `DoubleValue` is JSON number.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| value | [double](#double) |  | The double value. |






<a name="google.protobuf.FloatValue"></a>

### FloatValue
Wrapper message for `float`.

The JSON representation for `FloatValue` is JSON number.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| value | [float](#float) |  | The float value. |






<a name="google.protobuf.Int32Value"></a>

### Int32Value
Wrapper message for `int32`.

The JSON representation for `Int32Value` is JSON number.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| value | [int32](#int32) |  | The int32 value. |






<a name="google.protobuf.Int64Value"></a>

### Int64Value
Wrapper message for `int64`.

The JSON representation for `Int64Value` is JSON string.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| value | [int64](#int64) |  | The int64 value. |






<a name="google.protobuf.StringValue"></a>

### StringValue
Wrapper message for `string`.

The JSON representation for `StringValue` is JSON string.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| value | [string](#string) |  | The string value. |






<a name="google.protobuf.UInt32Value"></a>

### UInt32Value
Wrapper message for `uint32`.

The JSON representation for `UInt32Value` is JSON number.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| value | [uint32](#uint32) |  | The uint32 value. |






<a name="google.protobuf.UInt64Value"></a>

### UInt64Value
Wrapper message for `uint64`.

The JSON representation for `UInt64Value` is JSON string.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| value | [uint64](#uint64) |  | The uint64 value. |





 

 

 

 



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
| grpc_coroutines | [GrpcCoroutinesGenOptions](#krotoplus.compiler.GrpcCoroutinesGenOptions) | repeated | Configuration entries for the &#39;Grpc Coroutines&#39; code generator. |






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






<a name="krotoplus.compiler.GrpcCoroutinesGenOptions"></a>

### GrpcCoroutinesGenOptions
Configuration used by the &#39;gRPC Coroutines&#39; code generator.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| filter | [FileFilter](#krotoplus.compiler.FileFilter) |  | Filter used for limiting the input files that are processed by the code generator The default filter will match true against all input files. |






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

