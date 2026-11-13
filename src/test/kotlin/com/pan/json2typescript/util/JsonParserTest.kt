package com.pan.json2typescript.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("JsonParser 解析（严格 + 宽松）")
class JsonParserTest {

    @Test
    fun `标准 JSON 对象`() {
        val node = JsonParser.parse("""{"name": "tom", "age": 18}""")
        assertEquals("tom", node.asJsonObject.get("name").asString)
        assertEquals(18, node.asJsonObject.get("age").asInt)
    }

    @Test
    fun `标准 JSON 数组与标量`() {
        assertEquals(3, JsonParser.parse("[1, 2, 3]").asJsonArray.size())
    }

    @Test
    fun `顶层裸标量被拒绝 插件只处理对象或数组`() {
        assertThrows(Exception::class.java) { JsonParser.parse("\"abc\"") }
        assertThrows(Exception::class.java) { JsonParser.parse("1.5") }
        assertThrows(Exception::class.java) { JsonParser.parse("true") }
        assertThrows(Exception::class.java) { JsonParser.parse("null") }
    }

    @Test
    fun `未加引号的 key 可以解析`() {
        val node = JsonParser.parse("""{a: 1, foo_bar: 2}""")
        assertEquals(1, node.asJsonObject.get("a").asInt)
        assertEquals(2, node.asJsonObject.get("foo_bar").asInt)
    }

    @Test
    fun `尾逗号可以解析`() {
        val node = JsonParser.parse("""{"a": 1, "b": 2,}""")
        assertEquals(1, node.asJsonObject.get("a").asInt)
        assertEquals(2, node.asJsonObject.get("b").asInt)
    }

    @Test
    fun `单引号可以解析`() {
        val node = JsonParser.parse("""{'name': 'tom'}""")
        assertEquals("tom", node.asJsonObject.get("name").asString)
    }

    @Test
    fun `注释可以解析`() {
        val node = JsonParser.parse("""
            {
                // 用户信息
                "name": "tom", /* 注释 */
                "age": 18
            }
        """.trimIndent())
        assertEquals("tom", node.asJsonObject.get("name").asString)
        assertEquals(18, node.asJsonObject.get("age").asInt)
    }

    @Test
    fun `带连字符的 key 可以解析（走规范化）`() {
        val node = JsonParser.parse("""{my-key: 1, "a-b": 2}""")
        assertEquals(1, node.asJsonObject.get("my-key").asInt)
        assertEquals(2, node.asJsonObject.get("a-b").asInt)
    }

    @Test
    fun `规范化不会破坏字符串值里的 key 形态内容`() {
        val node = JsonParser.parse("""{"a": "x, y: z", "b": 2}""")
        assertEquals("x, y: z", node.asJsonObject.get("a").asString)
        assertEquals(2, node.asJsonObject.get("b").asInt)
    }

    @Test
    fun `数字前导零可以解析`() {
        val node = JsonParser.parse("""{"code": 007}""")
        assertEquals(7, node.asJsonObject.get("code").asInt)
    }

    @Test
    fun `null 值解析为 JsonNull`() {
        val node = JsonParser.parse("""{"a": null}""")
        assertEquals(true, node.asJsonObject.get("a").isJsonNull)
    }

    @Test
    fun `非法 JSON 抛出异常`() {
        assertThrows(Exception::class.java) {
            JsonParser.parse("""{a: """)
        }
        assertThrows(Exception::class.java) {
            JsonParser.parse("not json at all")
        }
    }
}
