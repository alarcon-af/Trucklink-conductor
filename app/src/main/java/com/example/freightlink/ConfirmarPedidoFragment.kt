package com.example.freightlink

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.os.Parcelable
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.findFragment
import com.example.freightlink.MainActivity.Companion.ACCESS_FINE_LOCATION
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ConfirmarPedidoFragment : Fragment() {
    private var pedido: Pedido? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var myRef: DatabaseReference
    private lateinit var myRef2: DatabaseReference

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
        val volver = view.findViewById<Button>(R.id.cancelar)
        val confirmar = view.findViewById<Button>(R.id.confirmar)

        myRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot){
                val user = dataSnapshot.getValue(User::class.java)
                if(user!=null){
                    val nombreCompleto = "${user.nombre} ${user.apellido}"
                    nombre.text = nombreCompleto
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
            myRef2 = database.getReference("usuarios/").child(idPedido)

            val pedidoUpdates = hashMapOf<String, Any>(
                "driver" to userID.toString(), // Suponiendo que userID es el ID del conductor que confirma el pedido
                "estado" to "confirmado" // Actualizar el estado del pedido
                // Puedes agregar más campos aquí si es necesario
            )
            myRef2.updateChildren(pedidoUpdates).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Pedido confirmado", Toast.LENGTH_SHORT).show()
                    val mapaFragment = MapaFragment.newInstance(pedido!!)
                    val fragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
                    fragmentTransaction.replace(R.id.fragment_container, mapaFragment)
                    fragmentTransaction.addToBackStack(null)
                    fragmentTransaction.commit()
                } else {
                    Toast.makeText(requireContext(), "Error al confirmar pedido", Toast.LENGTH_SHORT).show()
                }
            }
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
