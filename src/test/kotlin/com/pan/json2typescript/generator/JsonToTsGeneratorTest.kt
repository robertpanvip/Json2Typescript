package com.pan.json2typescript.generator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * JsonToTsGenerator 集成测试。
 * 重点覆盖：null 值按 key 推断类型、空数组按 key 推断、嵌套对象/数组、联合类型、可选字段、宽松 JSON、非法键名。
 */
@DisplayName("JsonToTsGenerator 集成")
class JsonToTsGeneratorTest {

    private val generator = JsonToTsGenerator()

    @Test
    fun `基本对象精确输出`() {
        val ts = generator.generate(
            "Root",
            """{"name":"John","age":30,"active":true,"score":9.5}"""
        )
        assertEquals(
            "export type Root = {\n" +
                "  name: string;\n" +
                "  age: number;\n" +
                "  active: boolean;\n" +
                "  score: number;\n" +
                "}",
            ts
        )
    }

    @Test
    fun `嵌套对象生成独立类型`() {
        val ts = generator.generate(
            "Root",
            """{"user":{"name":"x","age":1},"list":[1,2]}"""
        )
        assertTrue(ts.contains("export type User = {"), ts)
        assertTrue(ts.contains("  name: string;"), ts)
        assertTrue(ts.contains("  age: number;"), ts)
        assertTrue(ts.contains("  user: User;"), ts)
        assertTrue(ts.contains("  list: number[];"), ts)
    }

    @Test
    fun `对象数组生成 Item 类型`() {
        val ts = generator.generate(
            "Root",
            """{"items":[{"id":1,"name":"a"},{"id":2,"name":"b"}]}"""
        )
        assertTrue(ts.contains("export type Item = {"), ts)
        assertTrue(ts.contains("  id: number;"), ts)
        assertTrue(ts.contains("  name: string;"), ts)
        assertTrue(ts.contains("  items: Item[];"), ts)
    }

    @Test
    fun `null 值按 key 推断类型`() {
        val ts = generator.generate(
            "Root",
            """{"userId":null,"userName":null,"isDeleted":null,"createTime":null,"remark":null,"price":null,"widget":null}"""
        )
        assertTrue(ts.contains("  userId: number;"), ts)
        assertTrue(ts.contains("  userName: string;"), ts)
        assertTrue(ts.contains("  isDeleted: boolean;"), ts)
        assertTrue(ts.contains("  createTime: string;"), ts)
        assertTrue(ts.contains("  remark: string;"), ts)
        assertTrue(ts.contains("  price: number;"), ts)
        // 无法推断的 key 保持 null
        assertTrue(ts.contains("  widget: null;"), ts)
    }

    @Test
    fun `空数组按 key 推断元素类型`() {
        val ts = generator.generate(
            "Root",
            """{"tags":[],"ids":[],"list":[]}"""
        )
        assertTrue(ts.contains("  tags: string[];"), ts)
        assertTrue(ts.contains("  ids: number[];"), ts)
        assertTrue(ts.contains("  list: unknown[];"), ts)
    }

    @Test
    fun `原始类型数组`() {
        val ts = generator.generate(
            "Root",
            """{"nums":[1,2,3],"names":["a","b"]}"""
        )
        assertTrue(ts.contains("  nums: number[];"), ts)
        assertTrue(ts.contains("  names: string[];"), ts)
    }

    @Test
    fun `混合原始类型数组生成联合类型`() {
        val ts = generator.generate(
            "Root",
            """{"vals":[1,"a",true]}"""
        )
        assertTrue(ts.contains("  vals: (number | string | boolean)[];"), ts)
    }

    @Test
    fun `数组内缺失字段标记为可选`() {
        val ts = generator.generate("Root", """[{"a":1},{"b":2}]""")
        // 顶层数组：Root 应为数组类型，元素类型为 RootItem
        assertTrue(ts.contains("export type Root = RootItem[];"), ts)
        assertTrue(ts.contains("export type RootItem = {"), ts)
        assertTrue(ts.contains("  a?: number;"), ts)
        assertTrue(ts.contains("  b?: number;"), ts)
    }

    @Test
    fun `对象数组中出现 null 字段按 key 推断`() {
        val ts = generator.generate(
            "Root",
            """{"items":[{"id":1},{"id":null}]}"""
        )
        // 同字段既有具体类型（number）又有 null：null 不再按 key 猜测成可能冲突的类型，
        // 而是作为 | null 附加，体现字段可空语义
        assertTrue(ts.contains("  id: number | null;"), ts)
    }

    @Test
    fun `集合词后缀字段生成短元素类型名`() {
        val ts = generator.generate(
            "Root",
            """{"itemList":[{"id":1,"name":"a"}],"apples":["fuji"],"categories":[{"id":1}]}"""
        )
        // 集合词剥离：itemList -> Item，而不是 ItemListItem
        assertTrue(ts.contains("export type Item = {"), ts)
        assertTrue(ts.contains("  itemList: Item[];"), ts)
        // 常规复数：apples -> string[]（原始数组不建命名类型）、categories -> Category（ies -> y）
        assertTrue(ts.contains("  apples: string[];"), ts)
        assertTrue(ts.contains("export type Category = {"), ts)
        assertTrue(ts.contains("  categories: Category[];"), ts)
    }

    @Test
    fun `原始数组中的 null 元素使整个字段可空`() {
        val ts = generator.generate(
            "Root",
            """{"nums":[1,null,3]}"""
        )
        assertTrue(ts.contains("  nums: number[] | null;"), ts)
    }

    @Test
    fun `非法键名与保留字加引号`() {
        val ts = generator.generate(
            "Root",
            """{"user-name":"x","class":1,"data.type":"y","123":"z","delete":true}"""
        )
        assertTrue(ts.contains("  \"user-name\": string;"), ts)
        assertTrue(ts.contains("  \"class\": number;"), ts)
        assertTrue(ts.contains("  \"data.type\": string;"), ts)
        assertTrue(ts.contains("  \"123\": string;"), ts)
        assertTrue(ts.contains("  \"delete\": boolean;"), ts)
    }

    @Test
    fun `snake_case 嵌套对象生成驼峰类型名`() {
        val ts = generator.generate(
            "Root",
            """{"user_info":{"id":1,"name":"x"}}"""
        )
        assertTrue(ts.contains("export type UserInfo = {"), ts)
        assertTrue(ts.contains("  user_info: UserInfo;"), ts)
    }

    @Test
    fun `宽松 JSON 可以转换`() {
        val ts = generator.generate("Root", """{a:1, foo_bar:2}""")
        assertTrue(ts.contains("  a: number;"), ts)
        assertTrue(ts.contains("  foo_bar: number;"), ts)
    }

    @Test
    fun `二维数组生成嵌套数组类型`() {
        val ts = generator.generate(
            "Root",
            """{"matrix":[[1,2],[3,4]]}"""
        )
        assertTrue(ts.contains("  matrix: number[][];"), ts)
    }

    @Test
    fun `顶层数组直接生成 Root 数组类型，元素为 RootItem`() {
        val ts = generator.generate("Root", """[{"id":1,"name":"x"}]""")
        // 修复后：顶层数组 Root 必须是数组类型，元素类型以 Item 后缀命名
        assertTrue(ts.contains("export type Root = RootItem[];"), ts)
        assertTrue(ts.contains("export type RootItem = {"), ts)
        assertTrue(ts.contains("  id: number;"), ts)
        assertTrue(ts.contains("  name: string;"), ts)
    }

    @Test
    fun `数组内对象字段合并去重`() {
        val ts = generator.generate("Root", """[{"id":1},{"id":2,"name":"x"}]""")
        // 顶层数组：Root = RootItem[]
        assertTrue(ts.contains("export type Root = RootItem[];"), ts)
        assertTrue(ts.contains("  id: number;"), ts)
        assertTrue(ts.contains("  name?: string;"), ts)
    }

    @Test
    fun `数组同字段空数组与具体类型统一为具体类型`() {
        // 同一字段在数组不同元素中：空数组 [] -> unknown[] 与 [{b:456}] -> Child[]，
        // 应统一为 Child[]，而非产生 unknown[] | Child[] 无意义联合
        val ts = generator.generate(
            "Root",
            """[{"a":123,"children":[]},{"a":123,"children":[{"b":456}]}]"""
        )
        assertTrue(ts.contains("export type Root = RootItem[];"), ts)
        assertTrue(ts.contains("export type Child = {"), ts)
        assertTrue(ts.contains("  b: number;"), ts)
        assertTrue(ts.contains("  children: Child[];"), ts)
        assertFalse(ts.contains("unknown[]"), ts)
    }

    @Test
    fun `数组同字段空数组在后仍统一为具体类型`() {
        // 顺序倒置：具体类型在前、空数组在后，同样应统一为 Child[]
        val ts = generator.generate(
            "Root",
            """[{"a":1,"children":[{"b":2}]},{"a":3,"children":[]}]"""
        )
        assertTrue(ts.contains("export type Root = RootItem[];"), ts)
        assertTrue(ts.contains("  children: Child[];"), ts)
        assertFalse(ts.contains("unknown[]"), ts)
    }

    @Test
    fun `数组对象同字段数组与 null 合并为可空具体数组`() {
        // selectedLabelList 在第一个元素是 [{a:123}] -> SelectedLabel[]，
        // 在第二个元素是 null。null 不应按 key 猜成 string 与 SelectedLabel[] 联合，
        // 而应作为 | null 附加，最终为 SelectedLabel[] | null
        val ts = generator.generate(
            "Root",
            """[{"selectedLabelList":[{"a":123}]},{"selectedLabelList":null}]"""
        )
        // 修复后：顶层数组 -> Root = RootItem[]；元素类型名不再占用 Root
        assertTrue(ts.contains("export type Root = RootItem[];"), ts)
        assertTrue(ts.contains("export type RootItem = {"), ts)
        assertTrue(ts.contains("export type SelectedLabel = {"), ts)
        assertTrue(ts.contains("  a: number;"), ts)
        assertTrue(ts.contains("  selectedLabelList: SelectedLabel[] | null;"), ts)
        // 不应出现按 key 误猜的 string
        assertFalse(ts.contains("string"), ts)
    }

    @Test
    fun `顶层基本类型数组生成 RootItem 数组类型`() {
        val ts = generator.generate("Root", """[1,2,3]""")
        assertTrue(ts.contains("export type Root = number[];"), ts)
    }

    @Test
    fun `顶层混合基本类型数组加括号`() {
        val ts = generator.generate("Root", """[1,"a",true]""")
        assertTrue(ts.contains("export type Root = (number | string | boolean)[];"), ts)
    }

    @Test
    fun `顶层空数组为 unknown 数组`() {
        val ts = generator.generate("Root", """[]""")
        assertTrue(ts.contains("export type Root = unknown[];"), ts)
    }

    @Test
    fun `非对象数组空数组元素与具体类型统一`() {
        // 非对象数组分支：[1, []] -> number | unknown[]，应统一为 number（inline 字段类型）
        val ts = generator.generate("Root", """{"vals":[1,[]]}""")
        assertTrue(ts.contains("  vals: number[];"), ts)
        assertFalse(ts.contains("unknown[]"), ts)
    }

    @Test
    fun `数字与布尔混合字段`() {
        val ts = generator.generate(
            "Root",
            """{"pi":3.14,"ok":true,"big":9223372036854775807}"""
        )
        assertTrue(ts.contains("  pi: number;"), ts)
        assertTrue(ts.contains("  ok: boolean;"), ts)
        assertTrue(ts.contains("  big: number;"), ts)
    }

    @Test
    fun `订单综合场景 null 推断`() {
        val ts = generator.generate(
            "Root",
            """
            {
              "orderNo": null,
              "cardNumber": null,
              "idNumber": null,
              "userId": null,
              "totalAmount": null,
              "discountAmount": null,
              "status": null,
              "remark": null,
              "payTime": null,
              "isPaid": null,
              "items": [
                {"skuId": null, "skuName": null, "quantity": null, "unitPrice": null, "isGift": null}
              ]
            }
            """.trimIndent()
        )
        // 编号类：string 而非 number
        assertTrue(ts.contains("  orderNo: string;"), ts)
        assertTrue(ts.contains("  cardNumber: string;"), ts)
        assertTrue(ts.contains("  idNumber: string;"), ts)
        // 普通 id / 金额 / 数量
        assertTrue(ts.contains("  userId: number;"), ts)
        assertTrue(ts.contains("  totalAmount: number;"), ts)
        assertTrue(ts.contains("  discountAmount: number;"), ts)
        // 状态 / 文本 / 时间 / 布尔
        assertTrue(ts.contains("  status: string;"), ts)
        assertTrue(ts.contains("  remark: string;"), ts)
        assertTrue(ts.contains("  payTime: string;"), ts)
        assertTrue(ts.contains("  isPaid: boolean;"), ts)
        // 数组元素
        assertTrue(ts.contains("  skuId: number;"), ts)
        assertTrue(ts.contains("  skuName: string;"), ts)
        assertTrue(ts.contains("  quantity: number;"), ts)
        assertTrue(ts.contains("  unitPrice: number;"), ts)
        assertTrue(ts.contains("  isGift: boolean;"), ts)
    }

    @Test
    fun `用户资料综合场景`() {
        val ts = generator.generate(
            "Root",
            """
            {
              "profile": {
                "nickname": null,
                "avatarUrl": null,
                "birthday": null,
                "gender": null,
                "phoneNumber": null,
                "email": null,
                "city": null,
                "vipLevel": null,
                "isVerified": null
              }
            }
            """.trimIndent()
        )
        assertTrue(ts.contains("export type Profile = {"), ts)
        assertTrue(ts.contains("  nickname: string;"), ts)
        assertTrue(ts.contains("  avatarUrl: string;"), ts)
        assertTrue(ts.contains("  birthday: string;"), ts)
        assertTrue(ts.contains("  gender: string;"), ts)
        assertTrue(ts.contains("  phoneNumber: string;"), ts)
        assertTrue(ts.contains("  email: string;"), ts)
        assertTrue(ts.contains("  city: string;"), ts)
        assertTrue(ts.contains("  vipLevel: number;"), ts)
        assertTrue(ts.contains("  isVerified: boolean;"), ts)
        assertTrue(ts.contains("  profile: Profile;"), ts)
    }

    @Test
    fun `深层嵌套对象`() {
        val ts = generator.generate(
            "Root",
            """{"a":{"b":{"c":{"d":null,"name":null}}}}"""
        )
        assertTrue(ts.contains("export type A = {"), ts)
        assertTrue(ts.contains("export type B = {"), ts)
        assertTrue(ts.contains("export type C = {"), ts)
        assertTrue(ts.contains("  d: null;"), ts)
        assertTrue(ts.contains("  name: string;"), ts)
    }

    @Test
    fun `空对象与空数组根`() {
        val ts = generator.generate("Root", """{}""")
        assertTrue(ts.contains("export type Root = {\n}"), ts)

        val ts2 = generator.generate("Root", """[]""")
        assertTrue(ts2.contains("export type Root = unknown[]"), ts2)
    }

    @Test
    fun `数组对象中 null 与缺失字段推断`() {
        val ts = generator.generate(
            "Root",
            """{"list":[{"id":null,"name":null},{"id":2,"remark":null}]}"""
        )
        // id 在两个元素都出现：第一个 null、第二个 number -> number | null
        assertTrue(ts.contains("  id: number | null;"), ts)
        // name / remark 只在单个元素出现且为 null -> 可选，按 key 猜测为 string
        assertTrue(ts.contains("  name?: string;"), ts)
        assertTrue(ts.contains("  remark?: string;"), ts)
    }

    @Test
    fun `结构相同的两个对象复用同一类型（鸭子类型）`() {
        val ts = generator.generate(
            "Root",
            """{"user":{"id":1,"name":"a"},"owner":{"id":2,"name":"b"}}"""
        )
        // user 先定义，owner 结构相同应复用 User，而不是新建 Owner
        assertTrue(ts.contains("export type User = {"), ts)
        assertTrue(ts.contains("  user: User;"), ts)
        assertTrue(ts.contains("  owner: User;"), ts)
        // 不应再定义 Owner 类型
        assertFalse(ts.contains("export type Owner = {"), ts)
    }

    @Test
    fun `对象与数组元素结构相同复用类型（鸭子类型）`() {
        val ts = generator.generate(
            "Root",
            """{"profile":{"id":1,"name":"a"},"members":[{"id":2,"name":"b"}]}"""
        )
        // members 的元素结构与 profile 相同，应复用 Profile，而不是新建 Member
        assertTrue(ts.contains("export type Profile = {"), ts)
        assertTrue(ts.contains("  profile: Profile;"), ts)
        assertTrue(ts.contains("  members: Profile[];"), ts)
        assertFalse(ts.contains("export type Member = {"), ts)
    }

    @Test
    fun `商品列表场景`() {
        val ts = generator.generate(
            "Root",
            """
            {
              "goods": [
                {"goodsId": null, "goodsName": null, "price": null, "stock": null, "onSale": null, "tags": []}
              ]
            }
            """.trimIndent()
        )
        assertTrue(ts.contains("  goodsId: number;"), ts)
        assertTrue(ts.contains("  goodsName: string;"), ts)
        assertTrue(ts.contains("  price: number;"), ts)
        assertTrue(ts.contains("  stock: number;"), ts)
        assertTrue(ts.contains("  onSale: boolean;"), ts)
        assertTrue(ts.contains("  tags: string[];"), ts)
    }

    @Test
    fun `数组含 null 元素使整个字段可空但不按 key 猜测为 boolean`() {
        val ts = generator.generate(
            "Root",
            """{"selectedLabelList":[{"id":1,"name":"a"},null,{"id":2,"name":"b"}]}"""
        )
        // 类型名来自对象结构（SelectedLabel），而不是按 key 猜测成 boolean / null
        assertTrue(ts.contains("export type SelectedLabel = {"), ts)
        assertTrue(ts.contains("  id: number;"), ts)
        assertTrue(ts.contains("  name: string;"), ts)
        // null 元素使整个字段可空：SelectedLabel[] | null（元素本身仍是干净的 SelectedLabel）
        val line = ts.lines().first { it.contains("selectedLabelList") }
        assertTrue(line.contains("selectedLabelList: SelectedLabel[] | null;"), ts)
        // 但仍不应按 selected 关键字把元素猜测成 boolean
        assertFalse(line.contains("boolean"), ts)
    }

    @Test
    fun `数字开头的 key 生成合法类型名加 I 前缀`() {
        val ts = generator.generate(
            "Root",
            """{"2fa":{"code":1,"name":"x"},"123abc":[{"v":1}]}"""
        )
        // 数字开头的类型名非法，自动加 I 前缀：2fa -> I2fa
        assertTrue(ts.contains("export type I2fa = {"), ts)
        assertTrue(ts.contains("  code: number;"), ts)
        assertTrue(ts.contains("  name: string;"), ts)
        assertTrue(ts.contains("  \"2fa\": I2fa;"), ts)
        // 数组 key 123abc：元素类型来自 singularize（补 Item），同样是合法名 I123abcItem
        assertTrue(ts.contains("export type I123abcItem = {"), ts)
        assertTrue(ts.contains("  \"123abc\": I123abcItem[];"), ts)
    }

}
