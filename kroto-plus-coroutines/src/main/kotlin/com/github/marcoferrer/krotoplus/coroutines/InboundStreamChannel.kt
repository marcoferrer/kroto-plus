package com.github.marcoferrer.krotoplus.coroutines

import io.grpc.stub.StreamObserver
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

class InboundStreamChannel<T>(
        capacity: Int = Channel.UNLIMITED,
        private val channel: Channel<T> = Channel(capacity)
) : StreamObserver<T>, ReceiveChannel<T> by channel {

    override fun onNext(value: T) {
        channel.offer(value)
    }

    override fun onError(t: Throwable?) {
        channel.close(t)
    }

    override fun onCompleted() {
        channel.close()
    }
}