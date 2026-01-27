package com.pan.json2typescript.util

object NameUtils {

    private val specialMap = mapOf(
        "children" to "Child",
        "people" to "Person",
        "men" to "Man",
        "women" to "Woman",
        "data" to "DataItem",
        "list" to "ListItem"
    )

    fun toTypeName(key: String): String {
        val clean = key.replace(Regex("[^a-zA-Z0-9]"), "")
        return clean.replaceFirstChar { it.uppercase() }
    }

    fun singularize(key: String): String {
        val lower = key.lowercase()

        specialMap[lower]?.let { return it }

        return when {
            lower.endsWith("ies") ->
                key.dropLast(3).replaceFirstChar { it.uppercase() } + "y"

            lower.endsWith("ses") ||
                    lower.endsWith("xes") ||
                    lower.endsWith("ches") ||
                    lower.endsWith("shes") ->
                key.dropLast(2).replaceFirstChar { it.uppercase() }

            lower.endsWith("s") && lower.length > 1 ->
                key.dropLast(1).replaceFirstChar { it.uppercase() }

            else ->
                key.replaceFirstChar { it.uppercase() } + "Item"
        }
    }
}
