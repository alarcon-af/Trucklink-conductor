package com.example.freightlink

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.freightlink.MainActivity.Companion.CAMERA_REQUEST
import com.example.freightlink.MainActivity.Companion.GALLERY_REQUEST
import com.example.freightlink.databinding.ActivityMainBinding
import com.example.freightlink.databinding.ActivityRegistroBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class Registro : AppCompatActivity(){

    private lateinit var binding: ActivityRegistroBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storage: StorageReference

    private val img : ImageView by lazy {
        findViewById<ImageView>(R.id.profilePicReg)
    }
    private val mydb = FirebaseDatabase.getInstance()
    private var tempImageUri: Uri? = null
    var imageUrl: String? = null
    var uri: Uri? = null
    private var tempImageFilePath = ""
    private val albumLauncher = registerForActivityResult(ActivityResultContracts.GetContent()){
        img.setImageURI(it)
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            lifecycleScope.launch {
                val imageUri = saveImageToGallery()
                loadImageWithGlide(imageUri)
            }
        }
    }

    private suspend fun saveImageToGallery(): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }

        val contentResolver = applicationContext.contentResolver
        val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (imageUri != null) {
            var outStream: FileOutputStream? = null
            var inStream: FileInputStream? = null

            try {
                outStream = contentResolver.openOutputStream(imageUri) as FileOutputStream
                inStream = FileInputStream(File(tempImageFilePath))

                val buffer = ByteArray(1024)
                var bytesRead: Int

                while (inStream.read(buffer).also { bytesRead = it } != -1) {
                    outStream.write(buffer, 0, bytesRead)
                }

            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Registro, "Error al guardar la imagen en la galería", Toast.LENGTH_SHORT).show()
                }
                return null
            } finally {
                outStream?.close()
                inStream?.close()
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@Registro, "Error al guardar la imagen en la galería", Toast.LENGTH_SHORT).show()
            }
            return null
        }
        return imageUri
    }

    private fun loadImageWithGlide(imageUri: Uri?) {
        Glide.with(this)
            .load(imageUri)
            .into(img)
    }

    private fun askImageMethod(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Insertar Imagen")
        builder.setMessage("Como desea seleccionar la foto??")
        builder.setPositiveButton("Camara") { dialog, which ->
            checkCameraPermission()
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ){
                startCamera()
            }
        }
        builder.setNegativeButton("Galeria") { dialog, which ->
            // Si el usuario hace clic en "Cancelar", cierra el diálogo.
            dialog.dismiss()
            checkGalleryPermission()
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ){
                startGallery()
            }
        }
        builder.show()

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            GALLERY_REQUEST -> {
                if((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                    startGallery()
                }else{
                    showInContextUI()
                    requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), GALLERY_REQUEST)
                }
                return
            }
            CAMERA_REQUEST -> {
                if((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                    startCamera()

                }else{
                    showInContextUI()
                    requestPermissions(arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE), CAMERA_REQUEST)
                }
                return
            }
        }
    }

    private fun showInContextUI(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Permiso necesario")
        builder.setMessage("Esta función requiere acceso. Si deniegas el permiso, algunas funciones estarán deshabilitadas.")
        builder.setNegativeButton("Volver") { dialog, which ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ),
            MainActivity.CAMERA_REQUEST
        )
    }

    private fun checkCameraPermission(){
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.CAMERA
                ) || ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(this)
                    .setTitle("Permisos de camara necesarios")
                    .setMessage("Para su funcionamiento correcto, es necesario que acepte los permisos")
                    .setPositiveButton(
                        "OK"
                    ) { _, _ ->
                        //Prompt the user once explanation has been shown
                        requestCameraPermission()
                    }
                    .create()
                    .show()
            } else {
                // No explanation needed, we can request the permission.
                requestCameraPermission()
            }
        }
    }

    private fun startCamera(){
        tempImageUri = FileProvider.getUriForFile(this, "com.example.freightlink.provider", createImageFile().also {
            tempImageFilePath = it.absolutePath
        })
        cameraLauncher.launch(tempImageUri)
    }

    private fun checkGalleryPermission(){
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) || ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(this)
                    .setTitle("Permisos de galeria necesarios")
                    .setMessage("Para su funcionamiento correcto, es necesario que acepte los permisos")
                    .setPositiveButton(
                        "OK"
                    ) { _, _ ->
                        //Prompt the user once explanation has been shown
                        requestGalleryPermission()
                    }
                    .create()
                    .show()
            } else {
                // No explanation needed, we can request the permission.
                requestGalleryPermission()
            }
        }
    }

    private fun requestGalleryPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ),
            MainActivity.GALLERY_REQUEST
        )
    }

    private fun startGallery(){
        albumLauncher.launch("image/*")
    }

    private fun createImageFile() : File {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("temp-image", ".jpg", storageDir)
    }

    private fun isEmailValid(email: String): Boolean {
        if (!email.contains("@") ||
            !email.contains(".") ||
            email.length < 5){
            return false
        }
        return true
    }

    private fun validateForm(): Boolean {
        var valid = true
        val email = findViewById<EditText>(R.id.CorreoReg)
        if (TextUtils.isEmpty(email.text.toString())) {
            valid = false
        }
        val password = findViewById<EditText>(R.id.ContrasenaReg)
        if (TextUtils.isEmpty(password.text.toString())) {
            valid = false
        }
        return valid
    }

    @SuppressLint("SuspiciousIndentation")
    private fun uploadImageAndSaveUserData(
        correo: String, pass: String, nom: String,
        apellido: String, cedula: Long, telefono: Long,  photoUri: Uri?
    ) {
        val userId = auth.currentUser?.uid
        if (userId != null && photoUri != null) {
            val storageRef = FirebaseStorage.getInstance().reference.child("Imagenes")
            storageRef.putFile(photoUri).addOnSuccessListener { taskSnapshot ->
                val uriTask = taskSnapshot.storage.downloadUrl
                while(!uriTask.isComplete);
                    val uriImage = uriTask.result
                imageUrl = uriImage.toString()
                saveUserData(correo, pass, nom, apellido, cedula, telefono, imageUrl)
            }
        } else {
            saveUserData(correo, pass, nom, apellido, cedula, telefono, null)
        }
    }

    private fun saveUserData(
        correo: String, pass: String, nom: String,
        apellido: String, cedula: Long, telefono: Long, photoUrl: String?
    ) {
        val userId = auth.currentUser!!.uid
        database = Firebase.database.reference
        if (userId != null) {
            val user = Driver(
                correo = correo,
                password = pass,
                nombre = nom,
                apellido = apellido,
                cedula = cedula,
                telefono = telefono,
                foto = photoUrl
            )
            database = mydb.getReference("conductores/"+userId)
            database.setValue(user).addOnSuccessListener {
                Toast.makeText(
                    baseContext, "Conductor registrado y datos guardados.",
                    Toast.LENGTH_SHORT
                ).show()
                val intentMenu = Intent(this, Menu::class.java)
                startActivity(intentMenu)
            }
                .addOnFailureListener {
                    Toast.makeText(
                        baseContext, "Error al guardar los datos.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            Toast.makeText(
                baseContext, "Error en el registro del cliente.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val volver = binding.volver
        val reg = binding.registrar
        val cambiarfoto = binding.cambiarFoto
        var nombre = findViewById<EditText>(R.id.NombreReg)
        var apellido = findViewById<EditText>(R.id.ApellidoReg)
        var correo = findViewById<EditText>(R.id.CorreoReg)
        var pass = findViewById<EditText>(R.id.ContrasenaReg)
        var telefono = findViewById<EditText>(R.id.TelefonoReg)
        var cedula = findViewById<EditText>(R.id.CedulaReg)

        auth = FirebaseAuth.getInstance()

        cambiarfoto.setOnClickListener {
            askImageMethod()
        }

        volver.setOnClickListener {
            val intentBack = Intent(this, MainActivity::class.java)
            startActivity(intentBack)
        }
        reg.setOnClickListener {
            val nomReg = nombre.text.toString()
            val apeReg = apellido.text.toString()
            val correoReg = correo.text.toString()
            val passReg = pass.text.toString()
            val teleReg = telefono.text.toString().toLong()
            val ceduReg = cedula.text.toString().toLong()
            if(nomReg!="" && apeReg!="" && correoReg!="" && passReg!="" && teleReg!=0L && ceduReg!=0L) {
                if (validateForm() && isEmailValid(correoReg)) {
                    auth.createUserWithEmailAndPassword(correoReg, passReg)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                uploadImageAndSaveUserData(correoReg, passReg, nomReg, apeReg, ceduReg, teleReg, tempImageUri)
                            } else {
                                Toast.makeText(
                                    baseContext, "Error en el registro del cliente.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                }else{
                    Toast.makeText(this, "Introduzca valores validos para el correo y contraseña", Toast.LENGTH_LONG).show()
                }
            }else{
                Toast.makeText(this,"Llene todos los valores de registro",Toast.LENGTH_LONG).show()
            }
        }
    }
}