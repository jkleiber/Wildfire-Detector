package com.wildfiredetector.smokey
import android.Manifest
import android.content.Intent

import android.app.*
import android.bluetooth.*
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.material.snackbar.Snackbar
import com.wildfiredetector.smokey.ui.main.PageViewModel
import kotlinx.android.synthetic.main.settings_activity.*
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

class SettingsActivity : AppCompatActivity() {

    // Constants
    private val REQUEST_ENABLE_BT = 10
    private val REQUEST_ENABLE_BT_ADMIN = 11
    private val REQUEST_COARSE_LOC = 12
    private val REQUEST_FINE_LOC = 13

    var btDevices: ArrayList<BluetoothDevice> = ArrayList()
    var btReadableDevices: ArrayList<String> = ArrayList()

    private val reportURL = "http://smokey.x10.bz/php/report_fire.php"

    var currentLocation : Location? = null

    private lateinit var pageViewModel: PageViewModel




    override fun onStart() {
        super.onStart()
        d("BLEGATT", "onStart")
        bluetoothLeScanner.startScan(bleScanner)

    }

    override fun onStop()
    {
        super.onStop()
        d("BLEGATT",  "onStop")
        bluetoothLeScanner.stopScan(bleScanner)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)


        currentLocation = VolleySingleton.getInstance(this).currentLocation
        d("LAT", currentLocation?.latitude.toString())

        pageViewModel = this.run{
            ViewModelProviders.of(this).get(PageViewModel::class.java)
        }

        bSensorConnect.setOnClickListener {
            if (getPermissions())
            {
                // Notify discovery has started
                Snackbar.make(it, "Device Discovery Started...", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()

                updateDeviceList()

                // Notifies if there are no BLE devices in the are
                if(btDevices.isEmpty())
                {
                    // Notify no
                    Toast.makeText(this, "No Bluetooth Low-Energy devices found", Toast.LENGTH_SHORT).show()
                    onStop()
                }
            }

        }
        // When a bt device is clicked on the view get the device info
        bluetoothDeviceList.setOnItemClickListener{ parent, view, position, id ->

            // Get the device
            val clickedDevice: BluetoothDevice = btDevices[id.toInt()]

            Toast.makeText(this, "${clickedDevice.name}: ${clickedDevice.address}", Toast.LENGTH_SHORT).show()

            // implement gattCallback
            clickedDevice.connectGatt(this, true, BLESingleton.getInstance(this).gattCallback, TRANSPORT_LE)
        }



        // Report fires
        pageViewModel.bleUpdate.observe(this, Observer<Boolean>{
            val jsonPkt = JSONObject()
            jsonPkt.put("latitude", currentLocation?.latitude)
            jsonPkt.put("longitude", currentLocation?.longitude)

            d("FIREPKT", currentLocation?.latitude.toString())
            d("FIREPKT", currentLocation?.longitude.toString())
            d("FIREPKT", jsonPkt.toString(2))

            // Build a new request
            val request = JsonObjectRequest(
                Request.Method.POST, reportURL, jsonPkt,
                Response.Listener{
                    d("RESPONSE", it.toString())

                    // Update the map
                    pageViewModel.updateMap(true)
                    Toast.makeText(this@SettingsActivity, "Fire Detected", Toast.LENGTH_SHORT).show()
                },
                Response.ErrorListener {
                    e("RESPONSE", it?.message)
                    val errorText = "Failed to report fire: %s".format(it.message)
                }
            )

            // Add the fire to the database by sending a request using Volley
            VolleySingleton.getInstance(this.applicationContext).addToRequestQueue(request)
        })

    }

    /**
     * Permissions
     */
    private fun getPermissions(): Boolean {
        var result = true

        // Access bluetooth
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH), REQUEST_ENABLE_BT)
        } else {
            result = true
        }

        // Access coarse location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted
            requestPermissions(
                arrayOf(Manifest.permission.BLUETOOTH_ADMIN),
                REQUEST_ENABLE_BT_ADMIN
            )
        } else {
            result = result && true
        }

        // Access coarse location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_COARSE_LOC
            )
        } else {
            result = result && true
        }

        // Access fine location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_FINE_LOC)
        } else {
            result = result && true
        }

        return result
    }


    private fun updateDeviceList()
    {
        // Populate bluetooth device list
        val adapter = ArrayAdapter(applicationContext, android.R.layout.simple_list_item_1, btReadableDevices)
        bluetoothDeviceList.adapter = adapter
    }

    // Creates notification for the app
    fun showNotification(title: String, message: String)
    {
        val CHANNEL_ID = "Smokey Notification"
        val NOTIFICATION_ID = 1

        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID,
                "Smokey",
                NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "Fire has been detected"

            mNotificationManager.createNotificationChannel(channel)
        }


        val mBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // notification icon
            .setContentTitle(title) // title for notification
            .setContentText(message)// message for notification
            .setAutoCancel(true) // clear notification after click
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        val intent = Intent(this, MainScreenActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra("Tab", 1)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        mBuilder.setContentIntent(pi)
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build())
        d("Notification", "showNotification called")
    }
}




