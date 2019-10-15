package com.wildfiredetector.smokey
import android.Manifest
import android.app.Activity
import android.app.Service
import android.bluetooth.*
import android.bluetooth.BluetoothAdapter.STATE_CONNECTED
import android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED
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


    // This code is for GATT connection of the the bluetooth device
    suspend fun BluetoothDevice.logGattServices(tag: String = "BleGattCoroutines") {
        d("LOL", "am i even here")
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
                        e(tag, "Couldn't read characteristic with uuid: ${it.uuid}", e)
                    }
                }
            }
        } finally {
            deviceConnection.close() // Close when no longer used. Also triggers disconnect by default.
        }
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
            d("LOL", "This is reached")

            // Get the device
            val clickedDevice: BluetoothDevice = btDevices[id.toInt()]

            Toast.makeText(this, "${clickedDevice.name}: ${clickedDevice.address}", Toast.LENGTH_SHORT).show()

            d("LOL", "Gatt begin")

            var server: BluetoothGatt? = null
            // implement gattCallback
            server = clickedDevice.connectGatt(this, false, BluetoothLeService().gattCallback)

            bluetoothLeScanner.stopScan(bleScanner)

            server.discoverServices()

        }
    }

    override fun onStart() {
        d("DeviceListActivity", "onStart()")
        super.onStart()
        d("LOL", "onStart")
        bluetoothLeScanner.startScan(bleScanner)

    }

    override fun onResume() {
        super.onResume()
        d("LOL", "onResume")
        registerReceiver(gattUpdateReceiver, makeGattIntentFilter())
    }

    override fun onPause() {
        super.onPause()
        d("LOL", "onPause")

        unregisterReceiver(gattUpdateReceiver)
    }

    // A service that interacts with the BLE device via the Android BLE API.
    class BluetoothLeService : Service() {
        override fun onBind(p0: Intent?): IBinder? {
            w("LMAO", "onBind called from Bluetooth LE service")
            return Binder()
        }

        val TAG = "BLE GATT"
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


        private fun broadcastUpdate(action: String) {
            d("BLE GATT", "action $action")
            val intent = Intent(this,  SettingsActivity::class.java)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.action = action
            sendBroadcast(intent)
        }

        private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
            i(TAG, "broadcastUpdate")
            val intent = Intent(action)
            // parsing is carried out as per profile specifications.
            // For all profiles writes the data formatted in HEX.
            val data: ByteArray? = characteristic.value
            if (data?.isNotEmpty() == true) {
                val hexString: String = data.joinToString(separator = " ") {
                    String.format("%02X", it)
                }
                intent.putExtra(EXTRA_DATA, "$data\n$hexString")
            }

            sendBroadcast(intent)
        }

        private var connectionState = STATE_DISCONNECTED

        // Various callback methods defined by the BLE API.
        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int
            ) {
                i(TAG, "onConnectionStateChange newState: $newState")
                i(TAG, "STATE_CONNECTED is: $STATE_CONNECTED")
                var intentAction: String = "YOLO"
                when (newState) {
                    STATE_CONNECTED -> {
                        d(TAG, "inside STATE_CONNECTED block")
                        intentAction = ACTION_GATT_CONNECTED
                        connectionState = STATE_CONNECTED
                        broadcastUpdate(intentAction)
                        i(TAG, "Connected to GATT server.")
                        i(TAG, "Attempting to start service discovery: " +
                                gatt?.discoverServices())
                    }
                    STATE_DISCONNECTED -> {
                        d(TAG, "inside STATE_DISCONNECTED block")
                        intentAction = ACTION_GATT_DISCONNECTED
                        connectionState = STATE_DISCONNECTED
                        i(TAG, "Disconnected from GATT server.")
                        broadcastUpdate(intentAction)
                    }
                    else -> {
                        d(TAG, "newState in else: $newState")
                    }
                }
                d(TAG, "newState after when block: $newState")
            }

            // New services discovered
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                // no idea hope this somehow helps me
                var clickedDeviceUuidString = gatt.device.uuids[0].toString()
                var clickedDeviceUuid: UUID = UUID.fromString(clickedDeviceUuidString)
                val service = gatt.getService(clickedDeviceUuid)
                i(TAG, "onServicesDiscovered received: $service")
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
                    else -> w(TAG, "onServicesDiscovered received: $status")
                }
            }

            // Result of a characteristic read operation
            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                i(TAG, "onCharacteristicRead")
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
                        i(TAG, "onCharacteristicRead")
                    }
                }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                super.onCharacteristicWrite(gatt, characteristic, status)
                i(TAG, "onCharacteristicWrite")
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?
            ) {
                super.onCharacteristicChanged(gatt, characteristic)
                i(TAG, "onCharacteristicChanged")
                // Here you can read the characteristc's value
                // hopefully this is what someting I can use
                val temp = characteristic?.value
                i(TAG, "ByteArray sent: $temp")
            }

            override fun onDescriptorRead(
                gatt: BluetoothGatt?,
                descriptor: BluetoothGattDescriptor?,
                status: Int
            ) {
                super.onDescriptorRead(gatt, descriptor, status)
                i(TAG, "onDescriptorRead")
            }

            override fun onDescriptorWrite(
                gatt: BluetoothGatt?,
                descriptor: BluetoothGattDescriptor?,
                status: Int
            ) {
                super.onDescriptorWrite(gatt, descriptor, status)
                i(TAG, "onDescriptorWrite")
            }
        }
    }
    override fun onStop() {
        d("LOL", "Stopped the scan")
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

    private fun makeGattIntentFilter(): IntentFilter
    {
        var intentFilter: IntentFilter = IntentFilter()
        intentFilter.addAction(ACTION_DATA_AVAILABLE)
        intentFilter.addAction(ACTION_GATT_CONNECTED)
        intentFilter.addAction(ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(ACTION_GATT_SERVICES_DISCOVERED)

        return intentFilter
    }
    // Handles various events fired by the Service.
// ACTION_GATT_CONNECTED: connected to a GATT server.
// ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
// ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
// ACTION_DATA_AVAILABLE: received data from the device. This can be a
// result of read or notification operations.
    private val gattUpdateReceiver = object : BroadcastReceiver() {

        private lateinit var bluetoothLeService: BluetoothLeService

        override fun onReceive(context: Context, intent: Intent) {
            d("BLE GATT", "gattUpdateReceiver onReceive")
            val action = intent.action
            when (action){
                ACTION_GATT_CONNECTED -> {
                    d("BLE GATT", "ACTION_GATT_CONNECTED")
                    //connected = true
                    //updateConnectionState(R.string.connected)
                    (context as? Activity)?.invalidateOptionsMenu()
                }
                ACTION_GATT_DISCONNECTED -> {
                    d("BLE GATT", "ACTION_GATT_DISCONNECTED")
                    //connected = false
                    //updateConnectionState(R.string.disconnected)
                    (context as? Activity)?.invalidateOptionsMenu()
                    //clearUI()
                }
                ACTION_GATT_SERVICES_DISCOVERED -> {
                    d("BLE GATT", "ACTION_GATT_SERVICES_DISCOVERED")
                    // Show all the supported services and characteristics on the
                    // user interface.
                }
                ACTION_DATA_AVAILABLE -> {
                    d("BLE GATT", "ACTION_DATA_AVAILABLE")
                    //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA))
                }
            }
        }
    }
}




