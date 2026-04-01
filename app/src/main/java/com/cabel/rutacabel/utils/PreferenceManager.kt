package com.cabel.rutacabel.utils

import android.content.Context
import android.content.SharedPreferences
import com.cabel.rutacabel.data.local.entities.User

class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveSession(user: User) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_USER_ID, user.id)
            putLong(KEY_EMPLEADO_ID, user.id) // Backend employee ID
            putString(KEY_USERNAME, user.username)
            putString(KEY_FULL_NAME, user.fullName)
            putString(KEY_EMAIL, user.email)
            putInt(KEY_ROLE_ID, user.roleId)
            putString(KEY_ROLE, user.role)
            putInt(KEY_BRANCH_ID, user.branchId ?: 0)
            putString(KEY_PHONE, user.phone)
            putString(KEY_ADDRESS, user.address)
            putString(KEY_PHOTO_URL, user.photoUrl)
            apply()
        }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUserId(): Long {
        return prefs.getLong(KEY_USER_ID, 0)
    }
    
    fun getEmpleadoId(): Long {
        return prefs.getLong(KEY_EMPLEADO_ID, 0)
    }

    fun getUsername(): String {
        return prefs.getString(KEY_USERNAME, "") ?: ""
    }

    fun getFullName(): String {
        return prefs.getString(KEY_FULL_NAME, "") ?: ""
    }

    fun getRole(): String {
        return prefs.getString(KEY_ROLE, "") ?: ""
    }

    fun getPhotoUrl(): String {
        return prefs.getString(KEY_PHOTO_URL, "") ?: ""
    }
    
    fun getBranchId(): Int {
        return prefs.getInt(KEY_BRANCH_ID, 0)
    }

    fun getUser(): User? {
        if (!isLoggedIn()) return null
        return User(
            id = getUserId(),
            username = getUsername(),
            fullName = getFullName(),
            email = prefs.getString(KEY_EMAIL, null),
            roleId = prefs.getInt(KEY_ROLE_ID, 0),
            role = getRole(),
            branchId = getBranchId().takeIf { it != 0 },
            phone = prefs.getString(KEY_PHONE, null),
            address = prefs.getString(KEY_ADDRESS, null),
            photoUrl = getPhotoUrl()
        )
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun getBluetoothPrinterAddress(): String {
        return prefs.getString(KEY_BT_PRINTER, "") ?: ""
    }

    fun setBluetoothPrinterAddress(address: String) {
        prefs.edit().putString(KEY_BT_PRINTER, address).apply()
    }

    fun getBluetoothPrinterName(): String {
        return prefs.getString(KEY_BT_PRINTER_NAME, "") ?: ""
    }

    fun setBluetoothPrinterName(name: String) {
        prefs.edit().putString(KEY_BT_PRINTER_NAME, name).apply()
    }

    companion object {
        private const val PREF_NAME = "RutaCABELPrefs"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_USER_ID = "userId"
        private const val KEY_EMPLEADO_ID = "empleadoId"
        private const val KEY_USERNAME = "username"
        private const val KEY_FULL_NAME = "fullName"
        private const val KEY_EMAIL = "email"
        private const val KEY_ROLE_ID = "roleId"
        private const val KEY_ROLE = "role"
        private const val KEY_BRANCH_ID = "branchId"
        private const val KEY_PHONE = "phone"
        private const val KEY_ADDRESS = "address"
        private const val KEY_PHOTO_URL = "photoUrl"
        private const val KEY_BT_PRINTER = "btPrinterAddress"
        private const val KEY_BT_PRINTER_NAME = "btPrinterName"
    }
}
