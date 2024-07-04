package com.example.obd2_reader.data

data class OBD2Result(
    val engineCoolantTemp:Float,
    val engineOilTemp:Float,
    val intakeAirTemp:Float,
    val fuelTemp:Float,

    val engineRPM:Float,
    val loadValue:Float,
    val boostPressure:Float,
    val massAirFlowRate:Float,
    val throttlePosition:Float,
    val fuelPreassure:Float,
    val fuelConsumption:Float,

    val acceleratorPedalPosition:Float,
    val brakePedalPosition:Float,

    val vehicleSpeed:Float,
    val connectionState: ConnectionState
)
