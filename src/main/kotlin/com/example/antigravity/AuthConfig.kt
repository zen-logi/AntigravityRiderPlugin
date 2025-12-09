package com.example.antigravity

import com.intellij.openapi.diagnostic.Logger
import java.io.FileInputStream
import java.util.Properties

object AuthConfig {
    // プロジェクトルートからの相対パスで gradle.properties を読み込む
    private const val RELATIVE_PATH = "gradle.properties"

    private val LOG = Logger.getInstance(AuthConfig::class.java)
    private val props = Properties()

    init {
        try {
            FileInputStream(RELATIVE_PATH).use { props.load(it) }
        } catch (e: Exception) {
            LOG.warn("Failed to load $RELATIVE_PATH", e)
        }
    }

    // 環境変数とプロパティの値をログに出力しつつ取得
    val clientId: String =
        System.getenv("GOOGLE_CLIENT_ID")
            .also { LOG.info("Env GOOGLE_CLIENT_ID = $it") }
            ?: props.getProperty("google.client.id", "")
                .also { LOG.info("Property google.client.id = $it") }

    val clientSecret: String =
        System.getenv("GOOGLE_CLIENT_SECRET")
            .also { LOG.info("Env GOOGLE_CLIENT_SECRET = $it") }
            ?: props.getProperty("google.client.secret", "")
                .also { LOG.info("Property google.client.secret = $it") }
}
