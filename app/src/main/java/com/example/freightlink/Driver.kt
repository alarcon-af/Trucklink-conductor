package com.example.freightlink

import android.net.Uri

data class Driver (
    var correo: String = "",
    var password: String = "",
    var cedula: Long = 0,
    var nombre: String = "",
    var apellido: String = "",
    var telefono: Long = 0,
    var foto: Uri? = null
)
