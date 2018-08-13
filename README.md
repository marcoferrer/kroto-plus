# Kroto+
## Protoc plugin for bringing together Kotlin, Protobuf, Coroutines, and gRPC  
[![Build Status](https://travis-ci.org/marcoferrer/kroto-plus.svg?branch=master)](https://travis-ci.org/marcoferrer/kroto-plus)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![Download](https://api.bintray.com/packages/marcoferrer/kroto-plus/kroto-plus-compiler/images/download.svg) ](https://bintray.com/marcoferrer/kroto-plus/kroto-plus-compiler/_latestVersion)

## Lots of changes, improvements and features coming in *v0.1.3* ❗❗❗ 

* Code compiler has been refactored into a protoc plugin.

You can try out ```0.1.3-SNAPSHOT``` by adding the following repo to your build file.

```
  repositories {
      maven { url 'https://oss.jfrog.org/artifactory/oss-snapshot-local' }
  }
```
**Proper documentation to come shortly**. Until then you can reference the [Example Project](https://github.com/marcoferrer/kroto-plus/tree/master/example-project) or feel free to open an issue with any questions or suggestions.



## Version 0.1.3

* **[Getting Started With Gradle](https://github.com/marcoferrer/kroto-plus#getting-started-with-gradle)**
* **[Configuring Generators](https://github.com/marcoferrer/kroto-plus#configuring-generators)**
* Generators
  * **[gRPC Stub Extensions](https://github.com/marcoferrer/kroto-plus#grpc-stub-extensions)**
    * **[Rpc Method Coroutine Support](https://github.com/marcoferrer/kroto-plus#coroutine-support)**
  * **[Mock Service Generator](https://github.com/marcoferrer/kroto-plus#mock-service-generator)**
  * **[Proto Builder Generator](https://github.com/marcoferrer/kroto-plus#proto-builder-generator)**
  * **[Extendable Messages Generator](https://github.com/marcoferrer/kroto-plus#extendable-messages-generator-experimental)**
  * **[User Defined Generator Scripts](https://github.com/marcoferrer/kroto-plus#user-defined-generator-scripts)**
    * **[Insertion Scripts](https://github.com/marcoferrer/kroto-plus#insertion-scripts)**
    * **[Generator Scripts](https://github.com/marcoferrer/kroto-plus#generator-scripts)**

## Code Generators

* There are several built in code generators that each accept unique configuration options.

### gRPC Stub Extensions
#### [Configuration Options](https://github.com/marcoferrer/kroto-plus/blob/master/protoc-gen-kroto-plus/src/main/proto/krotoplus/compiler/config.proto#L61)

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

For unary rpc methods, the generator will create the following extensions
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
In addition to request message arguments as builder lambda rpc overloads, suspending overloads for rpc calls can also be generated.
This allows blocking style rpc calls without the use of the blocking stub, preventing any negative impact on coroutine performance. 
* This is accomplished by defining extension functions for async service stubs and combining a response observer with a coroutine builder.
* This option requires the artifact ```kroto-plus-coroutines``` as a dependency. This artifact is small and only consists of the bridging support for response observer to coroutine.
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
#### [Configuration Options](https://github.com/marcoferrer/kroto-plus/blob/master/protoc-gen-kroto-plus/src/main/proto/krotoplus/compiler/config.proto#L196)

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
            addAttacks {
                name = "ORA ORA ORA"
                damage = 100
                range = StandProto.Attack.Range.CLOSE
            }
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
### Proto Builder Generator
#### [Configuration Options](https://github.com/marcoferrer/kroto-plus/blob/master/protoc-gen-kroto-plus/src/main/proto/krotoplus/compiler/config.proto#L74)
This generator creates lambda based builders for message types
```kotlin
val attack = Attack {
                name = "ORA ORA ORA"
                damage = 100
                range = StandProto.Attack.Range.CLOSE
            }

//Copy extensions are also generated
val newAttack = attack.copy { damage = 200 }            

//As well as plus operator extensions 
val mergedAttack = attack + Attack { name = "Sunlight Yellow Overdrive" }
            
```

### Extendable Messages Generator (Experimental)
#### [Configuration Options](https://github.com/marcoferrer/kroto-plus/blob/master/protoc-gen-kroto-plus/src/main/proto/krotoplus/compiler/config.proto#L115)
Generated code relies on the ```kroto-plus-message``` artifact. This generator adds tagging interfaces to the java classes produce by protoc.
It also adds pseudo companion objects to provide a way to access proto message APIs in a non static manner.
The following is a small example of how to write generic methods and extensions that resolve both message and builders type.
  
```kotlin
inline fun <reified M, B> M.copy( block: B.() -> Unit ): M
        where M : KpMessage<M, B>, B : KpBuilder<M> {
    return this.toBuilder.apply(block).build()
}

// Usage
myMessage.copy { ... }

inline fun <reified M, B> build( block: B.() -> Unit ): M
        where M : KpMessage<M, B>, B : KpBuilder<M> {

    return KpCompanion.Registry[M::class.java].build(block)
}

// Usage
build<MyMessage> { ... }

inline fun <M, B> KpCompanion<M, B>.build( block: B.() -> Unit ): M
        where B : KpBuilder<M>,M : KpMessage<M,B> {

    return newBuilder().apply(block).build()
}

// Usage
MyMessage.Companion.build { ... }

```

### User Defined Generator Scripts
Users can define kotlin scripts that they would like to run during code generation.
For type completion, scripts can be couple with a small gradle build script, although this is completely optional.
Samples are available in the [kp-script](https://github.com/marcoferrer/kroto-plus/tree/master/example-project/kp-scripts) directory of the example project.

There are two categories of scripts available. 
* ##### **[Insertion Scripts](https://github.com/marcoferrer/kroto-plus/blob/master/example-project/kp-scripts/src/main/kotlin/sampleInsertionScript.kts)**
  * [Configuration Options](https://github.com/marcoferrer/kroto-plus/blob/master/protoc-gen-kroto-plus/src/main/proto/krotoplus/compiler/config.proto#L145)
  * Using the insertion api from the java protoc plugin, users can add code at specific points in generated java classes.
  * This is useful for adding code to allow more idiomatic use of generated java classes from Kotlin.
  * The entire ```ExtendableMessages``` generator can be implemented using an insertion script, an example can be in the example script [extendableMessages.kts](https://github.com/marcoferrer/kroto-plus/blob/master/example-project/kp-scripts/src/main/kotlin/extendableMessages.kts).   
  * Additional information regarding the insertion api can be found in the [official docs](https://developers.google.com/protocol-buffers/docs/reference/java-generated#plugins)
* ##### **[Generator Scripts](https://github.com/marcoferrer/kroto-plus/blob/master/example-project/kp-scripts/src/main/kotlin/helloThere.kts)**
  * [Configuration Options](https://github.com/marcoferrer/kroto-plus/blob/master/protoc-gen-kroto-plus/src/main/proto/krotoplus/compiler/config.proto#L89)
  * These scripts implement the ```Generator``` interface used by all internal kroto+ code generators.
  * Generators rely on the ```GeneratorContext```, which is available via the property ```context```. 
  * The ```context``` is used for iterating over files, messages, and services submitted by protoc.
  * Example usage can be found in the [kp-script](https://github.com/marcoferrer/kroto-plus/blob/master/example-project/kp-scripts/src/main/kotlin/helloThere.kts) directory of the example project, as well as inside the ```generators``` [package](https://github.com/marcoferrer/kroto-plus/tree/master/protoc-gen-kroto-plus/src/main/kotlin/com/github/marcoferrer/krotoplus/generators) of the ```protoc-gen-kroto-plus``` artifact.

#### Community Scripts
Community contributions for scripts are welcomed and more information regarding guidelines will be published soon.  
  
## Getting Started With Gradle

#### SNAPSHOT Usage
Add the following repository to your build
```groovy
repositories {
    maven { url 'https://oss.jfrog.org/artifactory/oss-snapshot-local' }
}
```

##### Configuring Protobuf Gradle Plugin
```groovy
plugins{
    id 'com.google.protobuf' version '0.8.6'
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:$protobufVersion"}

    plugins {
        kroto {
            artifact = "com.github.marcoferrer.krotoplus:protoc-gen-kroto-plus:$krotoPlusVersion:jvm8@jar"
        }
    }

    generateProtoTasks {
        def krotoConfig = file("krotoPlusConfig.json")

        all().each{ task ->
            // Adding the config file to the task inputs enabled UP-TO-DATE checks
            // to include changes to configuration
            task.inputs.files krotoConfig

            task.plugins {
                kroto {
                    outputSubDir = "java"
                    option "ConfigPath=$krotoConfig"
                }
            }
        }
    }
}
```

## Configuring Generators
#### All available generator options are documented in [config.proto](https://github.com/marcoferrer/kroto-plus/blob/master/protoc-gen-kroto-plus/src/main/proto/krotoplus/compiler/config.proto)
* Supported formats include [json](https://github.com/marcoferrer/kroto-plus/blob/master/example-project/krotoPlusConfig.json) and [asciipb](https://github.com/marcoferrer/kroto-plus/blob/master/example-project/krotoPlusConfig.asciipb) (proto plain text) formats.

#### Credit
This project relies on [Kotlin Poet](https://github.com/square/kotlinpoet) for building Kotlin sources. A big thanks to all it contributors. 
