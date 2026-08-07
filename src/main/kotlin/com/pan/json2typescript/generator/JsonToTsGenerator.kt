package com.pan.json2typescript.generator

import com.fasterxml.jackson.databind.JsonNode
import com.pan.json2typescript.util.JsonParser
import com.pan.json2typescript.util.NameUtils
import com.pan.json2typescript.util.TsKeyUtils
import com.pan.json2typescript.util.TypeGuesser
import java.lang.StringBuilder
import java.util.*

class JsonToTsGenerator {

    private val definitions = LinkedHashMap<String, String>()

    fun generate(rootName: String, json: String): String {
        definitions.clear()
        val root = JsonParser.parse(json)
        parseNode(rootName, root)

        return definitions.entries.joinToString("\n\n") {
            "export type ${it.key} = ${it.value}"
        }
    }

    private fun parseNode(typeName: String, node: JsonNode): String {
        if (definitions.containsKey(typeName)) return typeName

        val body = when {
            node.isObject -> parseObject(node)
            node.isArray -> parseArray(typeName, node, null)
            else -> getPrimitive(node, null)
        }

        // 顶层数组场景下 inferArrayItemType 可能已注册了对象 body，避免被 "Root[]" 覆盖
        definitions.putIfAbsent(typeName, body)
        return typeName
    }

    private fun parseObject(node: JsonNode): String {
        val sb = StringBuilder("{\n")

        node.properties().forEach { (key, value) ->
            val fieldType = when {
                value.isObject -> parseNode(NameUtils.toTypeName(key), value)
                value.isArray -> parseArray(NameUtils.singularize(key), value, key)
                else -> getPrimitive(value, key)
            }
            val tsKey = TsKeyUtils.toTsKey(key)
            sb.append("  $tsKey: $fieldType;\n")
        }

        sb.append("}")
        return sb.toString()
    }

    private fun inferArrayItemType(typeName: String, node: JsonNode, key: String?): String {
        val elements = node.toList()
        if (elements.isEmpty()) return "any"

        // 非对象数组（primitive / null / array 混合）
        if (!elements.all { it.isObject }) {
            return elements
                .map { resolveType(typeName, it, key) }
                .toSet()
                .joinToString(" | ")
        }

        val sb = StringBuilder("{\n")
        val fieldTypes = mutableMapOf<String, MutableSet<String>>()
        val fieldCount = mutableMapOf<String, Int>()

        elements.forEach { obj ->
            obj.properties().forEach { (fieldKey, value) ->
                val fieldType = resolveType(
                    NameUtils.singularize(fieldKey),
                    value,
                    fieldKey
                )

                fieldTypes
                    .computeIfAbsent(fieldKey) { mutableSetOf() }
                    .add(fieldType)

                fieldCount[fieldKey] = fieldCount.getOrDefault(fieldKey, 0) + 1
            }
        }

        fieldTypes.forEach { (fieldKey, types) ->
            val optional = fieldCount[fieldKey] != elements.size
            val optionalMark = if (optional) "?" else ""

            sb.append("  ${TsKeyUtils.toTsKey(fieldKey)}$optionalMark: ${types.joinToString(" | ")};\n")
        }

        sb.append("}")
        definitions[typeName] = sb.toString()
        return typeName
    }

    private fun parseArray(typeName: String, node: JsonNode, key: String?): String {
        if (node.isEmpty) {
            // 空数组：根据 key 猜测元素类型（ids -> number[]、tags -> string[]）
            val guessed = key?.let { TypeGuesser.guess(it) }
            return if (guessed != null) "$guessed[]" else "unknown[]"
        }
        val itemType = inferArrayItemType(typeName, node, key)
        // 联合类型需要加括号：(number | string)[]，避免被解析成 number | string[]
        val wrapped = if (itemType.contains(" | ")) "($itemType)" else itemType
        return "$wrapped[]"
    }

    private fun resolveType(typeName: String, node: JsonNode, key: String?): String {
        return when {
            node.isObject -> {
                parseNode(typeName, node)
                typeName
            }
            node.isArray -> parseArray(typeName, node, key)
            else -> getPrimitive(node, key)
        }
    }

    private fun getPrimitive(node: JsonNode, key: String?): String =
        when {
            node.isTextual -> "string"
            node.isInt || node.isLong -> "number"
            node.isDouble || node.isFloat -> "number"
            node.isBoolean -> "boolean"
            node.isNull -> key?.let { TypeGuesser.guess(it) } ?: "null"
            else -> "any"
        }
}
