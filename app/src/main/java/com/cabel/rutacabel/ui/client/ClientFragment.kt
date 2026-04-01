package com.cabel.rutacabel.ui.client

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.cabel.rutacabel.databinding.FragmentClientBinding
import com.cabel.rutacabel.ui.route.AddClientActivity
import com.cabel.rutacabel.utils.PreferenceManager

class ClientFragment : Fragment() {

    private var _binding: FragmentClientBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ClientViewModel
    private lateinit var clientAdapter: ClientAdapter
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClientBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferenceManager = PreferenceManager(requireContext())
        viewModel = ViewModelProvider(this)[ClientViewModel::class.java]

        setupRecyclerView()
        setupUI()
        observeViewModel()

        // Load clients for user's branch
        val branchId = preferenceManager.getBranchId()
        if (branchId > 0) {
            viewModel.loadClients(branchId)
        } else {
            Toast.makeText(
                requireContext(),
                "Error: No se encontró sucursal asignada",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun setupRecyclerView() {
        clientAdapter = ClientAdapter(showStartVisit = false) { client ->
            // Handle client click - could navigate to detail view
            Toast.makeText(
                requireContext(),
                "Cliente: ${client.nombre}",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.rvClients.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = clientAdapter
        }
    }

    private fun setupUI() {
        // FAB - Add new client
        binding.fabAddClient.setOnClickListener {
            startActivity(Intent(requireContext(), AddClientActivity::class.java))
        }

        // Search
        binding.etSearchClient.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                viewModel.searchClients(query)
            }
        })

        // Filter chips
        binding.chipAll.setOnClickListener {
            setActiveChip(binding.chipAll.id)
            viewModel.setFilter(ClientViewModel.ClientFilter.ALL)
        }

        binding.chipProspects.setOnClickListener {
            setActiveChip(binding.chipProspects.id)
            viewModel.setFilter(ClientViewModel.ClientFilter.PROSPECTS)
        }

        binding.chipWithCredit.setOnClickListener {
            setActiveChip(binding.chipWithCredit.id)
            viewModel.setFilter(ClientViewModel.ClientFilter.WITH_CREDIT)
        }

        binding.chipToVisit.setOnClickListener {
            setActiveChip(binding.chipToVisit.id)
            viewModel.setFilter(ClientViewModel.ClientFilter.TO_VISIT)
        }
    }

    private fun setActiveChip(activeChipId: Int) {
        val chips = listOf(
            binding.chipAll,
            binding.chipProspects,
            binding.chipWithCredit,
            binding.chipToVisit
        )

        chips.forEach { chip ->
            if (chip.id == activeChipId) {
                chip.setChipBackgroundColorResource(com.cabel.rutacabel.R.color.primary_blue)
                chip.setTextColor(requireContext().getColor(com.cabel.rutacabel.R.color.white))
            } else {
                chip.setChipBackgroundColorResource(com.cabel.rutacabel.R.color.grey_light)
                chip.setTextColor(requireContext().getColor(com.cabel.rutacabel.R.color.text_primary))
            }
        }
    }

    private fun observeViewModel() {
        viewModel.filteredClients.observe(viewLifecycleOwner) { clients ->
            updateClientList(clients)
            binding.tvClientCount.text = "TOTAL: ${clients.size} CLIENTES"
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

    private fun updateClientList(clients: List<com.cabel.rutacabel.data.remote.ClienteItem>) {
        if (clients.isEmpty()) {
            binding.rvClients.visibility = View.GONE
            binding.tvEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvClients.visibility = View.VISIBLE
            binding.tvEmptyState.visibility = View.GONE
            clientAdapter.submitList(clients)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
