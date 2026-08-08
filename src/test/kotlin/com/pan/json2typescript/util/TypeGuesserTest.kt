package com.pan.json2typescript.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * TypeGuesser 键名类型推断测试。
 * 覆盖：数字 / 字符串 / 布尔 / 时间日期 / 编号类特判（cardNumber、orderNo、maxValue）/
 * 各种命名风格 / 无法识别的 key。
 */
@DisplayName("TypeGuesser 键名类型推断")
class TypeGuesserTest {

    @ParameterizedTest(name = "key \"{0}\" 应推断为 number")
    @ValueSource(
        strings = [
            // 标识 / 序号
            "id", "userId", "orderId", "order_id", "userid", "uid", "ids",
            "index", "idx", "seq", "seqNo", "port",
            // 数量
            "count", "totalCount", "childrenCount", "activeCount", "counts",
            "quantity", "qty", "amount", "totalAmount", "sum", "total",
            "totalItems", "totalPages", "currentPage", "pageNum", "perPage",
            "page", "limit", "offset", "size", "pageSize",
            // 金额 / 财务
            "price", "cost", "fee", "salary", "balance", "budget", "deposit",
            "interest", "principal", "debt", "credit", "debit", "tax", "discount",
            "profit", "revenue", "income", "expense", "margin", "installment",
            "dividend", "points", "integral", "credits", "coins", "energy",
            "exp", "hp", "mp", "mana",
            // 统计 / 等级
            "score", "rating", "rate", "percent", "percentage", "ratio", "factor",
            "proportion", "avg", "average", "max", "min", "maxValue", "minValue",
            "totalValue", "median", "deviation", "variance", "coefficient",
            "rank", "priority", "progress", "level", "tier", "star", "stars",
            "grade", "weight", "mass",
            // 尺寸 / 空间
            "width", "height", "length", "distance", "area", "volume", "capacity",
            "radius", "diameter", "perimeter", "padding", "gap", "fontSize",
            "lineHeight", "letterSpacing", "opacity", "zIndex", "borderWidth",
            "altitude", "depth",
            // 物理量 / 性能
            "temperature", "humidity", "pressure", "density", "viscosity",
            "wavelength", "voltage", "current", "resistance", "capacitance",
            "inductance", "wattage", "power", "cores", "threads", "processors",
            "ram", "rom", "memory", "storage", "battery", "speed", "velocity",
            "acceleration", "frequency", "duration", "interval",
            // 时间片段
            "week", "quarter", "floor", "timestamp", "times", "timeout",
            "dayOfWeek",
            // 其他
            "order", "sort", "stock", "inventory", "longitude", "latitude",
            "lat", "lng", "lon", "steps", "version", "no", "num", "number"
        ]
    )
    fun `should_guess_number`(key: String) {
        assertEquals("number", TypeGuesser.guess(key), "key=$key")
    }

    @ParameterizedTest(name = "key \"{0}\" 应推断为 string")
    @ValueSource(
        strings = [
            // 通用文本
            "name", "userName", "username", "nickname", "title", "label",
            "message", "msg", "description", "desc", "remark", "note", "comment",
            "content", "text", "subject", "body", "header", "footer", "summary",
            "detail", "info", "reason", "cause", "error", "errorCode", "hint",
            "tip", "placeholder", "button", "value", "keyword", "search", "query",
            "condition", "filter", "privacy", "terms", "policy", "notice",
            "announcement", "notification", "alert", "log", "history", "record",
            "memo", "caption", "heading", "slogan", "signature", "checksum",
            "fingerprint",
            // 网络 / 地址
            "url", "uri", "link", "imageUrl", "videoUrl", "downloadUrl",
            "callbackUrl", "webhookUrl", "baseUrl", "redirectUrl", "avatarUrl",
            "address", "city", "country", "province", "district", "region",
            "language", "lang", "locale", "timezone", "zone", "domain", "host",
            "hostname", "ipAddress", "ip", "protocol", "scheme", "endpoint",
            "route", "gateway", "proxy", "socket", "topic", "queue", "channel",
            "stream", "namespace", "cluster", "service", "database", "schema",
            "table", "column", "sql", "param", "params", "argument", "args",
            "field", "property", "attribute", "feature", "capability",
            "macAddress", "imei", "imsi",
            // 标识 / 编码
            "code", "key", "token", "password", "pwd", "secret", "account",
            "passport", "license", "certificate", "document", "invoice", "receipt",
            "bill", "statement", "contract", "agreement", "voucher", "serial",
            "branch", "commit", "release", "build", "captcha", "otp",
            "verificationCode", "activationCode", "inviteCode", "referralCode",
            "shareCode", "promoCode", "couponCode", "smsCode", "emailCode",
            "verifyCode", "qrcode", "barcode", "model", "brand", "manufacturer",
            "supplier", "vendor", "sku", "isbn", "uuid", "guid", "dob", "mode",
            "layout", "workflow", "process", "standard", "spec", "requirement",
            "definition", "example", "sample", "question", "answer", "feedback",
            "suggestion", "article", "post", "blog", "review", "readme",
            "changelog",
            // 人员 / 组织
            "gender", "sex", "role", "permission", "authority", "unit", "company",
            "organization", "department", "dept", "position", "job", "jobTitle",
            "occupation", "industry", "education", "degree", "major", "school",
            "university", "college", "institute", "team", "group", "merchant",
            "shop", "store", "warehouse", "office", "building", "street", "road",
            "village", "town", "county", "room", "companyName", "projectName",
            "taskName", "departmentName", "positionName", "roleName", "teamName",
            "teacherName", "courseName",
            // 外观 / 多媒体
            "avatar", "icon", "image", "img", "picture", "file", "filename",
            "path", "folder", "directory", "dir", "location", "place", "site",
            "color", "colour", "theme", "style", "template", "format", "extension",
            "ext", "mime", "charset", "encoding", "currency", "symbol", "suffix",
            "prefix", "transform", "animation", "transition", "easing", "font",
            "fontFamily", "background", "foreground", "border", "thumbnail",
            "preview", "banner", "logo", "cover", "type", "category", "tag",
            "tags", "status", "state", "orderStatus", "statusCode",
            "userInfo", "user_info", "user-info",
            // 业务字段
            "schedule", "event", "meeting", "todo", "plan", "calendar", "timeline",
            "story", "news", "article", "instruction", "guide",
            // 编号类特判：xxxNumber / xxxNo 是 string 而不是 number
            "cardNumber", "accountNumber", "idNumber", "passportNumber",
            "licenseNumber", "invoiceNumber", "serialNumber", "contractNumber",
            "policyNumber", "employeeNumber", "ticketNumber", "trackingNumber",
            "phoneNumber", "mobileNumber", "idCardNumber", "bankAccount",
            "orderNo", "tradeNo", "serialNo", "cardNo", "accountNo", "invoiceNo",
            "contractNo", "policyNo", "licenseNo", "passportNo", "identityNo",
            "roomNo", "phoneNo", "mobileNo", "faxNo", "trackingNo", "waybillNo",
            "orderNo", "userNo", "taskNo",
            // 全小写拼接
            "username", "password", "phonenumber", "cardnumber", "orderno",
            "ordernumber", "trackingnumber", "idnumber",
            "subject", "case", "sn"
        ]
    )
    fun `should_guess_string`(key: String) {
        assertEquals("string", TypeGuesser.guess(key), "key=$key")
    }

    @ParameterizedTest(name = "key \"{0}\" 应推断为 boolean")
    @ValueSource(
        strings = [
            "isActive", "isDeleted", "isDelete", "isAdmin", "isDirty", "isSelected", "is",
            "hasChildren", "hasPermission", "hasChanged", "canEdit",
            "shouldNotify", "mustSync",
            // 状态 / 标志
            "deleteFlag", "flag", "enabled", "disabled", "active", "inactive",
            "valid", "invalid", "checked", "selected", "deleted", "published",
            "unpublished", "verified", "unverified", "confirmed", "unconfirmed",
            "completed", "finished", "success", "succeeded", "failed", "visible",
            "invisible", "hidden", "shown", "exists", "available", "unavailable",
            "locked", "unlocked", "online", "offline", "editable", "expandable",
            "expanded", "collapsed", "loaded", "loading", "saved", "saving",
            "done", "closed", "opened", "open", "starred", "favorite",
            "favorited", "liked", "followed", "following", "subscribed",
            "pinned", "muted", "readonly", "readOnly", "writeOnly",
            // 流程 / 提交
            "submitted", "submitting", "pending", "processing", "running",
            "stopped", "started", "paused", "cancelled", "canceled", "aborted",
            "retried", "approved", "rejected", "accepted", "declined", "granted",
            "denied", "featured", "recommended", "highlighted", "urgent",
            "archived", "unarchived", "removed", "restored", "trashed", "shared",
            "private", "public", "internal", "external", "premium", "paid",
            "free", "trial", "expired", "vip", "blocked", "banned", "forbidden",
            "default", "required", "optional", "mandatory", "show", "hide",
            "display", "read", "unread", "only",
            // 同步 / 连接 / 安装
            "synced", "syncing", "sync", "uploaded", "uploading", "downloaded",
            "downloading", "installed", "installing", "uninstalled", "connected",
            "disconnected", "automatic", "manual", "writable", "deletable",
            "draggable", "scrollable", "sortable", "selectable", "hovered",
            "initialized", "initializing", "configured", "registered",
            "unregistered", "loggedIn", "loggedOut", "authenticated",
            "authorized", "unauthorized", "dirty", "touched", "modified",
            "changed",
            // 全小写拼接
            "isactive", "isdelete", "haschildren", "loggedin"
        ]
    )
    fun `should_guess_boolean`(key: String) {
        assertEquals("boolean", TypeGuesser.guess(key), "key=$key")
    }

    @ParameterizedTest(name = "key \"{0}\" 应推断为 string（时间日期）")
    @ValueSource(
        strings = [
            "createTime", "updateTime", "createdAt", "updatedAt", "deleteTime",
            "birthday", "date", "datetime", "year", "month", "day", "hour",
            "minute", "second", "openDate", "closeTime", "showTime",
            "startDate", "endDate", "expireDate", "createDate", "anniversary"
        ]
    )
    fun `should_guess_date_as_string`(key: String) {
        assertEquals("string", TypeGuesser.guess(key), "key=$key")
    }

    @ParameterizedTest(name = "key \"{0}\" 无法识别应返回 null")
    @ValueSource(
        strings = [
            "user", "list", "data", "items", "result", "obj", "object",
            "boxList", "grid", "fluid", "analysis", "candidate", "island",
            "issue", "hash", "children", "foo", "a",
            "custom", "settings", "options", "update", "coupon",
            "misc", "others", "xy",
            // selected 出现在中间而非结尾：不判布尔（selectedItems 不应被猜成 boolean）
            "selectedItems"
        ]
    )
    fun `should_return_null_for_unknown`(key: String) {
        assertNull(TypeGuesser.guess(key), "key=$key")
    }

    @Test
    fun `空白 key 返回 null`() {
        assertNull(TypeGuesser.guess(""))
        assertNull(TypeGuesser.guess("   "))
    }
}
