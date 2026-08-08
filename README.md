# Json2Typescript

A lightweight IntelliJ / WebStorm plugin that converts copied JSON into TypeScript type definitions with one click.

---

## Features

- **One-click conversion**: turn JSON into TypeScript types directly from the editor's right-click menu.
- **Nested objects & arrays**: automatically generates an independent `export type` for each level of nested objects.
- **Union type inference**: mixed-type arrays produce `(number | string | boolean)[]` form (parenthesized to avoid ambiguity).
- **Optional fields auto-marked**: fields missing from some array elements are automatically marked with `?:`.
- **null value type inference by key**: when a JSON value is `null`, the type is guessed from the field name (see table below); if it cannot be guessed, it stays `null`.
- **Plural / collection-word stripping**: array field names produce the "shortest" element type name, e.g. `itemList → Item`, `apples → Apple`, `categories → Category`.
- **TS reserved-word quoting**: reserved words such as `class` / `default` / `interface` / `type` used as keys are automatically quoted (`"class"`).
- **Empty array element type by key**: `tags: [] → string[]`, `ids: [] → number[]`, `list: [] → unknown[]`.
- **Structural deduplication (duck typing)**: objects with identical structure generate only one type definition — whether they come from a nested object or an array element, as long as field names and types match, the same type is reused, with the first-defined name winning, avoiding duplicate types.

---

## Type Inference Rules

### 1. null value inference by key (`TypeGuesser`)

| Field name example | Inferred type | Basis |
|--------------------|---------------|-------|
| `userId` / `orderId` | `number` | contains `id` marker |
| `userName` / `remark` | `string` | textual semantics |
| `isDeleted` / `hasChild` | `boolean` | `is` / `has` prefix |
| `createTime` / `birthday` | `string` | date/time semantics |
| `price` / `count` | `number` | numeric semantics |
| `phone` / `uuid` / `email` | `string` | strong string markers (override number) |
| `orderNo` / `cardNumber` | `string` | number-like codes (not number) |
| `unknownField` | `null` | unrecognized, kept as null |

Supports `camelCase` / `snake_case` / `kebab-case` tokenization, plus simple plural normalization (`names → name`).

### 2. Plural / collection-word stripping (`NameUtils`)

Goal: keep array element type names as **short** as possible:

| Field name | Element type | Note |
|------------|--------------|------|
| `itemList` | `Item` | strips collection word `List` |
| `userList` / `userSet` | `User` | camelCase collection suffix |
| `apples` | `Apple` | regular `-s` removal |
| `categories` | `Category` | `ies → y` |
| `boxes` / `watches` | `Box` / `Watch` | `xes/ches/shes → x/ch/sh` |
| `item_list` | `Item` | snake last token is collection word |
| `order_items_list` | `OrderItem` | multi-level stripping |

> Collection words (`list` / `set` / `array` / `collection`) are stripped only when they form a standalone word, to avoid mis-stripping embedded words like `scientist` or `playlist`.

### 3. Reserved-word quoting (`TsKeyUtils`)

When a field name is a TS / JS reserved word, it is automatically quoted: `class` → `"class"`, `default` → `"default"`, etc.

### 4. Lenient JSON parsing (`JsonParser`)

`JsonParser` uses Jackson's streaming lenient mode — single quotes, unquoted field names, `//` and `/* */` comments, trailing commas, and leading zeros for numbers are all accepted with essentially zero extra overhead (no pre-scan of the input). JSON5-only syntax — hexadecimal numbers, leading/trailing decimal points, explicit plus signs, `Infinity` / `NaN` / `undefined` literals, `#` comments, and multiline string continuation — is **no longer supported**.

### 5. Structural deduplication (duck typing, `JsonToTsGenerator`)

A "structure signature → type name" mapping is maintained during conversion: before generating each object (nested object or array element), its structure signature is computed (field names and types, sorted by field name — order independent). If the signature already exists, the existing type name is reused; otherwise a new type is registered under the currently derived type name.

Example:

```json
{
  "user":    { "id": 1, "name": "a" },
  "owner":   { "id": 2, "name": "b" },
  "members": [ { "id": 3, "name": "c" } ]
}
```

```ts
export type Root = {
  user: User;
  owner: User;          // same structure as user, reuses User
  members: User[];      // array element has the same structure, reuses User
};

export type User = {
  id: number;
  name: string;
};
```

> Reuse rule: the first-defined name wins (`user` appears first → type name `User`). `members`' element type reuses `User` when its structure matches, instead of generating duplicate types like `Member` / `Owner`.

---

## Usage Example

Input JSON:

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

Output TypeScript:

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

## Build & Test

Requirements: JDK 21, Gradle (project ships with the wrapper). Plugin target: WebStorm 2025.3, `sinceBuild = 251`.

```bash
# Build the plugin
./gradlew build

# Run tests
./gradlew test --no-configuration-cache
```

> Note: the local configuration-cache lock file is occasionally held, so `--no-configuration-cache` is required when running tests.

Tests use JUnit 5 (pure JVM, no IntelliJ test framework dependency) and cover four modules: `TypeGuesser`, `NameUtils`, `TsKeyUtils`, `JsonToTsGenerator`. All currently pass.

---

## License

See the repository LICENSE file (if any).
