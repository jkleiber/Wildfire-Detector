package com.wildfiredetector.smokey
import android.Manifest
import android.app.Activity
import android.app.Service
import android.bluetooth.*
import android.bluetooth.BluetoothAdapter.STATE_CONNECTED
import android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.BluetoothGatt.STATE_CONNECTED
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.nfc.NfcAdapter.EXTRA_DATA
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.util.Log.*
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

    // BLE GATT services
    private val STATE_DISCONNECTED = 0
    private val STATE_CONNECTING = 1
    private val STATE_CONNECTED = 2
    val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
    val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
    val ACTION_GATT_SERVICES_DISCOVERED =
        "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
    val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
    val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"

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

    override fun onStart() {
        d("DeviceListActivity", "onStart()")
        super.onStart()
        d("BLEGAAT", "onStart")
        bluetoothLeScanner.startScan(bleScanner)

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        d("DeviceListActivity", "onCreate()")
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
                    onStop()
                    Toast.makeText(this, "No Buetooth Low-Energy devices found", Toast.LENGTH_SHORT).show()
                }
            }

        }
        // When a bt device is clicked on the view get the device info
        bluetoothDeviceList.setOnItemClickListener{ parent, view, position, id ->

            // Get the device
            val clickedDevice: BluetoothDevice = btDevices[id.toInt()]

            Toast.makeText(this, "${clickedDevice.name}: ${clickedDevice.address}", Toast.LENGTH_SHORT).show()

            // implement gattCallback
            clickedDevice.connectGatt(this, false, gattCallback, TRANSPORT_LE)

//            bluetoothLeScanner.stopScan(bleScanner)
            d("BLEGATT", "After connectGatt")

        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        val TAG: String = "BLEGATT"
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            d(TAG, "This is the: $newState")
            if (newState == BluetoothGatt.STATE_CONNECTED)
            {
                val mtu = gatt?.requestMtu(256)
                val discovered = gatt?.discoverServices()
                d(TAG, "Discovered: $discovered mtu: $mtu")

                d(TAG, "After onServiceDiscovered call in onConnectionStateChange")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            val gattService: String = "00110011-4455-6677-8899-AABBCCDDEEFF"
            val gattChar: String = "00110002-4455-6677-8899-AABBCCDDEEFF"

            val listGattService = gatt?.services
            d(TAG, "Are there any gattServices: $listGattService")
            listGattService?.forEach {
                d(TAG, "gatt Services are: $it")
            }

            d(TAG, "OnServicesDiscovered: status is: $status")
            val gattServiceUUID = UUID.fromString(gattService)
            d(TAG, "gattServiceUUID: $gattServiceUUID")
            val gattCharUUID =  UUID.fromString(gattChar)// this should be whatever we decide to have. In the example code they have expandUuid
            d(TAG, "gattCharUUID: $gattCharUUID")

            val gattServiceExists = gatt?.getService(UUID.fromString(gattService))
            d(TAG, "gattService: $gattServiceExists")

            val characteristic = gatt?.getService(UUID.fromString(gattService)) // this should be whatever we decide to have. In the example code they have expandUuid
                ?.getCharacteristic(UUID.fromString(gattChar)) // This is the specific charactersistcs
            d(TAG, "Right before read method in onServiceDiscovered")
            gatt?.readCharacteristic(characteristic)
//            gatt?.setCharacteristicNotification(characteristic, true)
            d(TAG, "Right after read method in onServiceDiscovered")
//            characteristic?.value = byteArrayOf(50)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            val readFire = characteristic!!.value[0].toInt()
            Log.d(TAG, "onCaracteristicRead")
            Log.d(TAG, "$readFire")
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            characteristic?.let {
                val readFire = characteristic.value[0].toInt()
                Log.d(TAG, "Fire flag is: $readFire")
            }
        }
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

    private fun makeGattIntentFilter(): IntentFilter
    {
        var intentFilter: IntentFilter = IntentFilter()
        intentFilter.addAction(ACTION_DATA_AVAILABLE)
        intentFilter.addAction(ACTION_GATT_CONNECTED)
        intentFilter.addAction(ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(ACTION_GATT_SERVICES_DISCOVERED)

        return intentFilter
    }
}




