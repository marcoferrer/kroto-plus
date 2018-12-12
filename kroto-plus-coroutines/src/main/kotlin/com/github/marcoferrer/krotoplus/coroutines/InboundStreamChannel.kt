package com.github.marcoferrer.krotoplus.coroutines

import io.grpc.stub.*
import kotlinx.coroutines.channels.*

@Deprecated("Use ")
class InboundStreamChannel<T>(
        capacity: Int = Channel.UNLIMITED,
        val channel: Channel<T> = Channel(capacity)
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