package com.cabel.rutacabel.ui.inventory

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cabel.rutacabel.RutaCABELApplication
import com.cabel.rutacabel.data.local.entities.Product
import androidx.lifecycle.ViewModelProvider
import com.cabel.rutacabel.databinding.ActivityAddProductBinding
import com.cabel.rutacabel.utils.PreferenceManager
import kotlinx.coroutines.launch

class AddProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProductBinding
    private lateinit var viewModel: androidx.lifecycle.ViewModelProvider
    private lateinit var addProductViewModel: AddProductViewModel
    private lateinit var preferenceManager: com.cabel.rutacabel.utils.PreferenceManager

    private var selectedCategoryId: Int = 0
    private var selectedPresentationId: Int = 0
    private var selectedUnitId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = com.cabel.rutacabel.utils.PreferenceManager(this)
        addProductViewModel = androidx.lifecycle.ViewModelProvider(this)[AddProductViewModel::class.java]

        setupUI()
        observeViewModel()
        
        addProductViewModel.loadCatalogs()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            saveProduct()
        }
    }

    private fun observeViewModel() {
        addProductViewModel.categories.observe(this) { categories ->
            val adapter = android.widget.ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                categories.map { it.nombre }
            )
            binding.actvCategory.setAdapter(adapter)
            binding.actvCategory.setOnItemClickListener { _, _, position, _ ->
                selectedCategoryId = categories[position].categoriaId ?: 0
            }
        }

        addProductViewModel.presentations.observe(this) { presentations ->
            val adapter = android.widget.ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                presentations.map { it.nombre }
            )
            binding.actvPresentation.setAdapter(adapter)
            binding.actvPresentation.setOnItemClickListener { _, _, position, _ ->
                selectedPresentationId = presentations[position].presentacionId ?: 0
            }
        }

        addProductViewModel.units.observe(this) { units ->
            val adapter = android.widget.ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                units.map { it.nombre }
            )
            binding.actvUnit.setAdapter(adapter)
            binding.actvUnit.setOnItemClickListener { _, _, position, _ ->
                selectedUnitId = units[position].unidadMedidaId ?: 0
            }
        }

        addProductViewModel.errorMessage.observe(this) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }

        addProductViewModel.saveSuccess.observe(this) { success ->
            if (success) {
                com.cabel.rutacabel.utils.UIUtils.showSuccessDialog(
                    this,
                    "Producto registrado correctamente"
                ) {
                    finish()
                }
            }
        }
    }

    private fun saveProduct() {
        val code = binding.etCode.text.toString().trim()
        val name = binding.etName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val price = binding.etBasePrice.text.toString().toDoubleOrNull() ?: 0.0
        val stock = binding.etStock.text.toString().toIntOrNull() ?: 0

        if (code.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "Código y nombre son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedCategoryId == 0 || selectedPresentationId == 0 || selectedUnitId == 0) {
            Toast.makeText(this, "Por favor seleccione categoría, presentación y unidad", Toast.LENGTH_SHORT).show()
            return
        }

        val usuarioId = preferenceManager.getUser()?.id ?: 0L
        val sucursalId = preferenceManager.getUser()?.branchId ?: 0
        
        addProductViewModel.saveProduct(
            name = name,
            code = code,
            description = description,
            categoryId = selectedCategoryId,
            presentationId = selectedPresentationId,
            unitId = selectedUnitId,
            price = price,
            stock = stock,
            usuarioId = usuarioId,
            sucursalId = sucursalId
        )
    }
}
