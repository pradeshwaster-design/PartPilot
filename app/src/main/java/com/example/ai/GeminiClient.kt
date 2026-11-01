package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiClient {

    private const val TAG = "GeminiClient"
    private const val MODEL_NAME = "gemini-2.5-flash"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun hasValidApiKey(): Boolean {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            key.isNotBlank() && key != "MY_GEMINI_API_KEY" && !key.startsWith("placeholder", ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun generateGroundedResponse(prompt: String, inventoryContext: String): String? = withContext(Dispatchers.IO) {
        if (!hasValidApiKey()) return@withContext null

        val apiKey = getApiKey()
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"

        val systemInstruction = """
            You are PartPilot AI, an expert robotics & electronics inventory companion.
            You must be strictly grounded in the user's current workshop inventory:
            $inventoryContext
            
            Guidelines:
            - If asked where something is or whether they own it, state the exact quantity, free stock, condition, and location code (e.g. A-1-B1).
            - If they don't have something, say so clearly and suggest the closest owned alternative from inventory.
            - Answer hardware questions (pinouts, voltages, compatibilities, what projects can be built) thoroughly and concisely.
            - Do not invent parts that aren't in the inventory.
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "$systemInstruction\n\nUser Question: $prompt"))
                    })
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API error: ${response.code} ${response.message}")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val respJson = JSONObject(body)
            val candidates = respJson.optJSONArray("candidates") ?: return@withContext null
            if (candidates.length() == 0) return@withContext null

            val content = candidates.getJSONObject(0).optJSONObject("content") ?: return@withContext null
            val parts = content.optJSONArray("parts") ?: return@withContext null
            if (parts.length() == 0) return@withContext null

            return@withContext parts.getJSONObject(0).optString("text")
        } catch (e: Exception) {
            Log.e(TAG, "Gemini request failed", e)
            return@withContext null
        }
    }

    suspend fun analyzeWorkbenchImage(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        if (!hasValidApiKey()) return@withContext null

        val apiKey = getApiKey()
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"

        // Resize bitmap to max 1200px to maintain fast upload
        val maxDim = 1200
        val width = bitmap.width
        val height = bitmap.height
        val scaledBitmap = if (width > maxDim || height > maxDim) {
            val ratio = width.toFloat() / height.toFloat()
            val newW = if (ratio > 1) maxDim else (maxDim * ratio).toInt()
            val newH = if (ratio > 1) (maxDim / ratio).toInt() else maxDim
            Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

        val prompt = """
            Examine this photo of an electronics component or workbench table.
            Identify all visible electronic parts, ICs, sensors, development boards, motors, resistors, modules or tools.
            Output ONLY a JSON array with objects matching:
            [
              {
                "name": "Component name (e.g. ESP32 DevKit, SG90 Servo, 220 Ohm Resistor)",
                "group": "One of: Microcontroller, Sensor, Motor Driver, Power & Battery, Display, Comm Module, Passive, Semiconductor, Connector & Wire, Tool & Consumable, Mechanical, Other",
                "specs": "Brief specs/voltage/package (e.g. 5V, 2cm-400cm, DIP-8)",
                "quantity": 1,
                "confidence": 0.95
              }
            ]
            If unable to identify a part clearly, set confidence lower than 0.70. Return ONLY valid JSON, no markdown formatting.
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini vision failed: ${response.code} ${response.message}")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val respJson = JSONObject(body)
            val candidates = respJson.optJSONArray("candidates") ?: return@withContext null
            if (candidates.length() == 0) return@withContext null

            val content = candidates.getJSONObject(0).optJSONObject("content") ?: return@withContext null
            val parts = content.optJSONArray("parts") ?: return@withContext null
            if (parts.length() == 0) return@withContext null

            var text = parts.getJSONObject(0).optString("text").trim()
            // Strip code fences if present
            if (text.startsWith("```json")) text = text.removePrefix("```json")
            if (text.startsWith("```")) text = text.removePrefix("```")
            if (text.endsWith("```")) text = text.removeSuffix("```")
            return@withContext text.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Gemini vision error", e)
            return@withContext null
        }
    }
}
