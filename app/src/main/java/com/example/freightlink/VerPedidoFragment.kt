package com.example.freightlink

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [VerPedidoFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class VerPedidoFragment : Fragment() {
    private var pedido: Pedido? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var myRef: DatabaseReference
    private lateinit var myRef2: DatabaseReference
    var imageUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            pedido = it.getParcelable(VerPedidoFragment.ARG_PEDIDO)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_ver_pedido, container, false)
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
        val estado = view.findViewById<TextView>(R.id.estadoPedido)
        val foto = view.findViewById<ImageView>(R.id.detailImage)
        val volver = view.findViewById<Button>(R.id.cancelar)

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
        estado.text = "${pedido!!.estado ?: ""}"

        volver.setOnClickListener {
            val historialPedidos =HistorialFragment()
            val fragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.fragment_container, historialPedidos)
            fragmentTransaction.addToBackStack(null)
            fragmentTransaction.commit()
        }

        return view
    }

    companion object {
        private const val ARG_PEDIDO = "pedido"

        @JvmStatic
        fun newInstance(pedido: Pedido) =
            VerPedidoFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PEDIDO, pedido)
                }
            }
    }
}