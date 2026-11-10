package com.budgy.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * The only place Budgy uses the internet (V5): crypto prices from CoinGecko's free public API.
 * Requests contain coin IDs (like "bitcoin") and your currency code — never amounts, accounts or transactions.
 */
object CoinGecko {
    private const val BASE = "https://api.coingecko.com/api/v3"

    data class Coin(val id: String, val name: String, val symbol: String, val rank: Int?)

    class RateLimited : IOException("CoinGecko rate limit")

    /** Quick picks shown before you search (CoinGecko IDs), so common coins work even if search is unavailable. */
    val popular = listOf(
        Coin("bitcoin", "Bitcoin", "BTC", null),
        Coin("ethereum", "Ethereum", "ETH", null),
        Coin("tether", "Tether", "USDT", null),
        Coin("ripple", "XRP", "XRP", null),
        Coin("binancecoin", "BNB", "BNB", null),
        Coin("solana", "Solana", "SOL", null),
        Coin("usd-coin", "USDC", "USDC", null),
        Coin("dogecoin", "Dogecoin", "DOGE", null),
        Coin("cardano", "Cardano", "ADA", null),
        Coin("tron", "TRON", "TRX", null),
        Coin("chainlink", "Chainlink", "LINK", null),
        Coin("avalanche-2", "Avalanche", "AVAX", null),
        Coin("shiba-inu", "Shiba Inu", "SHIB", null),
        Coin("the-open-network", "Toncoin", "TON", null),
        Coin("litecoin", "Litecoin", "LTC", null),
    )

    /** CoinGecko currency code for the main-currency symbols Budgy offers. */
    fun vsCurrency(symbol: String): String? = mapOf(
        "₱" to "php", "$" to "usd", "€" to "eur", "£" to "gbp", "¥" to "jpy",
        "₩" to "krw", "₹" to "inr", "S$" to "sgd", "A$" to "aud",
    )[symbol]

    suspend fun search(query: String): List<Coin> = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val json = JSONObject(get("$BASE/search?query=$q"))
        val coins = json.optJSONArray("coins") ?: return@withContext emptyList()
        (0 until minOf(coins.length(), 20)).map { i ->
            val c = coins.getJSONObject(i)
            Coin(
                id = c.getString("id"),
                name = c.optString("name"),
                symbol = c.optString("symbol").uppercase(),
                rank = if (c.isNull("market_cap_rank")) null else c.optInt("market_cap_rank"),
            )
        }
    }

    /** coin id -> price of one coin in [vs] (e.g. "php"). Coins CoinGecko doesn't know are left out. */
    suspend fun prices(ids: Collection<String>, vs: String): Map<String, Double> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptyMap()
        val list = ids.distinct().joinToString(",") { URLEncoder.encode(it, "UTF-8") }
        val json = JSONObject(get("$BASE/simple/price?ids=$list&vs_currencies=$vs"))
        buildMap {
            for (id in ids) {
                val p = json.optJSONObject(id)?.optDouble(vs, Double.NaN) ?: Double.NaN
                if (!p.isNaN() && p > 0) put(id, p)
            }
        }
    }

    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "Budgy-Android")
            val code = conn.responseCode
            if (code == 429) throw RateLimited()
            if (code !in 200..299) throw IOException("HTTP $code")
            return conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        } finally {
            conn.disconnect()
        }
    }
}
