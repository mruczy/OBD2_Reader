package com.example.obd2_reader.presentation

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.obd2_reader.data.ConnectionState
import com.example.obd2_reader.data.OBD2ReciveManager
import com.example.obd2_reader.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OBD2ViewModel @Inject constructor(
    private val obd2ReciveManager: OBD2ReciveManager
) : ViewModel() {

    var initializingMessage by mutableStateOf<String?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    var engineCoolantTemp by mutableStateOf(0f)
        private set
    var engineOilTemp by mutableStateOf(0f)
        private set
    var intakeAirTemp by mutableStateOf(0f)
        private set
    var fuelTemp by mutableStateOf(0f)
        private set

    var engineRPM by mutableStateOf(0f)
        private set
    var loadValue by mutableStateOf(0f)
        private set
    var boostPressure by mutableStateOf(0f)
        private set
    var massAirFlowRate by mutableStateOf(0f)
        private set
    var throttlePosition by mutableStateOf(0f)
        private set
    var fuelPressure by mutableStateOf(0f)
        private set
    var fuelConsumption by mutableStateOf(0f)
        private set

    var acceleratorPedalPosition by mutableStateOf(0f)
        private set
    var brakePedalPosition by mutableStateOf(0f)
        private set

    var vehicleSpeed by mutableStateOf(0f)
        private set

    var connectionState by mutableStateOf<ConnectionState>(ConnectionState.Uninitialized)

    init {
        Log.d("OBD2ViewModel", "ViewModel initialized")
        subscribeToChanges()
    }

    private fun subscribeToChanges() {
        viewModelScope.launch {
            Log.d("OBD2ViewModel", "Subscribing to changes")
            obd2ReciveManager.data.collect { result ->
                Log.d("OBD2ViewModel", "Data collected: $result")
                when (result) {
                    is Resource.Success -> {
                        connectionState = result.data.connectionState
                        engineCoolantTemp = result.data.engineCoolantTemp
                        engineOilTemp = result.data.engineOilTemp
                        intakeAirTemp = result.data.intakeAirTemp
                        fuelTemp = result.data.fuelTemp
                        engineRPM = result.data.engineRPM
                        loadValue = result.data.loadValue
                        boostPressure = result.data.boostPressure
                        massAirFlowRate = result.data.massAirFlowRate
                        throttlePosition = result.data.throttlePosition
                        fuelPressure = result.data.fuelPreassure
                        fuelConsumption = result.data.fuelConsumption
                        acceleratorPedalPosition = result.data.acceleratorPedalPosition
                        brakePedalPosition = result.data.brakePedalPosition
                        vehicleSpeed = result.data.vehicleSpeed

                        //logValues()
                    }
                    is Resource.Loading -> {
                        initializingMessage = result.message
                        connectionState = ConnectionState.CurrentlyInitializing
                    }
                    is Resource.Error -> {
                        errorMessage = result.errorMessage
                        connectionState = ConnectionState.Uninitialized
                    }
                }
            }
        }
    }

    private fun logValues() {
        Log.d("OBD2ViewModel", "Engine Coolant Temp: $engineCoolantTemp")
        Log.d("OBD2ViewModel", "Engine Oil Temp: $engineOilTemp")
        Log.d("OBD2ViewModel", "Intake Air Temp: $intakeAirTemp")
        Log.d("OBD2ViewModel", "Fuel Temp: $fuelTemp")
        Log.d("OBD2ViewModel", "Engine RPM: $engineRPM")
        Log.d("OBD2ViewModel", "Load Value: $loadValue")
        Log.d("OBD2ViewModel", "Boost Pressure: $boostPressure")
        Log.d("OBD2ViewModel", "Mass Air Flow Rate: $massAirFlowRate")
        Log.d("OBD2ViewModel", "Throttle Position: $throttlePosition")
        Log.d("OBD2ViewModel", "Fuel Pressure: $fuelPressure")
        Log.d("OBD2ViewModel", "Fuel Consumption: $fuelConsumption")
        Log.d("OBD2ViewModel", "Accelerator Pedal Position: $acceleratorPedalPosition")
        Log.d("OBD2ViewModel", "Brake Pedal Position: $brakePedalPosition")
        Log.d("OBD2ViewModel", "Vehicle Speed: $vehicleSpeed")
    }

    fun disconnect() {
        obd2ReciveManager.disconnect()
    }

    fun reconnect() {
        obd2ReciveManager.reconnect()
    }

    fun initializeConnection() {
        errorMessage = null
        subscribeToChanges()
        obd2ReciveManager.startReceiving()
        Log.d("YEET","obd2ReciveManager.startReceiving()")
    }

    override fun onCleared() {
        super.onCleared()
        obd2ReciveManager.closeConnection()
    }
}
