package jr.brian.inindy.data.remote.post

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Editable columns for an existing posts row. `user_id` and `neighborhood_id`
// stay untouched — those are set at create time.
//
// `group_id` is @EncodeDefault(ALWAYS) so setting it back to null (Neighborhood
// audience after editing away from a group) actually persists a `null` write
// rather than being dropped from the JSON body. maxAttendees needs the same
// treatment for the noLimit toggle to be able to clear an existing value.
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class UpdatePostDto(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String,
    @SerialName("location") val location: String,
    @SerialName("address") val address: String,
    @SerialName("starts_at") val startsAt: String,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("ends_at") val endsAt: String? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("max_attendees") val maxAttendees: Int? = null
)
