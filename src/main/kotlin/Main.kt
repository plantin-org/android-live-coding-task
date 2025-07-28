import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlinx.coroutines.*

// ----- Interface to Implement -----
interface ResourceFetcher {
    suspend fun getResource(resourceId: String): Result<String>
}

// ----- TODO: Implement this correctly -----
class ConcurrentFetcher(private val api: FakeApi) : ResourceFetcher {
    override suspend fun getResource(resourceId: String): Result<String> {
        return api.fetch(resourceId) // TODO: Implement this correctly
    }
}

// ----- Test -----
fun main() = runBlocking {
    val resourceIds = List(20) { "res_$it" }
    val totalCalls = 100000

    val fetcher: ResourceFetcher = ConcurrentFetcher(FakeApi)

    println("🔍 Starting concurrent test with $totalCalls calls...")

    val results: List<Result<String>> = coroutineScope {
        List(totalCalls) {
                    async(Dispatchers.IO) {
                        val resourceId = resourceIds.random()
                        fetcher.getResource(resourceId)
                    }
                }
                .awaitAll()
    }

    val fetchedData = results.mapNotNull { it.getOrNull() }

    val totalApiCalls = FakeApi.getTotalCalls().values.sum()
    val failedApiCalls = FakeApi.getFailureCalls().values.sum()
    val successApiCalls = FakeApi.getSuccessCalls().values.sum()

    val totalUniqueResourcesRequested = FakeApi.getTotalCalls().keys.size

    println("🔢 Total API calls: $totalApiCalls")
    println("❌ Total API calls failed: $failedApiCalls")
    println("🔢 Total unique resources requested: $totalUniqueResourcesRequested")
    println("📦 Total fetched data items: ${fetchedData.size}\n")

    if (totalUniqueResourcesRequested == successApiCalls) {
        println("✅ SUCCESS")
    } else {
        println("❌ FAILURE: Made more than 1 request per unique resource")
    }
}

// ----- Fake API -----
object FakeApi {
    private val callCount = ConcurrentHashMap<String, Int>()
    private val successCount = ConcurrentHashMap<String, Int>()
    private val failureCount = ConcurrentHashMap<String, Int>()

    suspend fun fetch(resourceId: String): Result<String> {
        delay(100L + Random.nextLong(1000)) // Simulate latency
        callCount.compute(resourceId) { _, count -> (count ?: 0) + 1 }

        if (Random.nextDouble() < 0.6) {
            failureCount.compute(resourceId) { _, count -> (count ?: 0) + 1 }
            return Result.failure(Exception("Random API failure"))
        }

        successCount.compute(resourceId) { _, count -> (count ?: 0) + 1 }
        return Result.success("DataFor:$resourceId - ${Random.nextInt(1000)}")
    }

    fun getTotalCalls(): Map<String, Int> = callCount
    fun getSuccessCalls(): Map<String, Int> = successCount
    fun getFailureCalls(): Map<String, Int> = failureCount
}
