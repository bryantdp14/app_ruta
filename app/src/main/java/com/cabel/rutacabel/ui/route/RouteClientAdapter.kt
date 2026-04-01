package com.cabel.rutacabel.ui.route

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cabel.rutacabel.data.local.entities.RouteDetail
import com.cabel.rutacabel.databinding.ItemRouteClientBinding

class RouteClientAdapter(
    private val onClientClick: (RouteDetail) -> Unit,
    private val onNavigateClick: (RouteDetail) -> Unit
) : ListAdapter<RouteDetail, RouteClientAdapter.RouteClientViewHolder>(RouteClientDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteClientViewHolder {
        val binding = ItemRouteClientBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RouteClientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RouteClientViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RouteClientViewHolder(
        private val binding: ItemRouteClientBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(routeDetail: RouteDetail) {
            binding.apply {
                tvClientName.text = routeDetail.clienteNombre
                tvConsecutivo.text = "# ${routeDetail.consecutivo}"
                tvFolioSucursal.text = routeDetail.folioSucursal ?: "Sin folio"
                
                // Indicador de visita
                if (routeDetail.visitado) {
                    ivVisitStatus.setImageResource(android.R.drawable.checkbox_on_background)
                    cardClient.alpha = 0.6f
                } else {
                    ivVisitStatus.setImageResource(android.R.drawable.checkbox_off_background)
                    cardClient.alpha = 1.0f
                }

                // Click listeners
                root.setOnClickListener { onClientClick(routeDetail) }
                btnNavigate.setOnClickListener { onNavigateClick(routeDetail) }
                btnStartVisit.setOnClickListener { onClientClick(routeDetail) } // Share the same logic as marking visited for now
            }
        }
    }

    class RouteClientDiffCallback : DiffUtil.ItemCallback<RouteDetail>() {
        override fun areItemsTheSame(oldItem: RouteDetail, newItem: RouteDetail): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RouteDetail, newItem: RouteDetail): Boolean {
            return oldItem == newItem
        }
    }
}
