package app.tit.reader.novel.android.data

import android.content.Context
import android.content.SharedPreferences
import app.tit.shared.storage.KeyValueDriver

class AndroidSharedPreferencesDriver(context: Context) : KeyValueDriver {
    private val prefs: SharedPreferences = context.getSharedPreferences("tit_reader_prefs", Context.MODE_PRIVATE)

    override fun getString(key: String): String? {
        return prefs.getString(key, null)
    }

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}