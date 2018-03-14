package com.github.mferrer.krotoplus.generators

import com.squareup.wire.schema.Schema
import kotlinx.coroutines.experimental.Job

interface SchemaConsumer{

    val schema: Schema
    fun consume(): Job
}
