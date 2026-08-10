package com.pan.json2typescript.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("TsKeyUtils 键名转换")
class TsKeyUtilsTest {

    @ParameterizedTest(name = "toTsKey({0}) = {1}")
    @CsvSource(
        "name, name",
        "fooBar, fooBar",
        "_private, _private",
        "\$ok, \$ok",
        "data1, data1",
        // TS/JS 保留字必须加引号
        "class, \"class\"",
        "default, \"default\"",
        "interface, \"interface\"",
        "type, \"type\"",
        "string, \"string\"",
        "delete, \"delete\"",
        "function, \"function\"",
        "new, \"new\"",
        "this, \"this\"",
        "undefined, \"undefined\"",
        "constructor, \"constructor\"",
        // 非法标识符加引号
        "user-name, \"user-name\"",
        "123abc, \"123abc\"",
        "foo bar, \"foo bar\"",
        "data.type, \"data.type\"",
        "a@b, \"a@b\""
    )
    fun `toTsKey 测试`(input: String, expected: String) {
        assertEquals(expected, TsKeyUtils.toTsKey(input), "input=$input")
    }

    @Test
    fun `key 内部含双引号和反斜杠需要转义`() {
        // 单独一个双引号字符作为 key -> "\""
        assertEquals("\"\\\"\"", TsKeyUtils.toTsKey("\""))
        // 反斜杠 -> "\\"
        assertEquals("\"\\\\\"", TsKeyUtils.toTsKey("\\"))
        // 用户给的 Bug2 案例：k:{"uid":"uuid"}
        val bug2Key = """k:{"uid":"edde99d7-8e7e-41f2-9d0a-9ef7de8122c1"}"""
        val bug2Expected = """"k:{\"uid\":\"edde99d7-8e7e-41f2-9d0a-9ef7de8122c1\"}""""
        assertEquals(bug2Expected, TsKeyUtils.toTsKey(bug2Key))
        // 混合："hello\"world\path"
        val mixed = """he"ll\o"""
        assertEquals(""""he\"ll\\o"""", TsKeyUtils.toTsKey(mixed))
    }
}
