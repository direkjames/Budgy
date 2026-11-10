package com.budgy.app.i18n

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

/**
 * In-app translations (V4). English text is the key; {0}, {1}… are filled with the arguments.
 * Translations live in assets/i18n/<lang>.json as { "English text": "Translated text" }.
 *
 * [lang] is Compose state, so every screen re-renders instantly when you switch languages.
 * Missing translations fall back to English, so nothing ever shows blank.
 */
object L10n {
    var lang by mutableStateOf("en")

    val languages = listOf(
        "en" to "English",
        "fil" to "Filipino",
        "ceb" to "Cebuano (Bisaya)",
    )

    @Volatile
    private var tables: Map<String, Map<String, String>> = emptyMap()

    /** Loads the translation files once at app start. */
    fun init(context: Context) {
        if (tables.isNotEmpty()) return
        tables = languages.map { it.first }.filter { it != "en" }.associateWith { code ->
            runCatching {
                val text = context.assets.open("i18n/$code.json").use { it.readBytes().toString(Charsets.UTF_8) }
                val json = JSONObject(text)
                buildMap { json.keys().forEach { k -> put(k, json.getString(k)) } }
            }.getOrDefault(emptyMap())
        }
    }

    fun tr(en: String, vararg args: Any?): String {
        var s = if (lang == "en") en else tables[lang]?.get(en) ?: en
        args.forEachIndexed { i, a -> s = s.replace("{$i}", a?.toString() ?: "") }
        return s
    }
}

/** Shorthand used across the UI. */
fun tr(en: String, vararg args: Any?): String = L10n.tr(en, *args)
