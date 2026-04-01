package com.cabel.rutacabel.ui.route

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cabel.rutacabel.data.local.entities.Client
import com.cabel.rutacabel.databinding.ItemClientBinding

class ClientsAdapter(
    private val onSaleClick: (Client) -> Unit,
    private val onPaymentClick: (Client) -> Unit,
    private val onNavigateClick: (Client) -> Unit,
    private val onIncidentClick: (Client) -> Unit
) : ListAdapter<Client, ClientsAdapter.ClientViewHolder>(ClientDiffCallback()) {

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

        fun bind(client: Client) {
            binding.apply {
                tvClientName.text = client.name
                tvClientAddress.text = client.address
                tvClientPhone.text = client.phone
                tvRouteNumber.text = client.routeOrder.toString()

                btnSale.setOnClickListener { onSaleClick(client) }
                btnPayment.setOnClickListener { onPaymentClick(client) }
                btnNavigate.setOnClickListener { onNavigateClick(client) }
                btnIncident.setOnClickListener { onIncidentClick(client) }
            }
        }
    }

    private class ClientDiffCallback : DiffUtil.ItemCallback<Client>() {
        override fun areItemsTheSame(oldItem: Client, newItem: Client): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Client, newItem: Client): Boolean {
            return oldItem == newItem
        }
    }
}
