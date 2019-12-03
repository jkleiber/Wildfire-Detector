package com.wildfiredetector.smokey
import android.Manifest
import android.app.*
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
import android.location.Location
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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.material.snackbar.Snackbar
import com.wildfiredetector.smokey.ui.main.FireMapFragment
import com.wildfiredetector.smokey.ui.main.PageViewModel
import kotlinx.android.synthetic.main.settings_activity.*
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList
import kotlin.experimental.and

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

////          Testing to see if BT device is removed when a device is clicked.
//            btDevices.remove(clickedDevice)
//            updateDeviceList()

            // implement gattCallback
            val gattDevice = clickedDevice.connectGatt(this, false, gattCallback, TRANSPORT_LE)


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
            if(newState == BluetoothGatt.STATE_DISCONNECTED)
            {
                d(TAG, "Disconnected")
                gatt?.disconnect()
                Thread.sleep(10)
                gatt?.close()
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
                if (readFire == 1) {
                    pageViewModel.updateBLEFireReport(true)
                }
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
                if(readFire == 1)
                {
                    d(TAG, "in if statement")
                    pageViewModel.updateBLEFireReport(true)
                }
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            d(TAG, "onDescriptorWrite")
            super.onDescriptorWrite(gatt, descriptor, status)
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




