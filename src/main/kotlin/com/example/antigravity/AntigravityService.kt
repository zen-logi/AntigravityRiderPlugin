package com.example.antigravity

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

@Service(Service.Level.APP)
class AntigravityService {
    companion object {
        private val LOG = Logger.getInstance(AntigravityService::class.java)

        // 2025年12月現在、gemini-2.0-flash は Free Tier 制限が厳しいため、実験的プロビジョニングの exp モデルを試行
        private const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent"

        val instance: AntigravityService
            get() = service<AntigravityService>()
    }

    private val httpClient = HttpClient.newHttpClient()

    suspend fun sendMessage(prompt: String): String =
        withContext(Dispatchers.IO) {
            val token = GoogleAuthService.instance.accessToken
            if (token.isNullOrBlank()) {
                return@withContext "ログインしていません。「Google でサインイン」ボタンを押してログインしてください。"
            }

            try {
                val requestBody =
                    """
                    {
                      "contents": [
                        {"role":"user","parts":[{"text":"$prompt"}]}
                      ]
                    }
                    """.trimIndent()

                val request =
                    HttpRequest.newBuilder()
                        .uri(URI.create(ENDPOINT))
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .header("Authorization", "Bearer $token")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                        .build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

                if (response.statusCode() == 200) {
                    return@withContext response.body()
                } else {
                    LOG.warn("Gemini API Error: ${response.statusCode()} - ${response.body()}")
                    return@withContext "エラー (${response.statusCode()}): ${response.body()}"
                }
            } catch (e: Exception) {
                LOG.error("Failed to call Gemini API", e)
                return@withContext "エラーが発生しました: ${e.message}"
            }
        }
}
