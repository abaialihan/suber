package com.example.suberdriver.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import com.bumptech.glide.Glide
import com.example.suberdriver.Common
import com.example.suberdriver.R
import com.example.suberdriver.databinding.ActivityDriverHomeBinding
import com.example.suberdriver.utils.UserUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class DriverHomeActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityDriverHomeBinding
    private lateinit var navView: NavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController
    private lateinit var imageAvatar: ImageView
    private lateinit var waitingDialog: AlertDialog
    private lateinit var storageReference: StorageReference
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDriverHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarDriverHome.toolbar)

        binding.appBarDriverHome.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        drawerLayout = binding.drawerLayout
        navView = binding.navView
        navController = findNavController(R.id.nav_host_fragment_content_driver_home)

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(setOf(R.id.nav_home), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        buildAlertDialogForSignOut()
    }

    //Build AlertDialog for sing out
    private fun buildAlertDialogForSignOut(){
        storageReference = FirebaseStorage.getInstance().reference

        buildWaitingDialog()

        navView.setNavigationItemSelectedListener {
            if (it.itemId == R.id.nav_sign_out){
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.menu_sign_out)
                    .setMessage(R.string.do_really_exit)
                    .setNegativeButton(R.string.cancel) {dialog, wich ->
                        dialog.dismiss()
                    }
                    .setPositiveButton(R.string.menu_sign_out) {dialog, wich ->
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(this, SplashScreenActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }.setCancelable(false)

                val dialog = builder.create()
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setTextColor(resources.getColor(R.color.black))
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                        .setTextColor(resources.getColor(R.color.black))
                }

                dialog.show()
            }
            true
        }
        initializeDriverInfo()
    }

    private fun buildWaitingDialog(){
        waitingDialog = AlertDialog.Builder(this)
            .setMessage(R.string.waiting)
            .setCancelable(false)
            .create()
    }

    //Parsing Driver info and show in panel
    private fun initializeDriverInfo(){
        val headerView = navView.getHeaderView(0)
        val txtName = headerView.findViewById<View>(R.id.txt_full_name) as TextView
        val txtPhone = headerView.findViewById<View>(R.id.txt_phone) as TextView
        val txtRate = headerView.findViewById<View>(R.id.txt_rate) as TextView
        imageAvatar = headerView.findViewById<View>(R.id.img_avatar) as ImageView

        txtName.text = Common.currentUser!!.firstName + " " + Common.currentUser!!.lastName
        txtPhone.text = Common.currentUser!!.phoneNumber
        txtRate.text = Common.currentUser!!.rating.toString()

        imageLoad()
    }

    //Image loader function
    private fun imageLoad(){
        if (Common.currentUser != null
            && Common.currentUser!!.avatar != null
            && !TextUtils.isEmpty(Common.currentUser!!.avatar))
        {
            Glide.with(this)
                .load(Common.currentUser!!.avatar)
                .into(imageAvatar)
        }

        imageAvatar.setOnClickListener{
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, R.string.select_picture.toString()), PICK_IMAGE_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK){
            if (data != null && data.data != null){
                imageUri = data.data
                imageAvatar.setImageURI(imageUri)

                showDialogUpload()
            }
        }
    }

    private fun showDialogUpload() {
        val builder = AlertDialog.Builder(this)

        builder.setTitle(R.string.change_avatar)
            .setMessage(R.string.do_really_change_avatar)
            .setNegativeButton(R.string.cancel) {dialog, wich ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.change) {dialog, wich ->
                if (imageUri != null) {
                    waitingDialog.show()
                    setAvatarFolder()
                }
            }.setCancelable(false)

        val dialog = builder.create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(resources.getColor(R.color.black))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(resources.getColor(R.color.black))
        }

        dialog.show()
    }

    private fun setAvatarFolder(){
        val avatarFolder = storageReference.child("avatar/" + FirebaseAuth.getInstance().currentUser!!.uid)

        avatarFolder.putFile(imageUri!!)
            .addOnFailureListener{ e ->
                Snackbar.make(drawerLayout, e.message!!, Snackbar.LENGTH_LONG).show()
                waitingDialog.dismiss()
            }
            .addOnCompleteListener{ task ->
                if(task.isSuccessful){
                    avatarFolder.downloadUrl.addOnSuccessListener { uri ->
                        val updateData = HashMap<String, Any>()

                        updateData.put("avatar", uri.toString())

                        UserUtils.updateData(drawerLayout, updateData)
                    }
                }
                waitingDialog.dismiss()
            }
            .addOnProgressListener { taskSnapshoot ->
                val progress = (100.0 * taskSnapshoot.bytesTransferred / taskSnapshoot.totalByteCount)
                waitingDialog.setMessage(R.string.uploading.toString() + progress + "%")
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.driver_home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_driver_home)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    companion object{
        const val PICK_IMAGE_REQUEST = 7272
    }
}