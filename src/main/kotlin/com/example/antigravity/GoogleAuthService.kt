package com.example.antigravity

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.Messages
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

@Service(Service.Level.APP)
class GoogleAuthService {
    companion object {
        private val LOG = Logger.getInstance(GoogleAuthService::class.java)
        val instance: GoogleAuthService
            get() = service<GoogleAuthService>()
    }

    private val httpClient = HttpClient.newHttpClient()

    // AuthState からアクセストークンを取得
    var accessToken: String?
        get() = AuthState.instance.accessToken.ifBlank { null }
        set(value) {
            AuthState.instance.accessToken = value ?: ""
        }

    /**
     * Google OAuth 認可ページを開き、ユーザーに認可コードの入力を求める
     */
    fun login() {
        val clientId = AuthConfig.clientId
        if (clientId.isBlank()) {
            LOG.warn("Google Client ID が設定されていません。")
            Messages.showErrorDialog("Client ID が設定されていません。gradle.properties を確認してください。", "エラー")
            return
        }

        try {
            // 必要なスコープ: cloud-platform と generative-language.retriever
            val scopes =
                "https://www.googleapis.com/auth/cloud-platform " +
                    "https://www.googleapis.com/auth/generative-language.retriever"
            val encodedScopes = URLEncoder.encode(scopes, StandardCharsets.UTF_8.toString())
            val authUrl =
                "https://accounts.google.com/o/oauth2/v2/auth?" +
                    "client_id=$clientId&" +
                    "redirect_uri=urn:ietf:wg:oauth:2.0:oob&" +
                    "response_type=code&" +
                    "scope=$encodedScopes"

            BrowserUtil.browse(authUrl)

            // ユーザーに認可コードを入力してもらう
            val code =
                Messages.showInputDialog(
                    "ブラウザで表示された認証コードを入力してください:",
                    "Google 認証",
                    Messages.getQuestionIcon(),
                )

            if (!code.isNullOrBlank()) {
                // 認可コードをアクセストークンに交換する (同期的に呼び出すが必要ならここもCoroutine化検討)
                // UIスレッドからの呼び出しなので、簡易的にここで処理するが、本来は非同期が望ましい
                // 今回はレガシー移行のステップ1として、内部ロジックをHttpClientにする
                exchangeCodeForToken(code, clientId, AuthConfig.clientSecret)
            }
        } catch (e: Exception) {
            LOG.warn("Login failed", e)
            Messages.showErrorDialog("ログイン中にエラーが発生しました: ${e.message}", "エラー")
        }
    }

    private fun exchangeCodeForToken(
        code: String,
        clientId: String,
        clientSecret: String,
    ) {
        try {
            val params =
                mapOf(
                    "client_id" to clientId,
                    "client_secret" to clientSecret,
                    "code" to code,
                    "grant_type" to "authorization_code",
                    "redirect_uri" to "urn:ietf:wg:oauth:2.0:oob",
                )

            val formBody =
                params.map { (k, v) ->
                    "${URLEncoder.encode(k, StandardCharsets.UTF_8)}=${URLEncoder.encode(v, StandardCharsets.UTF_8)}"
                }.joinToString("&")

            val request =
                HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build()

            // 簡易的に同期実行 (UIスレッドブロックの警告が出る可能性があるが、まずは動作優先)
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() == 200) {
                val responseBody = response.body()
                // 簡易的な JSON パース (正規表現で access_token を抽出)
                val match = "\"access_token\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(responseBody)
                if (match != null) {
                    accessToken = match.groupValues[1]
                    LOG.info("Access token retrieved successfully.")
                    Messages.showInfoMessage("ログインに成功しました！", "成功")
                } else {
                    LOG.warn("Failed to parse access token from response: $responseBody")
                }
            } else {
                LOG.warn("Token exchange failed: ${response.statusCode()} - ${response.body()}")
                Messages.showErrorDialog("トークンの取得に失敗しました。\n${response.body()}", "エラー")
            }
        } catch (e: Exception) {
            LOG.warn("Error exchanging code for token", e)
            Messages.showErrorDialog("トークン交換中にエラーが発生しました: ${e.message}", "エラー")
        }
    }
}
