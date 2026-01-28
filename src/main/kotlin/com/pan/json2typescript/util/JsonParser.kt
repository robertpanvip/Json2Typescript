package com.pan.json2typescript.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

object JsonParser {
    private val mapper = ObjectMapper()

    fun parse(json: String): JsonNode {
        val trimmed = json.trim()

        // 先尝试严格 JSON
        try {
            return mapper.readTree(trimmed)
        } catch (_: Exception) {
            // 失败了再走宽松模式
        }

        val normalized = normalizeJson(trimmed)
        return mapper.readTree(normalized)
    }

    /**
     * 把 {a:1, foo_bar:2} 变成 {"a":1,"foo_bar":2}
     */
    private fun normalizeJson(input: String): String {
        return input.replace(
            Regex("""([{,]\s*)([a-zA-Z_][\w-]*)(\s*:)"""),
            """$1"$2"$3"""
        )
    }
}
