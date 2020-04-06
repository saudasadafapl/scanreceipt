package ca.viinc.fntscanreceipt

/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.util.*

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
class UartService : Service() {

    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothDeviceAddress: String? = null
    private var mBluetoothGatt: BluetoothGatt? = null
    private var mConnectionState = STATE_DISCONNECTED


    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private val mGattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val intentAction: String

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED
                mConnectionState = STATE_CONNECTED
                broadcastUpdate(intentAction)
                Log.d(TAG, "Connected to GATT server.")
                // Attempts to discover services after successful connection.
                Log.d(TAG, "Attempting to start service discovery:" + mBluetoothGatt!!.discoverServices())

            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                Log.d(TAG, "-----GATT Connecting: Status code ${status}")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                Log.d(TAG, "-----GATT Connecting: Status code ${status}")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED
                mConnectionState = STATE_DISCONNECTED
                Log.d(TAG, "Disconnected from GATT server.")
                broadcastUpdate(intentAction)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "mBluetoothGatt = " + mBluetoothGatt!!)

                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }
    }

    private val mBinder = LocalBinder()
    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after `BluetoothGatt#discoverServices()` completes successfully.
     *
     * @return A `List` of supported services.
     */
    val supportedGattServices: List<BluetoothGattService>?
        get() = if (mBluetoothGatt == null) null else mBluetoothGatt!!.services

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun broadcastUpdate(
        action: String,
        characteristic: BluetoothGattCharacteristic
    ) {
        val intent = Intent(action)

        // This is handling for the notification on TX Character of NUS service
        if (TX_CHAR_UUID == characteristic.uuid) {

            // Log.d(TAG, String.format("Received TX: %d",characteristic.getValue() ));
            intent.putExtra(EXTRA_DATA, characteristic.value)
        } else {

        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    inner class LocalBinder : Binder() {
        internal val service: UartService
            get() = this@UartService
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    override fun onUnbind(intent: Intent): Boolean {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close()
        return super.onUnbind(intent)
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    fun initialize(): Boolean {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.")
                return false
            }
        }

        mBluetoothAdapter = mBluetoothManager!!.adapter
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.")
            return false
        }

        return true
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */
    fun connect(address: String?): Boolean {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.")
            return false
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address == mBluetoothDeviceAddress && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.")
            if (mBluetoothGatt!!.connect()) {
                mConnectionState = STATE_CONNECTING
                return true
            } else {
                return false
            }
        }

        val device = mBluetoothAdapter!!.getRemoteDevice(address)
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.")
            return false
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback)
        Log.d(TAG, "Trying to create a new connection.")
        mBluetoothDeviceAddress = address
        mConnectionState = STATE_CONNECTING
        return true
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */
    fun disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt!!.disconnect()
        // mBluetoothGatt.close();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    fun close() {
        if (mBluetoothGatt == null) {
            return
        }
        Log.w(TAG, "mBluetoothGatt closed")
        mBluetoothDeviceAddress = null
        mBluetoothGatt!!.close()
        mBluetoothGatt = null
    }

    /**
     * Request a read on a given `BluetoothGattCharacteristic`. The read result is reported
     * asynchronously through the `BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)`
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt!!.readCharacteristic(characteristic)
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     *
     */

    /**
     * Enable Notification on TX characteristic
     *
     * @return
     */
    fun enableTXNotification() {
        /*
    	if (mBluetoothGatt == null) {
    		showMessage("mBluetoothGatt null" + mBluetoothGatt);
    		broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
    		return;
    	}
    		*/
        val RxService = mBluetoothGatt!!.getService(RX_SERVICE_UUID)
        if (RxService == null) {
            showMessage("Rx service not found!")
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART)
            return
        }
        val TxChar = RxService.getCharacteristic(TX_CHAR_UUID)
        if (TxChar == null) {
            showMessage("Tx charateristic not found!")
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART)
            return
        }
        mBluetoothGatt!!.setCharacteristicNotification(TxChar, true)

        val descriptor = TxChar.getDescriptor(CCCD)
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        mBluetoothGatt!!.writeDescriptor(descriptor)

    }

    fun writeRXCharacteristic(value: ByteArray) {


        val RxService = mBluetoothGatt!!.getService(RX_SERVICE_UUID)
        showMessage("mBluetoothGatt null" + mBluetoothGatt!!)
        if (RxService == null) {
            showMessage("Rx service not found!")
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART)
            return
        }
        val RxChar = RxService.getCharacteristic(RX_CHAR_UUID)
        if (RxChar == null) {
            showMessage("Rx charateristic not found!")
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART)
            return
        }
        RxChar.value = value
        val status = mBluetoothGatt!!.writeCharacteristic(RxChar)

        Log.d(TAG, "write TXchar - status=$status")
    }

    private fun showMessage(msg: String) {
        Log.e(TAG, msg)
    }

    companion object {
        private val TAG = UartService::class.java!!.getSimpleName()

        private val STATE_DISCONNECTED = 0
        private val STATE_CONNECTING = 1
        private val STATE_CONNECTED = 2

        val ACTION_GATT_CONNECTED = "com.nordicsemi.nrfUART.ACTION_GATT_CONNECTED"
        val ACTION_GATT_DISCONNECTED = "com.nordicsemi.nrfUART.ACTION_GATT_DISCONNECTED"
        val ACTION_GATT_SERVICES_DISCOVERED = "com.nordicsemi.nrfUART.ACTION_GATT_SERVICES_DISCOVERED"
        val ACTION_DATA_AVAILABLE = "com.nordicsemi.nrfUART.ACTION_DATA_AVAILABLE"
        val EXTRA_DATA = "com.nordicsemi.nrfUART.EXTRA_DATA"
        val DEVICE_DOES_NOT_SUPPORT_UART = "com.nordicsemi.nrfUART.DEVICE_DOES_NOT_SUPPORT_UART"

        val TX_POWER_UUID = UUID.fromString("00001804-0000-1000-8000-00805f9b34fb")
        val TX_POWER_LEVEL_UUID = UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb")
        val CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        val FIRMWARE_REVISON_UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")
        val DIS_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")
        val RX_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        val RX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
        val TX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
    }
}
