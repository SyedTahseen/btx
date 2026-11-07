package com.betterx.app.data

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/** [json] holds object responses; [array] holds array responses (Firestore runQuery). */
internal class HttpResp(val status: Int, val json: JsonObject, val array: JsonArray = JsonArray(emptyList())) {
    val ok: Boolean get() = status in 200..299
}

/** Tiny HTTP helper shared by the Firebase Auth and Firestore calls. Throws IOException when offline. */
internal object Http {
    suspend fun request(
        method: String,
        url: String,
        body: String? = null,
        contentType: String = "application/json",
        bearer: String? = null,
    ): HttpResp = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 20_000
            if (bearer != null) setRequestProperty("Authorization", "Bearer $bearer")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", contentType)
            }
        }
        try {
            if (body != null) conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            var array = JsonArray(emptyList())
            val json = try {
                if (text.isBlank()) {
                    JsonObject(emptyMap())
                } else {
                    val el = Json.parseToJsonElement(text)
                    if (el is JsonArray) {
                        array = el
                        JsonObject(emptyMap())
                    } else {
                        el.jsonObject
                    }
                }
            } catch (e: Exception) {
                JsonObject(emptyMap())
            }
            HttpResp(status, json, array)
        } finally {
            conn.disconnect()
        }
    }
}
