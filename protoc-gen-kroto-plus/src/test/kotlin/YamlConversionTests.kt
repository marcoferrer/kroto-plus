/*
 * Copyright 2019 Kroto+ Contributors
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

package com.github.marcoferrer.krotoplus.utils

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test
import kotlin.test.assertEquals

class ConfigFileTests {

    val configYaml = """
protoBuilders:
- filter:
    excludePath:
    - google/*
  unwrapBuilders: true
  useDslMarkers: true
grpcStubExts:
- supportCoroutines: true
grpcCoroutines: []
extendableMessages:
- filter:
    excludePath:
    - google/*
mockServices:
- implementAsObject: true
  generateServiceList: true
  serviceListPackage: com.my.package
  serviceListName: MyMockServices
generatorScripts:
- scriptPath:
  - helloThere.kts
  scriptBundle: kp-scripts/build/libs/kp-scripts.jar
insertions:
- entry:
  - point: MESSAGE_IMPLEMENTS
    content:
    - com.my.Interface<{{message_type}}>
  - point: BUILDER_IMPLEMENTS
    content:
    - com.my.Interface<{{message_type}}>
  - point: CLASS_SCOPE
    scriptPath:
    - kp-scripts/src/main/kotlin/extendableMessages.kts
"""
    val configJson = """
{
    "protoBuilders": [
        {
            "filter": { "excludePath": ["google/*"] },
            "unwrapBuilders": true,
            "useDslMarkers": true
        }
    ],
    "grpcStubExts": [
        { "supportCoroutines": true }
    ],
    "grpcCoroutines": [],
    "extendableMessages": [
        { "filter": { "excludePath": ["google/*"] } }
    ],
    "mockServices": [
        {
            "implementAsObject": true,
            "generateServiceList": true,
            "serviceListPackage": "com.my.package",
            "serviceListName": "MyMockServices"
        }
    ],
    "generatorScripts": [
        {
            "scriptPath": ["helloThere.kts"],
            "scriptBundle": "kp-scripts/build/libs/kp-scripts.jar"
        }
    ],
    "insertions": [
        {
            "entry": [
                {
                    "point": "MESSAGE_IMPLEMENTS",
                    "content": ["com.my.Interface<{{message_type}}>"]
                },
                {
                    "point": "BUILDER_IMPLEMENTS",
                    "content": ["com.my.Interface<{{message_type}}>"]
                },
                {
                    "point": "CLASS_SCOPE",
                    "scriptPath": ["kp-scripts/src/main/kotlin/extendableMessages.kts"]
                }
            ]
        }
    ]
}
    """

    @Test
    fun `Convert yaml string to json string`(){

        val jsonMapper = ObjectMapper()

        // Ensures the json string has been formatted by the object mapper
        // before comparing to the yaml output
        val jsonStringFromJson = jsonMapper.writeValueAsString(jsonMapper.readTree(configJson))

        val jsonStringFromYaml = yamlToJson(configYaml)

        assertEquals(jsonStringFromYaml, jsonStringFromJson)
    }
}