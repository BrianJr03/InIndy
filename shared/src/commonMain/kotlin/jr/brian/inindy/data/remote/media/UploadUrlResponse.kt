package jr.brian.inindy.data.remote.media

import kotlinx.serialization.Serializable

@Serializable
data class UploadUrlResponse(
    val uploadUrl: String,
    val publicUrl: String,
    val key: String
)
