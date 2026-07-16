package jr.brian.inindy.data.remote.push

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceTokenInsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("token") val token: String,
    @SerialName("platform") val platform: String,
)
