package com.cabel.rutacabel.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.cabel.rutacabel.data.local.AppDatabase
import com.cabel.rutacabel.data.local.entities.User
import com.cabel.rutacabel.data.remote.LoginRequest
import com.cabel.rutacabel.data.remote.RetrofitClient
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: User) : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val userDao = database.userDao()
    private val apiService = RetrofitClient.apiService

    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> = _loginState

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                _loginState.value = LoginState.Loading

                val localUser = userDao.getUserByUsername(username)
                if (localUser != null && localUser.authToken.isNotEmpty()) {
                    _loginState.value = LoginState.Success(localUser)
                    tryRemoteLogin(username, password, localUser.id)
                    return@launch
                }

                val response = apiService.login(LoginRequest(username, password))
                if (response.isSuccessful && response.body()?.success == true) {
                    val userResponse = response.body()?.user
                    val authToken = response.body()?.token ?: ""
                    
                    if (userResponse != null) {
                        val user = User(
                            id = userResponse.id,
                            username = userResponse.username,
                            authToken = authToken,
                            fullName = userResponse.fullName,
                            email = userResponse.email,
                            roleId = userResponse.roleId,
                            role = userResponse.role,
                            branchId = userResponse.branchId,
                            phone = userResponse.phone,
                            address = userResponse.address,
                            photoUrl = userResponse.photoUrl
                        )
                        userDao.insertUser(user)
                        _loginState.value = LoginState.Success(user)
                    } else {
                        _loginState.value = LoginState.Error("Error en datos de usuario")
                    }
                } else {
                    _loginState.value = LoginState.Error(
                        response.body()?.message ?: "Usuario o contraseña incorrectos"
                    )
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(
                    "Error de conexión. Verificando datos locales..."
                )
                val localUser = userDao.getUserByUsername(username)
                if (localUser != null && localUser.authToken.isNotEmpty()) {
                    _loginState.value = LoginState.Success(localUser)
                } else {
                    _loginState.value = LoginState.Error(
                        "Sin conexión y no hay datos locales"
                    )
                }
            }
        }
    }

    private fun tryRemoteLogin(username: String, password: String, userId: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.login(LoginRequest(username, password))
                if (response.isSuccessful && response.body()?.success == true) {
                    val authToken = response.body()?.token ?: ""
                    if (authToken.isNotEmpty()) {
                        userDao.updateAuthToken(userId, authToken)
                    }
                }
            } catch (e: Exception) {
            }
        }
    }
}
