package com.example.obd2_reader.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ClipDescription
import android.content.Context
import android.util.Log
import com.example.obd2_reader.data.ConnectionState
import com.example.obd2_reader.data.OBD2ReciveManager
import com.example.obd2_reader.data.OBD2Result
import com.example.obd2_reader.util.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

@SuppressLint("MissingPermission")
class OBD2BLEReciveManager @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter,
    private val context: Context
) : OBD2ReciveManager {

    private val DEVICE_NAME = "OBD2"
    private val OBD2_SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
    private val OBD2_CHARACTERISTICS_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8"

    override val data: MutableSharedFlow<Resource<OBD2Result>> = MutableSharedFlow()

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private var gatt: BluetoothGatt? = null
    private var isScanning = false



    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val scanCallback = object : ScanCallback(){
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if(result.device.name == DEVICE_NAME){
                coroutineScope.launch {
                    data.emit(Resource.Loading(message = "Connecting to device..."))
                }
                if(isScanning){
                    result.device.connectGatt(context, false, gattCallback)
                    isScanning = false
                    bleScanner.stopScan(this)
                }
            }
        }
    }

    private var currentConnectionAttempt = 1
    private var MAXIMUM_CONNECTION_ATTEMPTS = 5

    private val gattCallback = object : BluetoothGattCallback(){
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                if(newState == BluetoothProfile.STATE_CONNECTED){
                    coroutineScope.launch {
                        data.emit(Resource.Loading(message = "Discovering Services..."))
                    }
                    gatt.discoverServices()
                    this@OBD2BLEReciveManager.gatt = gatt
                } else if(newState == BluetoothProfile.STATE_DISCONNECTED) {
                    coroutineScope.launch {
                        data.emit(Resource.Success(data = OBD2Result(0f,0f,
                            0f,0f,0f,0f,0f,0f,
                            0f,0f,0f,0f,0f,
                            0f, ConnectionState.Disconnected)))
                    }
                    gatt.close()
                }
            } else {
                gatt.close()
                currentConnectionAttempt+=1
                coroutineScope.launch {
                    data.emit(Resource.Loading(
                        message = "Attempting to connect $currentConnectionAttempt/$MAXIMUM_CONNECTION_ATTEMPTS"
                        )
                    )
                }
                if (currentConnectionAttempt<=MAXIMUM_CONNECTION_ATTEMPTS){
                    startReceiving()
                }else {
                    coroutineScope.launch {
                        data.emit(Resource.Error(errorMessage = "Could not connect to ble device"))
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                printGattTable()
                coroutineScope.launch {
                    data.emit(Resource.Loading(message = "Adjusting MTU space..."))
                }
                gatt.requestMtu(517)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            val characteristic = findCharacteristic(OBD2_SERVICE_UUID, OBD2_CHARACTERISTICS_UUID)
            if(characteristic == null){
                coroutineScope.launch {
                    data.emit(Resource.Error(errorMessage = "Could not find publisher"))
                }
                return
            }
            enableNotification(characteristic)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            with(characteristic){
                when(uuid){
                    UUID.fromString(OBD2_CHARACTERISTICS_UUID) -> {
                        val jsonString = String(value, Charsets.UTF_8)
                        Log.d("OBD2", "Received JSON string: $jsonString")
                        val jsonObject = JSONObject(jsonString)
                        val obd2Result = OBD2Result(
                            engineCoolantTemp = jsonObject.getDouble("Engine Coolant Temperature").toFloat(),
                            engineOilTemp = jsonObject.getDouble("Engine Oil Temperature").toFloat(),
                            intakeAirTemp = jsonObject.getDouble("Intake Air Temperature").toFloat(),
                            fuelTemp = jsonObject.getDouble("Fuel Temperature").toFloat(),
                            engineRPM = jsonObject.getDouble("Engine RPM").toFloat(),
                            loadValue = jsonObject.getDouble("Calculated Load Value").toFloat(),
                            boostPressure = jsonObject.getDouble("Boost Pressure").toFloat(),
                            massAirFlowRate = jsonObject.getDouble("Mass Air Flow Rate").toFloat(),
                            throttlePosition = jsonObject.getDouble("Throttle Position").toFloat(),
                            fuelPreassure = jsonObject.getDouble("Fuel Pressure").toFloat(),
                            fuelConsumption = jsonObject.getDouble("Fuel Consumption Rate").toFloat(),
                            acceleratorPedalPosition = jsonObject.getDouble("Accelerator Pedal Position").toFloat(),
                            brakePedalPosition = jsonObject.getDouble("Brake Pedal Position").toFloat(),
                            vehicleSpeed = jsonObject.getDouble("Vehicle Speed").toFloat(),
                            connectionState = ConnectionState.Connected
                        )
                        coroutineScope.launch {
                            data.emit(
                                Resource.Success(data = obd2Result)
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }

    }

    private fun enableNotification(characteristic: BluetoothGattCharacteristic){
        val cccdUuod = UUID.fromString(CCCD_DESCRIPTOR_UUID)
        val payload = when {
            characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> return
        }

        characteristic.getDescriptor(cccdUuod)?.let { cccdDescriptor ->
            if (gatt?.setCharacteristicNotification(characteristic, true) == false) {
                Log.d("BLEReciveManager", "set characteristics notification failed")
                return
            }
            writeDescription(cccdDescriptor,payload)
        }
    }

    private fun writeDescription(descriptor: BluetoothGattDescriptor, payload:ByteArray){
        gatt?.let { gatt->
            descriptor.value = payload
            gatt.writeDescriptor(descriptor)
        }?: error("Not connected to a ble device!")
    }

    private fun findCharacteristic(serviceUUID: String, characteristicsUUID: String):BluetoothGattCharacteristic?{
        return gatt?.services?.find { service ->
            service.uuid.toString() == serviceUUID
        }?.characteristics?.find { characteristics ->
            characteristics.uuid.toString() == characteristicsUUID
        }
    }

    override fun startReceiving() {
        coroutineScope.launch {
            data.emit(Resource.Loading(message = "Scanning Ble devicess..."))
        }
        isScanning = true
        bleScanner.startScan(null,scanSettings,scanCallback)
    }

    override fun reconnect() {
        gatt?.connect()
    }

    override fun disconnect() {
        gatt?.disconnect()
    }

    override fun closeConnection() {
        bleScanner.stopScan(scanCallback)
        val characteristic = findCharacteristic(OBD2_SERVICE_UUID,OBD2_CHARACTERISTICS_UUID)
        if(characteristic != null){
            disconnectCharecteristics(characteristic)
        }
        gatt?.close()
    }

    private fun disconnectCharecteristics(characteristic: BluetoothGattCharacteristic){
        val cccdUUID = UUID.fromString(CCCD_DESCRIPTOR_UUID)
        characteristic.getDescriptor(cccdUUID)?.let {cccdDescriptor ->
            if(gatt?.setCharacteristicNotification(characteristic, false) == false){
                Log.d("OBD2ReciveManager","set charecteristics notification failed")
                return
            }
            writeDescription(cccdDescriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
        }
    }
}