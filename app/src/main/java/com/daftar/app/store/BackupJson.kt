package com.daftar.app.store

import org.json.JSONArray
import org.json.JSONObject

// Backup as a single JSON document the owner can save (share to WhatsApp/Drive/email) and
// restore later. org.json is on-device only, so this is verified on the emulator, not in JUnit.

fun snapshotToJson(s: StoreSnapshot): String {
    val root = JSONObject()
    root.put("version", 1)
    root.put("usdRate", s.usdRate)
    root.put("sources", JSONArray().apply {
        s.sources.forEach {
            put(JSONObject().put("id", it.id).put("kind", it.kind.name).put("label", it.label).put("cost", it.cost ?: JSONObject.NULL))
        }
    })
    root.put("shelf", JSONArray().apply {
        s.shelf.forEach {
            put(
                JSONObject().put("id", it.id).put("name", it.name).put("tasira", it.tasira)
                    .put("shelved", it.shelved).put("sold", it.sold)
                    .put("counted", it.counted ?: JSONObject.NULL)
                    .put("sourceId", it.sourceId ?: JSONObject.NULL)
                    .put("buy", it.buy ?: JSONObject.NULL),
            )
        }
    })
    root.put("customers", JSONArray().apply {
        s.customers.forEach {
            put(JSONObject().put("id", it.id).put("name", it.name).put("phone", it.phone ?: JSONObject.NULL).put("openingDebt", it.openingDebt))
        }
    })
    root.put("entries", JSONArray().apply {
        s.entries.forEach {
            put(
                JSONObject().put("id", it.id).put("t", it.t).put("d", it.d).put("amt", it.amt).put("cls", it.cls)
                    .put("customerId", it.customerId ?: JSONObject.NULL).put("debtDelta", it.debtDelta)
                    .put("day", it.day).put("saleAmount", it.saleAmount).put("cashAmount", it.cashAmount)
                    .put("stockDelta", it.stockDelta),
            )
        }
    })
    return root.toString(2)
}

private fun JSONObject.optStr(key: String): String? = if (isNull(key)) null else optString(key, null)
private fun JSONObject.optIntN(key: String): Int? = if (isNull(key)) null else if (has(key)) getInt(key) else null
private fun JSONObject.optLongN(key: String): Long? = if (isNull(key)) null else if (has(key)) getLong(key) else null

fun snapshotFromJson(json: String): StoreSnapshot {
    val root = JSONObject(json)
    fun <T> JSONArray.map(f: (JSONObject) -> T): List<T> = (0 until length()).map { f(getJSONObject(it)) }
    return StoreSnapshot(
        seeded = true,
        usdRate = if (root.has("usdRate")) root.getLong("usdRate") else 1500,
        sources = root.getJSONArray("sources").map {
            Source(it.getString("id"), Kind.valueOf(it.getString("kind")), it.getString("label"), it.optLongN("cost"))
        },
        shelf = root.getJSONArray("shelf").map {
            Shelf(
                it.getString("id"), it.getString("name"), it.getLong("tasira"), it.getInt("shelved"), it.getInt("sold"),
                it.optIntN("counted"), it.optStr("sourceId"), it.optLongN("buy"),
            )
        },
        customers = root.getJSONArray("customers").map {
            Customer(it.getString("id"), it.getString("name"), it.optStr("phone"), it.getLong("openingDebt"))
        },
        entries = root.getJSONArray("entries").map {
            DayEntry(
                it.getString("id"), it.getString("t"), it.getString("d"), it.getString("amt"), it.getString("cls"),
                it.optStr("customerId"), it.getLong("debtDelta"), it.getLong("day"),
                it.getLong("saleAmount"), it.getLong("cashAmount"), it.optString("stockDelta", ""),
            )
        },
    )
}
