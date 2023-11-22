package com.example.freightlink

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.findFragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CuentaFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CuentaFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance()
    private lateinit var myRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_cuenta, container, false)
        auth = FirebaseAuth.getInstance()
        myRef = database.getReference("conductores/").child(auth.getUid().toString())
        var nombre = view.findViewById<TextView>(R.id.nombre)
        var cedula = view.findViewById<TextView>(R.id.cedula)
        var correo = view.findViewById<TextView>(R.id.correo)
        var telefono = view.findViewById<TextView>(R.id.telefono)
        var foto = view.findViewById<ImageView>(R.id.fotoCuenta)
        myRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot){
                val user = dataSnapshot.getValue(Driver::class.java)
                if(user!=null){
                    nombre.text = "${user.nombre} ${user.apellido}"
                    correo.text = user.correo
                    cedula.text = user.cedula.toString()
                    telefono.text = user.telefono.toString()
                    Glide.with(requireActivity()).load(user.foto).into(foto)
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(requireActivity(), "Error en la base de datos", Toast.LENGTH_LONG).show()
            }
        })
        val salirButton = view.findViewById<ImageButton>(R.id.salir)
        salirButton.setOnClickListener {
            // Realiza la transacción de fragmentos para volver al HomeFragment
            val homeFragment = HomeFragment()
            val fragmentManager = requireActivity().supportFragmentManager
            fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, homeFragment)
                .addToBackStack(null) // Opcional: para agregar al back stack
                .commit()
        }

        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment CuentaFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            CuentaFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}