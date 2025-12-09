package com.example.antigravity

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

class GenerateCodeAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val editor: Editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val selected = editor.selectionModel.selectedText
        val prompt =
            if (selected.isNullOrBlank()) {
                Messages.showInputDialog(project, "生成したいコードの指示を入力してください", "Antigravity コード生成", null)
            } else {
                "以下のコードを改善してください:\n$selected"
            } ?: return

        // 非同期で API 呼び出し（簡易的に同期実装）
        val result = AntigravityService.sendMessage(prompt)

        WriteCommandAction.runWriteCommandAction(project) {
            val caret = editor.caretModel
            editor.document.insertString(caret.offset, "\n// Antigravity 生成コード\n$result\n")
        }
    }
}
