package jr.brian.inindy.data.location

import jr.brian.inindy.domain.model.AddressResult

expect class LocationProvider() {
    suspend fun getCurrentLocation(): AddressResult?
}
