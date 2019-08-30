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

package com.github.marcoferrer.krotoplus.generators

import com.github.marcoferrer.krotoplus.config.InsertionPoint
import com.github.marcoferrer.krotoplus.config.InsertionsGenOptions
import com.github.marcoferrer.krotoplus.proto.*
import com.github.marcoferrer.krotoplus.script.ScriptManager
import com.github.marcoferrer.krotoplus.utils.addFile
import com.github.marcoferrer.krotoplus.utils.funcName
import com.github.marcoferrer.krotoplus.utils.key
import com.google.protobuf.compiler.PluginProtos

object InsertionsGenerator : Generator {

    override val isEnabled: Boolean
        get() = context.config.insertionsCount > 0

    override fun invoke(): PluginProtos.CodeGeneratorResponse {

        val responseBuilder = PluginProtos.CodeGeneratorResponse.newBuilder()

        for (options in context.config.insertionsList) {

            for (protoFile in context.schema.protoFiles)
                if (isFileToGenerate(protoFile.name,options.filter)) {
                    protoFile.buildInsertions(options, responseBuilder)
                }

            for (protoType in context.schema.protoTypes.values)
                if (isFileToGenerate(protoType.protoFile.name,options.filter)) {
                    protoType.buildInsertions(options, responseBuilder)
                }
        }
        return responseBuilder.build()
    }

    private fun ProtoFile.buildInsertions(
        options: InsertionsGenOptions,
        responseBuilder: PluginProtos.CodeGeneratorResponse.Builder
    ) {
        for (entry in options.entryList) {

            for (contentEntry in entry.contentList) contentEntry.takeIf { it.isNotEmpty() }
                ?.let { content ->
                    responseBuilder.addFile {
                        insertionPoint = InsertionPoint.OUTER_CLASS_SCOPE.key
                        this.content = content
                    }
                }

            for (templateScriptPathEntry in entry.scriptPathList)
                templateScriptPathEntry.takeIf { it.isNotEmpty() }?.let { scriptPath ->
                    val script = ScriptManager.getScript(scriptPath, entry.scriptBundle)
                    val content = script.invoke(InsertionPoint.OUTER_CLASS_SCOPE.funcName, this) as? String
                    content?.let {
                        responseBuilder.addFile {
                            name = this@buildInsertions.outputPath
                            insertionPoint = InsertionPoint.OUTER_CLASS_SCOPE.key
                            this.content = it
                        }
                    }
                }
        }
    }

    private fun ProtoType.buildInsertions(
        options: InsertionsGenOptions,
        responseBuilder: PluginProtos.CodeGeneratorResponse.Builder
    ) {
        for (entry in options.entryList) {
            if ((this is ProtoMessage && !this.isMapEntry && this.supportsInsertionPoint(entry.point)) ||
                this is ProtoEnum && this.supportsInsertionPoint(entry.point)
            ) {

                for (content in entry.contentList) {
                    buildFileForContentLiteral(entry.point, content)
                        ?.let { responseBuilder.addFile(it) }
                }

                for (templateScriptPath in entry.scriptPathList) {
                    buildFileForTemplateScript(entry.point, templateScriptPath, entry.scriptBundle)
                        ?.let { responseBuilder.addFile(it) }
                }
            }
        }
    }

    private fun ProtoType.buildFileForTemplateScript(
        insertionPoint: InsertionPoint,
        templateScriptPath: String,
        scriptBundle: String? = null
    ): PluginProtos.CodeGeneratorResponse.File? {

        val script = ScriptManager.getScript(templateScriptPath, scriptBundle)

        // We are explicitly casting our type since we have to invoke the script dynamically
        val content = when (this) {
            is ProtoMessage -> script.invoke(insertionPoint.funcName, this) as? String
            is ProtoEnum -> script.invoke(insertionPoint.funcName, this) as? String
        }

        return content?.let {
            buildFileForContent(insertionPoint, it)
        }
    }

    private fun ProtoType.buildFileForContentLiteral(insertionPoint: InsertionPoint, content: String) =
        buildFileForContent(insertionPoint, content.replace("{{message_type}}", canonicalJavaName))

    private fun ProtoType.buildFileForContent(insertionPoint: InsertionPoint, content: String) =
        PluginProtos.CodeGeneratorResponse.File.newBuilder()
            .apply {

                this.name = if (insertionPoint == InsertionPoint.INTERFACE_EXTENDS &&
                    this@buildFileForContent.protoFile.javaMultipleFiles
                )
                    outputFilePath.replace(Regex("\\.java$"), "OrBuilder.java") else
                    outputFilePath

                this.insertionPoint = "${insertionPoint.key}:$canonicalProtoName"
                this.content = content
            }
            .build()
}

