package com.cabel.rutacabel.ui.route

import android.content.Intent
import android.net.Uri
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
import com.cabel.rutacabel.databinding.FragmentRouteBinding
import com.cabel.rutacabel.utils.PreferenceManager
import com.cabel.rutacabel.ui.route.AddClientActivity
import com.cabel.rutacabel.ui.route.AddIncidentActivity

class RouteFragment : Fragment() {

    private var _binding: FragmentRouteBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: RouteViewModel
    private lateinit var routeClientAdapter: RouteClientAdapter
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRouteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferenceManager = PreferenceManager(requireContext())
        viewModel = ViewModelProvider(this)[RouteViewModel::class.java]

        setupRecyclerView()
        setupUI()
        observeViewModel()
        
        val userId = preferenceManager.getUserId()
        viewModel.checkRouteStatus(userId)

        val empleadoId = preferenceManager.getEmpleadoId()
        if (empleadoId > 0) {
            viewModel.loadRouteDetails(empleadoId)
        }
    }

    private fun setupRecyclerView() {
        routeClientAdapter = RouteClientAdapter(
            onClientClick = { routeDetail ->
                val options = arrayOf("Marcar Visitado", "Reportar Incidencia", "Cancelar")
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(routeDetail.clienteNombre)
                    .setItems(options) { dialog, which ->
                        when (which) {
                            0 -> {
                                viewModel.markClientAsVisited(routeDetail.id)
                                Toast.makeText(requireContext(), "Cliente visitado", Toast.LENGTH_SHORT).show()
                            }
                            1 -> {
                                val intent = Intent(requireContext(), AddIncidentActivity::class.java)
                                intent.putExtra("CLIENT_ID", routeDetail.clienteId)
                                intent.putExtra("ROUTE_ID", routeDetail.rutaId)
                                startActivity(intent)
                            }
                            2 -> dialog.dismiss()
                        }
                    }
                    .show()
            },
            onNavigateClick = { routeDetail ->
                if (routeDetail.latitud != 0.0 && routeDetail.longitud != 0.0) {
                    val gmmIntentUri = Uri.parse("google.navigation:q=${routeDetail.latitud},${routeDetail.longitud}")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    
                    if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
                        startActivity(mapIntent)
                    } else {
                        Toast.makeText(requireContext(), "Google Maps no instalado", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Cliente sin ubicación GPS", Toast.LENGTH_SHORT).show()
                }
            }
        )

        binding.rvClients.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = routeClientAdapter
        }
    }

    private fun setupUI() {
        binding.btnBack.visibility = View.GONE // Removed back button from this context

        binding.btnNewClient.setOnClickListener {
            startActivity(Intent(requireContext(), AddClientActivity::class.java))
        }

        binding.btnStartRoute.setOnClickListener {
            viewModel.startRoute(preferenceManager.getUserId())
        }

        binding.btnEndRoute.setOnClickListener {
            viewModel.endRoute()
        }

        binding.etSearchClient.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    viewModel.searchClients(query)
                } else {
                    viewModel.loadRouteDetails(preferenceManager.getEmpleadoId())
                }
            }
        })
    }

    private fun observeViewModel() {
        viewModel.routeState.observe(viewLifecycleOwner) { route ->
            if (route != null && route.status == "ACTIVE") {
                binding.btnStartRoute.visibility = View.GONE
                binding.btnEndRoute.visibility = View.VISIBLE
                binding.cvProgress.visibility = View.VISIBLE
                binding.rvClients.alpha = 1.0f
                binding.rvClients.isEnabled = true
            } else {
                binding.btnStartRoute.visibility = View.VISIBLE
                binding.btnEndRoute.visibility = View.GONE
                binding.cvProgress.visibility = View.GONE
                binding.rvClients.alpha = 0.5f // Visual cue that route is not active
                binding.rvClients.isEnabled = false
            }
        }

        viewModel.routeDetails.observe(viewLifecycleOwner) { routeDetails ->
            updateRouteDetailsList(routeDetails)
            updateProgress(routeDetails)
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.message.observe(viewLifecycleOwner) { message ->
            if (message != null && message.isNotEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun updateProgress(routeDetails: List<com.cabel.rutacabel.data.local.entities.RouteDetail>) {
        if (routeDetails.isEmpty()) return
        val total = routeDetails.size
        val visited = routeDetails.count { it.visitado || it.estatus == 0 } // Assuming 0 means completed/visited in API context, but we use it.visitado locally
        val percent = if (total > 0) (visited * 100) / total else 0
        
        binding.tvRouteProgress.text = "$visited / $total"
        binding.tvProgressPercentage.text = "$percent%"
        binding.progressCircular.progress = percent
    }

    private fun updateRouteDetailsList(routeDetails: List<com.cabel.rutacabel.data.local.entities.RouteDetail>) {
        if (routeDetails.isEmpty()) {
            binding.rvClients.visibility = View.GONE
            binding.tvEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvClients.visibility = View.VISIBLE
            binding.tvEmptyState.visibility = View.GONE
            routeClientAdapter.submitList(routeDetails)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkRouteStatus(preferenceManager.getUserId())
    }
}
