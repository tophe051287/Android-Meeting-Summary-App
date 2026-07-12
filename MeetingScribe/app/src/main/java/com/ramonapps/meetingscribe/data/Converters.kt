package com.ramonapps.meetingscribe.data

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONObject

/**
 * Room type converters. Everything is stored as JSON text so we don't need
 * extra tables for simple list fields.
 */
class Converters {

    @TypeConverter
    fun fromStatus(status: MeetingStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): MeetingStatus =
        runCatching { MeetingStatus.valueOf(value) }.getOrDefault(MeetingStatus.ERROR)

    @TypeConverter
    fun fromStringList(list: List<String>): String {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        return arr.toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(value)
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrDefault(emptyList())
    }

    @TypeConverter
    fun fromActionItems(items: List<ActionItem>): String {
        val arr = JSONArray()
        items.forEach {
            val obj = JSONObject()
            obj.put("task", it.task)
            obj.put("owner", it.owner)
            arr.put(obj)
        }
        return arr.toString()
    }

    @TypeConverter
    fun toActionItems(value: String?): List<ActionItem> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(value)
            (0 until arr.length()).map {
                val obj = arr.getJSONObject(it)
                ActionItem(
                    task = obj.optString("task"),
                    owner = obj.optString("owner")
                )
            }
        }.getOrDefault(emptyList())
    }
}
