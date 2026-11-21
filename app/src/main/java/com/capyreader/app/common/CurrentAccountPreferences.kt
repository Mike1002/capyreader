package com.capyreader.app.common

import com.capyreader.app.preferences.AppPreferences
import com.jocmp.capy.AccountPreferences
import com.jocmp.capy.PreferenceStoreProvider

/**
 * Resolves the signed-in account's preferences on demand, for use by
 * long-lived HTTP clients like Coil's that outlive account switches.
 */
class CurrentAccountPreferences(
    private val appPreferences: AppPreferences,
    private val preferenceStoreProvider: PreferenceStoreProvider,
) : () -> AccountPreferences? {
    @Volatile
    private var cached: Pair<String, AccountPreferences>? = null

    override fun invoke(): AccountPreferences? {
        val accountID = appPreferences.accountID.get()

        if (accountID.isBlank()) {
            return null
        }

        cached?.let { (id, preferences) ->
            if (id == accountID) {
                return preferences
            }
        }

        return preferenceStoreProvider.build(accountID).also {
            cached = accountID to it
        }
    }
}
