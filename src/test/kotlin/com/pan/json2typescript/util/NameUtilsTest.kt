package com.pan.json2typescript.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("NameUtils 单复数与类型命名")
class NameUtilsTest {

    @ParameterizedTest(name = "singularize({0}) = {1}")
    @CsvSource(
        // 特殊映射：不规则复数与常见集合词
        "children, Child",
        "people, Person",
        "men, Man",
        "women, Woman",
        "data, DataItem",
        "list, ListItem",
        // 以 s 结尾但不是复数（原实现会误删 s 变成 Statu/Addres/Clas）
        "status, Status",
        "statuses, Status",
        "address, Address",
        "addresses, Address",
        "class, Class",
        "classes, Class",
        "news, News",
        "goods, Good",
        "series, Series",
        "species, Species",
        "analysis, Analysis",
        "analyses, Analysis",
        "basis, Basis",
        "bases, Basis",
        "business, Business",
        "businesses, Business",
        "process, Process",
        "processes, Process",
        "access, Access",
        "progress, Progress",
        "success, Success",
        "focus, Focus",
        "bonus, Bonus",
        "bus, Bus",
        "buses, Bus",
        "gas, Gas",
        "glass, Glass",
        "glasses, Glass",
        "canvas, Canvas",
        "virus, Virus",
        "alias, Alias",
        "atlas, Atlas",
        // 不规则复数
        "index, Index",
        "indexes, Index",
        "indices, Index",
        "matrix, Matrix",
        "matrices, Matrix",
        "criterion, Criterion",
        "criteria, Criterion",
        "phenomenon, Phenomenon",
        "phenomena, Phenomenon",
        "thesis, Thesis",
        "theses, Thesis",
        "crisis, Crisis",
        "crises, Crisis",
        // 常规规则
        "categories, Category",
        "companies, Company",
        "bodies, Body",
        "keys, Key",
        "days, Day",
        "boxes, Box",
        "watches, Watch",
        "dishes, Dish",
        "cases, Case",
        "houses, House",
        "phases, Phase",
        "names, Name",
        "tags, Tag",
        "users, User",
        "items, Item",
        "orders, Order",
        // 集合词后缀：剥离后只取元素名（尽可能短）
        "itemList, Item",
        "userList, User",
        "orderList, Order",
        "userSet, User",
        "dataArray, DataItem",
        "item_list, Item",
        "users_list, User",
        "order_items_list, OrderItem",
        // 无法识别的单数形式补 Item
        "menu, MenuItem",
        "config, ConfigItem",
        "thing, ThingItem",
        "user_info, UserInfoItem",
        "my-files, MyFile"
    )
    fun `singularize 测试`(input: String, expected: String) {
        assertEquals(expected, NameUtils.singularize(input), "input=$input")
    }

    @ParameterizedTest(name = "toTypeName({0}) = {1}")
    @CsvSource(
        "user, User",
        "user_info, UserInfo",
        "user-info, UserInfo",
        "fooBar, FooBar",
        "123abc, 123abc",
        "a, A"
    )
    fun `toTypeName 测试`(input: String, expected: String) {
        assertEquals(expected, NameUtils.toTypeName(input), "input=$input")
    }

    @Test
    fun `大小写混合输入也能正确单数化`() {
        assertEquals("Category", NameUtils.singularize("Categories"))
        assertEquals("Status", NameUtils.singularize("STATUS"))
        assertEquals("User", NameUtils.singularize("Users"))
    }
}
