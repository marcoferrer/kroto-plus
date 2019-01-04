## Version 0.2.2-RC1
_2018-01-03_
* New: Update to kotlin ```1.3.11```

#### Protoc Plugin
* New: gRPC Coroutines Client & Server Code Generation 
* New: Stand alone version of gRPC code gen. ```protoc-gen-grpc-coroutines```

#### Coroutines
* New: Benchmark implementation of gRPC coroutines
* New: Experimental global dispatcher ```Dispatchers.Grpc```
* New: ```SendChannel``` utility api ```CoroutineScope.launchProducerJob```
* Fix: Lowered byte code target for Android compatibility    
* Deprecated: ```InboundStreamChannel``` in favor of new stub APIs
* Deprecated: ```ServerBidiCallChannel``` in favor of new stub APIs

#### Proto Builders (DSL)
* Fix: Nested messages are now tagged with ```@DslMarker``` annotation

#### Community Scripts
* New: ```grpc-gateway``` entry point generation script

## Version 0.2.1
_2018-11-02_
* Fix: Address regression in file filter matching

## Version 0.2.0
_2018-11-02_
* New: Added doc generation for configuration api
* New: Updated to Kotlin ```1.3.0``` 
* New: Updated [grpc-java](https://github.com/grpc/grpc-java/releases/tag/v1.15.1) to ```1.15.1```
* Deprecated: Legacy CLI Compiler in favor of protoc compiler plugin
* Fix: Multiple excludes in file filter regex 

#### Coroutines
* New: Updated to stable Coroutines ```1.0```
* New: Added ```GrpcContextElement``` as a replacement to ```GrpcContextContinuationInterceptor``` 

#### Proto Builders (DSL)
* New: [GH-7](https://github.com/marcoferrer/kroto-plus/issues/7) Support kotlin ```@DslMarker``` annotation for proto builders

## Version 0.1.3
_2018-08-09_

* New: Updated Kotlin runtime to ```1.2.60```
* New: Updated Coroutine version to ```0.24.0```
* New: Updated Protobuf version to ```3.6.1```
* New: Updated gRPC version to ```1.14.0```

#### Protoc Plugin
* New: Converted Kroto+ to protoc plugin and implemented default generators

#### User Defined Code Generation Scripts
* New: Allow users to define scripts to be used for adding content to Protoc insertion points
* New: Allow users to define scripts for arbitrary code generation.
* New: Support precompiled script jars as well as dynamic script compilation. 

#### Proto Builders
* New: Added unwrap option to declare builder extensions as top level members.
* New: Builder generator now supports creating builders for nested message types     
* New: Builder generator now creates extensions for nested message field builders.
* New: Added plus operator extensions for messages 
* Fix: Builder generator now explicitly sets the result type as non null. 

#### Coroutines
* New: Added ```GrpcContextContinuationInterceptor``` making the grpc context available during suspending service calls in coroutines 

#### Mock Services
* Deprecated: The ```ServiceBindingServerRule``` has been deprecated due to a change in the latest version of ```io.grpc:grpc-testing``` 
* New: Helper methods generated for clearing the response queue start in between tests.
* New: Option added for creating and naming a collection of mock services for easier usage.
* Fix: Moved generated response queue builders into Mock service file. 

#### Extendable Messages (Experimental)   
* New: Generator added for creating extendable messages and inserting pseudo companion objects into java message classes.

#### Legacy Compiler
* Fix: Changed visibility of cli arguments to internal
* Fix: Removed usage of deprecated ```RpcBidiChannel``` in ```StubRpcOverloadGenerator```    

#### Gradle Plugin 
* Fix: Added missing dsl configuration builder for ```ProtoTypeBuildersGeneratorConfig```
* Fix: Configuration is no longer overridden when used in a multi project build   
* Fix: Gradle Kotlin DSL support added for generator configuration 

## Version 0.1.2
_2018-04-23_

* New: Updated Kotlin runtime to ```1.2.40```
* New: Publish artifacts to jcenter

#### Compiler
* Fix: Generated pom no longer includes dependencies that have been embedded 

## Version 0.1.1
_2018-04-10_

#### Coroutines
* New: Added ```data``` modifier to ```ClientBidiCallChannel``` to support destructuring declarations
* New: Added ```ServerBidiCallChannel<ReqT,RespT>``` for future server side coroutine support 
* Fix: Renamed ```RpcBidiChannel<ReqT,RespT>``` to ```ClientBidiCallChannel<ReqT,RespT>``` 

#### Gradle Plugin 
* New: Published to gradle plugin portal

#### Compiler
* New: [GH-1](https://github.com/marcoferrer/kroto-plus/issues/1) Added copy extension for proto types.

## Version 0.1.0
_2018-03-26_

 * Initial release.