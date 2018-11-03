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