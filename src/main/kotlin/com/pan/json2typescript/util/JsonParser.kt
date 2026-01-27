package com.pan.json2typescript.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

object JsonParser {
    private val mapper = ObjectMapper()

    fun parse(json: String): JsonNode =
        mapper.readTree(json)
}
