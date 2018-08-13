import com.github.marcoferrer.krotoplus.coroutines.GrpcContextContinuationInterceptor
import com.github.marcoferrer.krotoplus.coroutines.asContinuationInterceptor
import io.grpc.Context
import kotlinx.coroutines.experimental.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.ContinuationInterceptor
import kotlin.coroutines.experimental.coroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class GrpcContextContinuationInterceptorTest{

    data class Person(var name: String)

    @Test
    fun `Test Grpc Context Continuation Interceptor Attach`() = runBlocking {

        val KEY_PERSON = Context.key<Person>("person")

        val bill = Person("Bill")

        Context.current().withValue(KEY_PERSON,bill).attach()

        val job = Job()

        launch(parent = job) {
            delay(1,TimeUnit.MILLISECONDS)
            assertNull(KEY_PERSON.get())
        }

        val grpcContextInterceptor = Context.current().asContinuationInterceptor()

        launch(grpcContextInterceptor,parent = job) {
            delay(1,TimeUnit.MILLISECONDS)

            val expectedGrpcContext = (coroutineContext[ContinuationInterceptor] as GrpcContextContinuationInterceptor)
                            .grpcContext

            assertEquals(bill,KEY_PERSON.get())
            assertEquals(bill,KEY_PERSON.get(expectedGrpcContext))

            launch {
                assertNotEquals(expectedGrpcContext, Context.current())
            }.join()

            launch(coroutineContext) {
                assertEquals(expectedGrpcContext, Context.current())
            }
        }

        job.joinChildren()
    }
}