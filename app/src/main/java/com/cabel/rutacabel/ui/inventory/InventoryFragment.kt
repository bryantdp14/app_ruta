package com.cabel.rutacabel.ui.inventory

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.cabel.rutacabel.R
import com.cabel.rutacabel.databinding.FragmentInventoryBinding
import com.cabel.rutacabel.utils.PreferenceManager
import com.google.android.material.chip.Chip

class InventoryFragment : Fragment() {

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: InventoryViewModel
    private lateinit var inventoryAdapter: InventoryAdapter
    private lateinit var preferenceManager: PreferenceManager
    private var selectedCategory: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferenceManager = PreferenceManager(requireContext())
        viewModel = ViewModelProvider(this)[InventoryViewModel::class.java]
        
        setupRecyclerView()
        setupUI()
        observeViewModel()
        
        // Load inventory for user's branch
        val branchId = preferenceManager.getBranchId()
        if (branchId > 0) {
            viewModel.loadInventory(branchId)
        } else {
            Toast.makeText(
                requireContext(),
                "Error: No se encontró sucursal asignada",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun setupRecyclerView() {
        inventoryAdapter = InventoryAdapter()
        
        binding.rvInventory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = inventoryAdapter
        }
    }

    private fun setupUI() {
        binding.btnSync.setOnClickListener {
            val branchId = preferenceManager.getBranchId()
            if (branchId > 0) {
                Toast.makeText(requireContext(), "Sincronizando inventario...", Toast.LENGTH_SHORT).show()
                viewModel.loadInventory(branchId)
            }
        }

        binding.btnAddProduct.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), AddProductActivity::class.java))
        }

        binding.btnAddProvider.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), AddProviderActivity::class.java))
        }
        
        binding.etSearchProduct.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                viewModel.searchInventory(query)
            }
        })
    }

    private fun observeViewModel() {
        viewModel.allItems.observe(viewLifecycleOwner) { items ->
            // Generate category chips from unique presentation types
            setupCategoryChips(items)
        }
        
        viewModel.filteredItems.observe(viewLifecycleOwner) { items ->
            updateInventoryList(items)
            updateInventoryCount(items.size)
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun setupCategoryChips(items: List<com.cabel.rutacabel.data.remote.InventarioItem>) {
        // Get unique presentation types
        val categories = items.map { it.presentacion }.distinct().sorted()
        
        android.util.Log.d("InventoryFragment", "Setting up chips for ${categories.size} categories: $categories")
        
        // Find the chip container directly by ID
        val chipContainer = view?.findViewById<LinearLayout>(R.id.chipContainer)
        
        if (chipContainer == null) {
            android.util.Log.e("InventoryFragment", "Chip container not found!")
            return
        }
        
        chipContainer.removeAllViews()
        
        // Add "Todos" chip
        val allChip = createCategoryChip("Todos", null)
        chipContainer.addView(allChip)
        
        // Add chips for each category
        categories.forEach { category ->
            val chip = createCategoryChip(category, category)
            chipContainer.addView(chip)
        }
        
        android.util.Log.d("InventoryFragment", "Added ${chipContainer.childCount} chips to container")
    }
    
    private fun createCategoryChip(label: String, categoryFilter: String?): Chip {
        return Chip(requireContext()).apply {
            text = label
            isCheckable = true
            isChecked = selectedCategory == categoryFilter
            
            // Set chip dimensions and margins
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            // convert 8dp to pixels for margin
            val marginInPx = (8 * resources.displayMetrics.density).toInt()
            layoutParams.setMargins(0, 0, marginInPx, 0)
            this.layoutParams = layoutParams
            
            // Set chip corner radius
            chipCornerRadius = 18f * resources.displayMetrics.density
            
            // Set colors based on selection
            updateChipColors(this, isChecked)
            
            setOnClickListener {
                selectedCategory = categoryFilter
                viewModel.filterByCategory(categoryFilter)
                
                // Update all chips in container
                val container = binding.root.findViewById<LinearLayout>(R.id.chipContainer)
                for (i in 0 until (container?.childCount ?: 0)) {
                    val chip = container?.getChildAt(i) as? Chip
                    chip?.let { chipView ->
                        val isSelected = chipView.text?.toString() == label
                        chipView.isChecked = isSelected
                        updateChipColors(chipView, isSelected)
                    }
                }
            }
        }
    }
    
    private fun updateChipColors(chip: Chip, isSelected: Boolean) {
        if (isSelected) {
            chip.chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.primary_blue)
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        } else {
            chip.chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.grey_light)
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        }
    }
    
    private fun updateInventoryCount(count: Int) {
        binding.tvInventoryCount.text = "Total: $count Productos"
    }
    
    private fun updateInventoryList(items: List<com.cabel.rutacabel.data.remote.InventarioItem>) {
        if (items.isEmpty()) {
            binding.rvInventory.visibility = View.GONE
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.tvEmptyState.text = "No hay productos en inventario"
        } else {
            binding.rvInventory.visibility = View.VISIBLE
            binding.tvEmptyState.visibility = View.GONE
            inventoryAdapter.submitList(items)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
