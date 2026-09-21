import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class RenderColdStartInterceptor : Interceptor {
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var futureTask: ScheduledFuture<*>? = null

        // 1. Schedule a task to trigger the banner if the server takes > 2 seconds
        try {
            futureTask = scheduler.schedule({
                ServerStateManager.setWakingUp(true)
            }, 2, TimeUnit.SECONDS)

            // 2. Proceed with the actual network call (Make sure timeout is 60s)
            val response = chain.proceed(request)

            return response
        } finally {
            // 3. The request finished! Cancel the timer and hide the banner
            futureTask?.cancel(true)
            ServerStateManager.setWakingUp(false)
        }
    }
}
