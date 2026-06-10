package jr.brian.inindy.data.remote.media

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.HttpClient
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class MediaRemoteDataSourceImpl(
    private val supabase: SupabaseClient,
    private val httpClient: HttpClient
) : MediaRemoteDataSource {

    override suspend fun getUploadUrl(
        fileName: String,
        contentType: String,
        context: String
    ): Result<UploadUrlResponse> = runCatching {
        val response: HttpResponse = supabase.functions.invoke(
            function = "get-upload-url",
            body = buildJsonObject {
                put("fileName", fileName)
                put("contentType", contentType)
                put("context", context)
            }
        )
        json.decodeFromString<UploadUrlResponse>(response.bodyAsText())
    }

    override suspend fun uploadImage(
        uploadUrl: String,
        bytes: ByteArray,
        contentType: String
    ): Result<Unit> = runCatching {
        val response = httpClient.put(uploadUrl) {
            setBody(bytes)
            contentType(ContentType.parse(contentType))
        }
        val status = response.status
        if (status.value !in HttpStatusCode.OK.value..299) {
            error("R2 upload failed with status ${status.value}")
        }
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
