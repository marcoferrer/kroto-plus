## Version 0.1.3
_2018-07-08_

* New: Updated Kotlin runtime to ```1.2.51```
* New: Updated Coroutine version to ```0.23.4```

#### Compiler
* Fix: Changed visibility of cli arguments to internal    

#### Coroutines
* New: Added ```GrpcContextContinuationInterceptor``` making the grpc context available during suspending service calls in coroutines 

#### Gradle Plugin 
* Fix: Added missing dsl configuration builder for ```ProtoTypeBuildersGeneratorConfig```
* New: Gradle Kotlin DSL support added for generator configuration 

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