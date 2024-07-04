package com.example.obd2_reader.data

import com.example.obd2_reader.util.Resource
import kotlinx.coroutines.flow.MutableSharedFlow

interface OBD2ReciveManager {

    val data: MutableSharedFlow<Resource<OBD2Result>>

    fun reconnect()

    fun disconnect()

    fun startReceiving()

    fun closeConnection()
}