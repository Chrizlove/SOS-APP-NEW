package com.chrizlove.helpapp

import android.R
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager


class SensorService: Service() {

    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null
    private var mShakeDetector: ShakeDetector? = null

    //no use right now
    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    //used for foreground
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        //debug toast
        //Toast.makeText(applicationContext,"Service started", Toast.LENGTH_SHORT).show()

        // start the foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startMyOwnForeground() else startForeground(
            1, Notification() )

        // ShakeDetector initialization
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mShakeDetector = ShakeDetector()
        mShakeDetector!!.setOnShakeListener(object : ShakeDetector.OnShakeListener {
            @SuppressLint("MissingPermission")
            override fun onShake(count: Int) {
                // check if the user has shacked
                // the phone for 3 time in a row
                if (count == 3) {

                    // vibrate the phone
                    vibrate()

                    //here i want to call the sendSos method that is in the mainActivity
                    sendMessage()
                    //Toast.makeText(applicationContext,"Shaken", Toast.LENGTH_SHORT).show()
                }
            }

        })
        // register the listener
        mSensorManager!!.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI)
    }



    // For Build versions higher than Android Oreo, we launch
    // a foreground service in a different way. This is due to the newly
    // implemented strict notification rules, which require us to identify
    // our own notification channel in order to view them correctly.
    @RequiresApi(Build.VERSION_CODES.O)
    private fun startMyOwnForeground() {
        val NOTIFICATION_CHANNEL_ID = "example.permanence"
        val channelName = "Background Service"
        val chan = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_MIN
        )
        val manager = (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        manager.createNotificationChannel(chan)
        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val notification: Notification = notificationBuilder.setOngoing(true)
            .setContentTitle("You are protected.")
            .setContentText("If this notification is not displaying, relaunch the app to have the SOS service active.") // this is important, otherwise the notification will show the way
            // you want i.e. it will show some default notification
            .setSmallIcon(R.drawable.btn_radio)
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(2, notification)
    }

    // method to vibrate the phone
    fun vibrate() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        val vibEff: VibrationEffect

        // Android Q and above have some predefined vibrating patterns
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibEff = VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
            vibrator.cancel()
            vibrator.vibrate(vibEff)
        } else {
            vibrator.vibrate(500)
        }
    }

    //sends broadcast for sendSOS method to be invoked
    private fun sendMessage() {
        val intent = Intent("my-event")
        // add data
        Log.d("sent ","broadcast")
        intent.putExtra("message", "sendSOS")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

}