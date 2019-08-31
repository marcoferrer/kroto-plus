## Version 0.5.0
_\*\*-\*\*_
* New: Update to Kotlin `1.3.50`
* New: Update to Kotlin Coroutines `1.3.0`

#### Protoc Plugin
* Fix: If no file filter is defined, fallback to `CodeGeneratorRequest.fileToGenerateList` [PR-70](https://github.com/marcoferrer/kroto-plus/pull/70) 


## Version 0.5.0-RC
_2019-08-22_
* New: Update to Kotlin `1.3.41`
* New: Update to Kotlin Coroutines `1.3.0-RC2`
* New: Update to gRPC `1.23.0`
* New: Update to protobuf `3.9.0`

#### Protoc Plugin
* Fix: Support malformed protobuf filenames [PR-63](https://github.com/marcoferrer/kroto-plus/pull/63) Thanks to @AlexeySoshin 
* Fix: Typo in config message name [GH-45](https://github.com/marcoferrer/kroto-plus/issues/57) Thanks to @RdeWilde

#### Coroutines
* Fix: Propagate inbound channel close as call cancellation

Thanks to @chris-blacker [PR-61](https://github.com/marcoferrer/kroto-plus/pull/61)
* Fix: Race condition in outbound flow control
* New: Improvements to the efficiency of outbound flow control handler 
* New: Integration tests for client and server coroutine implementations
* New: Improvements to determinism of unit tests


## Version 0.4.0
_2019-06-17_
* New: Update to kotlin `1.3.31`
* New: Update to kotlin Coroutines `1.2.1`
* New: Update to gRPC `1.20.1`

#### Proto Builders (DSL)
* Fix: Empty object generation when using maps and multiple files. [PR-51](https://github.com/marcoferrer/kroto-plus/pull/51) Thanks to @sauldhernandez

#### Coroutines
* New: Default server method execution to `CoroutineStart.ATOMIC`
* New: Introduce abstract stub ext for concatenating coroutine contexts, `AbstactStub.plusContext`.
* Fix: Don't propagate message for `UNKNOWN` exceptions in rpc exception mapper 
* Fix: Disable auto flow control for inbound client and server streams during bidi calls
* Fix: Reduce visibility of `FlowControlledInboundStreamObserver` to `internal`
* Deprecated: `AbstractStub.coroutineContext` ext in favor of `AbstractStub.context`     

#### Protoc Plugin
* New: Added support for Yaml as a configuration format [GH-45](https://github.com/marcoferrer/kroto-plus/issues/45)
* Fix: Address bug in parallelization of generator execution


## Version 0.3.0
_2019-04-02_
* Fix: Update codegen to support malformed rpc method names [GH-37](https://github.com/marcoferrer/kroto-plus/issues/37)([#38](https://github.com/marcoferrer/kroto-plus/pull/38))

#### Coroutines
* New: Updated docs for public apis
* New: Added `newGrpcStub` coroutine scope ext for creating new stubs. Generated client stubs no longer implement `CoroutineScope` ([#43](https://github.com/marcoferrer/kroto-plus/pull/43)) 
* New: Add flow control for outbound messages in both clients and servers ([#42](https://github.com/marcoferrer/kroto-plus/pull/42))
* New: Added example for multiple client streaming subscriptions
* Fix: Increased code coverage across all apis ([#41](https://github.com/marcoferrer/kroto-plus/pull/41))
* Fix: Unary observer has been converted to an internal api
* Removed: Legacy streaming apis have been removed

## Version 0.2.2-RC3
_2019-03-13_
* New: Update Kotlin Poet to ```1.0.1``` ([#30](https://github.com/marcoferrer/kroto-plus/pull/30))

#### Coroutines
* New: Propagate client scope cancellation to server using `ClientCall.cancel` ([#34](https://github.com/marcoferrer/kroto-plus/pull/34))
* New: Server rpc scope is now bound to cancellation notifications from the client ([#23](https://github.com/marcoferrer/kroto-plus/pull/23))
* Fix: Race condition between `StreamObserver.onNext` and `StreamObserver.onCompleted` when target channel is full 
* Fix: Reduce `@KrotoPlusInternalApi` experimental level to `Experimental.Level.ERROR` to prevent external usage  
* Fix: Remove redundant usages of `@ObsoleteCoroutinesApi` in call builders
* Fix: Remove unused experimental class `CompletableDeferredObserver`
* Fix: Annotate `SuspendingUnaryObserver` as an internal API 
* Fix: Remove unnecessary creation of `CoroutineScope` in `newSendChannelFromObserver`
* New: Introduce `ServiceScope` interface and remove `CoroutineScope` from generated service classes ([#35](https://github.com/marcoferrer/kroto-plus/pull/35))
* New: Use `Message.getDefaultInstance()` as default value of stub request parameters
* New: Increased code coverage across the board
* Deprecated: Legacy service stub rpc builders in favor of new back-pressure supporting stub APIs  

#### gRPC Stub Extension 
* New: Refactored code gen to support new coroutines APIs ([#31](https://github.com/marcoferrer/kroto-plus/pull/31))
* New: Generate no-arg extensions for all rpc methods with non streaming request parameters. Default request is now set to `Message.getDefaultInstance()`

#### Proto Builders (DSL)
* Fix: Resolve ```@DslMarker``` insertion regression introduced in `0.2.2-RC1` ([#32](https://github.com/marcoferrer/kroto-plus/pull/32))


## Version 0.2.2-RC2
_2019-02-17_

#### Coroutines
* Fix: Refine API Visibility
* Fix: Remove unused prototype response extensions
* Fix: Remove protobuf dependency from coroutine runtime, Resolves [GH-25](https://github.com/marcoferrer/kroto-plus/issues/25)
* Fix: Propagate client cancellation to server RPC scope
* Fix: Simplify client call stub extensions
* Fix: Remove obsolete annotations from generated stubs
* Fix: Default call option `CALL_OPTION_COROUTINE_CONTEXT` to `EmptyCoroutineContext`
* Fix: Convert `ClientBidiCallChannel` and `ClientStreamingCallChannel` to interfaces
* Fix: Remove unnecessary data modifier from client call channels
* Fix: Add `component1()` and `component2()` operators to client call channel interfaces
* Fix: Favor directly handling rpc exceptions over installing a completion handler.
* Fix: Improve rpc method exception handling and propagation.

## Version 0.2.2-RC1
_2019-01-03_
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
