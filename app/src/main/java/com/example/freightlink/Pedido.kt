package com.example.freightlink

import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng

class Pedido(
    var pedidoId: String = "",
    var carga: String = "",
    var peso: Int = 0,
    var cliente: String = "",
    var driver: String? = "",
    var direccion_recoger: String = "",
    var direccion_entregar: String = "",
    var latitud: Double = 0.0,
    var longitud: Double = 0.0,
    var estado: String = ""
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(pedidoId)
        parcel.writeString(carga)
        parcel.writeInt(peso)
        parcel.writeString(cliente)
        parcel.writeString(driver)
        parcel.writeString(direccion_recoger)
        parcel.writeString(direccion_entregar)
        parcel.writeDouble(latitud)
        parcel.writeDouble(longitud)
        parcel.writeString(estado)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun getUbicacion(): LatLng {
        return LatLng(latitud, longitud)
    }

    companion object CREATOR : Parcelable.Creator<Pedido> {
        override fun createFromParcel(parcel: Parcel): Pedido {
            return Pedido(parcel)
        }

        override fun newArray(size: Int): Array<Pedido?> {
            return arrayOfNulls(size)
        }
    }
}
