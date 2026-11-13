package com.example.data.remote

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.browser.customtabs.CustomTabsIntent
import com.example.data.model.WatchStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import java.security.SecureRandom

data class SimklPinChallenge(
    val userCode: String,
    val verificationUrl: String,
    val expiresInSeconds: Long
)

class TrackerAuthManager(
    private val client: OkHttpClient,
    private val aniListClient: AniListClient
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun generatePkceVerifier(): String {
        val bytes = ByteArray(48)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun generatePkceChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun launchAniListOAuth(context: Context, clientId: String, verifier: String) {
        val challenge = generatePkceChallenge(verifier)
        val uri = Uri.parse("https://anilist.co/api/v2/oauth/authorize").buildUpon()
            .appendQueryParameter("client_id", clientId.trim())
            .appendQueryParameter("redirect_uri", "maddookie://auth")
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
        CustomTabsIntent.Builder().build().launchUrl(context, uri)
    }

    fun launchMalOAuth(context: Context, clientId: String, verifier: String) {
        // MyAnimeList uses plain PKCE code_challenge == code_verifier
        val uri = Uri.parse("https://myanimelist.net/v1/oauth2/authorize").buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", clientId.trim())
            .appendQueryParameter("code_challenge", verifier)
            .appendQueryParameter("code_challenge_method", "plain")
            .appendQueryParameter("redirect_uri", "maddookie://mal-auth")
            .build()
        CustomTabsIntent.Builder().build().launchUrl(context, uri)
    }

    suspend fun verifyManualAniListToken(token: String): Result<String> {
        return aniListClient.fetchViewerUsername(token.trim())
    }

    suspend fun exchangeMalCode(
        clientId: String,
        code: String,
        verifier: String
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        runCatching {
            val form = FormBody.Builder()
                .add("client_id", clientId.trim())
                .add("code", code)
                .add("code_verifier", verifier)
                .add("grant_type", "authorization_code")
                .add("redirect_uri", "maddookie://mal-auth")
                .build()
            val req = Request.Builder()
                .url("https://myanimelist.net/v1/oauth2/token")
                .post(form)
                .build()
            val token = client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw IllegalStateException("MAL token HTTP ${resp.code}: ${body.take(120)}")
                }
                json.parseToJsonElement(body).jsonObject["access_token"]?.jsonPrimitive?.content
                    ?: throw IllegalStateException("MAL response missing access_token")
            }

            val profileReq = Request.Builder()
                .url("https://api.myanimelist.net/v2/users/@me")
                .header("Authorization", "Bearer $token")
                .build()
            val username = client.newCall(profileReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) "MAL User"
                else json.parseToJsonElement(body).jsonObject["name"]?.jsonPrimitive?.content ?: "MAL User"
            }
            username to token
        }
    }

    suspend fun syncMalProgress(
        accessToken: String,
        malId: Int,
        watchedEpisodes: Int,
        status: WatchStatus
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val form = FormBody.Builder()
                .add("status", status.malValue)
                .add("num_watched_episodes", watchedEpisodes.toString())
                .build()
            val req = Request.Builder()
                .url("https://api.myanimelist.net/v2/anime/$malId/my_list_status")
                .patch(form)
                .header("Authorization", "Bearer $accessToken")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IllegalStateException("MAL sync failed (${resp.code})")
                }
            }
        }
    }

    suspend fun requestSimklPin(clientId: String): Result<SimklPinChallenge> =
        withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder()
                    .url("https://api.simkl.com/oauth/pin?client_id=${clientId.trim()}")
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        throw IllegalStateException("Simkl PIN HTTP ${resp.code}: ${body.take(120)}")
                    }
                    val obj = json.parseToJsonElement(body).jsonObject
                    val code = obj["user_code"]?.jsonPrimitive?.content
                        ?: throw IllegalStateException("Missing Simkl user_code")
                    val url = obj["verification_url"]?.jsonPrimitive?.content ?: "https://simkl.com/pin"
                    val expiresIn = obj["expires_in"]?.jsonPrimitive?.longOrNull ?: 900L
                    SimklPinChallenge(code, url, expiresIn)
                }
            }
        }

    suspend fun pollSimklPin(clientId: String, userCode: String): Result<String?> =
        withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder()
                    .url("https://api.simkl.com/oauth/pin/$userCode?client_id=${clientId.trim()}")
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        throw IllegalStateException("Simkl poll HTTP ${resp.code}")
                    }
                    val obj = json.parseToJsonElement(body).jsonObject
                    val result = obj["result"]?.jsonPrimitive?.content
                    if (result == "OK") {
                        obj["access_token"]?.jsonPrimitive?.content
                    } else {
                        null
                    }
                }
            }
        }

    suspend fun syncSimklHistory(
        clientId: String,
        accessToken: String,
        malId: Int?,
        episode: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (malId == null) throw IllegalStateException("No MAL ID for Simkl scrobble")
            val payload = buildJsonObject {
                put(
                    "shows",
                    kotlinx.serialization.json.buildJsonArray {
                        add(
                            buildJsonObject {
                                put("ids", buildJsonObject { put("mal", malId) })
                                put(
                                    "episodes",
                                    kotlinx.serialization.json.buildJsonArray {
                                        add(buildJsonObject { put("number", episode) })
                                    }
                                )
                            }
                        )
                    }
                )
            }
            val req = Request.Builder()
                .url("https://api.simkl.com/sync/history")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .header("Authorization", "Bearer $accessToken")
                .header("simkl-api-key", clientId.trim())
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Simkl sync HTTP ${resp.code}")
                }
            }
        }
    }
}
