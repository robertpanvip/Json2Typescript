package com.pan.json2typescript.util

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

object JsonParser {

    /**
     * 宽松模式的 mapper：允许单引号、未加引号的 key、尾逗号、注释、数字前导零等，
     * 覆盖大多数「从控制台 / 后端文档复制来的不标准 JSON」。
     */
    private val mapper = ObjectMapper(
        JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)
            .build()
    )

    fun parse(json: String): JsonNode {
        val trimmed = json.trim()

        // 宽松特性已开启，大多数写法可直接解析
        try {
            return mapper.readTree(trimmed)
        } catch (_: Exception) {
            // 失败再走规范化：把 {my-key: 1} 变成 {"my-key": 1}
        }

        val normalized = normalizeJson(trimmed)
        return mapper.readTree(normalized)
    }

    /**
     * 把 {a:1, foo_bar:2, my-key:3} 变成 {"a":1,"foo_bar":2,"my-key":3}。
     * 主要处理带连字符的 key（宽松模式本身不支持）。
     * 字符串感知：不会误伤字符串值里形如 ", key:" 的内容。
     */
    private val unquotedKey = Regex("""([a-zA-Z_][\w-]*)(\s*:)""")

    private fun normalizeJson(input: String): String {
        val sb = StringBuilder(input.length + 16)
        var inString = false
        var i = 0

        while (i < input.length) {
            val c = input[i]

            // 字符串内的转义字符，直接跳过下一个字符
            if (c == '\\') {
                sb.append(c)
                if (i + 1 < input.length) sb.append(input[i + 1])
                i += 2
                continue
            }

            if (c == '"') {
                inString = !inString
                sb.append(c)
                i++
                continue
            }

            if (!inString) {
                val m = unquotedKey.find(input, i)
                if (m != null && m.range.first == i) {
                    sb.append('"').append(m.groupValues[1]).append('"').append(m.groupValues[2])
                    i = m.range.last + 1
                    continue
                }
            }

            sb.append(c)
            i++
        }
        return sb.toString()
    }
}
