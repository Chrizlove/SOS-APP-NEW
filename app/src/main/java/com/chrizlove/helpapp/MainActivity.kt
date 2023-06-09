package com.chrizlove.helpapp

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chrizlove.helpapp.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.w3c.dom.Text


class MainActivity : AppCompatActivity(){
    private lateinit var binding: ActivityMainBinding
    private lateinit var contactAdapter: ContactAdapter
    private lateinit var contactViewModel: ContactViewModel
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var location: Location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //requesting permissions
        requestAllPermission(loadData())

        //recycler view is setted up
        setUpReminderRecyclerView()

        //contactViewModel set up
        contactViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(ContactViewModel::class.java)

        contactViewModel.contacts.observe(this, Observer {

            //change visibility of no contacts yet textview
            if(contactViewModel.contacts.value?.isEmpty() == true)
            {
                binding.noContact.visibility = View.VISIBLE
            }
            else{
                binding.noContact.visibility = View.INVISIBLE
            }

            //updating the recyclerview on any change
            contactAdapter.updateContacts(it)

        })

        //adding new contact
        addContact()

        //swipe to delete functionality
        swipeToDelete()

        // start the service
        val sensorService = SensorService()
        val intent = Intent(this, sensorService.javaClass)
        if (!isMyServiceRunning(sensorService.javaClass)) {
            startService(intent)
            //Toast.makeText(applicationContext,"Service start",Toast.LENGTH_SHORT).show()
        }

        //button listener to send sos
        binding.buttonSOS.setOnClickListener {
            sendSOS(this,true)
        }

        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
            IntentFilter("my-event"));

    }

    private fun loadData(): Int {
        val sharedPreferences=getSharedPreferences("sharedPrefs",Context.MODE_PRIVATE)
        return sharedPreferences.getInt("serial",1)
    }

    private fun updateData(serial: Int) {
        val sharedPreferences=getSharedPreferences("sharedPrefs",Context.MODE_PRIVATE)
        val editor=sharedPreferences.edit()
        editor.apply {
            putInt("serial", serial)
        }.apply()
    }

    private fun requestAllPermission(serial: Int) {
        //requesting permissions
        Log.d("permissions",loadData().toString())
        if(serial<=2){
        if(!checkSMSPermission(this) || !checkLocationPermission(this)){
            //updateSharedPreferences
            updateData(serial+1)
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.SEND_SMS),
                PERMISSION_REQUEST_ALL)
        }
        }
        checkBackgroundLocation()
    }


    private fun checkBackgroundLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestBackgroundLocationPermission()
        }
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ),
                MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSIONS_REQUEST_LOCATION
            )
        }
    }

    // method to check if the service is running
    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                Log.i("Service status", "Running")
                return true
            }
        }
        Log.i("Service status", "Not running")
        return false
    }

    private fun sendSOS(context: Context, fromButton: Boolean) {
            fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(this)
            getCurrentLocationAndSendSMS(context, fromButton)
    }

    private fun sendSMS(code: Int,fromButton: Boolean) {
        Log.d(TAG,"2")
        if(checkSMSPermission(this)){
            val smsManager: SmsManager
            smsManager = SmsManager.getDefault()
            for (contact in contactViewModel.contacts.value!!){
                if(code==1){
                    smsManager.sendTextMessage(
                        contact.c_number, null,
                        "Hi, I am in an emergency! GPS was turned off, could not provide location. Kindly contact Authorities",
                        null, null)
                }
                else if(code==2) {
                    smsManager.sendTextMessage(
                        contact.c_number, null,
                        "Hi, I am in an emergency! This is my location "+ "http://maps.google.com/?q=" + location.latitude + "," + location.longitude,
                        null, null)
                }
            }
            Toast.makeText(applicationContext,"SMS Sent",Toast.LENGTH_SHORT).show()
        }
        else{
            //show lack of permission alert
            if(fromButton) showPermissionsAlert(1)
        }
    }


    private fun checkSMSPermission(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(context,Manifest.permission.SEND_SMS)==PackageManager.PERMISSION_GRANTED
    }

    private fun getCurrentLocationAndSendSMS(context: Context, fromButton: Boolean) {
        Log.d(TAG,"asked location permission")
        if(checkLocationPermission(context)){
           if(locationEnabled()){
                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this) {task->
                    if(task.result==null){
                    //send sms without location
                        sendSMS(1,fromButton)
                    }
                    else{
                        //SEND sms when gotten location
                        location=task.result
                        sendSMS(2,fromButton)
                    }
                }
            }
            else{
               //send sms without location
                sendSMS(1,fromButton)
            }
        }
        else{
            //show lack of permission alert
            if(fromButton) showPermissionsAlert(1)
        }
    }

    companion object{
        private const val  PERMISSION_REQUEST_ALL=11
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 96
        private const val MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION = 66
    }

    private fun locationEnabled(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun checkLocationPermission(context: Context): Boolean {
        if(ActivityCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)
        {
            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode== PERMISSION_REQUEST_ALL){
            if(grantResults.isNotEmpty()){
                if(grantResults[0]==PackageManager.PERMISSION_DENIED || grantResults[2]==PackageManager.PERMISSION_DENIED) {
                    Log.d("permissions","called by onRequest")
                    //Toast.makeText(applicationContext, "Permissions Required to Operate.", Toast.LENGTH_SHORT).show()
                    //show alert for required permissions
                    showPermissionsAlert(0)
                }
            }
        }
    }

    private fun showPermissionsAlert(code: Int) {
       // val builder = AlertDialog.Builder(this)
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.permission_alert_lyt)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val permission_message: TextView= dialog.findViewById(R.id.permission_alert_message)
        if(code==1){
            permission_message.text="Permissions are not granted. Kindly provide SMS and Location Access Permissions for the app to function by going to the Settings."
        }
        val permission_button : Button =dialog.findViewById(R.id.permission_alert_button)
        permission_button.setOnClickListener {
            requestAllPermission(loadData())
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun swipeToDelete() {
        val item= object: SwipeToDelete(this,0, ItemTouchHelper.RIGHT){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                //remove contact from room db
                contactViewModel.delete(contactAdapter.getContact(viewHolder.adapterPosition))
                //remove reminder from recyclerview
                contactAdapter.deleteContacts(viewHolder.adapterPosition)
            }
        }

        val itemTouchHelper = ItemTouchHelper(item)
        itemTouchHelper.attachToRecyclerView(binding.contactRecyclerView)
    }

    private fun addContact() {

        //adding contact manually
        binding.manualContactButton.setOnClickListener {
            //requesting background location access
            requestBackgroundLocationPermission()
            //checking if contact list size is less than 3
            if(checkContactListSize()) {
                val builder = AlertDialog.Builder(this)
                val inflater = layoutInflater
                val dialoglayout = inflater.inflate(R.layout.alert_edit_lyt, null)
                val dialogName = dialoglayout.findViewById<EditText>(R.id.alert_name)
                val dialogNumber = dialoglayout.findViewById<EditText>(R.id.alert_number)

                with(builder) {
                    setTitle("Add Contact Details")
                    setPositiveButton("ADD") { dialog, which ->
                        contactViewModel.insert(
                            Contact(
                                dialogName.text.toString(),
                                dialogNumber.text.toString()
                            )
                        )
                    }
                    setNegativeButton("Cancel") { dialog, which ->
                    }
                    setView(dialoglayout)
                    show()
                }
            }
        }

        //adding contact from contacts list
        binding.contactListButton.setOnClickListener {
            //requesting background location access
            requestBackgroundLocationPermission()
            //checking if contact list size is less than 3
            if(checkContactListSize()) {
                var i = Intent(Intent.ACTION_PICK)
                i.type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                startActivityForResult(i, 111)
            }
        }
    }

    private fun checkContactListSize(): Boolean {
        if(contactAdapter.itemCount>=3){
            Toast.makeText(applicationContext, "Maximum Contact Limit Reached!", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun setUpReminderRecyclerView() {
        contactAdapter = ContactAdapter(this)
        val layoutManager =  LinearLayoutManager(this)
        binding.contactRecyclerView.layoutManager = layoutManager
        binding.contactRecyclerView.adapter = contactAdapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //code when requestcode is 111 that is when we want to add contact from contacts list
        if (requestCode==111 && resultCode==Activity.RESULT_OK){
            var contacturi=data?.data ?: return
            var cols= arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            var rs=contentResolver.query(contacturi,cols,null,null,null)
            if(rs?.moveToFirst()!!){
                //Log.d(TAG, rs.getString(1))
                contactViewModel.insert(Contact(rs.getString(1), rs.getString(0)))
            }
        }
    }

    // handler for received Intents that calls sendSOS method
    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Extract data included in the Intent
            val message = intent.getStringExtra("message")
            Log.d("RECIEVED",message.toString())
            if(message.equals("sendSOS")){
                sendSOS(applicationContext,false)
            }
        }
    }

    override fun onDestroy() {
        // Unregister since the activity is not visible
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver)
        super.onDestroy()
    }

}