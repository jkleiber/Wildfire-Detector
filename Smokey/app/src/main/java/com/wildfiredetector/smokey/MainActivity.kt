package com.wildfiredetector.smokey

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    // Constants
    private val REQUEST_ENABLE_BT = 10

    /**
     * Bluetooth Setup
     **/
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



    /**
     * Android functions
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        bDetectWildfire.setOnClickListener { view ->
            Snackbar.make(view, "Fire Reported!", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        bSensorConnect.setOnClickListener{ view ->
            // Open bluetooth connections
            startBluetooth(view.context)

            // Connect to bluetooth devices
            listBluetoothDevices()

            // Clean up bluetooth connections
            stopBluetooth()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when(item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Bluetooth functions
     */
    fun startBluetooth(context: Context)
    {
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        // Establish connection to the proxy.
        bluetoothAdapter?.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET)
    }

    fun listBluetoothDevices()
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

    fun stopBluetooth()
    {
        // Close proxy connection after use.
        bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset)
    }
}
