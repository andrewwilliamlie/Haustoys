package com.haustoys.pos.data

import android.content.Context
import com.haustoys.pos.core.StoreInfo

class StoreSettings(context: Context) {
    private val prefs = context.getSharedPreferences("store", Context.MODE_PRIVATE)

    var store: StoreInfo
        get() {
            val d = StoreInfo()
            return StoreInfo(
                name = prefs.getString("name", d.name) ?: d.name,
                address = prefs.getString("address", d.address) ?: d.address,
                footer = prefs.getString("footer", d.footer) ?: d.footer,
            )
        }
        set(value) {
            prefs.edit()
                .putString("name", value.name)
                .putString("address", value.address)
                .putString("footer", value.footer)
                .apply()
        }

    var autoPrint: Boolean
        get() = prefs.getBoolean("auto_print", true)
        set(value) = prefs.edit().putBoolean("auto_print", value).apply()
}
