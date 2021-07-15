package com.example.suberdriver.utils

import android.view.View
import com.example.suberdriver.Common
import com.example.suberdriver.R
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object UserUtils {
    fun updateData(
        view: View?,
        updateData: HashMap<String, Any>
    ){
        FirebaseDatabase.getInstance()
            .getReference(Common.DRIVER_INFO_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .updateChildren(updateData)
            .addOnFailureListener {
                Snackbar.make(view!!, it.message!!, Snackbar.LENGTH_LONG).show()
            }
            .addOnSuccessListener {
                Snackbar.make(view!!, R.string.update_successfully, Snackbar.LENGTH_LONG).show()
            }
    }
}