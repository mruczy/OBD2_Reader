package com.example.obd2_reader.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun Navigation(
    onBluetoothStateChanged:()-> Unit
) {

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.CarInfoScreen.route ){
    //NavHost(navController = navController, startDestination = Screen.DriveStatsScreen.route ){
        composable(Screen.CarInfoScreen.route){
            CarInfoScreen(navController = navController, onBluetoothStateChanged)
        }

        composable(Screen.DriveStatsScreen.route){
            DriveStatsScreen()
        }
    }
}

sealed class Screen(val route:String){
    object CarInfoScreen:Screen("car_info_screen")
    object DriveStatsScreen:Screen("drive_stats_screen")
}