package com.pan.json2typescript.util

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.stream.JsonReader
import java.io.StringReader

object JsonParser {

    /**
     * 宽松模式解析器：
     * JsonReader.setLenient(true) 开启所有 Gson 宽松特性，允许：
     *   - 单引号 key / string
     *   - 未加引号的 field names
     *   - // 和 /* */ 注释
     *   - 对象和数组的尾逗号
     *   - NaN / Infinity 等非标准数字
     */
    private val gson = GsonBuilder().create()

    fun parse(json: String): JsonElement = JsonReader(StringReader(json.trim())).use { reader ->
        reader.isLenient = true
        gson.fromJson(reader, JsonElement::class.java)
            ?: throw IllegalArgumentException("Empty JSON")
    }
}
