package com.example.obd2_reader.presentation

import android.bluetooth.BluetoothAdapter
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import com.example.obd2_reader.data.ConnectionState
import com.example.obd2_reader.presentation.permissions.PermissionUtils
import com.example.obd2_reader.presentation.permissions.SystemBroadcastReceiver
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CarInfoScreen(
    navController: NavController,
    onBluetoothStateChanged:()-> Unit,
    viewModel: OBD2ViewModel = hiltViewModel()
) {

    SystemBroadcastReceiver(systemAction = BluetoothAdapter.ACTION_STATE_CHANGED) {bluetoothState ->
        val action = bluetoothState?.action ?: return@SystemBroadcastReceiver
        if (action == BluetoothAdapter.ACTION_STATE_CHANGED){
            onBluetoothStateChanged()
        }
    }

    val permissionState = rememberMultiplePermissionsState(permissions = PermissionUtils.permissions)
    val lifecycleOwner = LocalLifecycleOwner.current
    val bleConnectionState = viewModel.connectionState

    DisposableEffect(
        key1 = lifecycleOwner,
        effect = {
            val observer = LifecycleEventObserver{_,event ->
                if(event == Lifecycle.Event.ON_START){
                    permissionState.launchMultiplePermissionRequest()
                    if(permissionState.allPermissionsGranted && bleConnectionState == ConnectionState.Disconnected){
                        viewModel.reconnect()
                    }
                }
                if(event == Lifecycle.Event.ON_STOP){
                    if (bleConnectionState == ConnectionState.Connected){
                        viewModel.disconnect()
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    )

    LaunchedEffect(key1 = permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted){
            if (bleConnectionState == ConnectionState.Uninitialized){
                viewModel.initializeConnection()
                Log.d("YEET","viewModel.initializeConnection()")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Dynamic expanding box with "N"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            /*
            if(bleConnectionState == ConnectionState.CurrentlyInitializing){
                Column (
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    CircularProgressIndicator()
                    if(viewModel.initializingMessage != null){
                        Text(
                            text = viewModel.initializingMessage!!
                        )
                    }
                }
            } else if (!permissionState.allPermissionsGranted){
                Text(text = "Allow bt permissions",
                    modifier = Modifier.padding(10.dp),
                    textAlign = TextAlign.Center
                )
            }else if (viewModel.errorMessage != null) {
                Column (
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = viewModel.errorMessage!!
                    )
                    Button(
                        onClick = {
                            if(permissionState.allPermissionsGranted){
                                viewModel.initializeConnection()
                            }
                        }) {
                        Text (
                            "Try again"
                        )
                    }
                }
            }else if (bleConnectionState == ConnectionState.Connected){
                CornerValuesBox(
                    gear = "N",
                    speed = "158",
                    rpm = "3759",
                    topLeftValue = "20°C",
                    topRightValue = "50%",
                    bottomLeftValue = "100km",
                    bottomRightValue = "30L"
                )
            }*/

            CornerValuesBox(
                gear = "N",
                speed = "${viewModel.vehicleSpeed} km/h",
                rpm = "${viewModel.engineRPM} RPM",
                topLeftValue = "${viewModel.engineCoolantTemp} °C",
                topRightValue = "${viewModel.engineOilTemp} °C",
                bottomLeftValue = "${viewModel.intakeAirTemp} °C",
                bottomRightValue = "${viewModel.fuelTemp} °C"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Key data
        Column (
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Accelerator Pedal Position
            DashboardItem(label = "Accelerator Pedal Position", value = "${viewModel.acceleratorPedalPosition}%")

            // Brake Pedal Position
            DashboardItem(label = "Brake Pedal Position", value = "${viewModel.brakePedalPosition}0%")

            // Throttle Position
            DashboardItem(label = "Throttle Position", value = "${viewModel.throttlePosition}%")

            // Calculated Load Value
            DashboardItem(label = "Calculated Load Value", value = "${viewModel.loadValue}%")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid data
        Column (
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Boost Pressure
                GridItem(label = "Boost Pressure", value = "${viewModel.boostPressure} bar", modifier = Modifier.weight(1f))

                // Mass Air Flow Rate
                GridItem(label = "Mass Air Flow Rate", value = "${viewModel.massAirFlowRate} g/s", modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Fuel Consumption Rate
                GridItem(label = "Fuel Consumption", value = "${viewModel.fuelConsumption} L/h", modifier = Modifier.weight(1f))

                // Fuel Pressure
                GridItem(label = "Fuel Pressure", value = "3${viewModel.fuelPressure} kPa", modifier = Modifier.weight(1f))
            }
        }

        //Spacer(modifier = Modifier.height(16.dp))

        /*Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
        ) {
            RoundedButton(label = "Start")
            RoundedButton(label = "Menu")
            RoundedButton(label = "Trasy")
        }*/
    }

    /*Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ){
        Column (
            modifier = Modifier
                .fillMaxSize(0.8f)
                .aspectRatio(1.5f)
                .border(
                    BorderStroke(5.dp, Color.Gray),
                    RoundedCornerShape(5.dp)
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            if(bleConnectionState == ConnectionState.CurrentlyInitializing){
                Log.d("TAG","bleConnectionState == ConnectionState.CurrentlyInitializing")
                Column (
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    CircularProgressIndicator()
                    if(viewModel.initializingMessage != null){
                        Text(
                            text = viewModel.initializingMessage!!
                        )
                    }
                }
            }else if(!permissionState.allPermissionsGranted){
                Log.d("TAG","!permissionState.allPermissionsGranted")
                Text(text = "Allow bt permissions",
                    modifier = Modifier.padding(10.dp),
                    textAlign = TextAlign.Center
                )
            }else if(viewModel.errorMessage != null){
                Log.d("TAG","viewModel.errorMessage != null")
                Column (
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = viewModel.errorMessage!!
                    )
                    Button(
                        onClick = {
                            if(permissionState.allPermissionsGranted){
                                viewModel.initializeConnection()
                            }
                        }) {
                        Text (
                            "Try again"
                        )
                    }
                }
            }else if(bleConnectionState == ConnectionState.Connected){
                Log.d("TAG","bleConnectionState == ConnectionState.Connected")
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Engine Coolant: ${viewModel.engineCoolantTemp}")
                    Text(
                        text = "Engine Oil: ${viewModel.engineOilTemp}")
                    Text(
                        text = "Intake Air: ${viewModel.intakeAirTemp}")
                    Text(
                        text = "Fuel Temp: ${viewModel.fuelTemp}")

                    Text(
                        text = "RPM: ${viewModel.engineRPM}")
                    Text(
                        text = "Load Value: ${viewModel.loadValue}")
                    Text(
                        text = "Boost Pressure: ${viewModel.boostPressure}")
                    Text(
                        text = "Mass Air Flow Rate: ${viewModel.massAirFlowRate}")
                    Text(
                        text = "Throttle Position: ${viewModel.throttlePosition}")
                    Text(
                        text = "Fuel Pressure: ${viewModel.fuelPressure}")
                    Text(
                        text = "Fuel Consumption: ${viewModel.fuelConsumption}")

                    Text(
                        text = "Accelerator: ${viewModel.acceleratorPedalPosition}")
                    Text(
                        text = "Break: ${viewModel.brakePedalPosition}")

                    Text(
                        text = "Speed: ${viewModel.vehicleSpeed}")
                }
            }
        }
    

        
    }*/

    /*Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ){
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(Color.Blue, CircleShape)
                .clickable {
                    navController.navigate(Screen.DriveStatsScreen.route) {
                        popUpTo(Screen.CarInfoScreen.route) {
                            inclusive = true
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ){
            Text(
                text = "Start",
                fontSize = 35.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }*/

}

@Composable
fun SquareValueBox(value: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(50.dp)
            .background(Color.Gray)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
        )
    }
}

@Composable
fun CornerValuesBox(
    gear: String,
    speed: String,
    rpm: String,
    topLeftValue: String,
    topRightValue: String,
    bottomLeftValue: String,
    bottomRightValue: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            // Gear
            Text(
                text = gear,
                style = MaterialTheme.typography.headlineLarge.copy(color = Color.Black)
            )
            // Vehicle Speed
            Text(
                text = speed,
                style = MaterialTheme.typography.headlineMedium.copy(color = Color.Black)
            )
            // Engine RPM
            Text(
                text = rpm,
                style = MaterialTheme.typography.headlineMedium.copy(color = Color.Black)
            )
        }

        //Engine Coolant Temperature
        SquareValueBox(value = topLeftValue, modifier = Modifier.align(Alignment.TopStart))

        //Engine Oil Temperature
        SquareValueBox(value = topRightValue, modifier = Modifier.align(Alignment.TopEnd))

        //Intake Air Temperature
        SquareValueBox(value = bottomLeftValue, modifier = Modifier.align(Alignment.BottomStart))

        //Fuel Temperature
        SquareValueBox(value = bottomRightValue, modifier = Modifier.align(Alignment.BottomEnd))


    }
}

@Composable
fun DashboardItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.LightGray, RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 16.sp)
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GridItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.LightGray, RoundedCornerShape(8.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 14.sp)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RoundedButton(label: String) {
    Button(
        onClick = { /*TODO*/ },
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .size(80.dp)
            .padding(4.dp)
    ) {
        Text(text = label, fontSize = 14.sp)
    }
}