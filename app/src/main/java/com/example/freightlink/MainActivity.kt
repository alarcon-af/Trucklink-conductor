package com.example.freightlink

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
//import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import com.example.freightlink.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    companion object{
        const val GALLERY_REQUEST = 0
        const val CAMERA_REQUEST = 1
        const val PICK_IMAGE = 2
        const val CONTACTS_REQUEST = 3
        const val ACCESS_FINE_LOCATION = 4
        const val ACCESS_COARSE_LOCATION = 5
    }

    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            val intent = Intent(this, Menu::class.java)
            intent.putExtra("user", currentUser.email)
            startActivity(intent)
        } else {
            binding.user.setText("")
            binding.password.setText("")
        }
    }

    private fun validateForm(): Boolean {
        var valid = true
        val email = binding.user.text.toString()
        if (TextUtils.isEmpty(email)) {
            binding.user.error = "Required."
            valid = false
        } else {
            binding.user.error = null
        }
        val password = binding.password.text.toString()
        if (TextUtils.isEmpty(password)) {
            binding.password.error = "Required."
            valid = false
        } else {
            binding.password.error = null
        }
        return valid
    }

    private fun isEmailValid(email: String): Boolean {
        if (!email.contains("@") ||
            !email.contains(".") ||
            email.length < 5){
            return false
        }
        return true
    }

    private fun signInUser(email: String, password: String){
        if(validateForm() && isEmailValid(email)){
            auth.signInWithEmailAndPassword(email,password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
// Sign in success, update UI
                        Log.d(ContentValues.TAG, "signInWithEmail:success:")
                        val user = auth.currentUser
                        updateUI(user)

                    } else {
                        Log.w(ContentValues.TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(this, "Authentication failed.",
                            Toast.LENGTH_SHORT).show()
                        updateUI(null)
                    }
                }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide() //hiding the large purple bar
        auth = Firebase.auth
        var log = binding.LogIn
        var reg = binding.Registrar
        var user = findViewById<EditText>(R.id.user)
        var pass = findViewById<EditText>(R.id.password)

        log.setOnClickListener{
            auth.signInWithEmailAndPassword(user.text.toString(), pass.text.toString()).addOnCompleteListener(this){task ->
                Log.d(ContentValues.TAG, "signInWithEmail:onComplete: " + task.isSuccessful)
                if(!task.isSuccessful) {
                    Log.w(ContentValues.TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        this, "Authentication failed. ",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.user.setText("")
                    binding.password.setText("")
                }else{
                    signInUser(user.text.toString(), pass.text.toString())
                }
            }
        }

        reg.setOnClickListener{
            val intentReg = Intent(this, Registro::class.java)
            startActivity(intentReg)
        }

    }

//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.menu_main, menu)
//        return true
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        return when (item.itemId) {
//            R.id.action_settings -> true
//            else -> super.onOptionsItemSelected(item)
//        }
//    }
}