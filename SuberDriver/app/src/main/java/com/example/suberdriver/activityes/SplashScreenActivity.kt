package com.example.suberdriver.activityes

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import com.example.suberdriver.R
import com.example.suberdriver.activityes.model.DriverInfoModel
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import java.util.concurrent.TimeUnit

class SplashScreenActivity : AppCompatActivity() {

    companion object {
       private const val LOGIN_REQUEST_CODE = 7171
    }

    private lateinit var providers: List<AuthUI.IdpConfig>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var listener: FirebaseAuth.AuthStateListener
    private lateinit var database: FirebaseDatabase
    private lateinit var driverInfoReference: DatabaseReference

    override fun onStart() {
        super.onStart()
        delaySplashScreen()
    }

    override fun onStop() {
        if(firebaseAuth != null && listener != null)
            firebaseAuth.removeAuthStateListener(listener)
        super.onStop()
    }

    private fun delaySplashScreen() {
        Completable.timer(3, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
            .subscribe {
                firebaseAuth!!.addAuthStateListener(this.listener)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen_layout)

        init()
    }

    private fun init(){
        database = FirebaseDatabase.getInstance()
        driverInfoReference = database.getReference(Common.DRIVER_INFO_REFERENCE)

        providers = listOf(
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        firebaseAuth = FirebaseAuth.getInstance()
        listener = FirebaseAuth.AuthStateListener() {
            val user = firebaseAuth.currentUser?.uid
            if (user != null)
                checkUserFromFirebase()
             else{
                showLoginLayout()
             }
        }
    }

    private fun checkUserFromFirebase() {
        driverInfoReference
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()){
                        Toast.makeText(this@SplashScreenActivity, R.string.user_already_register, Toast.LENGTH_SHORT).show()
                    }else{
                        showRegisterLayout()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SplashScreenActivity, error.message, Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showRegisterLayout() {
        val builder = AlertDialog.Builder(this, R.style.DialogTheme)
        val itemView = LayoutInflater.from(this).inflate(R.layout.register_layout, null)

        val edtFirstName = itemView.findViewById<View>(R.id.edt_first_name) as TextInputEditText
        val edtLastName = itemView.findViewById<View>(R.id.edt_last_name) as TextInputEditText
        val edtPhoneNumber = itemView.findViewById<View>(R.id.edt_phone_number) as TextInputEditText

        val progressBar = findViewById<View>(R.id.progress_bar) as ProgressBar

        val btnRegister = itemView.findViewById<View>(R.id.btn_register) as Button


        //Set data
        if (FirebaseAuth.getInstance().currentUser!!.phoneNumber != null &&
                !TextUtils.isDigitsOnly(FirebaseAuth.getInstance().currentUser!!.phoneNumber))
                    edtPhoneNumber.setText(FirebaseAuth.getInstance().currentUser!!.phoneNumber)

        //View
        builder.setView(itemView)
        val dialog: AlertDialog = builder.create()
        dialog.show()

        //Event
        btnRegister.setOnClickListener{
            if (TextUtils.isDigitsOnly(edtFirstName.text.toString())){
                Toast.makeText(this, R.string.enter_first_name, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else if (TextUtils.isDigitsOnly(edtLastName.text.toString())){
                Toast.makeText(this, R.string.enter_last_name, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else if (TextUtils.isDigitsOnly(edtPhoneNumber.text.toString())){
                Toast.makeText(this, R.string.enter_phone_number, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else{
                val model = DriverInfoModel()
                model.firstName = edtFirstName.text.toString()
                model.lastName = edtLastName.text.toString()
                model.phoneNumber = edtPhoneNumber.text.toString()

                driverInfoReference.child(FirebaseAuth.getInstance().currentUser!!.uid)
                    .setValue(model)
                    .addOnFailureListener{
                        Toast.makeText(this, "On failure", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        progressBar.visibility = View.GONE
                    }
                    .addOnSuccessListener {
                        Toast.makeText(this, R.string.register_successfully, Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        progressBar.visibility = View.GONE
                    }
            }
        }
    }

    private fun showLoginLayout() {
        val authMethodPickerLayout = AuthMethodPickerLayout.Builder(R.layout.sign_in_layout)
            .setPhoneButtonId(R.id.btn_phone_sign_in)
            .setGoogleButtonId(R.id.btn_google_sign_in)
            .build()

        val intent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAuthMethodPickerLayout(authMethodPickerLayout)
            .setTheme(R.style.LoginTheme)
            .setAvailableProviders(providers)
            .setIsSmartLockEnabled(false)
            .build()

        startActivityForResult(intent, LOGIN_REQUEST_CODE)
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOGIN_REQUEST_CODE){
            val response = IdpResponse.fromResultIntent(data)

            if(resultCode == Activity.RESULT_OK){
                val user = FirebaseAuth.getInstance().currentUser
            }
            else
                Toast.makeText(this, "" + response!!.error!!.message, Toast.LENGTH_SHORT).show()
        }
    }

}
