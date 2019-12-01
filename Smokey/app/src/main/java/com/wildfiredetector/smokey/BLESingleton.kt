package com.wildfiredetector.smokey

import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.location.Location
import android.util.Log
import java.util.*

class BLESingleton constructor(context: Context) {

    // Manage the singleton instance
    companion object
    {
        @Volatile
        private var INSTANCE: BLESingleton? = null

        fun getInstance(context: Context) =
            INSTANCE ?: synchronized(this)
            {
                INSTANCE ?: BLESingleton(context).also {
                    INSTANCE = it
                }
            }
    }


    /**
     * Bluetooth Setup and scanning
     **/
    val bleScanner = object : ScanCallback() {
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

    val bluetoothLeScanner: BluetoothLeScanner
        get() {
            val bluetoothManager =
                context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            return bluetoothAdapter.bluetoothLeScanner
        }


    val gattCallback = object : BluetoothGattCallback() {
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
                    pageViewModel.updateBLEFireReport(true)
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
                    showNotification("Smokey", "Fire Detected")
                    pageViewModel.updateBLEFireReport(true)
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


    var currentLocation : Location? = null
}