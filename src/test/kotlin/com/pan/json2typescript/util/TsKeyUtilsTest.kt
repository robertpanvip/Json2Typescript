package com.pan.json2typescript.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
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
}
