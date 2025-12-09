package com.example.antigravity

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import javax.swing.JTextArea

class AntigravityToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow,
    ) {
        // AuthConfig を初期化してログを出力させる
        val clientIdCheck = AuthConfig.clientId

        // CoroutineScope for UI (Main Dispatcher)
        val uiScope = CoroutineScope(Dispatchers.Swing)

        val chatArea =
            JTextArea().apply {
                isEditable = false
                lineWrap = true
                wrapStyleWord = true
            }

        val inputField =
            JTextArea(3, 20).apply {
                lineWrap = true
                wrapStyleWord = true
            }

        // UI DSL v2
        val contentPanel =
            panel {
                row {
                    label("ステータス:")
                    val statusLabel = label("未確認").component

                    // 初期表示時にステータスを確認
                    val token = GoogleAuthService.instance.accessToken
                    statusLabel.text = if (token != null) "ログイン済み" else "未ログイン"

                    button("Google でサインイン") {
                        GoogleAuthService.instance.login()
                        val newToken = GoogleAuthService.instance.accessToken
                        statusLabel.text = if (newToken != null) "ログイン済み" else "未ログイン"
                    }
                }

                row {
                    scrollCell(chatArea)
                        .align(Align.FILL)
                        .resizableColumn()
                }.resizableRow()

                row {
                    scrollCell(inputField)
                        .align(Align.FILL)
                        .resizableColumn()
                }

                row {
                    button("送信") {
                        sendMessage(uiScope, chatArea, inputField)
                    }
                }
            }

        // Enter キーで送信するリスナーを追加
        inputField.addKeyListener(
            object : java.awt.event.KeyAdapter() {
                override fun keyPressed(e: java.awt.event.KeyEvent) {
                    if (e.keyCode == java.awt.event.KeyEvent.VK_ENTER) {
                        if (e.isShiftDown) {
                            // Shift + Enter で改行
                        } else {
                            e.consume() // 改行の入力を防ぐ
                            sendMessage(uiScope, chatArea, inputField)
                        }
                    }
                }
            },
        )

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(contentPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun sendMessage(
        scope: CoroutineScope,
        chatArea: JTextArea,
        inputField: JTextArea,
    ) {
        val message = inputField.text.trim()
        if (message.isEmpty()) return

        chatArea.append("User: $message\n")
        inputField.text = ""

        // 非同期処理 (Coroutines)
        scope.launch {
            try {
                // Suspend関数呼び出し (バックグラウンド処理は Service 内で記述)
                val response = AntigravityService.instance.sendMessage(message)

                // UI スレッドに戻ってくる
                chatArea.append("Antigravity: $response\n\n")
            } catch (e: Exception) {
                chatArea.append("Error: ${e.message}\n\n")
            }
        }
    }
}
