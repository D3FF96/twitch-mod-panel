package com.d3ff96.twitchmodpanel.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.d3ff96.twitchmodpanel.domain.model.QuickCommand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.quickCommandStore: DataStore<Preferences> by preferencesDataStore(name = "quick_commands")

/**
 * Persistence for reply templates via DataStore Preferences (JSON blob).
 */
class QuickCommandDataStore(private val context: Context) {
    private val key = stringPreferencesKey("commands_json")

    val commands: Flow<List<QuickCommand>> = context.quickCommandStore.data.map { prefs ->
        parse(prefs[key].orEmpty())
    }

    suspend fun save(commands: List<QuickCommand>) {
        context.quickCommandStore.edit { prefs ->
            prefs[key] = serialize(commands)
        }
    }

    private fun serialize(list: List<QuickCommand>): String {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(
                JSONObject()
                    .put("id", c.id)
                    .put("name", c.name)
                    .put("body", c.body)
            )
        }
        return arr.toString()
    }

    private fun parse(raw: String): List<QuickCommand> {
        if (raw.isBlank()) return defaultCommands()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        QuickCommand(
                            id = o.getString("id"),
                            name = o.getString("name"),
                            body = o.getString("body"),
                        )
                    )
                }
            }
        }.getOrElse { defaultCommands() }
    }

    companion object {
        fun defaultCommands(): List<QuickCommand> = listOf(
            QuickCommand("qc1", "Правила", "Пожалуйста, соблюдайте правила чата. Ссылки без разрешения — таймаут."),
            QuickCommand("qc2", "Клипы", "Клипы можно кидать в Discord / Discord link stub."),
            QuickCommand("qc3", "Музыка", "Музыка: !song / !sr — заглушка."),
            QuickCommand("qc4", "EN", "Please keep chat civil. No spoilers."),
        )
    }
}
