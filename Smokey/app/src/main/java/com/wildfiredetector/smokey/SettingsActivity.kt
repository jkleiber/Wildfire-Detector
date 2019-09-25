package com.wildfiredetector.smokey
import android.Manifest
import android.bluetooth.*
import android.bluetooth.BluetoothAdapter.STATE_CONNECTED
import android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.beepiz.bluetooth.gattcoroutines.GattConnection
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.settings_activity.*
import java.util.*
import kotlin.collections.ArrayList

class SettingsActivity : AppCompatActivity() {

    private var mBluetoothGatt: BluetoothGatt? = null
    private var mConnectionState = STATE_DISCONNECTED

    // Constants
    private val REQUEST_ENABLE_BT = 10
    private val REQUEST_ENABLE_BT_ADMIN = 11
    private val REQUEST_COARSE_LOC = 12
    private val REQUEST_FINE_LOC = 13

    var btGattServ: ArrayList<BluetoothGattService> = ArrayList()
    var btDevices: ArrayList<BluetoothDevice> = ArrayList()
    var btReadableDevices: ArrayList<String> = ArrayList()

    /**
     * Bluetooth Setup and scanning
     **/
    private val bleScanner = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            // makes sure that the name isn't null and that the device is also unique
            if (result?.device?.name != null && !(btDevices.contains(result.device))) {
                // Add a device to the device list
                btDevices.add(result.device)
                btReadableDevices.add("${result.device?.name}: ${result.device?.address}")
            }
        }

        // Not sure what this does tbh
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            Log.d("DeviceListActivity", "onBatchScanResults:${results.toString()}")
        }

        // Error Checking
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d("DeviceListActivity", "onScanFailed: $errorCode")
        }
    }

    private val bluetoothLeScanner: BluetoothLeScanner
        get() {
            val bluetoothManager =
                applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            return bluetoothAdapter.bluetoothLeScanner
        }


    // This code is for GATT connection of the the bluetooth device
    suspend fun BluetoothDevice.logGattServices(tag: String = "BleGattCoroutines") {
        val deviceConnection = GattConnection(bluetoothDevice = this@logGattServices)
        try {
            deviceConnection.connect() // Suspends until connection is established
            val gattServices = deviceConnection.discoverServices() // Suspends until completed
            gattServices.forEach {
                btGattServ.add(it)
                it.characteristics.forEach {
                    try {
                        deviceConnection.readCharacteristic(it) // Suspends until characteristic is read

                    } catch (e: Exception) {
                        Log.e(tag, "Couldn't read characteristic with uuid: ${it.uuid}", e)
                    }
                }
            }
        } finally {
            deviceConnection.close() // Close when no longer used. Also triggers disconnect by default.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("DeviceListActivity", "onCreate()")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

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
                    Toast.makeText(this, "No Buetooth Low-Energy devices found", Toast.LENGTH_SHORT).show()
                }
            }

        }
        // When a bt device is clicked on the view get the device info
        bluetoothDeviceList.setOnItemClickListener{ parent, view, position, id ->
            // Get the device
            val clickedDevice: BluetoothDevice = btDevices[id.toInt()]

            Toast.makeText(this, "${clickedDevice.name}: ${clickedDevice.address}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        Log.d("DeviceListActivity", "onStart()")
        super.onStart()

        bluetoothLeScanner.startScan(bleScanner)

    }

    override fun onStop() {
        bluetoothLeScanner.stopScan(bleScanner)
        super.onStop()
    }

    /**
     * Permissions
     */
    private fun getPermissions(): Boolean {
        var result: Boolean = true

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
}




