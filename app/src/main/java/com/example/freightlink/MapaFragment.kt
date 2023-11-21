package com.example.freightlink

import android.location.Geocoder
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapaFragment : Fragment() {

    private var mMap: GoogleMap? = null

    private val callback = OnMapReadyCallback { googleMap ->
        mMap = googleMap

        if (mMap != null) {
            mMap!!.uiSettings.isScrollGesturesEnabledDuringRotateOrZoom = true
            mMap!!.uiSettings?.isZoomControlsEnabled = true
            mMap!!.uiSettings?.isZoomGesturesEnabled = true

            val pedido = arguments?.getParcelable<Pedido>("pedido")

            val geocoder = Geocoder(requireContext())

            if (pedido != null) {
                val origen = geocoder.getFromLocationName(pedido.direccion_recoger, 1)
                val destino = geocoder.getFromLocationName(pedido.direccion_entregar, 1)

                if (origen?.isNotEmpty() == true) {
                    val location = LatLng(origen[0].latitude, origen[0].longitude)
                    mMap?.addMarker(MarkerOptions().position(location).title("Dirección donde recoger carga").icon(
                        BitmapDescriptorFactory.fromResource(R.drawable.start)))
                } else {
                    Toast.makeText(requireContext(), "Dirección de recogida no encontrada", Toast.LENGTH_SHORT).show()
                }

                if (destino?.isNotEmpty() == true) {
                    val location2 = LatLng(destino[0].latitude, destino[0].longitude)
                    mMap?.addMarker(MarkerOptions().position(location2).title("Dirección de entrega").icon(
                        BitmapDescriptorFactory.fromResource(R.drawable.end)))
                } else {
                    Toast.makeText(requireContext(), "Dirección de entrega no encontrada", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mapa, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
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