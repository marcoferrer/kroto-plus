## Kotlin Coroutines gRPC Example


## Quick Start: 
Run the following command to get started with a preconfigured template project. **(_[kotlin-coroutines-gRPC-template](https://github.com/marcoferrer/kotlin-coroutines-gRPC-template)_)**
```bash
git clone https://github.com/marcoferrer/kotlin-coroutines-gRPC-template && \
cd kotlin-coroutines-gRPC-template && \
./gradlew run 
```
* **Getting Started**
  * **[Using Kroto+ Plugin](https://github.com/marcoferrer/kroto-plus/tree/master/example-grpc-client-server#using-kroto-plugin)**
  * **[Gradle: Stand Alone Plugin](https://github.com/marcoferrer/kroto-plus/tree/master/example-grpc-client-server#gradle)**
  * **[Maven: Stand Alone Plugin](https://github.com/marcoferrer/kroto-plus/tree/master/example-grpc-client-server#maven)**

### Using Kroto+ Plugin
_[Quick Start Template](https://github.com/marcoferrer/kotlin-coroutines-gRPC-template/tree/kroto-plus-template)_

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

#### Gradle 
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
```xml
<plugin>
    <groupId>org.xolstice.maven.plugins</groupId>
    <artifactId>protobuf-maven-plugin</artifactId>
    <version>0.6.1</version>
    <configuration>
        <protocArtifact>com.google.protobuf:protoc:3.6.1:exe:${os.detected.classifier}</protocArtifact>
    </configuration>
    <executions>
        <execution>
            <goals><goal>compile</goal></goals>
        </execution>
        <execution>
            <id>grpc-java</id>
            <goals><goal>compile-custom</goal></goals>
            <configuration>
                <pluginId>grpc-java</pluginId>
                <pluginArtifact>io.grpc:protoc-gen-grpc-java:1.17.1:exe:${os.detected.classifier}</pluginArtifact>
            </configuration>
        </execution>
       
        <execution>
            <id>grpc-coroutines</id>
            <goals>
                <goal>compile-custom</goal>
            </goals>
            <configuration>
                <pluginId>grpc-coroutines</pluginId>
                <pluginArtifact>com.github.marcoferrer.krotoplus:protoc-gen-grpc-coroutines:0.2.2-RC2:jar:jvm8</pluginArtifact>
            </configuration>
        </execution>
    </executions>
</plugin>

```
Add generated sources to Kotlin plugin
```xml
<plugin>
    <artifactId>kotlin-maven-plugin</artifactId>
    <groupId>org.jetbrains.kotlin</groupId>
    <version>${kotlin.version}</version>
    <executions>
        <execution>
            <id>compile</id>
            <goals>
                <goal>compile</goal>
            </goals>
            <configuration>
                <sourceDirs>
                    <sourceDir>${project.basedir}/target/generated-sources/protobuf/grpc-java</sourceDir>
                    <sourceDir>${project.basedir}/target/generated-sources/protobuf/grpc-coroutines</sourceDir>
                </sourceDirs>
            </configuration>
        </execution>
    </executions>
</plugin>
```
