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
    // 结构签名 -> 已定义的类型名：用于鸭子类型去重（结构相同的对象复用同一类型）
    private val structureToName = LinkedHashMap<String, String>()

    fun generate(rootName: String, json: String): String {
        definitions.clear()
        structureToName.clear()
        val root = JsonParser.parse(json)
        parseNode(rootName, root)

        return definitions.entries.joinToString("\n\n") {
            "export type ${it.key} = ${it.value}"
        }
    }

    private fun parseNode(typeName: String, node: JsonNode): String {
        if (definitions.containsKey(typeName)) return typeName

        return when {
            node.isObject -> parseObject(typeName, node)
            node.isArray -> {
                // 顶层数组：元素对象注册在 typeName 下（见 inferArrayItemType）；
                // 空数组根等无元素对象的情况兜底注册数组类型（如 unknown[]）
                val arrType = parseArray(typeName, node, null)
                definitions.putIfAbsent(typeName, arrType)
                typeName
            }
            else -> {
                val p = getPrimitive(node, null)
                definitions.putIfAbsent(typeName, p)
                p
            }
        }
    }

    private fun parseObject(typeName: String, node: JsonNode): String {
        val fieldMap = LinkedHashMap<String, String>()
        node.properties().forEach { (key, value) ->
            val fieldType = when {
                value.isObject -> parseNode(NameUtils.toTypeName(key), value)
                value.isArray -> parseArray(NameUtils.singularize(key), value, key)
                else -> getPrimitive(value, key)
            }
            fieldMap[TsKeyUtils.toTsKey(key)] = fieldType
        }

        val sb = StringBuilder("{\n")
        fieldMap.forEach { (tsKey, fieldType) -> sb.append("  $tsKey: $fieldType;\n") }
        sb.append("}")
        val body = sb.toString()
        return registerOrReuse(typeName, body, signatureOf(fieldMap))
    }

    private fun inferArrayItemType(typeName: String, node: JsonNode, key: String?): String {
        val elements = node.toList()
        if (elements.isEmpty()) return "any"

        // 忽略数组中的 null 元素：null 不代表一种元素类型，也不计入对象字段出现次数
        // （字段级可空由 parseArray 根据「数组是否含 null 元素」统一加 | null 处理）
        val nonNull = elements.filter { !it.isNull }
        if (nonNull.isEmpty()) {
            // 全部为 null：无法从内容推断，按 key 猜测兜底
            return key?.let { TypeGuesser.guess(it) } ?: "any"
        }

        // 非对象数组（primitive / array 混合，已排除 null）
        if (!nonNull.all { it.isObject }) {
            val types = nonNull
                .map { resolveType(typeName, it, key) }
                .toSet()
            return resolveUnion(types)
        }

        val fieldTypes = mutableMapOf<String, MutableSet<String>>()
        val fieldCount = mutableMapOf<String, Int>()

        nonNull.forEach { obj ->
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

        val fieldMap = LinkedHashMap<String, String>()
        val sb = StringBuilder("{\n")
        fieldTypes.forEach { (fieldKey, types) ->
            val optional = fieldCount[fieldKey] != nonNull.size
            val optionalMark = if (optional) "?" else ""

            // 同一字段在数组不同元素中出现 unknown[] / unknown / any 等「无法推断」通配类型时，
            // 若存在更具体的类型（如空数组 [] -> unknown[] 与 [{b:1}] -> Child[] 同列），
            // 丢弃通配类型，避免产生 unknown[] | Child[] 这种无意义联合。
            val union = resolveUnion(types)
            val tsKey = TsKeyUtils.toTsKey(fieldKey)
            fieldMap[tsKey] = union
            sb.append("  $tsKey$optionalMark: $union;\n")
        }
        sb.append("}")
        val body = sb.toString()
        return registerOrReuse(typeName, body, signatureOf(fieldMap))
    }

    /**
     * 按结构注册或复用类型（鸭子类型）。
     * 若已有相同结构签名的定义，直接复用其类型名；否则以 [typeName] 注册新类型。
     * 字段顺序不影响去重（签名按字段名排序）。
     */
    private fun registerOrReuse(typeName: String, body: String, signature: String): String {
        structureToName[signature]?.let { existingName ->
            return existingName
        }
        structureToName[signature] = typeName
        definitions.putIfAbsent(typeName, body)
        return typeName
    }

    private fun signatureOf(fieldMap: LinkedHashMap<String, String>): String =
        fieldMap.entries.sortedBy { it.key }
            .joinToString(";") { "${it.key}|${it.value}" }

    /** 无法从内容推断的通配类型：出现更具体的类型时应被丢弃（空数组 [] -> unknown[] 即属此类） */
    private val wildcardTypes = setOf("unknown", "unknown[]", "any")

    /**
     * 将字段在数组各元素中的类型集合解析为联合类型字符串。
     *
     * 若集合中同时存在通配类型（unknown[] / unknown / any，通常来自空数组或无法推断的值）
     * 和具体类型，则丢弃通配类型：同一字段在数组中应统一为具体类型，
     * 而不是产生 unknown[] | Child[] 这种无意义联合。
     * 全为通配类型时保持原样（确实无法推断）。
     */
    private fun resolveUnion(types: Collection<String>): String {
        val concrete = types.filter { it !in wildcardTypes }
        val resolved = if (concrete.isNotEmpty()) concrete else types
        return resolved.joinToString(" | ")
    }

    private fun parseArray(typeName: String, node: JsonNode, key: String?): String {
        // 数组含 null 元素时，整个字段可空（ItemType[] | null），而不是把 null 塞进元素联合类型
        val hasNullElement = node.toList().any { it.isNull }
        if (node.isEmpty) {
            // 空数组：根据 key 猜测元素类型（ids -> number[]、tags -> string[]）
            val guessed = key?.let { TypeGuesser.guess(it) }
            val arrType = if (guessed != null) "$guessed[]" else "unknown[]"
            return if (hasNullElement) "$arrType | null" else arrType
        }
        val itemType = inferArrayItemType(typeName, node, key)
        // 联合类型需要加括号：(number | string)[]，避免被解析成 number | string[]
        val wrapped = if (itemType.contains(" | ")) "($itemType)" else itemType
        val arrType = "$wrapped[]"
        return if (hasNullElement) "$arrType | null" else arrType
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
