package com.pan.json2typescript.generator

/**
 * JsonToTsGenerator 输出选项。
 *
 * @param readonly      所有字段加 readonly 修饰符（API 响应不可修改）
 * @param enumThreshold 数组中字符串唯一值 ≤ 这个阈值时，生成字面量联合类型
 *                      (如 ["pending","done"] -> "pending" | "done")；
 *                      超过阈值时回退为 string。设为 0 关闭字面量检测。
 * @param detectDate    识别 ISO 8601 / yyyy-MM-dd 等日期字符串 → Date 类型
 */
data class GeneratorOptions(
    val readonly: Boolean = false,
    val enumThreshold: Int = 6,
    val detectDate: Boolean = true
)
