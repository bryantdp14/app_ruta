package com.cabel.rutacabel.ui.dashboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.cabel.rutacabel.R
import com.cabel.rutacabel.data.SyncWorker
import com.cabel.rutacabel.databinding.FragmentDashboardBinding
import com.cabel.rutacabel.utils.PreferenceManager

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DashboardViewModel
    private lateinit var preferenceManager: PreferenceManager

    private val syncReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == SyncWorker.ACTION_SYNC_STATUS) {
                val syncStarted = intent.getBooleanExtra(SyncWorker.EXTRA_SYNC_STARTED, false)
                val syncFinished = intent.getBooleanExtra(SyncWorker.EXTRA_SYNC_FINISHED, false)
                val progress = intent.getIntExtra(SyncWorker.EXTRA_PROGRESS, -1)
                val detail = intent.getStringExtra(SyncWorker.EXTRA_DETAIL) ?: ""

                if (syncStarted) viewModel.onSyncStarted()
                if (progress != -1) viewModel.updateSyncProgress(progress, detail)
                if (syncFinished) viewModel.onSyncCompleted()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferenceManager = PreferenceManager(requireContext())
        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]

        setupUI()
        observeViewModel()
        loadUserProfile()
    }

    private fun loadUserProfile() {
        binding.tvUserName.text = preferenceManager.getFullName().ifEmpty { preferenceManager.getUsername() }
        binding.tvUserRole.text = preferenceManager.getRole().ifEmpty { "Usuario" }

        val photoUrl = preferenceManager.getPhotoUrl()
        if (photoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(photoUrl)
                .circleCrop()
                .placeholder(android.R.drawable.ic_menu_myplaces)
                .into(binding.ivUserAvatar)
        }
    }

    private fun setupUI() {
        binding.btnSync.setOnClickListener {
            viewModel.triggerSync(preferenceManager.getUserId())
        }
        
        binding.cvPrinterCard.setOnClickListener {
            // Option to go to printer settings if we added one, 
            // but for now just showing info.
            Toast.makeText(requireContext(), "Impresora se configura al crear un pedido", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.message.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }

        // --- Printer Status Observers ---
        viewModel.printerName.observe(viewLifecycleOwner) { name ->
            binding.tvPrinterName.text = name
        }

        viewModel.isPrinterConfigured.observe(viewLifecycleOwner) { isConfigured ->
            if (isConfigured) {
                binding.ivPrinterIcon.setImageResource(android.R.drawable.ic_menu_set_as)
                binding.ivPrinterIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary_blue))
                binding.cvPrinterIconBg.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary_blue_light))
                binding.tvPrinterStatus.text = "Configurada y Lista"
                binding.tvPrinterStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.success_green))
            } else {
                binding.ivPrinterIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                binding.ivPrinterIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_hint))
                binding.cvPrinterIconBg.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.grey_light))
                binding.tvPrinterStatus.text = "Sin impresora configurada"
                binding.tvPrinterStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_hint))
            }
        }

        // --- Sync Status Observers ---
        viewModel.syncItems.observe(viewLifecycleOwner) { items ->
            updateSyncRow(binding.ivSyncClientes, binding.tvSyncClientes, items.getOrNull(0))
            updateSyncRow(binding.ivSyncProductos, binding.tvSyncProductos, items.getOrNull(1))
            updateSyncRow(binding.ivSyncVentas, binding.tvSyncVentas, items.getOrNull(2))
            updateSyncRow(binding.ivSyncCobros, binding.tvSyncCobros, items.getOrNull(3))
            updateSyncRow(binding.ivSyncPedidos, binding.tvSyncPedidos, items.getOrNull(4))
        }

        viewModel.syncProgress.observe(viewLifecycleOwner) { progress ->
            binding.syncProgressBar.progress = progress
        }

        viewModel.syncDetail.observe(viewLifecycleOwner) { detail ->
            binding.tvSyncDetail.text = detail
        }

        viewModel.hasPendingSync.observe(viewLifecycleOwner) { hasPending ->
            binding.btnSync.isEnabled = hasPending
            if (hasPending) {
                binding.btnSync.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.success_green))
                binding.btnSync.text = "Sincronizar Ahora"
            } else {
                binding.btnSync.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.grey_medium))
                binding.btnSync.text = "Todo sincronizado"
            }
        }

        viewModel.isSyncing.observe(viewLifecycleOwner) { isSyncing ->
            binding.btnSync.isEnabled = !isSyncing
            binding.syncProgressBar.visibility = if (isSyncing) View.VISIBLE else View.GONE
            binding.tvSyncDetail.visibility = if (isSyncing) View.VISIBLE else View.GONE
            if (isSyncing) {
                binding.btnSync.text = "Sincronizando..."
            }
        }
    }

    private fun updateSyncRow(icon: ImageView, label: TextView, item: SyncItemStatus?) {
        if (item == null) return
        val ctx = requireContext()

        if (item.pendingCount > 0) {
            icon.setColorFilter(ContextCompat.getColor(ctx, R.color.warning_orange))
            label.text = "${item.pendingCount} pendiente${if (item.pendingCount > 1) "s" else ""}"
            label.setTextColor(ContextCompat.getColor(ctx, R.color.warning_orange))
        } else {
            icon.setColorFilter(ContextCompat.getColor(ctx, R.color.success_green))
            if (item.totalCount > 0) {
                label.text = "${item.totalCount} sincronizados"
            } else {
                label.text = "Sincronizado"
            }
            label.setTextColor(ContextCompat.getColor(ctx, R.color.text_hint))
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(SyncWorker.ACTION_SYNC_STATUS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(syncReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            requireContext().registerReceiver(syncReceiver, filter)
        }
        // Refresh data on resume
        viewModel.loadDashboardData()
    }

    override fun onPause() {
        super.onPause()
        try {
            requireContext().unregisterReceiver(syncReceiver)
        } catch (_: Exception) { }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
