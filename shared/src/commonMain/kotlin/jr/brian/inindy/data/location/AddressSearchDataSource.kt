package jr.brian.inindy.data.location

import jr.brian.inindy.domain.model.AddressResult
import kotlinx.coroutines.delay

interface AddressSearchDataSource {
    suspend fun search(query: String): Result<List<AddressResult>>
}

class FakeAddressSearchDataSource : AddressSearchDataSource {

    override suspend fun search(query: String): Result<List<AddressResult>> {
        delay(200L)
        if (query.isBlank()) return Result.success(emptyList())
        val matches = INDIANAPOLIS_PLACES.filter {
            it.address.contains(query, ignoreCase = true)
        }
        return Result.success(matches.ifEmpty { INDIANAPOLIS_PLACES.take(3) })
    }

    private companion object {
        val INDIANAPOLIS_PLACES = listOf(
            AddressResult("Monument Circle, Indianapolis, IN", 39.7684, -86.1581),
            AddressResult("Eagle Creek Park, Indianapolis, IN", 39.8283, -86.2779),
            AddressResult("Broad Ripple Park, Indianapolis, IN", 39.8714, -86.1442),
            AddressResult("Garfield Park, Indianapolis, IN", 39.7365, -86.1425),
            AddressResult("Holliday Park, Indianapolis, IN", 39.8676, -86.1820),
            AddressResult("White River State Park, Indianapolis, IN", 39.7684, -86.1735),
            AddressResult("Mass Ave & College Ave, Indianapolis, IN", 39.7806, -86.1505),
            AddressResult("Fountain Square, Indianapolis, IN", 39.7522, -86.1424)
        )
    }
}
