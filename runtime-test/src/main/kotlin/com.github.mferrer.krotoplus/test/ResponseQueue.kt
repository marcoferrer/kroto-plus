package com.github.mferrer.krotoplus.test

import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusRuntimeException
import java.util.*


sealed class QueueEntry<out T>
data class QueueMessage<out T> (val value: T) : QueueEntry<T>()
data class QueueError(val status: Status, val metadata: Metadata? = null) : QueueEntry<Nothing>(){

    fun asException() = StatusRuntimeException(status,metadata)
}

class ResponseQueue<E> : Deque<QueueEntry<E>> by ArrayDeque() {

    fun addMessage(message: E) = add(QueueMessage(message))
    fun pushMessage(message: E) = push(QueueMessage(message))

    @JvmOverloads
    fun addError(statusError: Status, metadata: Metadata? = null) =
            add(QueueError(statusError, metadata))

    @JvmOverloads
    fun pushError(statusError: Status, metadata: Metadata? = null) =
            push(QueueError(statusError, metadata))
}