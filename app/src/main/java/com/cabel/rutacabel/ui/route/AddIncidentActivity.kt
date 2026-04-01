package com.cabel.rutacabel.ui.route

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.cabel.rutacabel.databinding.ActivityAddIncidentBinding

class AddIncidentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddIncidentBinding
    private val viewModel: AddIncidentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddIncidentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupSpinner()
        setupListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupSpinner() {
        val types = listOf("Cliente Cerrado", "No tiene dinero", "No quiso producto", "Otro")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spIncidentType.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            val clientId = intent.getLongExtra("CLIENT_ID", 0)
            val routeId = intent.getLongExtra("ROUTE_ID", 0)
            val type = binding.spIncidentType.selectedItem.toString()
            val description = binding.etDescription.text.toString()

            viewModel.saveIncident(clientId, routeId, type, description)
        }
    }

    private fun observeViewModel() {
        viewModel.saveSuccess.observe(this) { success ->
            if (success) {
                finish()
            }
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
             binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
             binding.btnSave.isEnabled = !isLoading
        }
    }
}
