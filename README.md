# BLE Serial Android Example
This is a simple app, which scans for BLE Peripherials and connect to them. The example works with NORDIC_UART_SERVICE.

# UUIDS
Here are the UUIDS:

SERVICE_UUID:           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"

CHARACTERISTIC_UUID_RX: "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"

CHARACTERISTIC_UUID_TX: "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"
  
# How to Run the App
Clone the source code and open it in Android Studio. You'll need an android phone to run it. The emulator wont work with BLE.

![Build Image](https://github.com/hammad1201/Images/blob/main/android_studio.png)

# Esp32 Part
Download or clone [this](https://github.com/hammad1201/NordicUARTExampleEsp32) repository and open the **Nordic_UART_Example.ino** file. Upload it to esp32 to start advertising.

![Esp32 Arduino Code](https://github.com/hammad1201/Images/blob/main/Screenshot%202021-10-21%20at%205.07.55%20PM.png)

# Screenshots
Here are some screenshots of the app communication with esp32 using Nordic UART Service.

## Splash Screen
This is the splash screen for the app.

<img src="https://github.com/hammad1201/Images/blob/main/Screenshot_20220709-140138_BLE_Serial.png" alt="Splash Screen" width="250px" height="500px">

## Scan Screen
Press the scan button to start scanning for BLE Peripherals. Select a device to connect to it.

<img src="https://github.com/hammad1201/Images/blob/main/Screenshot_20220709-140202_BLE_Serial.png" alt="Scan Screen" width="250px" height="500px">

## Communication Terminal Screen
Press the connect button to connect to the device.

<img src="https://github.com/hammad1201/Images/blob/main/Screenshot_20220709-140209_BLE_Serial.png" alt="Communication Terminal Screen" width="250px" height="500px">

# Communication
Presing the Connect Button creates a connection with the esp32.

## Device Connected

<img src="https://github.com/hammad1201/Images/blob/main/Screenshot_20220709-140222_BLE_Serial.png" alt="Device Connected" width="250px" height="500px">

## Exchanging Messages
Once Connected, then we can exchange text messages between Android App and the esp32.

<img src="https://github.com/hammad1201/Images/blob/main/Screenshot_20220709-141308_BLE_Serial.png" alt="Exchanging Messages" width="250px" height="500px">

## Esp32 Data Received
Here is a screenshot of the data received to the esp32, which is displayed on Serial monitor.

![Esp32 Data Received](https://github.com/hammad1201/Images/blob/main/Screenshot%202022-07-09%20at%202.23.03%20PM%20(2).png)

# Contact me
Name: Muhammad Hammad

Email: [muhammad.hammad1201@gmail.com](mailto:muhammad.hammad1201@gmail.com)

LinkedIn: [https://www.linkedin.com/in/muhammad-hammad-174984175/](https://www.linkedin.com/in/muhammad-hammad-174984175/)

Fiverr: [https://www.fiverr.com/mhammad1201](https://www.fiverr.com/mhammad1201)

----------------------------------------------------------------------------------------------------------
