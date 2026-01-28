package com.pan.json2typescript.util

private val TS_KEYWORD = setOf(
)

object TsKeyUtils {

    fun toTsKey(key: String): String {
        // 合法标识符 + 不是关键字
        if (key.matches(Regex("""^[a-zA-Z_$][\w$]*$""")) &&
            key !in TS_KEYWORD
        ) {
            return key
        }

        // 否则一律加引号
        return "\"$key\""
    }
}