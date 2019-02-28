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

import com.github.marcoferrer.krotoplus.config.InsertionPoint

//TODO: Move all these props to embedded fields via script
val InsertionPoint.key: String
    inline get() = this.name.toLowerCase()

val InsertionPoint.funcName: String
    get() = insertionScriptFuncNames[this] ?: this.name

val insertionScriptFuncNames = mapOf(
    InsertionPoint.INTERFACE_EXTENDS to "interfaceExtends",
    InsertionPoint.MESSAGE_IMPLEMENTS to "messageImplements",
    InsertionPoint.BUILDER_IMPLEMENTS to "builderImplements",
    InsertionPoint.BUILDER_SCOPE to "builderScope",
    InsertionPoint.CLASS_SCOPE to "classScope",
    InsertionPoint.ENUM_SCOPE to "enumScope",
    InsertionPoint.OUTER_CLASS_SCOPE to "outerClassScope"
)