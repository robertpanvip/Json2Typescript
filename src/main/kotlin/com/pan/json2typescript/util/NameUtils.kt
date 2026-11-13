package com.pan.json2typescript.util

object NameUtils {

    /**
     * 特殊映射：不规则复数 / 以 s 结尾但不是复数的单词（status、address、class、news…），
     * 避免被通用规则误删末尾 s 变成 Statu / Addres / Clas 这种残词。
     */
    private val specialMap = mapOf(
        "children" to "Child",
        "people" to "Person",
        "men" to "Man",
        "women" to "Woman",
        "data" to "DataItem",
        "list" to "ListItem",
        // 以 s 结尾但不是复数
        "status" to "Status",
        "statuses" to "Status",
        "address" to "Address",
        "addresses" to "Address",
        "class" to "Class",
        "classes" to "Class",
        "news" to "News",
        "goods" to "Good",
        "series" to "Series",
        "species" to "Species",
        "analysis" to "Analysis",
        "analyses" to "Analysis",
        "basis" to "Basis",
        "bases" to "Basis",
        "campus" to "Campus",
        "business" to "Business",
        "businesses" to "Business",
        "process" to "Process",
        "processes" to "Process",
        "access" to "Access",
        "progress" to "Progress",
        "success" to "Success",
        "focus" to "Focus",
        "bonus" to "Bonus",
        "canvas" to "Canvas",
        "atlas" to "Atlas",
        "alias" to "Alias",
        "virus" to "Virus",
        "minus" to "Minus",
        "plus" to "Plus",
        "bus" to "Bus",
        "buses" to "Bus",
        "gas" to "Gas",
        "glass" to "Glass",
        "glasses" to "Glass",
        "chaos" to "Chaos",
        "cosmos" to "Cosmos",
        "ethos" to "Ethos",
        "pathos" to "Pathos",
        "octopus" to "Octopus",
        "cactus" to "Cactus",
        "apparatus" to "Apparatus",
        // 不规则复数
        "index" to "Index",
        "indexes" to "Index",
        "indices" to "Index",
        "matrix" to "Matrix",
        "matrices" to "Matrix",
        "vertex" to "Vertex",
        "vertices" to "Vertex",
        "criterion" to "Criterion",
        "criteria" to "Criterion",
        "phenomenon" to "Phenomenon",
        "phenomena" to "Phenomenon",
        "thesis" to "Thesis",
        "theses" to "Thesis",
        "crisis" to "Crisis",
        "crises" to "Crisis",
        "diagnosis" to "Diagnosis",
        "diagnoses" to "Diagnosis",
        "hypothesis" to "Hypothesis",
        "hypotheses" to "Hypothesis",
        "mice" to "Mouse",
        "feet" to "Foot",
        "teeth" to "Tooth",
        "geese" to "Goose",
        "oxen" to "Ox"
    )

    /**
     * 后端 Bean 尾缀：Java/Spring 后端常用的对象后缀，在生成 TS 类型名时剥离。
     * userVO -> User、orderDTO -> Order、productEntity -> Product
     *
     * 按长度降序匹配（先长后短），避免 Request 没剥就被 Req 抢走。
     * 匹配要求：后缀必须是 key 的最后一个 token（camelCase 首字母大写 或 snake_case 最后一段），
     * 且剥离后剩下的主体至少还有 2 个字符（避免 VO -> 空 / UserVO -> Us 这种残词）。
     */
    private val BEAN_SUFFIXES = listOf(
        "Request", "Response", "Req", "Resp",
        "Entity", "Model", "Bean", "Form", "Param",
        "DTO", "Dto", "VO", "Bo", "BO", "Po", "PO", "DO", "Do"
    ).sortedByDescending { it.length }

    private fun stripBeanSuffix(key: String): String {
        // 先按非字母数字切 token，拿到最后一段（camelCase 里就是最后一个驼峰段）
        val tokens = key.split(Regex("[^a-zA-Z0-9]+"))
        if (tokens.isEmpty()) return key
        val last = tokens.last()
        val matched = BEAN_SUFFIXES.firstOrNull { suffix ->
            last.equals(suffix, ignoreCase = true)
        } ?: return key
        // 剥离后主体至少保留 2 字符（防止 UserVO -> Us / AVO -> 空 等）
        val body = key.removeSuffix(last)
        val bodyTokens = body.split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotEmpty() }
        if (bodyTokens.isEmpty() || bodyTokens.joinToString("").length < 2) return key
        return body
    }

    /**
     * 把字段名转成类型名：先剥离后端 Bean 尾缀 → 去分隔符 → 首字母大写。
     * user -> User、user_info -> UserInfo、user-info -> UserInfo
     * userVO -> User、orderDTO -> Order、productEntity -> Product
     *
     * 数字开头的类型名在 TS 中非法（不能以数字开头），自动加 I 前缀：
     * 123abc -> I123abc、2fa -> I2fa
     *
     * 如果去掉所有非字母数字字符后为空（比如 key 本身只有 "."、".."、"@#$" 等），
     * 则回退到默认类型名 "Field"，避免生成空名字的 export type。
     */
    fun toTypeName(key: String): String {
        val stripped = stripBeanSuffix(key)
        val raw = stripped
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.isNotEmpty() }
            .joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
            .ifEmpty { "Field" }
        return if (raw.first().isDigit()) "I$raw" else raw
    }

    /** 集合词：作为字段名后缀时剥离（itemList -> item、user_list -> user），只取元素名 */
    private val COLLECTION_WORDS = setOf("list", "set", "map", "array", "collection")

    fun singularize(key: String): String = singularizeInternal(key, true)

    private fun singularizeInternal(key: String, allowItemFallback: Boolean): String {
        val lower = key.lowercase()

        specialMap[lower]?.let { return it }

        // 剥离集合词后缀：itemList -> item、userSet -> user、orderItems_list -> orderItems
        // 仅当集合词是独立单词（camel 首字母大写 / snake 末 token），避免命中 scientist 等
        for (w in COLLECTION_WORDS) {
            val cap = w.replaceFirstChar { it.uppercase() }   // "List"
            if (key.endsWith(cap) && key.length > cap.length) {
                return singularizeInternal(key.removeSuffix(cap), false)
            }
        }
        if (key.contains(Regex("""[_\\-]"""))) {
            val idx = key.lastIndexOfAny(charArrayOf('_', '-'))
            val lastToken = key.substring(idx + 1).lowercase()
            if (lastToken in COLLECTION_WORDS && idx > 0) {
                return singularizeInternal(key.substring(0, idx), false)
            }
        }

        val singular = when {
            // categories -> category
            lower.endsWith("ies") && lower.length > 4 ->
                key.dropLast(3) + "y"

            // classes -> class（双 s + es）
            lower.endsWith("sses") ->
                key.dropLast(2)

            // boxes -> box / watches -> watch / dishes -> dish
            lower.endsWith("xes") ||
                    lower.endsWith("ches") ||
                    lower.endsWith("shes") ->
                key.dropLast(2)

            // cases -> case / houses -> house
            lower.endsWith("ses") ->
                key.dropLast(1)

            // names -> name / tags -> tag
            lower.endsWith("s") && lower.length > 1 ->
                key.dropLast(1)

            // 无法识别单数形式的，补 Item 后缀（剥离过集合词的不在此列）
            else -> null
        }

        return singular?.let { toTypeName(it) }
            ?: if (allowItemFallback) (toTypeName(key) + "Item") else toTypeName(key)
    }
}
