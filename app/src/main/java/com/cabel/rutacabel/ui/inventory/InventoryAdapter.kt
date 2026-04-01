package com.cabel.rutacabel.ui.inventory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cabel.rutacabel.data.remote.InventarioItem
import com.cabel.rutacabel.databinding.ItemInventoryBinding
import java.text.NumberFormat
import java.util.*

class InventoryAdapter : ListAdapter<InventarioItem, InventoryAdapter.InventoryViewHolder>(InventoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val binding = ItemInventoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InventoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class InventoryViewHolder(
        private val binding: ItemInventoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: InventarioItem) {
            binding.apply {
                // Product ID
                tvProductId.text = "ID: ${item.productoId}"
                
                // Codigo (Barcode)
                if (!item.codigo.isNullOrEmpty()) {
                    tvCodigo.text = "Código: ${item.codigo}"
                } else {
                    tvCodigo.text = "Código: N/A"
                }
                
                // Product Name
                tvProductName.text = item.nombre
                
                // Almacen
                tvAlmacen.text = item.almacen
                
                // Sucursal
                tvSucursal.text = "📍 ${item.sucursal}"
                
                // Presentacion
                tvPresentacion.text = item.presentacion
                
                // Quantity
                tvQuantity.text = item.cantidad.toString()
                
                // Price (Public Price mapped from Prices API)
                if (item.precioPublico != null) {
                    tvCosto.text = String.format("%.2f", item.precioPublico)
                } else {
                    tvCosto.text = "N/A"
                }
                
                // Stock status
                when {
                    item.cantidad == 0 -> {
                        tvStockStatus.text = "Sin Stock"
                        cvStockStatus.setCardBackgroundColor(itemView.context.getColor(android.R.color.holo_red_light))
                        tvStockStatus.setTextColor(itemView.context.getColor(android.R.color.white))
                    }
                    item.cantidad < 10 -> {
                        tvStockStatus.text = "Stock Bajo"
                        cvStockStatus.setCardBackgroundColor(itemView.context.getColor(android.R.color.holo_orange_light))
                        tvStockStatus.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                    }
                    else -> {
                        tvStockStatus.text = "En Stock"
                        cvStockStatus.setCardBackgroundColor(itemView.context.getColor(android.R.color.holo_green_light))
                        tvStockStatus.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                    }
                }
            }
        }
    }

    class InventoryDiffCallback : DiffUtil.ItemCallback<InventarioItem>() {
        override fun areItemsTheSame(oldItem: InventarioItem, newItem: InventarioItem): Boolean {
            return oldItem.productoId == newItem.productoId
        }

        override fun areContentsTheSame(oldItem: InventarioItem, newItem: InventarioItem): Boolean {
            return oldItem == newItem
        }
    }
}
