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
import com.beepiz.bluetooth.gattcoroutines.extensions.get
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.settings_activity.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.experimental.and

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
            d("DeviceListActivity", "onBatchScanResults:${results.toString()}")
        }

        // Error Checking
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            d("DeviceListActivity", "onScanFailed: $errorCode")
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
                    Toast.makeText(this, "No Buetooth Low-Energy devices found", Toast.LENGTH_SHORT).show()
                    onStop()
                }
            }

        }
        // When a bt device is clicked on the view get the device info
        bluetoothDeviceList.setOnItemClickListener{ parent, view, position, id ->

            // Get the device
            val clickedDevice: BluetoothDevice = btDevices[id.toInt()]

            Toast.makeText(this, "${clickedDevice.name}: ${clickedDevice.address}", Toast.LENGTH_SHORT).show()

            // Testing to see if BT device is removed when a device is clicked.
            //btDevices.remove(clickedDevice)
            //updateDeviceList()

            // implement gattCallback
            clickedDevice.connectGatt(this, false, gattCallback, TRANSPORT_LE)

        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        val TAG: String = "BLEGATT"
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            d(TAG, "onConnectionStateChange")
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
            d(TAG,"inside onServicesDiscovered")


            val characteristic = gatt?.getService(UUID.fromString(gattService)) // this should be whatever we decide to have. In the example code they have expandUuid
                ?.getCharacteristic(UUID.fromString(gattChar)) // This is the specific characteristic


             val descriptor = characteristic?.getDescriptor(UUID.fromString(gattDescript))
            d(TAG, "Descriptor that I set using 2902 is: $descriptor")

            val descriptor_list = characteristic?.descriptors
            d(TAG, "Descriptor list is: $descriptor_list")

            descriptor_list?.forEach {
                d(TAG, "Descriptor is: $it")
            }
            gatt?.readCharacteristic(characteristic)

            d(TAG, "Right before setCharacteristicNotification")
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
                d(TAG, "reading in value: $readFire")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            d(TAG, "in onCharacteristicChanged")
            characteristic?.let {
                val readFire = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                d(TAG, "Fire flag is: $readFire")
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            d(TAG, "onDescriptorWrite")
            val gattService = "00110011-4455-6677-8899-AABBCCDDEEFF"
            val gattChar = "00000002-0000-1000-8000-00805f9b34fb"
            val characteristic = gatt?.getService(UUID.fromString(gattService)) // this should be whatever we decide to have. In the example code they have expandUuid
                ?.getCharacteristic(UUID.fromString(gattChar)) // This is the specific characteristic
            characteristic?.setValue(byteArrayOf(0x01, 0x01))
            gatt?.writeCharacteristic(characteristic)
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

}




