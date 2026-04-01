package com.cabel.rutacabel.ui.inventory

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cabel.rutacabel.RutaCABELApplication
import com.cabel.rutacabel.data.local.entities.Provider
import com.cabel.rutacabel.databinding.ActivityAddProviderBinding
import kotlinx.coroutines.launch

class AddProviderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProviderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProviderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            saveProvider()
        }
    }

    private fun saveProvider() {
        val name = binding.etName.text.toString().trim()
        val contactPerson = binding.etContactPerson.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "El nombre del proveedor es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }

        val provider = Provider(
            name = name,
            contactPerson = contactPerson,
            address = address,
            phone = phone,
            email = email,
            needsSync = true
        )

        lifecycleScope.launch {
            try {
                val db = (application as RutaCABELApplication).database
                db.providerDao().insertProvider(provider)
                com.cabel.rutacabel.utils.UIUtils.showSuccessDialog(
                    this@AddProviderActivity,
                    "Proveedor guardado exitosamente"
                ) {
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddProviderActivity, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
