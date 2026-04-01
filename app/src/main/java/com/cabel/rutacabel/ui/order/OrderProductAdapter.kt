package com.cabel.rutacabel.ui.order

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cabel.rutacabel.databinding.ItemOrderProductBinding
import java.text.NumberFormat
import java.util.*

class OrderProductAdapter(
    private val onQuantityChanged: (OrderProduct, Int) -> Unit,
    private val onPriceListClicked: (OrderProduct) -> Unit
) : ListAdapter<OrderProduct, OrderProductAdapter.OrderProductViewHolder>(OrderProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderProductViewHolder {
        val binding = ItemOrderProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class OrderProductViewHolder(
        private val binding: ItemOrderProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(orderProduct: OrderProduct) {
            val item = orderProduct.inventarioItem
            binding.apply {
                tvProductCode.text = "Cód: ${item.codigo ?: "N/A"}"
                tvProductName.text = item.nombre
                tvStock.text = "Stock: ${item.cantidad}"
                
                val formatter = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
                tvPrice.text = formatter.format(orderProduct.selectedPrice?.precio ?: 0.0)
                tvPriceListName.text = if (orderProduct.selectedPrice != null) 
                    "(${orderProduct.selectedPrice?.listaPrecio})" else ""
                
                etQuantity.setText(orderProduct.quantity.toString())
                
                llPriceContainer.setOnClickListener {
                    onPriceListClicked(orderProduct)
                }

                btnMinus.setOnClickListener {
                    val currentQty = etQuantity.text.toString().toIntOrNull() ?: 0
                    if (currentQty > 0) {
                        val newQty = currentQty - 1
                        etQuantity.setText(newQty.toString())
                        onQuantityChanged(orderProduct, newQty)
                    }
                }
                
                btnPlus.setOnClickListener {
                    val currentQty = etQuantity.text.toString().toIntOrNull() ?: 0
                    if (currentQty < item.cantidad) {
                        val newQty = currentQty + 1
                        etQuantity.setText(newQty.toString())
                        onQuantityChanged(orderProduct, newQty)
                    }
                }
            }
        }
    }
    class OrderProductDiffCallback : DiffUtil.ItemCallback<OrderProduct>() {
        override fun areItemsTheSame(oldItem: OrderProduct, newItem: OrderProduct): Boolean {
            return oldItem.inventarioItem.productoId == newItem.inventarioItem.productoId
        }

        override fun areContentsTheSame(oldItem: OrderProduct, newItem: OrderProduct): Boolean {
            return oldItem == newItem
        }
    }
}
