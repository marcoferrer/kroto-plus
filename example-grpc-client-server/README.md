## Kotlin Coroutines gRPC Example


## Quick Start: 
Run the following command to get started with a preconfigured template project. (_[kotlin-coroutines-gRPC-template](https://github.com/marcoferrer/kotlin-coroutines-gRPC-template)_)
```bash
git clone https://github.com/marcoferrer/kotlin-coroutines-gRPC-template && \
cd kotlin-coroutines-gRPC-template && \
./gradlew run 
```

### Getting Started: Kroto+ Plugin
_[Template](https://github.com/marcoferrer/kotlin-coroutines-gRPC-template/kroto-plus-template)_
Add the following configuration to your existing Kroto configuration file.

#### Asciipb (Proto Plain Text)
```asciipb
grpc_coroutines {}
```
#### JSON
```json
{
    "grpcCoroutines": []
}
```


### Getting Started: Stand Alone Plugin
If you are not using any additional features from Kroto-Plus, then configuration can be simplified and use the stand alone version of the ```grpc-coroutines``` protoc plugin.

#### Gradle Protobuf 
_[Gradle Template](https://github.com/marcoferrer/kotlin-coroutines-gRPC-template)_
```groovy
dependencies {
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version"
    implementation "com.github.marcoferrer.krotoplus:kroto-plus-coroutines:$krotoplus_version"
}

protobuf {
    protoc { 
        artifact = "com.google.protobuf:protoc:$protobuf_version"
    }
    
    plugins {
        grpc { artifact = "io.grpc:protoc-gen-grpc-java:$grpc_version" }
        coroutines {
            artifact = "com.github.marcoferrer.krotoplus:protoc-gen-grpc-coroutines:$krotoplus_version:jvm8@jar"
        }
    }

    generateProtoTasks {
        all().each { task ->
            task.plugins {
                grpc {}
                coroutines {}
            }
        }
    }
}
```
#### Maven
_[Maven Template](https://github.com/marcoferrer/kotlin-coroutines-gRPC-template/maven)_
```xml
TODO
```