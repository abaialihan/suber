package com.example.suberdriver

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.auth.api.Auth
import com.google.firebase.auth.FirebaseAuth
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import java.util.*
import java.util.concurrent.TimeUnit

class SplashScreenActivity : AppCompatActivity() {

    companion object{
       private val LOGIN_REQUEST_CODE = 7366
    }

    private lateinit var providers: List<AuthUI.IdpConfig>
    private lateinit var firebaseAut: FirebaseAuth
    private lateinit var listener: FirebaseAuth.AuthStateListener

    override fun onStart() {
        super.onStart()
        delaySplashScreen()
    }

    override fun onStop() {
        if(firebaseAut != null && listener != null)
            firebaseAut.removeAuthStateListener { listener }
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
    }

    private fun delaySplashScreen() {
        Completable.timer(3, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
            .subscribe {
                firebaseAut.addAuthStateListener { listener }
            }
    }

    private fun init(){
        providers = listOf(
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        firebaseAut = FirebaseAuth.getInstance()
        listener = FirebaseAuth.AuthStateListener { myFirebaseAuth ->
            val user = myFirebaseAuth.currentUser
            if (user != null)
                Toast.makeText(this, "Welcome " + user.uid, Toast.LENGTH_SHORT).show()
            else
                resultLauncher
                //showLoginLayout()
        }
    }
    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        val authMethodPickerLayout = AuthMethodPickerLayout.Builder(R.layout.sign_in_layout)
            .setPhoneButtonId(R.id.btn_phone_sign_in)
            .setGoogleButtonId(R.id.btn_google_sign_in)
            .build()

        AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAuthMethodPickerLayout(authMethodPickerLayout)
            .setTheme(R.style.loginTheme)
            .setAvailableProviders(providers)
            .setIsSmartLockEnabled(false)
            .build()

        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            val user = FirebaseAuth.getInstance().currentUser
        }else{

        }
    }

//    private fun showLoginLayout() {
//        val authMethodPickerLayout = AuthMethodPickerLayout.Builder(R.layout.sign_in_layout)
//            .setPhoneButtonId(R.id.btn_phone_sign_in)
//            .setGoogleButtonId(R.id.btn_google_sign_in)
//            .build()
//
//        resultLauncher.launch(intent, LOGIN_REQUEST_CODE)
//
//        startActivityForResult(AuthUI.getInstance()
//            .createSignInIntentBuilder()
//            .setAuthMethodPickerLayout(authMethodPickerLayout)
//            .setTheme(R.style.loginTheme)
//            .setAvailableProviders(providers)
//            .setIsSmartLockEnabled(false)
//            .build(),
//        LOGIN_REQUEST_CODE )
//    }

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
