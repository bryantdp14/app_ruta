package com.cabel.rutacabel.ui.client

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cabel.rutacabel.R
import com.cabel.rutacabel.data.remote.ClienteItem
import com.cabel.rutacabel.databinding.ItemClientBinding
import com.cabel.rutacabel.ui.order.OrderActivity
import com.cabel.rutacabel.ui.payment.PaymentActivity

class ClientAdapter(
    private val showStartVisit: Boolean = false,
    private val onClientClick: (ClienteItem) -> Unit
) : ListAdapter<ClienteItem, ClientAdapter.ClientViewHolder>(ClientDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientViewHolder {
        val binding = ItemClientBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ClientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ClientViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ClientViewHolder(
        private val binding: ItemClientBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(client: ClienteItem) {
            binding.apply {
                // Client name
                tvClientName.text = client.nombre
                
                // Address
                val fullAddress = buildString {
                    append(client.direccion)
                    if (!client.colonia.isNullOrEmpty()) append(", ${client.colonia}")
                    if (!client.municipio.isNullOrEmpty()) append(", ${client.municipio}")
                }
                tvClientAddress.text = fullAddress
                
                // Phone (reusing tvClientPhone)
                tvClientPhone.text = client.telefono ?: ""
                tvClientPhone.visibility = if (!client.telefono.isNullOrEmpty()) View.VISIBLE else View.GONE
                
                // Status badge
                when {
                    client.esProspecto == 1 -> {
                        tvStatus.text = "PROSPECTO"
                        cvStatus.setCardBackgroundColor(itemView.context.getColor(R.color.warning_yellow))
                        tvStatus.setTextColor(itemView.context.getColor(android.R.color.black))
                    }
                    client.credito == 1 -> {
                        tvStatus.text = "CRÉDITO: $${client.limiteCredito}"
                        cvStatus.setCardBackgroundColor(itemView.context.getColor(R.color.success_green_light))
                        tvStatus.setTextColor(itemView.context.getColor(R.color.success_green))
                    }
                    else -> {
                        tvStatus.text = "CLIENTE"
                        cvStatus.setCardBackgroundColor(itemView.context.getColor(R.color.primary_blue_light))
                        tvStatus.setTextColor(itemView.context.getColor(R.color.primary_blue))
                    }
                }
                
                // Route number - use initial
                tvRouteNumber.text = client.nombre.firstOrNull()?.uppercase() ?: "?"
                
                // --- Action Buttons Visibility ---
                btnStartVisit.visibility = if (showStartVisit) View.VISIBLE else View.GONE
                btnIncident.visibility = View.GONE // Hide as per user request (only Pedido, Ruta, Cobro)
                
                // --- Button Icons and Colors ---
                // btnSale -> Hacer Pedido
                // btnPayment -> Hacer Cobro
                // btnNavigate -> Ver Ruta
                
                // Navigate button (Ver Ruta)
                btnNavigate.setOnClickListener {
                    client.latitude?.let { lat ->
                        client.longitude?.let { lng ->
                            val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            itemView.context.startActivity(mapIntent)
                        }
                    }
                }

                // btnSale -> Hacer Pedido
                btnSale.setOnClickListener {
                    val intent = Intent(itemView.context, OrderActivity::class.java).apply {
                        putExtra("CLIENT_ID", client.clienteId)
                        putExtra("CLIENT_NAME", client.nombre)
                        putExtra("CLIENT_TEL", client.telefono ?: "")
                    }
                    itemView.context.startActivity(intent)
                }

                btnPayment.setOnClickListener {
                    val intent = Intent(itemView.context, PaymentActivity::class.java)
                    itemView.context.startActivity(intent)
                }
                
                // Card click
                root.setOnClickListener {
                    onClientClick(client)
                }
            }
        }
    }

    class ClientDiffCallback : DiffUtil.ItemCallback<ClienteItem>() {
        override fun areItemsTheSame(oldItem: ClienteItem, newItem: ClienteItem): Boolean {
            return oldItem.clienteId == newItem.clienteId
        }

        override fun areContentsTheSame(oldItem: ClienteItem, newItem: ClienteItem): Boolean {
            return oldItem == newItem
        }
    }
}
