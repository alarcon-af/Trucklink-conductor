package com.example.freightlink

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.freightlink.MainActivity.Companion.ACCESS_FINE_LOCATION
import com.example.freightlink.databinding.ActivityMenuBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Menu : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var fab: FloatingActionButton
    private lateinit var bnv: BottomNavigationView
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var binding: ActivityMenuBinding
    private var pedido: Pedido? = null
    private lateinit var myRef: DatabaseReference
    private lateinit var myRef2: DatabaseReference
    var eventListener: ValueEventListener? = null


    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        binding =ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        navigationView = findViewById<NavigationView>(R.id.nav_view)
        toolbar = findViewById<Toolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)
        navigationView.setNavigationItemSelectedListener(this)
        toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_nav, R.string.close_nav)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.bottomNavigationView.setOnItemSelectedListener {
            when(it.itemId){

                R.id.home -> replaceFragment(HomeFragment())
                R.id.mapa -> {
                    checkLocationPermission()
                }
                R.id.historial -> replaceFragment(HistorialFragment())
                R.id.cuenta -> replaceFragment(CuentaFragment())

                else ->{

                }
            }
            true
        }

        if(savedInstanceState == null)
            supportFragmentManager.beginTransaction().replace(R.id.fragment_container, HomeFragment()).commit()
            navigationView.setCheckedItem(R.id.nav_home)

    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.nav_home -> supportFragmentManager.beginTransaction().replace(R.id.fragment_container, HomeFragment()).commit()
            R.id.nav_settings -> supportFragmentManager.beginTransaction().replace(R.id.fragment_container, AjustesFragment()).commit()
            R.id.nav_share -> supportFragmentManager.beginTransaction().replace(R.id.fragment_container, HomeFragment()).commit()
            R.id.nav_about -> supportFragmentManager.beginTransaction().replace(R.id.fragment_container, AcercaFragment()).commit()
            R.id.nav_cuenta -> supportFragmentManager.beginTransaction().replace(R.id.fragment_container, CuentaFragment()).commit()
            R.id.nav_historial -> supportFragmentManager.beginTransaction().replace(R.id.fragment_container, HistorialFragment()).commit()
            R.id.nav_logout -> {
                auth.signOut()
                val intentMain = Intent(this, MainActivity::class.java)
                startActivity(intentMain)
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            ACCESS_FINE_LOCATION -> {
                if((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                    //Aqui va el proceso pa sacar los datos
                    database =FirebaseDatabase.getInstance()
                    myRef = database.getReference("pedidos")
                    val currentUser = auth.currentUser
                    val currentUserId = currentUser?.uid
                    eventListener = myRef.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for(itemSnapshot in snapshot.children){
                                val pedido = itemSnapshot.getValue(Pedido::class.java)
                                if(pedido!=null && pedido.driver == currentUserId){
                                    val fragmentTransaction = supportFragmentManager.beginTransaction()
                                    val mapaFragment = MapaFragment.newInstance(pedido)
                                    fragmentTransaction.replace(R.id.fragment_container, mapaFragment)
                                    fragmentTransaction.commit()
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            TODO("Not yet implemented")
                        }
                    } )


                    Toast.makeText(this, "permission granted :)", Toast.LENGTH_LONG).show()

                }else{
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show()
                }
                return
            }
            /*ACCESS_COARSE_LOCATION -> {
                if((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                    //Aqui va el proceso pa sacar los datos
                    Toast.makeText(this, "permission granted :)", Toast.LENGTH_LONG).show()
                }else{
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show()
                }
                return
            }*/
        }

    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) /*|| ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            */) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(this)
                    .setTitle("Location Permission Needed")
                    .setMessage("This app needs the Location permission, please accept to use location functionality")
                    .setPositiveButton(
                        "OK"
                    ) { _, _ ->
                        //Prompt the user once explanation has been shown
                        requestLocationPermission()
                    }
                    .create()
                    .show()
            } else {
                // No explanation needed, we can request the permission.
                requestLocationPermission()
            }
        }else{
            database =FirebaseDatabase.getInstance()
            myRef = database.getReference("pedidos")
            val currentUser = auth.currentUser
            val currentUserId = currentUser?.uid
            eventListener = myRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for(itemSnapshot in snapshot.children){
                        val pedido = itemSnapshot.getValue(Pedido::class.java)
                        if(pedido!=null && pedido.driver == currentUserId){
                            val fragmentTransaction = supportFragmentManager.beginTransaction()
                            val mapaFragment = MapaFragment.newInstance(pedido)
                            fragmentTransaction.replace(R.id.fragment_container, mapaFragment)
                            fragmentTransaction.commit()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            } )


            Toast.makeText(this, "permission granted :)", Toast.LENGTH_LONG).show()

        }
    }


    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
            ),
            ACCESS_FINE_LOCATION
        )
    }

    private fun replaceFragment(fragment: Fragment){
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container, fragment)
        fragmentTransaction.commit()
    }

    override fun onBackPressed(){
        if(drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(GravityCompat.START)
        } else{
            onBackPressedDispatcher.onBackPressed()
        }
    }




}