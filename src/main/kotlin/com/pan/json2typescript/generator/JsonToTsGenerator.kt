package com.pan.json2typescript.generator

import com.fasterxml.jackson.databind.JsonNode
import com.pan.json2typescript.util.JsonParser
import com.pan.json2typescript.util.NameUtils
import java.lang.StringBuilder
import java.util.*

class JsonToTsGenerator {

    private val definitions = LinkedHashMap<String, String>()

    fun generate(rootName: String, json: String): String {
        val root = JsonParser.parse(json)
        parseNode(rootName, root)

        return definitions.entries.joinToString("\n\n") {
            "export type ${it.key} = ${it.value}"
        }
    }

    private fun parseNode(typeName: String, node: JsonNode): String {
        if (definitions.containsKey(typeName)) return typeName

        val body = when {
            node.isObject -> parseObject( node)
            node.isArray -> parseArray(typeName, node)
            else -> getPrimitive(node)
        }

        definitions[typeName] = body
        return typeName
    }

    private fun parseObject( node: JsonNode): String {
        val sb = StringBuilder("{\n")

        node.properties().forEach { (key, value) ->
            val fieldType = when {
                value.isObject -> {
                    val child =  key.replaceFirstChar { it.uppercase() }
                    parseNode(child, value)
                }
                value.isArray -> {
                    val child = NameUtils.singularize(key)
                    parseArray(child, value)
                }
                else -> getPrimitive(value)
            }

            sb.append("  $key: $fieldType;\n")
        }

        sb.append("}")
        return sb.toString()
    }

    private fun inferArrayItemType(typeName: String, node: JsonNode): String {
        val elements = node.toList()
        if (elements.isEmpty()) return "any"

        // 非对象数组（primitive / null / array 混合）
        if (!elements.all { it.isObject }) {
            return elements
                .map { resolveType(typeName, it) }
                .toSet()
                .joinToString(" | ")
        }

        val sb = StringBuilder("{\n")
        val fieldTypes = mutableMapOf<String, MutableSet<String>>()
        val fieldCount = mutableMapOf<String, Int>()

        elements.forEach { obj ->
            obj.properties().forEach { (key, value) ->
                val fieldType = resolveType(
                    NameUtils.singularize(key).replaceFirstChar { it.uppercase() },
                    value
                )

                fieldTypes
                    .computeIfAbsent(key) { mutableSetOf() }
                    .add(fieldType)

                fieldCount[key] = fieldCount.getOrDefault(key, 0) + 1
            }
        }

        fieldTypes.forEach { (key, types) ->
            val optional = fieldCount[key] != elements.size
            val optionalMark = if (optional) "?" else ""

            sb.append("  $key$optionalMark: ${types.joinToString(" | ")};\n")
        }

        sb.append("}")
        definitions[typeName] = sb.toString()
        return typeName
    }

    private fun parseArray(typeName: String, node: JsonNode): String {
        if (node.isEmpty) return "unknown[]"
        val itemType = inferArrayItemType(typeName, node)
        return "$itemType[]"
    }

    private fun resolveType(typeName: String, node: JsonNode): String {
        return when {
            node.isObject -> {
                parseNode(typeName, node)
                typeName
            }
            node.isArray -> parseArray(typeName, node)
            else -> getPrimitive(node)
        }
    }

    private fun getPrimitive(node: JsonNode): String =
        when {
            node.isTextual -> "string"
            node.isInt || node.isLong -> "number"
            node.isDouble || node.isFloat -> "number"
            node.isBoolean -> "boolean"
            node.isNull -> "null"
            else -> "any"
        }
}
