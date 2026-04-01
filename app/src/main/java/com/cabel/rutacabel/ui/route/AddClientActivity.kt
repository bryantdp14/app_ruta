package com.cabel.rutacabel.ui.route

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.cabel.rutacabel.databinding.ActivityAddClientBinding
import com.cabel.rutacabel.data.remote.RetrofitClient
import com.cabel.rutacabel.data.remote.ClienteRegistroRequest
import com.cabel.rutacabel.utils.PreferenceManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch

class AddClientActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddClientBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var preferenceManager: PreferenceManager
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddClientBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        preferenceManager = PreferenceManager(this)
        
        setupUI()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnCaptureLocation.setOnClickListener {
            captureLocation()
        }

        binding.btnSave.setOnClickListener {
            saveClient()
        }
    }

    private fun captureLocation() {
        if (checkLocationPermission()) {
            getCurrentLocation()
        } else {
            requestLocationPermission()
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCurrentLocation() {
        if (!checkLocationPermission()) {
            return
        }

        try {
            val cancellationTokenSource = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    binding.etLatitude.setText(String.format("%.6f", currentLatitude))
                    binding.etLongitude.setText(String.format("%.6f", currentLongitude))
                    Toast.makeText(this, "Ubicación capturada exitosamente", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Error al obtener ubicación: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Error de permisos: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveClient() {
        val name = binding.etName.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val colonia = binding.etColonia.text.toString().trim()
        val municipio = binding.etMunicipio.text.toString().trim()
        val estado = binding.etEstado.text.toString().trim()
        val codigoPostal = binding.etCodigoPostal.text.toString().toIntOrNull() ?: 0
        val phone = binding.etPhone.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val taxId = binding.etTaxId.text.toString().trim()
        val regimen = binding.etRegimen.text.toString().trim()
        val creditLimit = binding.etCreditLimit.text.toString().toDoubleOrNull() ?: 0.0
        val diasEntrega = binding.etDiasEntrega.text.toString().trim()
        val horarioEntrega = binding.etHorarioEntrega.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentLatitude == 0.0 && currentLongitude == 0.0) {
            Toast.makeText(this, "Por favor capture la ubicación del cliente", Toast.LENGTH_SHORT).show()
            return
        }

        val request = ClienteRegistroRequest(
            nombre = name,
            direccion = address,
            colonia = colonia,
            municipio = municipio,
            estado = estado,
            codigoPostal = codigoPostal,
            sexo = 1, // Default
            telefono = phone,
            correoElectronico = email,
            rfc = taxId,
            regimen = regimen,
            esProspecto = binding.switchProspecto.isChecked,
            latitud = currentLatitude,
            longitud = currentLongitude,
            foto = null,
            credito = binding.switchCredito.isChecked,
            limiteCredito = if (binding.switchCredito.isChecked) creditLimit else 0.0,
            diasEntrega = diasEntrega,
            horarioEntrega = horarioEntrega,
            usuarioId = preferenceManager.getUserId()
        )

        lifecycleScope.launch {
            binding.btnSave.isEnabled = false
            try {
                if (com.cabel.rutacabel.utils.NetworkUtils.isNetworkAvailable(this@AddClientActivity)) {
                    val response = RetrofitClient.apiService.registrarCliente(request)
                    if (response.isSuccessful && response.body()?.success == true) {
                        com.cabel.rutacabel.utils.UIUtils.showSuccessDialog(
                            this@AddClientActivity,
                            "Cliente registrado exitosamente"
                        ) {
                            finish()
                        }
                        return@launch
                    }
                }
                
                // Offline Logic
                val database = com.cabel.rutacabel.data.local.AppDatabase.getDatabase(applicationContext)
                val client = com.cabel.rutacabel.data.local.entities.Client(
                    name = name,
                    address = address,
                    colonia = colonia,
                    municipio = municipio,
                    estado = estado,
                    codigoPostal = codigoPostal,
                    phone = phone,
                    email = email,
                    taxId = taxId,
                    regimen = regimen,
                    esProspecto = binding.switchProspecto.isChecked,
                    latitude = currentLatitude,
                    longitude = currentLongitude,
                    credito = binding.switchCredito.isChecked,
                    creditLimit = if (binding.switchCredito.isChecked) creditLimit else 0.0,
                    diasEntrega = diasEntrega,
                    horarioEntrega = horarioEntrega,
                    sexo = 1,
                    needsSync = true,
                    remoteId = 0
                )
                database.clientDao().insertClient(client)
                
                // Trigger Sync if Online
                if (com.cabel.rutacabel.utils.NetworkUtils.isNetworkAvailable(this@AddClientActivity)) {
                    com.cabel.rutacabel.data.SyncWorker.runOnce(applicationContext, preferenceManager.getUserId())
                }
                
                com.cabel.rutacabel.utils.UIUtils.showSuccessDialog(
                    this@AddClientActivity,
                    "Guardado localmente (sin conexión)"
                ) {
                    finish()
                }

            } catch (e: Exception) {
                 // Final fallback for database error
                 Toast.makeText(this@AddClientActivity, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnSave.isEnabled = true
            }
        }
    }
}
