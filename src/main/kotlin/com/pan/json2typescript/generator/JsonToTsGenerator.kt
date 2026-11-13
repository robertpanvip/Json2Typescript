package com.pan.json2typescript.generator

import com.google.gson.JsonElement
import com.pan.json2typescript.util.JsonParser
import com.pan.json2typescript.util.NameUtils
import com.pan.json2typescript.util.TsKeyUtils
import com.pan.json2typescript.util.TypeGuesser
import java.lang.StringBuilder
import java.util.*
import java.util.regex.Pattern

class JsonToTsGenerator(
    private val options: GeneratorOptions = GeneratorOptions()
) {

    private val definitions = LinkedHashMap<String, String>()
    // 结构签名 -> 已定义的类型名：用于鸭子类型去重（结构相同的对象复用同一类型）
    private val structureToName = LinkedHashMap<String, String>()

    fun generate(rootName: String, json: String): String {
        definitions.clear()
        structureToName.clear()
        val root = JsonParser.parse(json)
        parseNode(rootName, root)

        return definitions.entries.joinToString("\n\n") {
            "export type ${it.key} = ${it.value};"
        }
    }

    private fun parseNode(typeName: String, node: JsonElement): String {
        return when {
            node.isJsonObject -> parseObject(typeName, node)
            node.isJsonArray -> {
                // 顶层数组：用 typeName+Item 作为元素类型名，
                // 避免 typeName 被 inferArrayItemType 内部注册成元素对象类型，
                // 从而导致 typeName 被占用后无法注册数组类型。
                val itemTypeName = typeName + "Item"
                val arrType = parseArray(itemTypeName, node, null)
                val name = uniqueName(typeName)
                definitions[name] = arrType
                name
            }
            else -> {
                val p = getPrimitive(node, null)
                val name = uniqueName(typeName)
                definitions[name] = p
                p
            }
        }
    }

    private fun fieldPrefix(): String = if (options.readonly) "readonly " else ""

    private fun parseObject(typeName: String, node: JsonElement): String {
        val fieldMap = LinkedHashMap<String, String>()
        node.asJsonObject.entrySet().forEach { (key, value) ->
            val fieldType = when {
                value.isJsonObject -> parseNode(NameUtils.toTypeName(key), value)
                value.isJsonArray -> parseArray(NameUtils.singularize(key), value, key)
                else -> getPrimitive(value, key)
            }
            fieldMap[TsKeyUtils.toTsKey(key)] = fieldType
        }

        val sb = StringBuilder("{\n")
        fieldMap.forEach { (tsKey, fieldType) ->
            sb.append("  ${fieldPrefix()}$tsKey: $fieldType;\n")
        }
        sb.append("}")
        val body = sb.toString()
        return registerOrReuse(typeName, body, signatureOf(fieldMap))
    }

    private fun inferArrayItemType(typeName: String, node: JsonElement, key: String?): String {
        val elements = node.asJsonArray.toList()
        if (elements.isEmpty()) return "any"

        // 忽略数组中的 null 元素：null 不代表一种元素类型，也不计入对象字段出现次数
        // （字段级可空由 parseArray 根据「数组是否含 null 元素」统一加 | null 处理）
        val nonNull = elements.filter { !it.isJsonNull }
        if (nonNull.isEmpty()) {
            // 全部为 null：无法从内容推断，按 key 猜测兜底
            return key?.let { TypeGuesser.guess(it) } ?: "any"
        }

        // 非对象数组（primitive / array 混合，已排除 null）
        if (!nonNull.all { it.isJsonObject }) {
            // 枚举检测：如果是纯字符串数组，且唯一值数量 <= 阈值 → 字面量联合
            val enumType = tryInferLiteralUnion(nonNull)
            if (enumType != null) return enumType

            val types = nonNull
                .map { resolveType(typeName, it, key) }
                .toSet()
            return resolveUnion(types)
        }

        // 区分收集：非 null 值的实际类型 vs null 值标记
        // 这样合并时，若同字段既有具体类型又有 null，直接 | null，
        // 而不是把 null 按 key 猜成可能冲突的类型（如 selectedLabelList:null 被猜成 string）
        val fieldConcreteTypes = mutableMapOf<String, MutableSet<String>>()
        val fieldHasNull = mutableMapOf<String, Boolean>()
        val fieldCount = mutableMapOf<String, Int>()
        // 额外：收集每个字段的所有原始字符串值，用于字段级枚举检测
        val fieldStringValues = mutableMapOf<String, MutableList<String>>()

        nonNull.forEach { obj ->
            obj.asJsonObject.entrySet().forEach { (fieldKey, value) ->
                fieldCount[fieldKey] = fieldCount.getOrDefault(fieldKey, 0) + 1

                if (value.isJsonNull) {
                    // null 不立即按 key 猜测：等合并时根据是否存在具体类型再决定
                    fieldHasNull[fieldKey] = true
                } else if (value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                    // 收集原始字符串值，方便后面判断字段级枚举
                    fieldStringValues
                        .computeIfAbsent(fieldKey) { mutableListOf() }
                        .add(value.asJsonPrimitive.asString)
                    val fieldType = resolveType(
                        NameUtils.singularize(fieldKey),
                        value,
                        fieldKey
                    )
                    fieldConcreteTypes
                        .computeIfAbsent(fieldKey) { mutableSetOf() }
                        .add(fieldType)
                } else {
                    val fieldType = resolveType(
                        NameUtils.singularize(fieldKey),
                        value,
                        fieldKey
                    )
                    fieldConcreteTypes
                        .computeIfAbsent(fieldKey) { mutableSetOf() }
                        .add(fieldType)
                }
            }
        }

        val fieldMap = LinkedHashMap<String, String>()
        val sb = StringBuilder("{\n")
        fieldCount.keys.forEach { fieldKey ->
            val optional = fieldCount[fieldKey] != nonNull.size
            val optionalMark = if (optional) "?" else ""

            val concrete = fieldConcreteTypes[fieldKey] ?: emptySet()
            val hasNull = fieldHasNull[fieldKey] == true

            val union = if (concrete.isEmpty()) {
                // 该字段在所有出现位置都是 null：按 key 猜测兜底，猜不到则 null
                TypeGuesser.guess(fieldKey) ?: "null"
            } else {
                // 字段级枚举检测：收集到了重复的字符串值 → 字面量联合
                val literalEnum = fieldStringValues[fieldKey]?.let {
                    tryInferFieldLiteralUnion(it)
                }
                if (literalEnum != null) {
                    if (hasNull) "$literalEnum | null" else literalEnum
                } else {
                    // 存在具体类型：null 不再按 key 猜测（避免猜出 string 等与实际冲突的类型），
                    // 而是作为 | null 附加，体现字段可空语义
                    val concreteUnion = resolveUnion(concrete)
                    if (hasNull) "$concreteUnion | null" else concreteUnion
                }
            }

            val tsKey = TsKeyUtils.toTsKey(fieldKey)
            fieldMap[tsKey] = union
            sb.append("  ${fieldPrefix()}$tsKey$optionalMark: $union;\n")
        }
        sb.append("}")
        val body = sb.toString()
        return registerOrReuse(typeName, body, signatureOf(fieldMap))
    }

    /**
     * 字段级字面量联合检测：与数组级逻辑相同，但输入是字段的原始字符串列表。
     * 例如 fieldKey="type" 在数组元素中出现 ["done","ok","done","todo"] → "done" | "ok" | "todo"
     */
    private fun tryInferFieldLiteralUnion(allStrings: List<String>): String? {
        if (options.enumThreshold <= 0) return null
        val unique = allStrings.distinct()
        if (unique.size <= 1 || unique.size > options.enumThreshold) return null
        val hasDuplicates = unique.size < allStrings.size
        if (!hasDuplicates) return null
        val looksLikeEnum = unique.all { isEnumLike(it) } && unique.any { it.length >= 3 }
        if (!looksLikeEnum) return null
        return unique.joinToString(" | ") { "\"${escapeString(it)}\"" }
    }

    /**
     * 纯字符串数组 → 字面量联合类型。
     * 触发条件（全部满足）：
     *   1. 所有非 null 元素都是字符串
     *   2. 至少有一个值**重复出现**（枚举的铁证）——["done","ok","done","todo"] 可判枚举，
     *      而 ["Alice","Bob","Charlie"] 全唯一只是普通字符串数据
     *   3. 唯一值数量 ≥ 2 且 ≤ enumThreshold
     *   4. 每个值都是"像枚举"的标识符（字母开头、字母/数字/下划线/连字符、≤20字符）
     *      —— 避免把 ["a","b","a"] 这种测试/占位数组误判为枚举
     *   5. 至少一个值长度 ≥ 3（排除单字母 ["a","b","a"]）
     *
     * 超过阈值、关闭检测（threshold=0）、没有重复、或值看起来不像枚举时返回 null，
     * 由调用方回退为 string。
     *
     * 注意：这里只列"已观察到"的几个值。后端真实枚举可能不止这些（用户可能只拷贝了部分数据），
     * 所以输出是联合类型 `'done' | 'ok' | 'todo'`，而不是 TS enum 声明。
     */
    private fun tryInferLiteralUnion(nonNull: List<JsonElement>): String? {
        if (options.enumThreshold <= 0) return null
        if (nonNull.any { !it.isJsonPrimitive || !it.asJsonPrimitive.isString }) return null

        val allStrings = nonNull.map { it.asJsonPrimitive.asString }
        val unique = allStrings.distinct()
        if (unique.size <= 1 || unique.size > options.enumThreshold) return null

        // 必须有重复值（枚举的铁证）——完全唯一的字符串数组通常是数据，不是枚举
        val hasDuplicates = unique.size < allStrings.size
        if (!hasDuplicates) return null

        // 每个值都像枚举标识符
        val looksLikeEnum = unique.all { isEnumLike(it) } && unique.any { it.length >= 3 }
        if (!looksLikeEnum) return null

        return unique.joinToString(" | ") { "\"${escapeString(it)}\"" }
    }

    /**
     * 判断一个字符串值是否"像枚举值"——适合提升为字面量联合。
     * 宽松标准：长度 1~20，全是字母/数字/下划线，允许连字符（常见 kebab-case 枚举）。
     * 全数字值（如 "1"、"42"）不算枚举（那是 number 类型的字符串化，通常应保持 string）。
     */
    private fun isEnumLike(v: String): Boolean {
        if (v.isEmpty() || v.length > 20) return false
        // 必须以字母开头（排除纯数字/符号开头）
        val first = v.firstOrNull() ?: return false
        if (!first.isLetter()) return false
        // 允许字母/数字/下划线/连字符
        return v.all { it.isLetterOrDigit() || it == '_' || it == '-' }
    }

    private fun escapeString(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"")

    /**
     * 按结构注册或复用类型（鸭子类型）。
     * 若已有相同结构签名的定义，直接复用其类型名；否则以 [typeName] 注册新类型。
     * 若 [typeName] 已被结构不同的类型占用，自动生成唯一后缀名（A → A_2 → A_3）。
     * 字段顺序不影响去重（签名按字段名排序）。
     */
    private fun registerOrReuse(typeName: String, body: String, signature: String): String {
        structureToName[signature]?.let { existingName ->
            return existingName
        }
        val name = uniqueName(typeName)
        definitions[name] = body
        structureToName[signature] = name
        return name
    }

    /**
     * 生成不冲突的类型名：若 [base] 未被占用直接返回，否则追加 _2、_3… 后缀。
     */
    private fun uniqueName(base: String): String {
        if (!definitions.containsKey(base)) return base
        var idx = 2
        var candidate = "${base}_$idx"
        while (definitions.containsKey(candidate)) {
            idx++
            candidate = "${base}_$idx"
        }
        return candidate
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

    private fun parseArray(typeName: String, node: JsonElement, key: String?): String {
        // 数组含 null 元素时，整个字段可空（ItemType[] | null），而不是把 null 塞进元素联合类型
        val hasNullElement = node.asJsonArray.any { it.isJsonNull }
        if (node.asJsonArray.isEmpty()) {
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

    private fun resolveType(typeName: String, node: JsonElement, key: String?): String {
        return when {
            node.isJsonObject -> parseNode(typeName, node)
            node.isJsonArray -> parseArray(typeName, node, key)
            else -> getPrimitive(node, key)
        }
    }

    private fun getPrimitive(node: JsonElement, key: String?): String {
        if (node.isJsonNull) {
            return key?.let { TypeGuesser.guess(it) } ?: "null"
        }
        if (node.isJsonPrimitive) {
            val p = node.asJsonPrimitive
            if (p.isString) {
                // 日期字符串识别（先看 key 有没有日期语义，再看值的格式）
                if (options.detectDate && key != null && TypeGuesser.isDateKey(key)) {
                    if (looksLikeDate(p.asString)) return "Date"
                }
                return "string"
            }
            if (p.isBoolean) return "boolean"
            if (p.isNumber) return "number"
        }
        return "any"
    }

    // ---------- 日期识别 ----------

    companion object {
        /** ISO 8601 / RFC 3339 日期时间：2024-01-15 / 2024-01-15T10:30:00Z / 2024-01-15T10:30:00+08:00 */
        private val ISO_DATETIME_PATTERNS: List<Pattern> = listOf(
            // 严格 RFC 3339 / ISO 8601
            Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:?\\d{2})?$"),
            // 纯日期
            Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$"),
            // 常见后端格式：yyyy/MM/dd HH:mm:ss、yyyy-MM-dd HH:mm:ss
            Pattern.compile("^\\d{4}[-/]\\d{2}[-/]\\d{2}[ T]\\d{2}:\\d{2}(:\\d{2}(\\.\\d+)?)?$"),
        )

        /**
         * 字符串看起来像日期格式。只做格式匹配，不做日历合法性校验（避免 9999-99-99 这种极端值）。
         */
        fun looksLikeDate(s: String): Boolean {
            if (s.length < 8 || s.length > 40) return false
            return ISO_DATETIME_PATTERNS.any { it.matcher(s).matches() }
        }
    }
}
