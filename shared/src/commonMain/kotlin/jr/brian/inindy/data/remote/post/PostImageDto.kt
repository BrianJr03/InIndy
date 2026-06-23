package jr.brian.inindy.data.remote.post

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PostImageDto(
    @SerialName("id") val id: String? = null,
    @SerialName("post_id") val postId: String? = null,
    @SerialName("storage_url") val storageUrl: String,
    // @EncodeDefault.ALWAYS — Postgrest's JSON config omits properties whose value
    // matches the default, so sortOrder = 0 would be dropped from the batch row.
    // In a multi-row insert Postgrest takes the union of keys across rows; the row
    // missing sort_order then inserts NULL and the NOT NULL constraint fires.
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("sort_order") val sortOrder: Int = 0
)
