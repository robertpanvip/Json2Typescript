#!/usr/bin/env python3
"""
Simulate JsonToTsGenerator.generate() to see what it should output.
Closely follows the Kotlin logic in JsonToTsGenerator.kt.
"""
import json
import re
from collections import OrderedDict

# ============================================================
# Copy of TypeGuesser logic
# ============================================================
NUMBER_WORDS = {
    "id", "ids", "count", "amount", "total", "price", "num", "number",
    "page", "size", "limit", "offset", "level", "sort", "score",
    "age", "year", "quantity", "stock", "height", "width", "weight",
    "grade", "rank", "index", "pid", "uid",
}

STRING_WORDS = {
    "name", "title", "label", "content", "desc", "description", "remark",
    "note", "comment", "message", "info", "detail", "summary", "intro",
    "url", "uri", "link", "href", "path", "address", "avatar", "icon",
    "image", "img", "logo", "video", "audio", "color", "status", "type",
    "category", "tag", "tags", "code", "no", "no_", "number_", "number2",
    "password", "token", "session", "key", "secret", "signature", "mobile",
    "phone", "email", "mail", "time", "date", "datetime", "create_time",
    "createtime", "createat", "created_at", "createAt", "createdAt",
    "update_time", "updatetime", "updateat", "updatedAt", "updated_at",
    "delete_time", "deletetime", "deleteat", "deletedAt", "deleted_at",
    "pay_time", "paytime", "username", "nickname", "city", "province",
    "country", "address", "birthday", "gender", "cardnumber", "card_number",
    "orderno", "order_no", "idnumber", "id_number",
}

BOOLEAN_WORDS = {
    "is", "has", "can", "need", "should", "must", "enable", "enabled",
    "disable", "disabled", "active", "deleted", "paid", "done", "finished",
    "success", "flag", "allow", "allow_", "visible", "public", "private",
    "locked", "readonly", "required", "selected",
}

def guess(key: str):
    if key is None:
        return None
    lower = key.lower()
    stripped = re.sub(r"[^a-z0-9]+", " ", lower).strip()
    tokens = stripped.split() if stripped else []

    # exact match BOOLEAN_WORDS (full key)
    if lower in BOOLEAN_WORDS:
        return "boolean"

    # token / suffix match BOOLEAN_WORDS (isVip, hasPaid, selected, selectedLabelList)
    for w in BOOLEAN_WORDS:
        if (lower == w or lower.endswith("_" + w) or
                any(t == w for t in tokens) or
                (w.islower() and w.isalpha() and lower.endswith(w) and
                 len(lower) > len(w) and not lower[-len(w)-1].isalpha())):
            pass  # handled below for "isXxx/hasXxx" camelCase patterns

    # camelCase / PascalCase BOOLEAN prefix: isActive, hasPermission, CanEdit...
    for w in ["is", "Is", "has", "Has", "can", "Can", "need", "Need",
              "should", "Should", "must", "Must"]:
        if lower.startswith(w.lower()) and len(lower) > len(w):
            rest = lower[len(w):]
            if rest and rest[0].isalpha() and (rest[0].islower() or rest[0].isupper()):
                return "boolean"

    # exact boolean
    if any(t in BOOLEAN_WORDS for t in tokens):
        return "boolean"

    # id / ids -> number only when the exact word is id/ids
    # but idnumber, orderNo... -> string
    if lower in {"id", "ids"}:
        return "number"
    if (lower.endswith("_id") or lower.endswith("Id") or lower.endswith("_ids") or lower.endswith("Ids")
            or lower.endswith("Pid") or lower.endswith("_pid") or lower.endswith("Uid") or lower.endswith("_uid")):
        return "number"
    if any(t in {"userId", "userid", "skuId", "skuid", "goodsid", "goodsId",
                 "price", "amount", "total", "discountamount", "discountAmount",
                 "quantity", "stock", "unitprice", "unitPrice", "score", "age",
                 "viplevel", "vipLevel", "pi", "big"} for t in [lower]):
        return "number"

    for t in tokens:
        if t in NUMBER_WORDS:
            return "number"

    # orderNo, cardNumber, idNumber, phoneNumber -> string (编号类)
    if any(lower.endswith(suf) for suf in ["no", "number", "idnumber"]):
        return "string"

    for t in tokens:
        if t in STRING_WORDS:
            return "string"
    if lower in STRING_WORDS:
        return "string"

    return None


# ============================================================
# Copy of NameUtils logic
# ============================================================
SPECIAL_MAP = {
    "children": "Child", "people": "Person", "men": "Man", "women": "Woman",
    "data": "DataItem", "list": "ListItem",
    "status": "Status", "statuses": "Status",
    "address": "Address", "addresses": "Address",
    "class": "Class", "classes": "Class",
    "news": "News", "goods": "Good",
    "series": "Series", "species": "Species",
    "analysis": "Analysis", "analyses": "Analysis",
    "basis": "Basis", "bases": "Basis",
    "campus": "Campus",
    "business": "Business", "businesses": "Business",
    "process": "Process", "processes": "Process",
    "access": "Access", "progress": "Progress", "success": "Success",
    "focus": "Focus", "bonus": "Bonus", "canvas": "Canvas",
    "atlas": "Atlas", "alias": "Alias", "virus": "Virus",
    "minus": "Minus", "plus": "Plus",
    "bus": "Bus", "buses": "Bus",
    "gas": "Gas", "glass": "Glass", "glasses": "Glass",
    "chaos": "Chaos", "cosmos": "Cosmos", "ethos": "Ethos",
    "pathos": "Pathos", "octopus": "Octopus", "cactus": "Cactus",
    "apparatus": "Apparatus",
    "index": "Index", "indexes": "Index", "indices": "Index",
    "matrix": "Matrix", "matrices": "Matrix",
    "vertex": "Vertex", "vertices": "Vertex",
    "criterion": "Criterion", "criteria": "Criterion",
    "phenomenon": "Phenomenon", "phenomena": "Phenomenon",
    "thesis": "Thesis", "theses": "Thesis",
    "crisis": "Crisis", "crises": "Crisis",
    "diagnosis": "Diagnosis", "diagnoses": "Diagnosis",
    "hypothesis": "Hypothesis", "hypotheses": "Hypothesis",
    "mice": "Mouse", "feet": "Foot", "teeth": "Tooth",
    "geese": "Goose", "oxen": "Ox",
}
COLLECTION_WORDS = {"list", "set", "map", "array", "collection"}

def toTypeName(key):
    parts = re.split(r"[^a-zA-Z0-9]+", key)
    parts = [p for p in parts if p]
    raw = "".join(p[:1].upper() + p[1:] for p in parts)
    if raw and raw[0].isdigit():
        raw = "I" + raw
    return raw

def singularize(key, allowItemFallback=True):
    return _singularizeInternal(key, allowItemFallback)

def _singularizeInternal(key, allowItemFallback):
    lower = key.lower()
    if lower in SPECIAL_MAP:
        return SPECIAL_MAP[lower]
    for w in COLLECTION_WORDS:
        cap = w[:1].upper() + w[1:]
        if key.endswith(cap) and len(key) > len(cap):
            return _singularizeInternal(key[:-len(cap)], False)
    if re.search(r"[_\-]", key):
        idx = max(key.rfind("_"), key.rfind("-"))
        last = key[idx+1:].lower()
        if last in COLLECTION_WORDS and idx > 0:
            return _singularizeInternal(key[:idx], False)
    s = None
    if lower.endswith("ies") and len(lower) > 4:
        s = key[:-3] + "y"
    elif lower.endswith("sses"):
        s = key[:-2]
    elif lower.endswith(("xes", "ches", "shes")):
        s = key[:-2]
    elif lower.endswith("ses"):
        s = key[:-1]
    elif lower.endswith("s") and len(lower) > 1:
        s = key[:-1]
    if s is not None:
        return toTypeName(s)
    if allowItemFallback:
        return toTypeName(key) + "Item"
    return toTypeName(key)

def toTsKey(key):
    if re.match(r"^[A-Za-z_$][A-Za-z0-9_$]*$", key) and key not in {
        "break", "case", "catch", "class", "const", "continue", "debugger",
        "default", "delete", "do", "else", "export", "extends", "finally",
        "for", "function", "if", "import", "in", "instanceof", "new",
        "return", "super", "switch", "this", "throw", "try", "typeof",
        "var", "void", "while", "with", "yield", "enum", "implements",
        "interface", "let", "package", "private", "protected", "public",
        "static", "null", "true", "false"
    }:
        return key
    return '"' + key + '"'


# ============================================================
# Generator (mirrors JsonToTsGenerator)
# ============================================================
WILDCARD_TYPES = {"unknown", "unknown[]", "any"}

def resolveUnion(types):
    concrete = [t for t in types if t not in WILDCARD_TYPES]
    resolved = concrete if concrete else list(types)
    return " | ".join(resolved)


class Generator:
    def __init__(self):
        self.definitions = OrderedDict()
        self.structureToName = {}

    def generate(self, rootName, jsonStr):
        self.definitions.clear()
        self.structureToName.clear()
        root = json.loads(jsonStr)
        self.parseNode(rootName, root)
        return "\n\n".join(f"export type {k} = {v}" for k, v in self.definitions.items())

    def parseNode(self, typeName, node):
        if typeName in self.definitions:
            return typeName
        if isinstance(node, dict):
            return self.parseObject(typeName, node)
        if isinstance(node, list):
            itemTypeName = typeName + "Item"
            arrType = self.parseArray(itemTypeName, node, None)
            self.definitions.setdefault(typeName, arrType)
            return typeName
        else:
            p = self.getPrimitive(node, None)
            self.definitions.setdefault(typeName, p)
            return p

    def parseObject(self, typeName, node):
        fieldMap = OrderedDict()
        for k, v in node.items():
            if isinstance(v, dict):
                ftype = self.parseNode(toTypeName(k), v)
            elif isinstance(v, list):
                ftype = self.parseArray(singularize(k), v, k)
            else:
                ftype = self.getPrimitive(v, k)
            fieldMap[toTsKey(k)] = ftype

        body = "{\n"
        for tk, ft in fieldMap.items():
            body += f"  {tk}: {ft};\n"
        body += "}"
        return self.registerOrReuse(typeName, body, self.signatureOf(fieldMap))

    def inferArrayItemType(self, typeName, node, key):
        elements = list(node)
        if not elements:
            return "any"
        nonNull = [e for e in elements if e is not None]
        if not nonNull:
            return guess(key) or "any"

        # Non-object array (primitive / array mix, no null)
        if not all(isinstance(e, dict) for e in nonNull):
            types = [self.resolveType(typeName, e, key) for e in nonNull]
            return resolveUnion(set(types))

        # Object array: split concrete types vs null markers per field
        fieldConcreteTypes = {}
        fieldHasNull = {}
        fieldCount = {}

        for obj in nonNull:
            for fk, fv in obj.items():
                fieldCount[fk] = fieldCount.get(fk, 0) + 1
                if fv is None:
                    fieldHasNull[fk] = True
                else:
                    tname = singularize(fk)
                    ft = self.resolveType(tname, fv, fk)
                    fieldConcreteTypes.setdefault(fk, set()).add(ft)

        fieldMap = OrderedDict()
        sb = "{\n"
        for fk in list(fieldCount.keys()):
            optional = fieldCount[fk] != len(nonNull)
            optionalMark = "?" if optional else ""

            concrete = fieldConcreteTypes.get(fk, set())
            hasNull = fieldHasNull.get(fk, False)

            if not concrete:
                union = guess(fk) or "null"
            else:
                cu = resolveUnion(concrete)
                union = f"{cu} | null" if hasNull else cu

            tsKey = toTsKey(fk)
            fieldMap[tsKey] = union
            sb += f"  {tsKey}{optionalMark}: {union};\n"
        sb += "}"
        body = sb
        return self.registerOrReuse(typeName, body, self.signatureOf(fieldMap))

    def registerOrReuse(self, typeName, body, signature):
        if signature in self.structureToName:
            return self.structureToName[signature]
        self.structureToName[signature] = typeName
        self.definitions.setdefault(typeName, body)
        return typeName

    def signatureOf(self, fieldMap):
        return ";".join(f"{k}|{v}" for k, v in sorted(fieldMap.items()))

    def parseArray(self, typeName, node, key):
        hasNullElement = any(x is None for x in node)
        if not node:
            guessed = guess(key)
            arrType = f"{guessed}[]" if guessed else "unknown[]"
            return f"{arrType} | null" if hasNullElement else arrType
        itemType = self.inferArrayItemType(typeName, node, key)
        wrapped = f"({itemType})" if " | " in itemType else itemType
        arrType = f"{wrapped}[]"
        return f"{arrType} | null" if hasNullElement else arrType

    def resolveType(self, typeName, node, key):
        if isinstance(node, dict):
            self.parseNode(typeName, node)
            return typeName
        if isinstance(node, list):
            return self.parseArray(typeName, node, key)
        return self.getPrimitive(node, key)

    def getPrimitive(self, node, key):
        if isinstance(node, bool):
            return "boolean"
        if isinstance(node, int):
            return "number"
        if isinstance(node, float):
            return "number"
        if isinstance(node, str):
            return "string"
        if node is None:
            return guess(key) or "null"
        return "any"


# ============================================================
# Test scenarios from the failing tests
# ============================================================
if __name__ == "__main__":
    g = Generator()

    def run(name, root, json_str, *expected_substrings):
        print(f"\n=== {name} ===")
        out = g.generate(root, json_str)
        print(out)
        print()
        for exp in expected_substrings:
            ok = exp in out
            print(f"  OK? {ok}: expect({exp!r})")

    run("数组内缺失字段标记为可选", "Root",
        """[{"a":1},{"b":2}]""",
        "export type Root = RootItem[];",
        "export type RootItem = {",
        "  a?: number;",
        "  b?: number;")

    run("顶层数组直接生成 Root 数组类型，元素为 RootItem", "Root",
        """[{"id":1,"name":"x"}]""",
        "export type Root = RootItem[];",
        "export type RootItem = {",
        "  id: number;",
        "  name: string;")

    run("数组内对象字段合并去重", "Root",
        """[{"id":1},{"id":2,"name":"x"}]""",
        "export type Root = RootItem[];",
        "  id: number;",
        "  name?: string;")

    run("数组同字段空数组与具体类型统一为具体类型", "Root",
        """[{"a":123,"children":[]},{"a":123,"children":[{"b":456}]}]""",
        "export type Root = RootItem[];",
        "export type Child = {",
        "  b: number;",
        "  children: Child[];")

    run("数组同字段空数组在后仍统一为具体类型", "Root",
        """[{"a":1,"children":[{"b":2}]},{"a":3,"children":[]}]""",
        "export type Root = RootItem[];",
        "  children: Child[];")

    run("数组对象同字段数组与 null 合并为可空具体数组", "Root",
        """[{"selectedLabelList":[{"a":123}]},{"selectedLabelList":null}]""",
        "export type Root = RootItem[];",
        "export type RootItem = {",
        "export type SelectedLabel = {",
        "  a: number;",
        "  selectedLabelList: SelectedLabel[] | null;")

    run("顶层基本类型数组", "Root", "[1,2,3]",
        "export type Root = number[];")

    run("顶层混合基本类型数组", "Root", """[1,"a",true]""",
        "export type Root = (number | string | boolean)[];")

    run("顶层空数组", "Root", "[]",
        "export type Root = unknown[];")

    # Also print the "selectedLabel" guess result because this was the
    # original bug (selectedLabelList -> string via label token match)
    print()
    print(f"TypeGuesser.guess('selectedLabelList') = {guess('selectedLabelList')!r}")
    print(f"TypeGuesser.guess('id') = {guess('id')!r}")
    print(f"TypeGuesser.guess('name') = {guess('name')!r}")
    print(f"NameUtils.singularize('selectedLabelList') = {singularize('selectedLabelList')!r}")
    print(f"NameUtils.toTypeName('a') = {toTypeName('a')!r}")
    print(f"NameUtils.toTypeName('children') = {toTypeName('children')!r}")
    print(f"NameUtils.singularize('children') = {singularize('children')!r}")
