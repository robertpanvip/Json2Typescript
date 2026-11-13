package com.pan.json2typescript.util

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.Strictness
import com.google.gson.stream.JsonReader
import java.io.StringReader

object JsonParser {

    /**
     * 宽松模式解析：
     * 1. 预处理：去掉对象尾逗号（Gson 从不支持 ",}" 这种尾逗号）
     *    数组尾逗号 Gson 2.10 及以下会解析成 null 元素，2.11+ 也不支持，这里一并处理
     * 2. Strictness.LENIENT 开启 Gson 所有宽松特性：
     *    - 单引号 key / string
     *    - 未加引号的 field names
     *    - // 和 /* */ 注释
     *    - NaN / Infinity 等非标准数字
     */
    fun parse(json: String): JsonElement {
        val trimmed = json.trim()
        // 这个插件只处理后端 JSON 响应（对象/数组），
        // 顶层裸标量（"abc"、1.5、true）无法生成任何有意义的 TS 类型，直接拒。
        // 同时也挡住 Gson LENIENT 把 "not json at all" 当成未加引号字符串错误解析的坑。
        if (trimmed.isEmpty() || (trimmed.first() != '{' && trimmed.first() != '[')) {
            throw IllegalArgumentException(
                "Invalid JSON: expected top-level object or array"
            )
        }
        val cleaned = stripTrailingCommas(trimmed)
        return JsonReader(StringReader(cleaned)).use { reader ->
            reader.setStrictness(Strictness.LENIENT)
            JsonParser.parseReader(reader)
        }
    }

    /**
     * 去掉 JSON 里的尾逗号：字符串外的 ",}" 和 ",]" 里的那个逗号。
     * 状态机遍历，跳过字符串内容和转义字符，只处理 JSON 结构层面的逗号。
     */
    private fun stripTrailingCommas(json: String): String {
        val sb = StringBuilder(json.length)
        var i = 0
        var inString = false
        var escapeNext = false
        while (i < json.length) {
            val c = json[i]
            if (escapeNext) {
                sb.append(c)
                escapeNext = false
            } else if (inString) {
                sb.append(c)
                if (c == '\\') escapeNext = true
                else if (c == '"') inString = false
            } else {
                if (c == '"') {
                    inString = true
                    sb.append(c)
                } else if (c == ',' && i + 1 < json.length) {
                    // 跳过空白，看后面是不是 } 或 ]
                    var j = i + 1
                    while (j < json.length && json[j].isWhitespace()) j++
                    if (j < json.length && (json[j] == '}' || json[j] == ']')) {
                        // 跳过逗号，保留后面的空白，让 }/] 跟上来
                        sb.append(json, i + 1, j)
                        i = j
                        continue
                    }
                    sb.append(c)
                } else {
                    sb.append(c)
                }
            }
            i++
        }
        return sb.toString()
    }
}
