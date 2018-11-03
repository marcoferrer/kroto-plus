import com.github.marcoferrer.krotoplus.coroutines.GrpcContextContinuationInterceptor
import com.github.marcoferrer.krotoplus.coroutines.asContinuationInterceptor
import io.grpc.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.test.withTestContext
import java.util.concurrent.TimeUnit
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class GrpcContextContinuationInterceptorTest {

    data class Person(val name: String)

    @Test
    fun `Test Grpc Context Continuation Interceptor Attach`() = runBlocking<Unit> {

        val KEY_PERSON = Context.key<Person>("person")
        val bill = Person("Bill")
        val ctx = Context.current().withValue(KEY_PERSON, bill)
        ctx.attach()

        GlobalScope.launch {
            // Yield so that we can make sure we suspend at least once
            yield()
            assertNull(KEY_PERSON.get())
        }

        val grpcContextInterceptor = ctx.asContinuationInterceptor()

        launch(grpcContextInterceptor) {
            delay(1L)

            val expectedGrpcContext =
                (coroutineContext[ContinuationInterceptor] as GrpcContextContinuationInterceptor)
                    .grpcContext

            assertEquals(bill, KEY_PERSON.get())
            assertEquals(bill, KEY_PERSON.get(expectedGrpcContext))

            GlobalScope.launch {
                assertNotEquals(expectedGrpcContext, Context.current())
            }

            launch {
                assertEquals(expectedGrpcContext, Context.current())
            }
        }
    }
}