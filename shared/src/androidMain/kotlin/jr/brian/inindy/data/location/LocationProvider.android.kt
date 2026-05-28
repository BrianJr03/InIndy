package jr.brian.inindy.data.location

import jr.brian.inindy.domain.model.AddressResult

actual class LocationProvider actual constructor() {

    actual suspend fun getCurrentLocation(): AddressResult? =
        AddressResult("Current location (Indianapolis)", 39.7684, -86.1581)
}
