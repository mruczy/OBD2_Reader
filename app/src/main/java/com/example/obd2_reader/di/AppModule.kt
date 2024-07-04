package com.example.obd2_reader.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.example.obd2_reader.data.OBD2ReciveManager
import com.example.obd2_reader.data.ble.OBD2BLEReciveManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideBluetoothAdapter(@ApplicationContext context: Context):BluetoothAdapter{
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return manager.adapter
    }

    @Provides
    @Singleton
    fun provideOBD2ReciveManager(
        @ApplicationContext context: Context,
        bluetoothAdapter: BluetoothAdapter
    ):OBD2ReciveManager{
        return OBD2BLEReciveManager(bluetoothAdapter, context)
    }


}