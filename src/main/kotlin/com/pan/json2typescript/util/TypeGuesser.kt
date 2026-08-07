package com.pan.json2typescript.util

/**
 * 根据字段名（key）猜测其 TypeScript 类型。
 *
 * 用于 JSON 中值为 null 的场景：null 本身无法推断类型，但键名通常能给出线索，
 * 例如 userId 通常是 number、userName 是 string、isDeleted 是 boolean。
 *
 * 猜测策略（按优先级从高到低）：
 *  1. 强字符串标记（phone / mobile / email / uuid / ip 等，优先于 number 猜测）
 *  2. 编号类标识符（cardNumber / orderNo / idNumber 是 string，不是 number）
 *  3. 数值限定词（maxValue / minValue / totalValue 是 number，不是 string）
 *  4. 布尔前缀（is / has / can / should / need / allow 开头）
 *  5. 末尾 token 最精确匹配（number > 日期时间 > boolean > string）
 *  6. contains 语义：key 中任意 token 命中即返回（用户诉求：key 含有 id 即 number）
 *  7. 全小写拼接兜底（userid / isactive / createtime 这类无法分词的 key）
 *
 * 支持 camelCase / PascalCase / snake_case / kebab-case。
 * 无法识别时返回 null，由调用方决定兜底类型。
 */
object TypeGuesser {

    /** 强字符串标记：出现即判定为 string，优先于数字猜测（如 phoneNumber 不应是 number） */
    private val STRING_OVERRIDES = setOf(
        "phone", "mobile", "tel", "fax", "email", "uuid", "guid", "idcard",
        "ip", "mac", "imei", "imsi", "iccid", "sn", "isbn"
    )

    private val NUMBER_WORDS = setOf(
        // 标识 / 序号
        "id", "uid", "no", "num", "number", "index", "idx", "seq", "port",
        // 数量
        "count", "cnt", "quantity", "qty", "amount", "amt", "sum", "total", "subtotal",
        "totalcount", "totalitems", "totalpages", "currentpage", "pagenum", "perpage",
        "page", "pages", "limit", "offset", "size", "numberof", "length", "depth",
        // 金额 / 财务
        "price", "cost", "fee", "salary", "balance", "budget", "deposit", "interest",
        "principal", "debt", "credit", "debit", "tax", "discount", "profit", "revenue",
        "income", "expense", "expenditure", "margin", "installment", "dividend",
        "points", "integral", "credits", "coins", "energy", "exp", "hp", "mp", "mana",
        // 统计
        "score", "rating", "rate", "percent", "percentage", "ratio", "factor",
        "proportion", "avg", "average", "max", "min", "maxvalue", "minvalue",
        "median", "deviation", "variance", "coefficient", "rank", "priority",
        "progress", "level", "tier", "star", "stars", "grade", "weight", "mass",
        // 尺寸 / 空间
        "width", "height", "length", "distance", "area", "volume", "capacity",
        "radius", "diameter", "perimeter", "circumference", "padding", "gap",
        "margin", "spacing", "fontsize", "lineheight", "letterspacing", "opacity",
        "zindex", "borderwidth", "altitude", "depth",
        // 物理量
        "temperature", "humidity", "pressure", "density", "viscosity", "wavelength",
        "voltage", "current", "resistance", "capacitance", "inductance", "wattage",
        "power", "cores", "threads", "processors", "ram", "rom", "memory", "storage",
        "battery", "speed", "velocity", "acceleration", "frequency", "duration",
        "interval", "week", "quarter", "floor", "timestamp", "times", "timeout",
        // 其他
        "order", "sort", "stock", "inventory", "capacity", "longitude", "latitude",
        "lat", "lng", "lon", "steps", "version", "dayofweek", "dayofmonth",
        "monthofyear", "yearofbirth"
    )

    private val DATE_WORDS = setOf(
        "date", "datetime", "time", "at", "year", "month", "day", "hour", "minute",
        "second", "birthday", "anniversary"
    )

    private val BOOLEAN_WORDS = setOf(
        // 状态 / 标志
        "flag", "enabled", "disabled", "active", "activated", "inactive", "valid",
        "invalid", "checked", "unchecked", "selected", "unselected", "deleted",
        "published", "unpublished",
        "verified", "unverified", "confirmed", "unconfirmed", "completed", "uncompleted",
        "finished", "unfinished", "success", "succeeded", "failed", "visible",
        "invisible", "hidden", "shown", "exists", "available", "unavailable",
        "locked", "unlocked", "online", "offline", "readonly", "editable", "expandable",
        "expanded", "collapsed", "loaded", "loading", "saved", "saving", "done",
        "closed", "opened", "open", "shown", "starred", "favorite", "favorited",
        "liked", "followed", "following", "subscribed", "pinned", "muted",
        // 流程 / 提交
        "submitted", "submitting", "pending", "processing", "running", "stopped",
        "started", "paused", "cancelled", "canceled", "aborted", "retried",
        "approved", "rejected", "accepted", "declined", "granted", "denied",
        "featured", "recommended", "highlighted", "urgent", "archived", "unarchived",
        "removed", "restored", "trashed", "shared", "private", "public", "internal",
        "external", "premium", "paid", "free", "trial", "expired", "vip",
        "blocked", "banned", "forbidden", "default", "required", "optional",
        "mandatory", "show", "hide", "display", "read", "unread", "only",
        // 同步 / 连接 / 安装
        "synced", "syncing", "sync", "uploaded", "uploading", "downloaded",
        "downloading", "installed", "installing", "uninstalled", "connected",
        "disconnected", "automatic", "manual", "writable", "deletable",
        "draggable", "scrollable", "sortable", "selectable", "hovered",
        "initialized", "initializing", "configured", "registered", "unregistered",
        "logged", "authenticated", "authorized", "unauthorized",
        "dirty", "touched", "modified", "changed", "dirtyflag", "sale", "onsale"
    )

    private val BOOLEAN_PREFIXES = setOf("is", "has", "can", "should", "need", "allow", "must", "does")

    private val STRING_WORDS = setOf(
        // 通用文本
        "name", "title", "label", "message", "msg", "description", "desc", "remark",
        "note", "comment", "content", "text", "subject", "body", "header", "footer",
        "summary", "detail", "info", "reason", "cause", "error", "err", "hint",
        "tooltip", "tip", "placeholder", "button", "value", "keyword", "search",
        "query", "condition", "filter", "privacy", "terms", "policy", "notice",
        "announcement", "notification", "alert", "log", "history", "record", "memo",
        "message", "title", "caption", "heading", "headline", "slogan", "tagline",
        "signature", "digest", "checksum", "fingerprint",
        // 标识 / 编号类
        "code", "key", "token", "password", "pwd", "secret", "username", "account",
        "nickname", "passport", "license", "certificate", "cert", "document", "doc",
        "invoice", "receipt", "bill", "statement", "contract", "agreement", "voucher",
        "serial", "branch", "commit", "release", "build", "captcha", "otp",
        "verificationcode", "activationcode", "invitecode", "referralcode", "sharecode",
        "promocode", "couponcode", "vouchercode", "discountcode", "redeemcode",
        "giftcode", "bonuscode", "smscode", "emailcode", "verifycode", "qrcode",
        "barcode", "upc", "ean", "model", "brand", "manufacturer", "supplier",
        "vendor", "sku", "isbn", "cvv", "cvc", "iban", "swift", "bic",
        // 网络 / 地址
        "url", "uri", "link", "email", "address", "city", "country", "province",
        "district", "region", "language", "lang", "locale", "timezone", "zone",
        "domain", "host", "hostname", "ipaddress", "ip", "protocol", "scheme",
        "endpoint", "route", "gateway", "proxy", "socket", "topic", "queue",
        "channel", "stream", "namespace", "cluster", "service", "database", "schema",
        "table", "column", "sql", "param", "params", "argument", "args",
        "field", "property", "attribute", "feature", "capability", "skill", "hobby",
        // 人员 / 组织
        "gender", "sex", "role", "permission", "authority", "unit", "company",
        "organization", "department", "dept", "position", "job", "jobtitle",
        "occupation", "industry", "education", "degree", "major", "school",
        "university", "college", "institute", "faculty", "team", "group", "merchant",
        "shop", "store", "warehouse", "office", "building", "street", "road",
        "village", "town", "county", "room",
        // 外观 / 多媒体
        "avatar", "icon", "image", "img", "picture", "file", "filename", "path",
        "folder", "directory", "dir", "location", "place", "site", "color", "colour",
        "theme", "style", "template", "format", "extension", "ext", "mime", "charset",
        "encoding", "currency", "symbol", "suffix", "prefix", "transform", "animation",
        "transition", "easing", "font", "fontfamily", "background", "foreground",
        "border", "thumbnail", "preview", "banner", "logo", "cover", "type",
        "category", "tag", "status", "state", "model", "version",
        // 业务字段
        "companyname", "projectname", "taskname", "jobtitle", "deptname",
        "departmentname", "positionname", "rolename", "groupname", "teamname",
        "orgname", "teachername", "coursename", "lessonname", "chaptername",
        "sectionname", "pagetitle", "menuname", "filename", "foldername",
        "schedule", "event", "meeting", "todo", "plan", "calendar", "timeline",
        "story", "news", "article", "post", "blog", "review", "ratingcomment",
        "feedback", "suggestion", "opinion", "question", "answer", "faq",
        "instruction", "manual", "guide", "doc", "readme", "changelog",
        // 编号字段（常见后端单号 / 号码）
        "orderno", "tradeno", "serialno", "cardno", "accountno", "invoiceno",
        "contractno", "policyno", "licenseno", "passportno", "identityno", "roomno",
        "phoneno", "mobileno", "faxno", "trackingno", "waybillno", "parcelno",
        "shipmentno", "paymentno", "refundno", "transactionno", "transferno",
        "depositno", "withdrawno", "settlementno", "statementno", "agreementno",
        "certno", "employeeno", "staffno", "studentno", "ticketno", "memberno",
        "vipno", "bankno", "taxno", "vatno", "gstno", "logisticsno", "expressno",
        "cardnumber", "accountnumber", "idnumber", "passportnumber", "licensenumber",
        "invoicenumber", "serialnumber", "contractnumber", "policynumber",
        "certificatenumber", "registrationnumber", "employeenumber", "staffnumber",
        "studentnumber", "ticketnumber", "membernumber", "vipnumber", "ordernumber",
        "transactionnumber", "paymentnumber", "refundnumber", "shipmentnumber",
        "trackingnumber", "waybillnumber", "expressnumber", "logisticsnumber",
        "phonenumber", "mobilenumber", "telnumber", "faxnumber", "extensionnumber",
        "roomnumber", "seatnumber", "buildingnumber", "unitnumber", "apartmentnumber",
        "idcardnumber", "identitynumber", "socialsecuritynumber", "bankaccount",
        "postcode", "postalcode", "zipcode", "areacode", "countrycode", "regioncode",
        "citycode", "dialcode", "bankcode", "swiftcode", "routingnumber", "sortcode",
        // 其他
        "sn", "case", "dob", "mode", "format", "layout", "design", "workflow",
        "process", "procedure", "standard", "spec", "specification", "requirement",
        "definition", "example", "sample", "defaultvalue", "fallback", "label",
        "unit", "measure", "dimension", "category", "classification", "segment",
        "channel", "source", "medium", "campaign", "target", "audience", "stage",
        "phase", "step", "milestone", "type", "category"
    )

    /** 以 Number 结尾但属于编号/号码类的词根：cardNumber / idNumber / orderNumber -> string */
    private val ID_NUMBER_PREFIXES = setOf(
        "card", "account", "id", "passport", "license", "tracking", "serial",
        "invoice", "contract", "policy", "certificate", "registration", "employee",
        "staff", "student", "ticket", "member", "vip", "order", "transaction",
        "payment", "refund", "shipment", "waybill", "parcel", "express", "logistics",
        "phone", "mobile", "tel", "fax", "extension", "room", "seat", "building",
        "apartment", "unit", "suite", "identity", "social", "security", "tax", "vat",
        "gst", "bank", "coupon", "voucher", "receipt", "bill", "settlement", "statement",
        "transfer", "deposit", "withdraw", "trade", "file", "task", "project", "user"
    )

    /** 以 No 结尾但属于编号/号码类的词根：orderNo / tradeNo / cardNo -> string */
    private val ID_NO_PREFIXES = setOf(
        "card", "account", "id", "passport", "license", "tracking", "serial",
        "invoice", "contract", "policy", "certificate", "registration", "employee",
        "staff", "student", "ticket", "member", "vip", "order", "trade", "transaction",
        "payment", "refund", "shipment", "waybill", "parcel", "express", "logistics",
        "phone", "mobile", "tel", "fax", "room", "seat", "identity", "social",
        "security", "tax", "vat", "gst", "bank", "coupon", "voucher", "receipt",
        "bill", "settlement", "statement", "transfer", "deposit", "withdraw",
        "file", "task", "project", "user", "build", "batch", "lot"
    )

    /** value 后缀但属于数值限定词：maxValue / minValue / totalValue -> number */
    private val VALUE_NUMBER_PREFIXES = setOf(
        "max", "min", "avg", "average", "total", "sum", "subtotal", "mean", "median"
    )

    private val NUMBER_WORD = setOf("number")
    private val NO_WORD = setOf("no")
    private val VALUE_WORD = setOf("value")

    /** 全小写拼接的 is/has 前缀误判排除词（island / issue / hash 等以 is/has 开头的英文单词） */
    private val IS_HAS_FALSE_PREFIXES = listOf(
        "island", "issue", "isbn", "iso", "hash", "isolat", "islam", "isosc",
        "isomet", "isoth", "ischi", "hast", "hasp", "hass", "harsh", "hazard",
        "haste", "hashtag"
    )

    /** id 后缀兜底时的误判黑名单：这些英文单词以 id 结尾但并非数字标识 */
    private val ID_SUFFIX_BLACKLIST = setOf(
        "valid", "invalid", "fluid", "solid", "avoid", "void", "rigid", "humid",
        "hybrid", "vivid", "candid", "morbid", "rabid", "vapid", "acrid", "turgid",
        "putrid", "fetid", "stolid", "squalid", "splendid", "lurid", "sordid",
        "florid", "sapid", "grid", "acid", "raid", "lid"
    )

    /** 以 date 结尾但并非日期字段的英文单词 */
    private val DATE_SUFFIX_BLACKLIST = setOf(
        "update", "candidate", "mandate", "validate", "predate", "postdate",
        "sedate", "antedate", "outdate", "backdate", "accommodate", "consolidate",
        "liquidate", "intimidate", "dilapidate", "elucidate"
    )

    /** 编号类词根（全小写拼接兜底用）：cardnumber / orderno -> string */
    private val ID_PREFIX_SUBSTRING = listOf(
        "card", "account", "order", "trade", "serial", "invoice", "contract",
        "policy", "license", "passport", "identity", "room", "phone", "mobile",
        "tracking", "waybill", "parcel", "express", "logistics", "employee",
        "staff", "student", "ticket", "member", "vip", "payment", "refund",
        "transaction", "settlement", "statement", "certificate", "registration",
        "bank", "tax", "vat", "gst", "user", "task", "project", "file", "build",
        "batch", "lot", "extension", "fax", "tel", "seat", "building", "unit",
        "apartment", "suite", "social", "security", "id"
    )

    /**
     * 根据 key 猜测类型，返回 "number" / "string" / "boolean"，猜不到返回 null。
     */
    fun guess(key: String): String? {
        val k = key.trim()
        if (k.isEmpty()) return null

        val tokens = tokenize(k)
        if (tokens.isEmpty()) return null

        // 1) 强字符串标记：优先于数字猜测（phoneNumber -> string，而不是 number）
        if (tokens.any { tokenIn(it, STRING_OVERRIDES) }) return "string"

        val first = tokens.first()
        val last = tokens.last()

        // 2) 编号类标识符：cardNumber / orderNo / idNumber 是 string，不是 number
        if (tokens.size >= 2) {
            val prev = tokens[tokens.size - 2]
            if (tokenIn(last, NUMBER_WORD) && tokenIn(prev, ID_NUMBER_PREFIXES)) return "string"
            if (tokenIn(last, NO_WORD) && tokenIn(prev, ID_NO_PREFIXES)) return "string"
        }

        // 3) 数值限定词：maxValue / minValue / totalValue 是 number，不是 string
        if (tokens.size >= 2 && tokenIn(last, VALUE_WORD) &&
            tokenIn(tokens[tokens.size - 2], VALUE_NUMBER_PREFIXES)
        ) return "number"

        // 4) 布尔前缀
        if (tokenIn(first, BOOLEAN_PREFIXES)) return "boolean"

        // 5) 末尾 token 最精确（最贴近字段语义）
        lastTokenGuess(last)?.let { return it }

        // 6) contains 语义：key 中任意 token 命中即返回
        tokens.forEach { t ->
            if (tokenIn(t, NUMBER_WORDS)) return "number"
        }
        tokens.forEach { t ->
            if (tokenIn(t, DATE_WORDS)) return "string"
        }
        tokens.forEach { t ->
            if (tokenIn(t, BOOLEAN_WORDS)) return "boolean"
        }
        tokens.forEach { t ->
            if (tokenIn(t, STRING_WORDS)) return "string"
        }

        // 7) 全小写拼接兜底（userid / isactive / createtime）
        return substringGuess(k)
    }

    private fun lastTokenGuess(last: String): String? {
        if (tokenIn(last, NUMBER_WORDS)) return "number"
        if (tokenIn(last, DATE_WORDS)) return "string"
        if (tokenIn(last, BOOLEAN_WORDS)) return "boolean"
        if (tokenIn(last, STRING_WORDS)) return "string"
        return null
    }

    private fun substringGuess(k: String): String? {
        STRING_OVERRIDES.forEach { if (k.contains(it)) return "string" }
        if (containsAny(k, "name", "title", "message", "avatar", "address", "password", "nickname")) return "string"
        // 时间日期：time 出现即命中；date 需是后缀（避免 candidate / update / validate 误判）
        if (k.contains("time") || k.endsWith("datetime") ||
            (k.endsWith("date") && DATE_SUFFIX_BLACKLIST.none { k.endsWith(it) })
        ) return "string"

        // id 后缀：userid / orderid / roleid，排除英文单词误判
        if ((k.endsWith("id") || k.endsWith("ids")) && k.length >= 4 &&
            ID_SUFFIX_BLACKLIST.none { k.endsWith(it) }
        ) return "number"

        // 编号类后缀兜底（全小写拼接）：cardnumber / orderno 是 string，不是 number
        if ((k.endsWith("number") || k.endsWith("no")) &&
            ID_PREFIX_SUBSTRING.any { k.contains(it) }
        ) return "string"

        if (containsAny(k, "count", "amount", "price", "total", "num", "age", "size")) return "number"

        // 全小写拼接的布尔字段：isactive / isdelete / haschildren（排除 island / issue / hash 等误判）
        val isLikePrefix = (k.startsWith("is") || k.startsWith("has")) &&
                k.length in 4..16 &&
                IS_HAS_FALSE_PREFIXES.none { k.startsWith(it) }
        if (isLikePrefix) return "boolean"

        if (containsAny(k, "flag", "enabled", "deleted", "valid", "logged")) return "boolean"
        return null
    }

    private fun containsAny(s: String, vararg keys: String): Boolean =
        keys.any { s.contains(it) }

    /** 把 key 拆成小写 token：userId -> [user, id]、user_id -> [user, id]、HTTPServer -> [http, server] */
    private fun tokenize(key: String): List<String> {
        return key
            .replace(Regex("""([a-z0-9])([A-Z])"""), "$1_$2")
            .replace(Regex("""([A-Z]+)([A-Z][a-z])"""), "$1_$2")
            .split(Regex("""[^a-zA-Z]+"""))
            .filter { it.isNotEmpty() }
            .map { it.lowercase() }
    }

    /** token 命中集合，带简单复数归一（names -> name、categories -> category、ids -> id） */
    private fun tokenIn(token: String, set: Set<String>): Boolean =
        token in set || singular(token) in set

    /** 简单单数化：categories -> category、boxes -> box、names -> name、ids -> id */
    private fun singular(token: String): String = when {
        token.endsWith("ies") && token.length > 4 -> token.dropLast(3) + "y"
        token.endsWith("xes") || token.endsWith("ches") || token.endsWith("shes") ->
            token.dropLast(2)
        token.endsWith("ses") -> token.dropLast(1)
        token.endsWith("s") && token.length > 2 -> token.dropLast(1)
        else -> token
    }
}
