package com.pan.json2typescript.util

object TsKeyUtils {

    /** TypeScript / JavaScript 保留字与内置类型名：作为字段名时必须加引号 */
    private val reservedWords = setOf(
        // JS 保留字
        "break", "case", "catch", "class", "const", "continue", "debugger", "default",
        "delete", "do", "else", "enum", "export", "extends", "false", "finally", "for",
        "function", "if", "import", "in", "instanceof", "new", "null", "return", "super",
        "switch", "this", "throw", "true", "try", "typeof", "var", "void", "while", "with",
        // strict mode / 模块
        "implements", "interface", "let", "package", "private", "protected", "public",
        "static", "yield",
        // TS 关键词与内置类型
        "abstract", "any", "as", "asserts", "async", "await", "bigint", "boolean",
        "constructor", "declare", "get", "infer", "is", "keyof", "module", "namespace",
        "never", "number", "object", "out", "override", "readonly", "require", "set",
        "string", "symbol", "type", "undefined", "unique", "unknown", "global",
        "intrinsic", "satisfies", "using", "accessor"
    )

    fun toTsKey(key: String): String {
        // 保留字必须先于合法标识符判断（class / default 等本身是合法标识符）
        if (key in reservedWords) return "\"$key\""

        // 合法标识符 + 不是关键字
        if (key.matches(Regex("""^[a-zA-Z_$][\w$]*$"""))) {
            return key
        }

        // 否则一律加引号，并对内部的反斜杠和双引号进行转义
        // 这样 key 内本身含有 \ 或 " 时不会破坏外层引号
        val escaped = key
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        return "\"$escaped\""
    }
}
