package com.pan.json2typescript.util

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

object JsonParser {

    /**
     * 宽松模式的 mapper（流式解析，几乎零额外开销）：
     * 允许单引号、未加引号的 key、行注释与块注释、尾逗号、数字前导零等。
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

    fun parse(json: String): JsonNode = mapper.readTree(json.trim())
}
