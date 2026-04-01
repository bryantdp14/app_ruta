package com.cabel.rutacabel.ui.payment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cabel.rutacabel.data.remote.CuentaPorCobrarItem
import com.cabel.rutacabel.databinding.ItemPaymentDebtBinding
import java.text.NumberFormat
import java.util.Locale

class PaymentAdapter(
    private val onPayClick: (CuentaPorCobrarItem) -> Unit
) : ListAdapter<CuentaPorCobrarItem, PaymentAdapter.PaymentViewHolder>(PaymentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val binding = ItemPaymentDebtBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PaymentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PaymentViewHolder(
        private val binding: ItemPaymentDebtBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "MX"))

        fun bind(item: CuentaPorCobrarItem) {
            binding.apply {
                tvFolio.text = "Folio: ${item.folioSucursal}"
                tvAmount.text = currencyFormat.format(item.monto)
                tvClientName.text = item.clienteNombre
                tvDate.text = "Vence: ${item.fechaVencimiento ?: "N/A"}"

                btnPay.setOnClickListener { onPayClick(item) }
            }
        }
    }

    class PaymentDiffCallback : DiffUtil.ItemCallback<CuentaPorCobrarItem>() {
        override fun areItemsTheSame(oldItem: CuentaPorCobrarItem, newItem: CuentaPorCobrarItem): Boolean {
            return oldItem.folioSucursal == newItem.folioSucursal
        }

        override fun areContentsTheSame(oldItem: CuentaPorCobrarItem, newItem: CuentaPorCobrarItem): Boolean {
            return oldItem == newItem
        }
    }
}
