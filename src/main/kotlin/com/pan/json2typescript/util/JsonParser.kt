package com.pan.json2typescript.util

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.Strictness
import com.google.gson.stream.JsonReader
import java.io.StringReader

object JsonParser {

    private val gson = GsonBuilder().create()

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
        val cleaned = stripTrailingCommas(json.trim())
        val element = JsonReader(StringReader(cleaned)).use { reader ->
            reader.setStrictness(Strictness.LENIENT)
            gson.fromJson(reader, JsonElement::class.java)
        }
        when {
            element == null ->
                throw IllegalArgumentException("Empty JSON")
            !element.isJsonObject && !element.isJsonArray ->
                // LENIENT 太宽：`not json at all` 会被当成未加引号字符串解析
                // 这个插件只处理顶层对象/数组（生成 TS interface 才有意义）
                throw IllegalArgumentException(
                    "Invalid JSON: top-level value must be object or array"
                )
        }
        return element
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
