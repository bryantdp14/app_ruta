package com.cabel.rutacabel.ui.route

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.cabel.rutacabel.data.local.AppDatabase
import com.cabel.rutacabel.data.local.entities.Incident
import com.cabel.rutacabel.utils.PreferenceManager
import com.cabel.rutacabel.utils.UIUtils
import kotlinx.coroutines.launch

class AddIncidentViewModel(application: Application) : AndroidViewModel(application) {

    private val incidentDao = AppDatabase.getDatabase(application).incidentDao()
    private val prefs = PreferenceManager(application)

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun saveIncident(clientId: Long, routeId: Long, type: String, description: String) {
        if (description.isBlank()) {
            UIUtils.toast(getApplication(), "La descripción es requerida")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Offline Logic mainly, as incidents might not have a direct API yet or we sync them later
                // But generally we save to local DB with needsSync=true
                
                val userId = prefs.getUserId()

                val incident = Incident(
                    clientId = clientId,
                    userId = userId,
                    routeId = routeId,
                    incidentType = type,
                    description = description,
                    needsSync = true,
                    status = "PENDING"
                )

                incidentDao.insertIncident(incident)
                
                // Trigger Sync if Online
                if (com.cabel.rutacabel.utils.NetworkUtils.isNetworkAvailable(getApplication())) {
                    com.cabel.rutacabel.data.SyncWorker.runOnce(getApplication(), userId)
                }
                
                _saveSuccess.value = true
                UIUtils.toast(getApplication(), "Incidencia guardada localmente")

            } catch (e: Exception) {
                UIUtils.toast(getApplication(), "Error al guardar: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
