package com.wildfiredetector.smokey

import android.app.*
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject
import java.util.*
import kotlin.concurrent.schedule

class FireReportService : Service(), LocationListener {

    var currentLocation: Location? = null
    var fireDetected: Boolean = false

    val FOREGROUND_CHANNEL_ID = "Smokey Service"
    val FOREGROUND_NOTIFICATION_ID = 9001

    // Database information
    private val reportURL = "http://smokey.x10.bz/php/report_fire.php"

    var timerTask: TimerTask? = null

    override fun onLocationChanged(location: Location?) {
        if(location != null)
        {
            currentLocation = location
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        timerTask = Timer("CheckFires", false).schedule(1000)
        {
            checkFires()
        }

        val notification: Notification = getPersistentNotification("Smokey", "Smokey is detecting fires in the background")
        startForeground(FOREGROUND_NOTIFICATION_ID, notification)

        return super.onStartCommand(intent, flags, startId)
    }


    override fun onDestroy() {
        super.onDestroy()

        val broadcastIntent = Intent(this, ServiceRestartBroadcastReceiver::class.java)
        sendBroadcast(broadcastIntent)
    }


    private val gattCallback = object : BluetoothGattCallback() {
        val TAG: String = "BLEGATT"
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Log.d(TAG, "onConnectionStateChange")
            if (newState == BluetoothGatt.STATE_CONNECTED)
            {
                while(gatt?.discoverServices() == false)
                {
                    gatt.discoverServices()
                    gatt.requestMtu(256)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            val gattService = "00110011-4455-6677-8899-AABBCCDDEEFF"
            val gattChar = "00000002-0000-1000-8000-00805f9b34fb"
            val gattDescript = "000002902-0000-1000-8000-00805f9b34fb"
            Log.d(TAG, "inside onServicesDiscovered")

            val characteristic = gatt?.getService(UUID.fromString(gattService)) // this should be whatever we decide to have. In the example code they have expandUuid
                ?.getCharacteristic(UUID.fromString(gattChar)) // This is the specific characteristic


            val descriptor = characteristic?.getDescriptor(UUID.fromString(gattDescript))
            gatt?.readCharacteristic(characteristic)

            Log.d(TAG, "Right before setCharacteristicNotification")
            gatt?.setCharacteristicNotification(characteristic, true)
            //Enable notification can also enable Indication if I want to. Notification is faster
            descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            //Write descriptor callback should be invoked
            gatt?.writeDescriptor(descriptor)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val readFire =
                    characteristic!!.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                Log.d(TAG, "reading in value: $readFire")
                if (readFire == 1) {
                    updateBLEFireReport(true)
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            Log.d(TAG, "in onCharacteristicChanged")
            characteristic?.let {
                val readFire = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                Log.d(TAG, "Fire flag is: $readFire")
                if(readFire == 1)
                {
                    Log.d(TAG, "in if statement")
                    updateBLEFireReport(true)
                }
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            Log.d(TAG, "onDescriptorWrite")
            super.onDescriptorWrite(gatt, descriptor, status)
        }
    }


    fun updateBLEFireReport(isFire: Boolean)
    {
        fireDetected = isFire
    }

    private fun checkFires()
    {
        if (fireDetected)
        {
            // Create a JSON packet for sending the data to the database
            val jsonPkt = JSONObject()
            jsonPkt.put("latitude", currentLocation?.latitude)
            jsonPkt.put("longitude", currentLocation?.longitude)

            // Build a new request
            val request = JsonObjectRequest(
                Request.Method.POST, reportURL, jsonPkt,
                Response.Listener{
                    Log.d("RESPONSE", it.toString())

                    // Update the map
                    //pageViewModel.updateMap(true)
                },
                Response.ErrorListener {
                    var errorText = "Failed to report fire."
                    if(it.message != null)
                    {
                        Log.e("RESPONSE", it.message)
                        errorText = "Failed to report fire: %s".format(it.message)
                    }
                }
            )

            // Add the fire to the database by sending a request using Volley
            VolleySingleton.getInstance(this).addToRequestQueue(request)
        }
    }


    // Creates notification for the app
    private fun getPersistentNotification(title: String, message: String) : Notification
    {
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(FOREGROUND_CHANNEL_ID,
                "Smokey",
                NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "Smokey is looking for fires"

            mNotificationManager.createNotificationChannel(channel)
        }


        val mBuilder = NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // notification icon
            .setContentTitle(title) // title for notification
            .setContentText(message)// message for notification
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val intent = Intent(this, MainScreenActivity::class.java)
        intent.putExtra("Tab", 1)

        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        mBuilder.setContentIntent(pi)

        return mBuilder.build()
    }






    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
    }

    override fun onProviderEnabled(p0: String?) {
    }

    override fun onProviderDisabled(p0: String?) {
    }




}