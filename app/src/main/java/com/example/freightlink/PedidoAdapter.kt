package com.example.freightlink

import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import android.content.Context

class PedidoAdapter(private val context:Context, private val pedidos: List<Pedido>,  private val callback: PedidoAdapterCallback) : RecyclerView.Adapter<PedidoAdapter.PedidoViewHolder>() {

    interface PedidoAdapterCallback {
        fun onPedidoClicked(pedido: Pedido)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pedido, parent, false)
        return PedidoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        val pedido = pedidos[position]
        Glide.with(context).load(R.drawable.white).into(holder.recImage)
        holder.recTitle.text = pedidos[position].cliente
        holder.recDesc.text = pedidos[position].carga
        holder.recCard.setOnClickListener{
            var pedidoVer =pedidos[holder.absoluteAdapterPosition]
            callback.onPedidoClicked(pedidoVer)
        }
    }

    override fun getItemCount(): Int = pedidos.size

    class PedidoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var recImage: ImageView
        var recTitle: TextView
        var recPrice: TextView
        var recDesc: TextView
        var recCard: CardView

        init {
            recImage = itemView.findViewById(R.id.recImage)
            recTitle = itemView.findViewById(R.id.recTitle)
            recPrice = itemView.findViewById(R.id.recPrice)
            recDesc = itemView.findViewById(R.id.recDesc)
            recCard = itemView.findViewById(R.id.recCard)
        }


        fun bind(pedido: Pedido, onPedidoClicked: (Pedido) -> Unit) {
            // Configura los elementos de la vista (TextViews, etc.) con la información del pedido
            // itemView.findViewById<TextView>(R.id.nombre_pedido).text = pedido.nombre
            itemView.setOnClickListener { onPedidoClicked(pedido) }
        }
    }
}
