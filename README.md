# Json2Typescript

一个轻量级的 IntelliJ / WebStorm 插件：一键把复制的 JSON 数据转换成 TypeScript 类型定义。

> Json2Typescript is a lightweight IntelliJ / WebStorm plugin that converts copied JSON into TypeScript type definitions with one click.

---

## 功能特性

- **一键转换**：在编辑器右键菜单中直接把 JSON 转为 TypeScript 类型。
- **嵌套对象与数组**：自动为每一层嵌套对象生成独立的 `export type`。
- **联合类型推断**：混合类型数组会生成 `(number | string | boolean)[]` 形式（带括号避免歧义）。
- **可选字段自动标记**：数组中部分元素缺失的字段自动标记为 `?:`。
- **null 值按字段名推断类型**：JSON 里值为 `null` 时，根据字段名猜测类型（见下表），猜不到则保持 `null`。
- **复数 / 集合词剥离**：数组字段名生成"最短"的元素类型名，例如 `itemList → Item`、`apples → Apple`、`categories → Category`。
- **JSON5 解析**：在宽松 JSON 基础上完整支持 JSON5——十六进制数字（`0xFF`）、前导/尾随小数点（`.5` / `5.`）、显式正号（`+5`）、`Infinity` / `NaN` 字面量、`undefined` 值、多行字符串（行继续）、`#` 行注释，以及单引号、未加引号 key、尾逗号、`//` 与 `/* */` 注释等非标准写法。
- **TS 保留字加引号**：`class` / `default` / `interface` / `type` 等保留字作为 key 时自动加引号（`"class"`）。
- **空数组按 key 猜测元素类型**：`tags: [] → string[]`、`ids: [] → number[]`、`list: [] → unknown[]`。
- **结构去重（鸭子类型）**：结构相同的对象只生成一个类型定义——无论它来自嵌套对象还是数组元素，只要字段名与类型一致就复用同一类型，先定义者胜出类型名，避免重复类型。

---

## 类型推断规则

### 1. null 值按 key 推断（`TypeGuesser`）

| 字段名示例 | 推断类型 | 依据 |
|-----------|---------|------|
| `userId` / `orderId` | `number` | 含 `id` 标识 |
| `userName` / `remark` | `string` | 文本类语义 |
| `isDeleted` / `hasChild` | `boolean` | `is` / `has` 前缀 |
| `createTime` / `birthday` | `string` | 日期时间语义 |
| `price` / `count` | `number` | 数值语义 |
| `phone` / `uuid` / `email` | `string` | 强字符串标记（优先于数字） |
| `orderNo` / `cardNumber` | `string` | 编号类（不是 number） |
| `unknownField` | `null` | 无法识别，保持 null |

支持 `camelCase` / `snake_case` / `kebab-case` 分词，并做简单复数归一（`names → name`）。

### 2. 复数 / 集合词剥离（`NameUtils`）

目标是让数组元素的类型名**尽可能短**：

| 字段名 | 元素类型 | 说明 |
|--------|---------|------|
| `itemList` | `Item` | 剥离集合词 `List` |
| `userList` / `userSet` | `User` | camelCase 集合词后缀 |
| `apples` | `Apple` | 常规去 `s` |
| `categories` | `Category` | `ies → y` |
| `boxes` / `watches` | `Box` / `Watch` | `xes/ches/shes → x/ch/sh` |
| `item_list` | `Item` | snake 末 token 是集合词 |
| `order_items_list` | `OrderItem` | 多层剥离 |

> 集合词（`list`/`set`/`array`/`collection`）仅当它是独立单词时才剥离，避免误伤 `scientist`、`playlist` 等内嵌词。

### 3. 保留字加引号（`TsKeyUtils`）

字段名为 TS / JS 保留字时自动加双引号：`class` → `"class"`、`default` → `"default"` 等。

### 4. JSON5 支持（`JsonParser`）

`JsonParser` 在交给 Jackson 之前会先做一层「JSON5 感知」的归一化（字符串 / 注释感知，逐字符扫描），把 Jackson 原生不支持的写法翻译成标准 JSON：

| JSON5 写法 | 说明 | 归一化结果 / 类型 |
|-----------|------|------------------|
| `{a: 1, 'b': 2}` | 未加引号 / 单引号 key | 标准 JSON key |
| `0xFF` / `0x1F` | 十六进制数字 | `255` / `31`（number） |
| `.5` / `.25` | 前导小数点 | `0.5` / `0.25`（number） |
| `5.` / `10.` | 尾随小数点 | `5.0` / `10.0`（number） |
| `+5` / `+3.14` | 显式正号 | `5` / `3.14`（number） |
| `Infinity` / `-Infinity` | 无穷字面量 | `number` 类型 |
| `NaN` | 非数字面量 | `number` 类型（值占位为 `0`） |
| `undefined` | 未定义值 | 视为 `null`，按 key 推断类型 |
| `"a\<换行>b"` | 多行字符串（行继续） | `"ab"` |
| `//` `#` `/* */` | 行 / 块注释 | 解析时去除 |

> 标识符片段（如 `InfinityKey`）不会被误当作关键字替换；数值 / 关键字后紧跟单词字符时也不会被错误转换。

### 5. 结构去重（鸭子类型，`JsonToTsGenerator`）

转换时维护一个「结构签名 → 类型名」的映射：每个对象（嵌套对象或数组元素对象）在生成前先计算其结构签名（字段名与类型，按字段名排序、与字段顺序无关）。若签名已存在，则直接复用已有类型名；否则以当前推导出的类型名注册新类型。

示例：

```json
{
  "user":   { "id": 1, "name": "a" },
  "owner":  { "id": 2, "name": "b" },
  "members": [ { "id": 3, "name": "c" } ]
}
```

```ts
export type Root = {
  user: User;
  owner: User;          // 与 user 结构相同，复用 User
  members: User[];      // 数组元素结构也相同，复用 User
};

export type User = {
  id: number;
  name: string;
};
```

> 复用规则：先定义者胜出类型名（`user` 先出现 → 类型名 `User`）。`members` 的元素类型与 `profile` 等结构相同则统一复用，不再生成 `Member` / `Owner` 等重复类型。

---

## 使用示例

输入 JSON：

```json
{
  "userId": 1,
  "userName": "John",
  "isDeleted": null,
  "createTime": null,
  "price": 9.5,
  "itemList": [{ "id": 1, "name": "a" }],
  "tags": [],
  "ids": [],
  "unknownField": null
}
```

输出 TypeScript：

```ts
export type Root = {
  userId: number;
  userName: string;
  isDeleted: boolean;
  createTime: string;
  price: number;
  itemList: Item[];
  tags: string[];
  ids: number[];
  unknownField: null;
};

export type Item = {
  id: number;
  name: string;
};
```

---

## 构建与测试

环境要求：JDK 21、Gradle（项目自带 wrapper）。插件目标：WebStorm 2025.3，`sinceBuild = 251`。

```bash
# 构建插件
./gradlew build

# 运行测试
./gradlew test --no-configuration-cache
```

> 说明：本机配置缓存锁文件偶发被占用，运行测试需加 `--no-configuration-cache`。

测试基于 JUnit 5（纯 JVM 单元，不依赖 IntelliJ 测试框架），覆盖 `TypeGuesser`、`NameUtils`、`TsKeyUtils`、`JsonToTsGenerator`、`JsonParser` 五个模块，当前全部通过。

---

## 许可证

见仓库 LICENSE 文件（如有）。
