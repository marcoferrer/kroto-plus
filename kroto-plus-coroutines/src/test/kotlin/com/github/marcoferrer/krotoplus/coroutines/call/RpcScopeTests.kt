package com.github.marcoferrer.krotoplus.coroutines.call

import com.github.marcoferrer.krotoplus.coroutines.GrpcContextElement
import io.grpc.Context
import io.grpc.MethodDescriptor
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class RpcScopeTests {

    val mockMethodName = "mock.full.method.name"
    val methodDescriptor = mockk<MethodDescriptor<*, *>>().apply {
        every { fullMethodName } returns mockMethodName
    }

    @Test
    fun `Rpc scope is named after rpc method`(){
        val newScope = newRpcScope(EmptyCoroutineContext,methodDescriptor)
        val coroutineName = newScope.coroutineContext[CoroutineName]
        assertNotNull(coroutineName)
        assertEquals(mockMethodName, coroutineName.name)
    }

    @Test
    fun `Rpc scope has a Job associated is none present`(){
        val newScope = newRpcScope(EmptyCoroutineContext,methodDescriptor)
        val job = newScope.coroutineContext[Job]
        assertNotNull(job)
    }

    @Test
    fun `Rpc scope inherits Job from coroutine context`(){
        val expectedJob = Job()
        val unexpectedJob = Job()
        val newScope = newRpcScope(EmptyCoroutineContext + expectedJob,methodDescriptor)
        val rpcScopeJob = newScope.coroutineContext[Job]
        assertNotNull(rpcScopeJob)
        assertEquals(expectedJob,rpcScopeJob)
        assertNotEquals(unexpectedJob,rpcScopeJob)
    }

    @Test
    fun `Rpc scope has current grpc context present as ThreadLocalElement`(){
        val expectedGrpcContext = Context.current()
        val newScope = newRpcScope(EmptyCoroutineContext,methodDescriptor)

        val testKey = Context.key<String>("test_key")

        Context.current()
            .withValue(testKey,"test_value")
            .attach()

        val rpcScopeGrpcContext = newScope.coroutineContext[GrpcContextElement]?.context

        assertNotNull(rpcScopeGrpcContext)
        assertEquals(expectedGrpcContext, rpcScopeGrpcContext)
        assertNotEquals(Context.current(), rpcScopeGrpcContext)
    }
}