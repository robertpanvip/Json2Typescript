package com.pan.json2typescript.action

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.Messages
import com.pan.json2typescript.generator.JsonToTsGenerator
import java.awt.datatransfer.DataFlavor

class JsonToTsAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.EDT
    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = editor != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        // 优先：编辑器选中的文本（用户直接在 .json 文件或接口响应里选中要转的块）
        // 没有选中内容时降级到剪贴板（原行为）
        val raw = getSelection(editor)
            ?: getClipboard()
            ?: run {
                Messages.showErrorDialog(
                    project,
                    "Please select JSON text in the editor or copy JSON to clipboard.",
                    "JSON to TypeScript"
                )
                return
            }

        val json = raw.trim()

        if (!json.startsWith("{") && !json.startsWith("[")) {
            Messages.showErrorDialog(
                project,
                "The input is not valid JSON (must start with { or [).",
                "JSON to TypeScript"
            )
            return
        }

        val ts = try {
            JsonToTsGenerator().generate("Root", json)
        } catch (ex: Exception) {
            Messages.showErrorDialog(
                project,
                ex.message ?: "Failed to parse JSON.",
                "JSON to TypeScript"
            )
            return
        }

        WriteCommandAction.runWriteCommandAction(project) {
            val selModel = editor.selectionModel
            if (selModel.hasSelection()) {
                // 有选中内容 → 替换选中为生成的 TS 类型
                editor.document.replaceString(
                    selModel.selectionStart,
                    selModel.selectionEnd,
                    ts
                )
            } else {
                editor.document.insertString(editor.caretModel.offset, ts)
            }
        }
    }

    private fun getSelection(editor: com.intellij.openapi.editor.Editor): String? {
        val sel = editor.selectionModel.selectedText
        return sel?.let { if (it.isBlank()) null else it }
    }

    private fun getClipboard(): String? = CopyPasteManager.getInstance()
        .getContents<String>(DataFlavor.stringFlavor)
        ?.let { if (it.isBlank()) null else it }
}
