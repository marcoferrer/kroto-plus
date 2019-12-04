/*
 *  Copyright 2019 Kroto+ Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.github.marcoferrer.krotoplus.proto.ProtoMessage
import com.github.marcoferrer.krotoplus.utils.memoize
import com.google.protobuf.DescriptorProtos

// language=java
fun builderScope(message: ProtoMessage): String? = buildString {
    val schema = message.protoFile.schema

    message.descriptorProto.fieldList.asSequence()
        .filter { it.type == DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE }
        .map {
            it to requireNotNull(schema.protoTypes[it.typeName] as? ProtoMessage) {
                "${it.typeName} was not found in schema type map."
            }
        }
        .filterNot { it.second.isMapEntry }
        .forEach { (fieldDescriptorProto, protoMessageForField) ->

            val fieldNameCamelCase = camelCaseFieldName(fieldDescriptorProto.name)

            val addStatement= if (fieldDescriptorProto.label == DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED)
                "add$fieldNameCamelCase(builder)" else "set$fieldNameCamelCase(builder)"

            append("""
                /**GRADLE DSL INSERTION**/
                
                public void ${fieldNameCamelCase.decapitalize()}(){
                    ${protoMessageForField.builderClassName.canonicalName} builder = ${protoMessageForField.className.canonicalName}.newBuilder();
                    $addStatement;
                }
                
                public void ${fieldNameCamelCase.decapitalize()}( org.gradle.api.Action<${protoMessageForField.builderClassName.canonicalName}> action){
                    ${protoMessageForField.builderClassName.canonicalName} builder = ${protoMessageForField.className.canonicalName}.newBuilder();
                    action.execute(builder);
                    $addStatement;
                }

                public void ${fieldNameCamelCase.decapitalize()}( groovy.lang.Closure<${protoMessageForField.builderClassName.canonicalName}> closure){
                    ${protoMessageForField.builderClassName.canonicalName} builder = ${protoMessageForField.className.canonicalName}.newBuilder();
                    org.gradle.util.ConfigureUtil.configure(closure,builder);
                    $addStatement;
                }
            """.trimIndent()
            )
            appendln()
        }
}




val camelCaseFieldName = { it: String ->
    // We cant use CaseFormat.UPPER_CAMEL since
    // protoc is lenient with malformed field names
    if (it.contains("_"))
        it.split("_").joinToString(separator = "") { it.capitalize() } else
        it.capitalize()

}.memoize()