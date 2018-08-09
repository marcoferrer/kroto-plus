# Kroto+
## Code generator for bringing together Kotlin, Protobuf, Coroutines, and gRPC  
[![Build Status](https://travis-ci.org/marcoferrer/kroto-plus.svg?branch=master)](https://travis-ci.org/marcoferrer/kroto-plus)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![Download](https://api.bintray.com/packages/marcoferrer/kroto-plus/kroto-plus-compiler/images/download.svg) ](https://bintray.com/marcoferrer/kroto-plus/kroto-plus-compiler/_latestVersion)

## Lots of changes, improvements and features coming in *v0.1.3* ❗❗❗ 

To name a few:

* Code compiler has been refactored into a protoc plugin.
* **[User defined scripts](https://github.com/marcoferrer/kroto-plus/tree/master/example-project/templates/src/main/kotlin)**. Custom generators using kotlin scripts (```kts```)
* More advanced configuration options available, supporting both [json](https://github.com/marcoferrer/kroto-plus/blob/master/example-project/krotoPlusConfig.json) and [asciipb](https://github.com/marcoferrer/kroto-plus/blob/master/example-project/krotoPlusConfig.asciipb) (proto plain text) formats. These are backed by [config.proto](https://github.com/marcoferrer/kroto-plus/blob/master/kroto-plus-protoc/src/main/proto/krotoplus/compiler/config.proto) (Still requires proper documenting)
* Improvements to the ```MockService``` api
* New options, bug fixes, and improvements to generated ```ProtoBuilders```
* Experimental ```ExtendableMessages``` generator for adding extendable interfaces into java messages.   

**More info and documentation to come in the next few days.**

## Version 0.1.2

* **[Getting Started With Gradle](https://github.com/marcoferrer/kroto-plus#getting-started-with-gradle)**
* **[Stub Rpc Method Overloads](https://github.com/marcoferrer/kroto-plus#stub-rpc-method-overloads)**
* **[Rpc Method Coroutine Support](https://github.com/marcoferrer/kroto-plus#coroutine-support)**
* **[Mock Service Generator](https://github.com/marcoferrer/kroto-plus#mock-service-generator)**
* **[Message Builder Lambda Generator](https://github.com/marcoferrer/kroto-plus#message-builder-lambda-generator)**
* **[User Defined External Generators](https://github.com/marcoferrer/kroto-plus#user-defined-external-generators)**

## Code Generators

* There are several built in code generators that each accept unique configuration options.
* There is also preliminary support for registering custom external code generators. The api for doing so will be documented in the near future and accompanied by an example project.  

### Stub Rpc Method Overloads

This modules generates extension methods that overload the request message argument for rpc methods with a builder lambda block.

```kotlin
//Original Java-Style builders
val response = serviceStub.myRpcMethod(ExampleServiceGrpc.MyRpcMethodRequest
                                   .newBuilder()
                                   .setId(100)
                                   .setName("some name")
                                   .build())
                                   
//Kroto+ Overloaded
val response = serviceStub.myRpcMethod{
                         id = 100
                         name = "some name"
                    }
```
For rpc methods with a request type of ```com.google.protobuf.Empty``` then a no args overload is supplied.
```kotlin
//Original 
val response = serviceStub.myRpcMethod(Empty.getDefaultInstance())
                                   
//Kroto+ Overloaded
val response = serviceStub.myRpcMethod()
```


For unary rpc methods, the stub overload generator will create the following extensions
```kotlin
//If request type is Empty
inline fun ExampleServiceStub.myRpcMethod(): ExampleServiceGrpc.MyRpcMethodResponse =
    myRpcMethod(Empty.getDefaultInstace())

//Otherwise

//Future Stub
inline fun ExampleServiceFutureStub.myRpcMethod(block: ExampleServiceGrpc.MyRpcMethodRequest.Builder.() -> Unit): ListenableFuture<ExampleServiceGrpc.MyRpcMethodResponse> {
    val request = ExampleServiceGrpc.MyRpcMethodRequest.newBuilder().apply(block).build()
    return myRpcMethod(request)
}
    
//BlockingStub
inline fun ExampleServiceBlockingStub.myRpcMethod(block: ExampleServiceGrpc.MyRpcMethodRequest.Builder.() -> Unit): ExampleServiceGrpc.MyRpcMethodResponse {
    val request = ExampleServiceGrpc.MyRpcMethodRequest.newBuilder().apply(block).build()
    return myRpcMethod(request)
}

``` 

### Coroutine Support
In addition to request message arguments as builder lambda rpc overloads, this module can also generate suspending overloads for rpc calls.
This allows blocking style rpc calls without the use of the blocking stub, preventing any negative impact on coroutine performance. 
* This is accomplished by defining extension functions for async service stubs and combining a response observer with a coroutine builder.
* This option requires the artifact ```kroto-plus-coroutines``` as a dependency. This artifact is small and only consists of the bridging support for response observer to coroutine.
* If your code relies on thread local objects, such as those stored in ```io.grpc.Context``` then extra care needs to be taken to ensure these objects will be reattached via a ```ContinuationInterceptor```. The ```kroto-plus-coroutines``` artifact will provide support for this in the next release.   
```kotlin
//Async Stub
suspend fun ExampleServiceStub.myRpcMethod(request: ExampleServiceGrpc.MyRpcMethodRequest): ExampleServiceGrpc.MyRpcMethodResponse =
    suspendingUnaryCallObserver{ observer -> myRpcMethod(request,observer) }
    
suspend inline fun ExampleServiceStub.myRpcMethod(block: ExampleServiceGrpc.MyRpcMethodRequest.Builder.() -> Unit): ExampleServiceGrpc.MyRpcMethodResponse {
    val request = ExampleServiceGrpc.MyRpcMethodRequest.newBuilder().apply(block).build()
    return myRpcMethod(request)
}
```
* If using rpc interceptors or other code that relies on ```io.grpc.Context``` then you need to be sure to add a ```GrpcContextContinuationInterceptor``` to your ```CoroutineContext``` when launching a coroutine.
Child coroutines will inherit this ```ContinuationInterceptor``` if ```coroutineContext``` is passed down from the parent coroutine.   
```kotlin
Context.current().withValue(MY_KEY, myValue).attach()

val myGrpcContext = Context.current()

val job = launch( myGrpcContext.asContinuationInterceptor() ) {
    
    launch{
        assertNotEquals(myGrpcContext, Context.current())
    }
    
    launch(coroutineContext) {
        assertEquals(myGrpcContext, Context.current())
    }
    
}
```

There are also overloads generated for bridging Client, Server, and Bidirectional streaming methods with coroutine ```Channels``` 
The included example project contains full samples. [TestRpcCoroutineSupport](https://github.com/marcoferrer/kroto-plus/blob/master/example-project/src/test/kotlin/krotoplus/example/TestRpcCoroutineSupport.kt)
```kotlin
suspend fun findStrongestAttack(): StandProto.Attack {
    
    val standService = StandServiceGrpc.newStub(managedChannel)
    val characterService = CharacterServiceGrpc.newStub(managedChannel)
    
    val deferredStands = characterService.getAllCharactersStream() //Service call returns a ReceiveChannel<Character>
            .map { character ->
                //Suspending unary call. Using the generated overloads we can rely on
                //coroutines for deferred calls instead of listenable futures 
                async { standService.getStandByCharacter(character) }
            }
            .toList()
            
    val strongestAttack = deferredStands
            .flatMap { it.await().attacksList }
            .maxBy { it.damage }
            
    return strongestAttack ?: StandProto.Attack.getDefaultInstance()
}
```
Bidirectional Rpc Channel Example
```kotlin
@Test fun `Test Bidirectional Rpc Channel`() = runBlocking {
    
    val stub = StandServiceGrpc.newStub(grpcServerRule.channel)
    
    //Bidi method overload returns a channel that accepts our request type (A Character) and
    //returns our response type (A Stand)
    val rpcChannel = stub.getStandsForCharacters()
    
    //Our dummy service is sending three responses for each request it receives
     
    rpcChannel.send(characters["Dio Brando"]!!)
    stands["The World"].toString().let {
        assertEquals(it,rpcChannel.receive().toString())
        assertEquals(it,rpcChannel.receive().toString())
        assertEquals(it,rpcChannel.receive().toString())
    }
    
    rpcChannel.send(characters["Jotaro Kujo"]!!)
    stands["Star Platinum"].toString().let {
        assertEquals(it,rpcChannel.receive().toString())
        assertEquals(it,rpcChannel.receive().toString())
        assertEquals(it,rpcChannel.receive().toString())
    }
    
    //Closing the channel has the same behavior as calling onComplete on the request stream observer.
    //Calling close(throwable) behaves the same as onError(throwable)
    rpcChannel.close()
    
    //Assert that we consumed the expected number of responses from the stream
    assertNull(rpcChannel.receiveOrNull(),"Response quantity was greater than expected")
}

```   
   
### Mock Service Generator

This generator creates mock implementations of proto service definitions. This is useful for orchestrating a set of expected responses, aiding in unit testing methods that rely on rpc calls.
[Full example for mocking services in unit tests](https://github.com/marcoferrer/kroto-plus/blob/master/example-project/src/test/kotlin/krotoplus/example/TestMockServiceResponseQueue.kt). The code generated relies on the ``` kroto-plus-test ``` artifact as a dependency. It is a small library that provides utility methods used by the mock services. 
* If no responses are added to the response queue then the mock service will return the default instance of the response type.
* Currently only unary methods are being mocked, with support for other method types on the way 
 ```kotlin
@Test fun `Test Unary Response Queue`(){
     
     MockStandService.getStandByNameResponseQueue.apply {
        //Queue up a valid response message
        addMessage {
            name = "Star Platinum"
            powerLevel = 500
            speed = 550
            addAttacks(StandProtoBuilders.Attack {
                name = "ORA ORA ORA"
                damage = 100
                range = StandProto.Attack.Range.CLOSE
            })
        }   
           
        //Queue up an error
        addError(Status.INVALID_ARGUMENT)
    }
    
    val standStub = StandServiceGrpc.newBlockingStub(grpcServerRule.channel)
    
    standStub.getStandByName { name = "Star Platinum" }.let{ response ->
        assertEquals("Star Platinum",response.name)
        assertEquals(500,response.powerLevel)
        assertEquals(550,response.speed)
        response.attacksList.first().let{ attack ->
            assertEquals("ORA ORA ORA",attack.name)
            assertEquals(100,attack.damage)
            assertEquals(StandProto.Attack.Range.CLOSE,attack.range)
        }
    }
    
    try{
        standStub.getStandByName { name = "The World" }
        fail("Exception was expected with status code: ${Status.INVALID_ARGUMENT.code}")
    }catch (e: StatusRuntimeException){
        assertEquals(Status.INVALID_ARGUMENT.code, e.status.code)
    }
}
```
### Message Builder Lambda Generator
This generator creates lambda based builders for message types
```kotlin
val attack = StandProtoBuilders.Attack {
                name = "ORA ORA ORA"
                damage = 100
                range = StandProto.Attack.Range.CLOSE
            }

//Copy extensions are also generated
val newAttack = attack.copy { damage = 200 }            
            
```

### User Defined External Generators
This feature is currently in development. Api documentation and sample project are in the works. 

## Getting Started With Gradle

##### Using Plugin DSL
```groovy
plugins{
    id 'com.github.marcoferrer.kroto-plus' version '0.1.2'
}
```
##### Using buildscript block (Legacy)
```groovy
buildscript{
    ext.krotoplusVersion = '0.1.2'
    
    repositories {
        jcenter()
    }
    
    dependencies{
        classpath "com.github.marcoferrer.krotoplus:kroto-plus-gradle-plugin:${krotoplusVersion}"
    }
}

apply plugin: 'com.github.marcoferrer.kroto-plus'
```
##### Configuring Kroto+ Codegen
```groovy
def generatedOutputDir = "$buildDir/generated-sources/main/kotlin"

sourceSets {
    main {
        kotlin{
            srcDirs += generatedOutputDir
        }
    }
}

clean.doFirst{
    delete generatedOutputDir
}
       
krotoPlus{
    //Proto definition source directories, or path to a jar containing proto definitions
    sources = [
        "$projectDir/src/main/proto",
        "$buildDir/extracted-include-protos/main"
    ]
    
    //The default file output directory for all generators
    defaultOutputDir = file(generatedOutputDir)
    
    //Number of concurrent file writers (Default 3)
    //More does not equal better here. Too many writers can lead to a decrease in performance
    //and adjustments should be based on the overall quantity of proto files being processed.
    fileWriterCount = 4
    
    //Block used for enabling individual code generators and configuring their settings
    generators{
        
        stubOverloads{
    
            //[Optional] Output directory specific to the files created by this generator
            outputDir = file(generatedOutputDir)
            
            //[Optional (Default: false)] Generate coroutine extensions for service stub rpc methods
            supportCoroutines = true
        }
        
        mockServices{
            
            //[Optional] Output directory specific to the files created by this generator
            //Normally this should point to a test sources directory
            outputDir = file(generatedOutputDir)
        }
        
        //Enabling a generator with no configurable or sufficient default options
        protoTypeBuilders
        
        /*
            Enabling a custom external code generator
            This feature is incubating and will be fully enabled in the near future
            and include proper documentation
            
            external('com.some.package.MyCustomGenerator'){
                args = ['-foo','bar','-flag']
            }
        */
    }
}
```

## Road Map
* Document API for defining custom code generators 
* Increase test coverage. 
* Implement UP-TO-DATE checks in the gradle plugin
* Add Android compatibility to project
* Update gradle plugin to support Java 1.7 runtime


This project was made possible by the great work being done by the devs and contributors at [Square](https://github.com/square) and 
relies heavily on their open source projects [Kotlin Poet](https://github.com/square/kotlinpoet) and [Wire](https://github.com/square/wire)
