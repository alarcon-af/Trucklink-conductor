package com.example.freightlink

import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.SensorEvent
import android.location.Location
import com.google.android.gms.location.*
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.example.freightlink.MainActivity.Companion.ACCESS_FINE_LOCATION
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MapaFragment : Fragment() {
    private var pedido: Pedido? = null
    private var mMap: GoogleMap? = null
    private var currentLocationMarker: Marker? = null
    private var lastRecordedLocation: LatLng? = null
    private var currentZoomLevel: Float = 18F
    private val distanceThreshold = 30.0
    private lateinit var sensorManager: SensorManager
    private lateinit var lightSensor: Sensor
    private lateinit var lightSensorListener: SensorEventListener
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var myRef: DatabaseReference
    private lateinit var myRef2: DatabaseReference
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback
    private lateinit var locationOrigen: LatLng
    private lateinit var locationDestino: LatLng
    private val LOCATION_PERMISSION_REQUEST = 1

    val noroesteMapa = LatLng(2.676054, -75.666414)
    val suresteMapa = LatLng(6.367807, -73.438245)
    val bounds = LatLngBounds(noroesteMapa, suresteMapa)

    private fun getLocationAccess(){
        if(ContextCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            mMap!!.isMyLocationEnabled = true
            getLocationUpdates()
            startLocationUpdates()
        }else{
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST -> {
                if((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                    enableMyLocation()
                    Toast.makeText(requireContext(), "permission granted :)", Toast.LENGTH_LONG).show()

                }else{
                    Toast.makeText(requireContext(), "permission denied", Toast.LENGTH_LONG).show()
                }
                return
            }
        }

    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap?.isMyLocationEnabled = true
        }
    }

    private fun getLocationUpdates(){
        mLocationRequest = LocationRequest()
        mLocationRequest.interval = 30000
        mLocationRequest.fastestInterval = 20000
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val idPedido = "${pedido!!.pedidoId ?: ""}"
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult){
                if (locationResult.locations.isNotEmpty()){
                    val location = locationResult.lastLocation

                    myRef = database.getReference("pedidos/").child(idPedido)
                    myRef.child("latitud").setValue(location!!.latitude)
                        .addOnSuccessListener {
                            Toast.makeText(requireActivity(), "latitud inscrita", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener{
                            Toast.makeText(requireActivity(), "latitud no inscrita", Toast.LENGTH_LONG).show()
                        }
                    myRef.child("longitud").setValue(location!!.longitude)
                        .addOnSuccessListener {
                            Toast.makeText(requireActivity(), "longitud inscrita", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener{
                            Toast.makeText(requireActivity(), "longitud no inscrita", Toast.LENGTH_LONG).show()
                        }

                    if(location != null){
                        val latLng = LatLng(location.latitude, location.longitude)
                        val markerOptions = MarkerOptions().position(latLng)
                        mMap!!.addMarker(markerOptions)
                        mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    }
                }
            }
        }
    }

    private val callback = OnMapReadyCallback { googleMap ->
        mMap = googleMap

        if (mMap != null) {
            mMap!!.uiSettings.isScrollGesturesEnabledDuringRotateOrZoom = true
            mMap!!.uiSettings?.isZoomControlsEnabled = true
            mMap!!.uiSettings?.isZoomGesturesEnabled = true
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
            val noroesteMapa = LatLng(4.4845, -74.1479)
            val suresteMapa =  LatLng(4.7634, -74.0039)
            val pedido = arguments?.getParcelable<Pedido>("pedido")
            centrarMapa(noroesteMapa, suresteMapa)
            //val geocoder = Geocoder(requireContext())
            mLocationRequest = createLocationRequest()
            if (pedido != null) {
                val origen = pedido.direccion_recoger
                val destino = pedido.direccion_entregar

                if (origen?.isNotEmpty() == true) {
                    locationOrigen = searchAddress(origen)!!
                    mMap?.addMarker(MarkerOptions().position(locationOrigen!!).title("Dirección donde recoger carga").icon(
                        BitmapDescriptorFactory.fromResource(R.drawable.start)))
                } else {
                    Toast.makeText(requireContext(), "Dirección de recogida no encontrada", Toast.LENGTH_SHORT).show()
                }

                if (destino?.isNotEmpty() == true) {
                    locationDestino = searchAddress(destino)!!
                    mMap?.addMarker(MarkerOptions().position(locationDestino!!).title("Dirección de entrega").icon(
                        BitmapDescriptorFactory.fromResource(R.drawable.end)))
                } else {
                    Toast.makeText(requireContext(), "Dirección de entrega no encontrada", Toast.LENGTH_SHORT).show()
                }

                if (locationOrigen!=null && locationDestino!=null){
                    centrarMapa(locationOrigen, locationDestino)
                }
                if (ActivityCompat.checkSelfPermission(
                        requireActivity(),
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ){
                    mLocationCallback = object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult) {
                            super.onLocationResult(locationResult)
                            val location = locationResult.lastLocation
                            Log.i("LOCATION", "Location update in the callback: $location")
                            if (location != null) {
                                val newLocation = LatLng(location.latitude, location.longitude)
                                if(lastRecordedLocation==null || calculateDistanceInMeters(
                                        lastRecordedLocation!!, newLocation) >= distanceThreshold){
                                    lastRecordedLocation = newLocation
                                    updateLocationOnMap(location.latitude, location.longitude)
                                    var zoomI = LatLng(location.latitude, location.longitude)
                                    mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(zoomI, currentZoomLevel))
                                }
                            }
                        }
                    }
                    getLocationAccess()
                    lightSensorListener = object : SensorEventListener {
                        override fun onSensorChanged(event: SensorEvent) {
                            val light = event.values[0]
                            if (mMap != null) {
                                if (light < 10) {
                                    mMap!!.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.style_dark))
                                } else {
                                    mMap!!.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.style_json))
                                }
                            }
                        }

                        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

                        }
                    }

                }else{
                    checkLocationPermission()
                }

            }



        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mapa, container, false)
        val sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        var estado = view.findViewById<TextView>(R.id.descPedido)
        var nombre = view.findViewById<TextView>(R.id.nombreCliente)
        var precio = view.findViewById<TextView>(R.id.precioPedido)
        var carga = view.findViewById<TextView>(R.id.cargaPedido)
        var destino = view.findViewById<TextView>(R.id.direccionSig)
        val botonSig = view.findViewById<Button>(R.id.sigDir)
        val idCli ="${pedido!!.cliente ?: ""}"
        val idPedido = "${pedido!!.pedidoId ?: ""}"
        myRef = database.getReference("usuarios/").child(idCli)
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
        precio.text = "\$xxx,xxx,xxx"
        carga.text = "${pedido!!.carga ?: ""}"
        destino.text = "${pedido!!.direccion_recoger ?: ""}"
        if (pedido!!.estado == "confirmado"){
            estado.text = "En camino a recoger"
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) /*|| ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            */) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(requireContext())
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
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
            ),
            MainActivity.ACCESS_FINE_LOCATION
        )
    }

    private fun centrarMapa(noroesteMapa: LatLng, suresteMapa: LatLng) {
        val centroMapa= LatLng(
            (noroesteMapa.latitude + suresteMapa.latitude) / 2,
            (noroesteMapa.longitude + suresteMapa.longitude) / 2
        )
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(centroMapa, 10f))
    }

    private fun searchAddress(address: String): LatLng? {
        val geocoder = Geocoder(requireActivity())
        val addresses = geocoder.getFromLocationName(address, 1)

        if (addresses?.isNotEmpty() == true) {
            val location = LatLng(addresses[0].latitude, addresses[0].longitude)
            return location
        } else {
            Toast.makeText(requireActivity(), "Dirección no encontrada", Toast.LENGTH_SHORT).show()
            return null
        }
    }

    private fun createLocationRequest(): LocationRequest {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000L
            fastestInterval = 5000L
        }

        return locationRequest
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                requireActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper())
        }
    }

    private fun updateLocationOnMap(latitude: Double, longitude: Double) {
        val currentLocation = LatLng(latitude, longitude)
        if (currentLocationMarker == null) {
            currentLocationMarker = mMap?.addMarker(
                MarkerOptions()
                    .position(currentLocation)
                    .title("Ubicación actual")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
        } else {
            currentLocationMarker?.position = currentLocation
        }
        mMap?.moveCamera(CameraUpdateFactory.newLatLng(currentLocation))
        currentZoomLevel = mMap?.cameraPosition?.zoom ?: currentZoomLevel
        mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, currentZoomLevel))
    }

    private fun calculateDistanceInMeters(a: LatLng, b: LatLng): Double {
        val locationA = Location("pointA").apply {
            latitude = a.latitude
            longitude = a.longitude
        }
        val locationB = Location("pointB").apply {
            latitude = b.latitude
            longitude = b.longitude
        }
        return locationA.distanceTo(locationB).toDouble()
    }


    companion object {
        fun newInstance(pedido: Pedido): MapaFragment {
            val fragment = MapaFragment()
            val args = Bundle()
            args.putParcelable("pedido", pedido)
            fragment.arguments = args
            return fragment
        }
    }
}