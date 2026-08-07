package com.pan.json2typescript.util

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.math.BigInteger

object JsonParser {

    /**
     * 宽松模式的 mapper：允许单引号、未加引号的 key、尾逗号、数字前导零等。
     * JSON5 中 Jackson 原生不支持的部分（十六进制、Infinity/NaN、行继续等）
     * 由 [normalizeJson5] 在解析前预处理成标准 JSON。
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
        val normalized = normalizeJson5(json.trim())
        return mapper.readTree(normalized)
    }

    /** 未加引号的 key（可含连字符）：a: / foo_bar: / my-key: */
    private val unquotedKey = Regex("""([a-zA-Z_][\w-]*)(\s*:)""")

    /**
     * JSON5 数字 / 关键字 token（仅在结构区域匹配，字符串与注释内不会触发）。
     * 顺序很重要：hex 必须在 num 之前，lead 必须在 num 之前。
     */
    private val token = Regex(
        """(?ix)
          (?<inf>-?Infinity\b)
        | (?<nan>NaN\b)
        | (?<undef>undefined\b)
        | (?<hex>0[xX][0-9a-fA-F]+)
        | (?<plus>\+[0-9]+(?:\.[0-9]+)?(?:[eE][+-]?[0-9]+)?)
        | (?<lead>\.[0-9]+(?:[eE][+-]?[0-9]+)?)
        | (?<num>[0-9]+(?:\.[0-9]*)?(?:[eE][+-]?[0-9]+)?)
        """
    )

    private val wordChar = Regex("""[A-Za-z0-9_]""")

    /**
     * 把 JSON5 输入翻译为标准 JSON：
     * - 去除 //、/* */、# 注释
     * - 为未加引号的 key 加引号（含带连字符的 key）
     * - 字符串内的行继续（反斜杠 + 行终止符）就地删除
     * - 把十六进制、Infinity/NaN/undefined、前导/尾随小数点、显式正号
     *   转换成标准 JSON 可表示的等价写法
     */
    private fun normalizeJson5(input: String): String {
        val sb = StringBuilder(input.length + 16)
        var i = 0
        val n = input.length
        var state = 0 // 0=normal, 1=string-double, 2=string-single

        while (i < n) {
            val c = input[i]

            // ---- 字符串内 ----
            if (state == 1 || state == 2) {
                if (c == '\\') {
                    // 行继续：反斜杠 + 行终止符 -> 两者都丢弃
                    if (i + 1 < n && isLineTerminator(input[i + 1])) {
                        i += 1 + lineTermLen(input, i + 1)
                        continue
                    }
                    // 普通转义：保留反斜杠与下一个字符
                    sb.append(c)
                    if (i + 1 < n) {
                        sb.append(input[i + 1])
                        i += 2
                    } else {
                        i++
                    }
                    continue
                }
                if (c == '"' && state == 1) { state = 0; sb.append(c); i++; continue }
                if (c == '\'' && state == 2) { state = 0; sb.append(c); i++; continue }
                sb.append(c); i++; continue
            }

            // ---- 注释（仅 normal） ----
            if (c == '/' && i + 1 < n && input[i + 1] == '/') {
                i = skipLineComment(input, i + 2); continue
            }
            if (c == '/' && i + 1 < n && input[i + 1] == '*') {
                i = skipBlockComment(input, i + 2); continue
            }
            if (c == '#') {
                i = skipLineComment(input, i + 1); continue
            }

            // ---- 字符串起始 ----
            if (c == '"') { state = 1; sb.append(c); i++; continue }
            if (c == '\'') { state = 2; sb.append(c); i++; continue }

            // ---- 结构区域：未加引号 key ----
            val k = unquotedKey.matchAt(input, i)
            if (k != null && k.range.first == i) {
                sb.append('"').append(k.groupValues[1]).append('"').append(k.groupValues[2])
                i = k.range.last + 1
                continue
            }

            // ---- 结构区域：JSON5 数字 / 关键字 ----
            val t = token.find(input, i)
            if (t != null && t.range.first == i) {
                val end = t.range.last + 1
                // 数字/关键字后若紧跟单词字符，说明是标识符片段，不转换
                if (end < n && wordChar.matches(input[end].toString())) {
                    sb.append(c); i++; continue
                }
                sb.append(transformToken(t)); i = end; continue
            }

            sb.append(c); i++
        }
        return sb.toString()
    }

    private fun transformToken(m: MatchResult): String {
        val g = m.groups
        return when {
            g["inf"] != null -> if (m.value.startsWith("-")) "-1e9999" else "1e9999"
            g["nan"] != null -> "0"
            g["undef"] != null -> "null"
            g["hex"] != null -> BigInteger(m.value.substring(2), 16).toString()
            g["plus"] != null -> m.value.substring(1)
            g["lead"] != null -> "0" + m.value
            g["num"] != null -> {
                val v = m.value
                if (v.endsWith('.')) v + "0" else v
            }
            else -> m.value
        }
    }

    private fun isLineTerminator(c: Char): Boolean =
        c == '\n' || c == '\r' || c == '\u2028' || c == '\u2029'

    private fun lineTermLen(input: String, idx: Int): Int {
        if (input[idx] == '\r') {
            return if (idx + 1 < input.length && input[idx + 1] == '\n') 2 else 1
        }
        return 1
    }

    private fun skipLineComment(input: String, start: Int): Int {
        var j = start
        while (j < input.length && !isLineTerminator(input[j])) j++
        return j // 行终止符保留（作为空白），不写入
    }

    private fun skipBlockComment(input: String, start: Int): Int {
        var j = start
        while (j < input.length) {
            if (input[j] == '*' && j + 1 < input.length && input[j + 1] == '/') return j + 2
            j++
        }
        return j
    }
}
