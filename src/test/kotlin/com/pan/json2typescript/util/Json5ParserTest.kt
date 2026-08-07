package com.pan.json2typescript.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("JsonParser 的 JSON5 特性支持")
class Json5ParserTest {

    @Test
    fun `十六进制数字`() {
        val node = JsonParser.parse("""{"a": 0xFF, "b": 0x1F}""")
        assertEquals(255, node["a"].asInt())
        assertEquals(31, node["b"].asInt())
    }

    @Test
    fun `前导小数点`() {
        val node = JsonParser.parse("""{"a": .5, "b": .25}""")
        assertEquals(0.5, node["a"].asDouble())
        assertEquals(0.25, node["b"].asDouble())
    }

    @Test
    fun `尾随小数点`() {
        val node = JsonParser.parse("""{"a": 5., "b": 10.}""")
        assertEquals(5.0, node["a"].asDouble())
        assertEquals(10.0, node["b"].asDouble())
    }

    @Test
    fun `显式正号`() {
        val node = JsonParser.parse("""{"a": +5, "b": +3.14}""")
        assertEquals(5, node["a"].asInt())
        assertEquals(3.14, node["b"].asDouble())
    }

    @Test
    fun `Infinity 与 NaN 推断为 number`() {
        val node = JsonParser.parse("""{"a": Infinity, "b": -Infinity, "c": NaN}""")
        assertTrue(node["a"].isNumber)
        assertTrue(node["b"].isNumber)
        assertTrue(node["c"].isNumber)
        assertTrue(node["a"].asDouble().isInfinite())
        // NaN 被归一化为数字占位(0)，类型推断为 number；值语义不参与类型生成
    }

    @Test
    fun `undefined 视为 null`() {
        val node = JsonParser.parse("""{"a": undefined}""")
        assertTrue(node["a"].isNull)
    }

    @Test
    fun `多行字符串（行继续）`() {
        val src = "{\"a\": \"line1\\\nline2\"}"
        val node = JsonParser.parse(src)
        assertEquals("line1line2", node["a"].asText())
    }

    @Test
    fun `井号行注释`() {
        val node = JsonParser.parse(
            """
            # 这是 JSON5 注释
            {
                "name": "tom", # 行尾注释
                "age": 18
            }
            """.trimIndent()
        )
        assertEquals("tom", node["name"].asText())
        assertEquals(18, node["age"].asInt())
    }

    @Test
    fun `JSON5 综合用法`() {
        val src = """
            {
                // 用户信息
                'name': 'tom',
                userId: 1001,        // 未加引号 key
                tags: ['a', 'b'],    /* 块注释 */
                score: 0x10,
                ratio: .5,
                big: +99,
                max: Infinity,
                empty: undefined,
            }
        """.trimIndent()
        val node = JsonParser.parse(src)
        assertEquals("tom", node["name"].asText())
        assertEquals(1001, node["userId"].asInt())
        assertEquals(2, node["tags"].size())
        assertEquals(16, node["score"].asInt())
        assertEquals(0.5, node["ratio"].asDouble())
        assertEquals(99, node["big"].asInt())
        assertTrue(node["max"].isNumber)
        assertTrue(node["empty"].isNull)
    }

    @Test
    fun `标识符片段不会被误转换`() {
        // InfinityKey 作为未加引号 key 应被正常加引号，而非被当作 Infinity 关键字替换
        val node = JsonParser.parse("""{"InfinityKey": 1}""")
        assertTrue(node.has("InfinityKey"))
        assertFalse(node.has("Infinity"))
    }
}
