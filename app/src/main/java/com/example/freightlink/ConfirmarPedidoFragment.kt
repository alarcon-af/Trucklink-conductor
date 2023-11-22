package com.example.freightlink

import com.bumptech.glide.Glide
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ConfirmarPedidoFragment : Fragment() {
    private var pedido: Pedido? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var myRef: DatabaseReference
    private lateinit var myRef2: DatabaseReference
    var imageUrl = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            pedido = it.getParcelable(ARG_PEDIDO)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_confirmar_pedido, container, false)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        val idCli ="${pedido!!.cliente ?: ""}"
        val idPedido = "${pedido!!.pedidoId ?: ""}"
        myRef = database.getReference("usuarios/").child(idCli)
        val userID = auth.currentUser!!.uid
        val nombre =view.findViewById<TextView>(R.id.nombreCliente)
        val carga = view.findViewById<TextView>(R.id.cargaPedido)
        val peso = view.findViewById<TextView>(R.id.pesoPedido)
        val recoger = view.findViewById<TextView>(R.id.recolectarPedido)
        val entregar = view.findViewById<TextView>(R.id.entregaPedido)
        val precio = view.findViewById<TextView>(R.id.precioPedido)
        val foto = view.findViewById<ImageView>(R.id.detailImage)
        val volver = view.findViewById<Button>(R.id.cancelar)
        val confirmar = view.findViewById<Button>(R.id.confirmar)

        myRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot){
                val user = dataSnapshot.getValue(User::class.java)
                if(user!=null){
                    val nombreCompleto = "${user.nombre} ${user.apellido}"
                    nombre.text = nombreCompleto
                    Glide.with(requireActivity()).load(user.foto).into(foto)
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(requireContext(), "Error en la base de datos", Toast.LENGTH_LONG).show()
            }
        })
        carga.text = "${pedido!!.carga ?: ""}"
        peso.text = "${pedido!!.peso ?: ""}"
        recoger.text = "${pedido!!.direccion_recoger ?: ""}"
        entregar.text = "${pedido!!.direccion_entregar ?: ""}"
        precio.text = "\$xxx,xxx,xxx"

        volver.setOnClickListener {
            val listaPedidos =ListaPedidosFragment()
            val fragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.fragment_container, listaPedidos)
            fragmentTransaction.addToBackStack(null)
            fragmentTransaction.commit()
        }

        confirmar.setOnClickListener {
            myRef2 = database.getReference("pedidos").child(idPedido)

            myRef2.child("driver").setValue(userID.toString()) // Actualizar el campo "driver"
            myRef2.child("estado").setValue("confirmado") // Actualizar el campo "estado"
            // Puedes agregar más líneas para actualizar otros campos si es necesario

            myRef2.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // Actualización completada, puedes realizar acciones adicionales aquí
                    Toast.makeText(requireContext(), "Pedido confirmado", Toast.LENGTH_SHORT).show()
                    val mapaFragment = MapaFragment.newInstance(pedido!!)
                    val fragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
                    fragmentTransaction.replace(R.id.fragment_container, mapaFragment)
                    fragmentTransaction.addToBackStack(null)
                    fragmentTransaction.commit()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Manejar errores si es necesario
                    Toast.makeText(requireContext(), "Error al confirmar pedido", Toast.LENGTH_SHORT).show()
                }
            })
        }

        return view
    }

    companion object {
        private const val ARG_PEDIDO = "pedido"

        @JvmStatic
        fun newInstance(pedido: Pedido) =
            ConfirmarPedidoFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PEDIDO, pedido)
                }
            }
    }
}
