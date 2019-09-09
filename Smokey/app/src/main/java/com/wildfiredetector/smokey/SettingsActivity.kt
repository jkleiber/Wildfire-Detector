package com.wildfiredetector.smokey

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log.d
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.settings_activity.*
import android.Manifest

class SettingsActivity : AppCompatActivity() {

    // Constants
    private val REQUEST_ENABLE_BT = 10
    private val REQUEST_ENABLE_BT_ADMIN = 11
    private val REQUEST_COARSE_LOC = 12
    private val REQUEST_FINE_LOC = 13

    /**
     * Bluetooth Setup
     **/
    // Enabled
    private var bluetoothEnabled: Boolean = false

    // "Headset"
    private var bluetoothHeadset: BluetoothHeadset? = null

    // Bluetooth Profile
    private val profileListener = object : BluetoothProfile.ServiceListener {

        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HEADSET) {
                bluetoothHeadset = proxy as BluetoothHeadset
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HEADSET) {
                bluetoothHeadset = null
            }
        }
    }

    // Get the default adapter
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    // List of paired devices
    val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices

    var btDevices: ArrayList<BluetoothDevice> = ArrayList()
    var btReadableDevices: ArrayList<String> = ArrayList()

    // Create a BroadcastReceiver for ACTION_FOUND.
    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action: String = intent.action
            d("BT_ACTION", action)

            when(action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                    d("BLUETOOTH", "RECEIVED")
                    d("BLUETOOTH", "${device.name}")

                    // Add a device to the device list
                    addDeviceToList(device)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Register for broadcasts when a device is discovered.
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)

        // Sensor connection button
        bSensorConnect.setOnClickListener{ view ->
            // Request all the bluetooth permissions
            if(getPermissions()) {
                // Open bluetooth connections
                //startBluetooth(view.context)

                // Connect to bluetooth devices
                bluetoothAdapter?.startDiscovery()

                // Notify discovery has started
                Snackbar.make(view, "Device Discovery Started...", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()

                // Clean up bluetooth connections
                //stopBluetooth()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode)
        {
            REQUEST_ENABLE_BT -> bluetoothEnabled = resultCode == Activity.RESULT_OK
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver)
    }

    /**
     * Permissions
     */
    private fun getPermissions(): Boolean
    {
        var result: Boolean = false

        // Access bluetooth
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH), REQUEST_ENABLE_BT)
        }
        else
        {
            result = true
        }

        // Access coarse location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_ADMIN), REQUEST_ENABLE_BT_ADMIN)
        }
        else
        {
            result = result &&  true
        }

        // Access coarse location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_COARSE_LOC)
        }
        else
        {
            result = result &&  true
        }

        // Access fine location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_FINE_LOC)
        }
        else
        {
            result = result && true
        }

        return result
    }

    /**
     * Bluetooth functions
     */
    private fun startBluetooth(context: Context)
    {
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        // Establish connection to the proxy.
        bluetoothAdapter?.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET)
    }

    private fun listBluetoothDevices()
    {
        var devices: ArrayList<String> = ArrayList()

        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address

            devices.add("$deviceName: $deviceHardwareAddress")
        }

        // Populate bluetooth device list
        var adapter = ArrayAdapter(applicationContext, android.R.layout.simple_list_item_1, devices)
        bluetoothDeviceList.adapter = adapter
    }

    private fun stopBluetooth()
    {
        // Close proxy connection after use.
        bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset)
    }

    private fun addDeviceToList(newDevice: BluetoothDevice)
    {
        // Add a device to the device list
        btDevices.add(newDevice)

        // Add to the readable list
        btReadableDevices.add("${newDevice.name}: ${newDevice.address}")

        // Update the device list
        updateDeviceList()
    }

    private fun updateDeviceList()
    {
        // Populate bluetooth device list
        var adapter = ArrayAdapter(applicationContext, android.R.layout.simple_list_item_1, btReadableDevices)
        bluetoothDeviceList.adapter = adapter
    }
}