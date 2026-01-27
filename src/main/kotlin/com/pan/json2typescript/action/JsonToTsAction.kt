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

        val raw = CopyPasteManager.getInstance()
            .getContents<String>(DataFlavor.stringFlavor)
            ?: return

        val json = raw.trim()

        if (!json.startsWith("{") && !json.startsWith("[")) {
            Messages.showErrorDialog(
                project,
                "剪贴板内容不是有效的 JSON",
                "JSON to TypeScript"
            )
            return
        }

        val ts = try {
            JsonToTsGenerator().generate("Root", json)
        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                e.message ?: "JSON 解析失败",
                "JSON to TypeScript"
            )
            return
        }

        WriteCommandAction.runWriteCommandAction(project) {
            editor.document.insertString(
                editor.caretModel.offset,
                ts
            )
        }
    }

}
