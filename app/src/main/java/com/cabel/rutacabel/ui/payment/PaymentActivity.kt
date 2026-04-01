package com.cabel.rutacabel.ui.payment

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.cabel.rutacabel.data.remote.CobroRegistroRequest
import com.cabel.rutacabel.data.remote.CuentaPorCobrarItem
import com.cabel.rutacabel.databinding.ActivityPaymentBinding
import com.cabel.rutacabel.utils.PreferenceManager

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private lateinit var viewModel: PaymentViewModel
    private lateinit var adapter: PaymentAdapter
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)
        viewModel = ViewModelProvider(this)[PaymentViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        observeViewModel()

        // Load initial data
        val branchId = preferenceManager.getUser()?.branchId ?: 1
        viewModel.loadPendingPayments(branchId)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = PaymentAdapter { debt ->
            showPaymentDialog(debt)
        }
        binding.rvPayments.layoutManager = LinearLayoutManager(this)
        binding.rvPayments.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.filterPayments(s.toString())
            }
        })
    }

    private fun observeViewModel() {
        viewModel.pendingPayments.observe(this) { payments ->
            adapter.submitList(payments)
            binding.tvEmptyState.visibility = if (payments.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.paymentSuccess.observe(this) { success ->
            if (success) {
                com.cabel.rutacabel.utils.UIUtils.showSuccessDialog(
                    this,
                    "Cobro registrado correctamente"
                ) {
                    // Reload list
                    val branchId = preferenceManager.getUser()?.branchId ?: 1
                    viewModel.loadPendingPayments(branchId)
                }
            }
        }
    }

    private fun showPaymentDialog(debt: CuentaPorCobrarItem) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Registrar Pago")
        
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 20, 50, 10)

        val etAmount = EditText(this)
        etAmount.hint = "Monto a pagar"
        etAmount.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        etAmount.setText(debt.monto.toString())
        layout.addView(etAmount)

        val etRef = EditText(this)
        etRef.hint = "Referencia / Comentario"
        layout.addView(etRef)

        builder.setView(layout)

        builder.setPositiveButton("Registrar") { dialog, _ ->
            val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
            val reference = etRef.text.toString()
            
            if (amount > 0) {
                val user = preferenceManager.getUser()
                val request = CobroRegistroRequest(
                    folio = debt.folioSucursal,
                    monto = amount,
                    metodoPagoId = 1, // Default Effective for now
                    referencia = reference,
                    comentarios = reference, // Using reference as comments for now
                    sucursalId = user?.branchId ?: 1,
                    usuarioId = user?.id ?: 0
                )
                viewModel.registerPayment(request)
            } else {
                Toast.makeText(this, "Ingrese un monto válido", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }
}
