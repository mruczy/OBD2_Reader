/*
  Based on Neil Kolban example for IDF: https://github.com/nkolban/esp32-snippets/blob/master/cpp_utils/tests/BLE%20Tests/SampleNotify.cpp
  Ported to Arduino ESP32 by Evandro Copercini
  updated by chegewara and MoThunderz
*/
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic = NULL;
BLEDescriptor *pDescr;
BLE2902 *pBLE2902;

bool deviceConnected = false;
bool oldDeviceConnected = false;

// See the following for generating UUIDs:
// https://www.uuidgenerator.net/

#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
    }
};

// Define the maximum and minimum values for each parameter
struct Parameter {
  const char* name;
  float value;
  float minValue;
  float maxValue;
  float increment;
  bool increasing;
  const char* unit;
};

// Initialize each parameter with its starting value, min value, max value, increment value, and unit
Parameter parameters[] = {
  {"Engine Coolant Temperature", 50.0, 50.0, 120.0, 0.1, true, "°C"},
  {"Engine Oil Temperature", 50.0, 50.0, 120.0, 0.1, true, "°C"},
  {"Intake Air Temperature", 10.0, 10.0, 60.0, 0.1, true, "°C"},
  {"Fuel Temperature", 10.0, 10.0, 60.0, 0.1, true, "°C"},
  {"Engine RPM", 1000.0, 1000.0, 6000.0, 10.0, true, "RPM"},
  {"Calculated Load Value", 20.0, 0.0, 100.0, 0.5, true, "%"},
  {"Boost Pressure", 0.5, 0.5, 2.0, 0.01, true, "bar"},
  {"Mass Air Flow Rate", 2.0, 2.0, 20.0, 0.1, true, "g/s"},
  {"Throttle Position", 10.0, 0.0, 100.0, 0.5, true, "%"},
  {"Fuel Pressure", 50.0, 30.0, 100.0, 0.5, true, "kPa"},
  {"Fuel Consumption Rate", 5.0, 1.0, 20.0, 0.1, true, "L/h"},
  {"Accelerator Pedal Position", 0.0, 0.0, 100.0, 1.0, true, "%"},
  {"Brake Pedal Position", 0.0, 0.0, 100.0, 1.0, true, "%"},
  {"Vehicle Speed", 0.0, 0.0, 200.0, 1.0, true, "km/h"}
};

const int numParameters = sizeof(parameters) / sizeof(parameters[0]);

void setup() {
  Serial.begin(115200);

  // Create the BLE Device
  BLEDevice::init("OBD2");

  // Create the BLE Server
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // Create the BLE Service
  BLEService *pService = pServer->createService(SERVICE_UUID);

  // Create a BLE Characteristic
  pCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID,
                      BLECharacteristic::PROPERTY_NOTIFY
                    );                   

  // Create a BLE Descriptor
  
  pDescr = new BLEDescriptor((uint16_t)0x2901);
  pDescr->setValue("Dane");
  pCharacteristic->addDescriptor(pDescr);
  
  pBLE2902 = new BLE2902();
  pBLE2902->setNotifications(true);
  pCharacteristic->addDescriptor(pBLE2902);

  // Start the service
  pService->start();

  // Start advertising
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(false);
  pAdvertising->setMinPreferred(0x0);  // set value to 0x00 to not advertise this parameter
  BLEDevice::startAdvertising();
  Serial.println("Waiting a client connection to notify...");
}

void loop() {
  // Create a JSON string with all parameters
  String jsonData = "{";
  for (int i = 0; i < numParameters; ++i) {
    Parameter &p = parameters[i];

    // Update the parameter value
    if (p.increasing) {
      p.value += p.increment;
      if (p.value >= p.maxValue) {
        p.value = p.maxValue;
        p.increasing = false;
      }
    } else {
      p.value -= p.increment;
      if (p.value <= p.minValue) {
        p.value = p.minValue;
        p.increasing = true;
      }
    }

    // Append the parameter to the JSON string
    jsonData += "\"" + String(p.name) + "\":" + String(p.value) + ", ";
  }
  jsonData.remove(jsonData.length() - 2); // Remove the last comma and space
  jsonData += "}";

  // notify changed value
  if (deviceConnected) {
    pCharacteristic->setValue(jsonData.c_str());
    pCharacteristic->notify();
    // Print the JSON data to the serial monitor
    Serial.println(jsonData);
    delay(1000);
  }
  // disconnecting
  if (!deviceConnected && oldDeviceConnected) {
    delay(500); // give the bluetooth stack the chance to get things ready
    pServer->startAdvertising(); // restart advertising
    Serial.println("start advertising");
    oldDeviceConnected = deviceConnected;
  }
  // connecting
  if (deviceConnected && !oldDeviceConnected) {
      // do stuff here on connecting
      oldDeviceConnected = deviceConnected;
  }
}