package com.example.freightlink

import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.location.LocationRequest
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapaFragment : Fragment() {

    private var mMap: GoogleMap? = null
    private var currentLocationMarker: Marker? = null
    private var lastRecordedLocation: LatLng? = null
    private var currentZoomLevel: Float = 18F
    private lateinit var sensorManager: SensorManager
    private lateinit var lightSensor: Sensor
    private lateinit var lightSensorListener: SensorEventListener

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback
    private lateinit var locationOrigen: LatLng
    private lateinit var locationDestino: LatLng

    val noroesteMapa = LatLng(6.367807, -75.666414)
    val suresteMapa = LatLng(2.676054, -73.438245)
    val bounds = LatLngBounds(noroesteMapa, suresteMapa)

    private val callback = OnMapReadyCallback { googleMap ->
        mMap = googleMap

        if (mMap != null) {
            mMap!!.uiSettings.isScrollGesturesEnabledDuringRotateOrZoom = true
            mMap!!.uiSettings?.isZoomControlsEnabled = true
            mMap!!.uiSettings?.isZoomGesturesEnabled = true
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
            val noroesteMapa = LatLng(6.367807, -75.666414)
            val suresteMapa = LatLng(2.676054, -73.438245)
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
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
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