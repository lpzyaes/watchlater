/*
 * Copyright (c) 2015 - 2022
 *
 * Maximilian Hille <mh@lambdasoup.com>
 * Juliane Lehmann <jl@lambdasoup.com>
 *
 * This file is part of Watch Later.
 *
 * Watch Later is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Watch Later is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Watch Later.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.lambdasoup.watchlater.data

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Build
import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class AccountRepository(
    private val accountManager: AccountManager,
    private val sharedPreferences: SharedPreferences,
) : OnSharedPreferenceChangeListener {

    private val liveData = MutableLiveData<Account?>()

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        updateLiveData()
    }

    private fun getAccount(): Account? {
        val name = sharedPreferences.getString(PREF_KEY_DEFAULT_ACCOUNT_NAME, null) ?: return null
        return Account(name, ACCOUNT_TYPE_GOOGLE)
    }

    fun getAvailableAccounts(): List<Account> {
        val result = mutableListOf<Account>()
        getAccount()?.let { current ->
            result.add(current)
        }
        try {
            val systemAccounts = accountManager.getAccountsByType(ACCOUNT_TYPE_GOOGLE)
            for (acc in systemAccounts) {
                if (result.none { it.name.equals(acc.name, ignoreCase = true) }) {
                    result.add(acc)
                }
            }
        } catch (e: Exception) {
            Log.w("AccountRepository", "Failed to retrieve accounts: ${e.message}")
        }
        return result
    }

    @MainThread
    private fun updateLiveData() {
        val account = getAccount()
        liveData.value = account
    }

    fun put(account: Account) {
        val prefEditor = sharedPreferences.edit()
        prefEditor.putString(PREF_KEY_DEFAULT_ACCOUNT_NAME, account.name)
        prefEditor.apply()
    }

    fun get(): LiveData<Account?> {
        return liveData
    }

    @WorkerThread
    fun getAuthToken(): AuthTokenResult {
        val account = getAccount() ?: return AuthTokenResult.Error
        
        try {
            try {
                val accounts = accountManager.getAccountsByType(ACCOUNT_TYPE_GOOGLE)
                if (accounts.isNotEmpty() && !listOf(*accounts).contains(account)) {
                    val prefEditor = sharedPreferences.edit()
                    prefEditor.remove(PREF_KEY_DEFAULT_ACCOUNT_NAME)
                    prefEditor.apply()
                    return AuthTokenResult.Error
                }
            } catch (e: Exception) {
                Log.w("AccountRepository", "Could not check accounts by type: ${e.message}")
            }

            val future = accountManager.getAuthToken(account, SCOPE_YOUTUBE, null, false, null, null)

            val result = future.result
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.getParcelable(AccountManager.KEY_INTENT, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.getParcelable<Intent>(AccountManager.KEY_INTENT)
            }
            if (intent != null) {
                return AuthTokenResult.HasIntent(intent)
            }
            val token = result.getString(AccountManager.KEY_AUTHTOKEN)
            if (!token.isNullOrEmpty()) {
                return AuthTokenResult.AuthToken(token)
            }
            return AuthTokenResult.Error
        } catch (e: Exception) {
            Log.e("AccountRepository", "Could not get token: ${e.message}", e)
            return AuthTokenResult.Error
        }
    }

    fun invalidateToken(token: String?) {
        try {
            accountManager.invalidateAuthToken(ACCOUNT_TYPE_GOOGLE, token)
        } catch (e: Exception) {
            Log.w("AccountRepository", "Failed to invalidate token: ${e.message}")
        }
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        updateLiveData()
    }

    sealed class AuthTokenResult {
        object Error : AuthTokenResult()
        data class AuthToken(val token: String) : AuthTokenResult()
        data class HasIntent(val intent: Intent) : AuthTokenResult()
    }

    companion object {
        private const val PREF_KEY_DEFAULT_ACCOUNT_NAME = "pref_key_default_account_name"
        private const val SCOPE_YOUTUBE = "oauth2:https://www.googleapis.com/auth/youtube"
        private const val ACCOUNT_TYPE_GOOGLE = "com.google"
    }
}
