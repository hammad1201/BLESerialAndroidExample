package com.example.bleserial.bluetooth;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;


public class BluetoothAdapterService extends Service {
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothManager bluetoothManager;
    private Handler activityHandler = null;
    private BluetoothDevice device;

    private BluetoothGattDescriptor descriptor;
    private boolean connected = false;
    public boolean alarmPlaying = false;

    private final IBinder binder = new LocalBinder();

    // messages sent back to activity
    public static final int GATT_CONNECTED = 1;
    public static final int GATT_DISCONNECT = 2;
    public static final int GATT_SERVICES_DISCOVERED = 3;
    public static final int GATT_CHARACTERISTIC_READ = 4;
    public static final int GATT_CHARACTERISTIC_WRITTEN = 5;
    public static final int MESSAGE = 6;
    public static final int NOTIFICATION_OR_INDICATION_RECEIVED = 7;

    // message params
    public static final String PARCEL_DESCRIPTOR_UUID = "DESCRIPTOR_UUID";
    public static final String PARCEL_CHARACTERISTIC_UUID = "CHARACTERISTIC_UUID";
    public static final String PARCEL_SERVICE_UUID = "SERVICE_UUID";
    public static final String PARCEL_VALUE = "VALUE";
    public static final String PARCEL_RSSI = "RSSI";
    public static final String PARCEL_TEXT = "TEXT";

    public static String UART_SERVICE_UUID = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
    public static String CHARACTERISTIC_UUID_RX = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E";
    public static String CHARACTERISTIC_UUID_TX = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E";
    public static String BLUETOOTH_LE_CCCD = "00002902-0000-1000-8000-00805F9B34FB";


    public class LocalBinder extends Binder {
        public BluetoothAdapterService getService() {
            return BluetoothAdapterService.this;
        }
    }

    public boolean isConnected() {
        return connected;
    }

    @Override
    public void onCreate() {
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                return;
            }
        }
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            return;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    // set activity the will receive the messages
    public void setActivityHandler(Handler handler) {
        activityHandler = handler;
    }

    private void sendConsoleMessage(String text) {
        Message msg = Message.obtain(activityHandler, MESSAGE);
        Bundle data = new Bundle();
        data.putString(PARCEL_TEXT, text);
        msg.setData(data);
        msg.sendToTarget();
    }

    // connect to the device
    public boolean connect(final String address) {
        if (bluetoothAdapter == null || address == null) {
            sendConsoleMessage("connect: bluetooth_adapter=null");
            return false;
        }
        device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            sendConsoleMessage("connect: device=null");
            return false;
        }
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Log.d(Constants.TAG, "connect: No Permission");

            } else {
                bluetoothGatt = device.connectGatt(this, false, gattCallback);
            }
        }

        return true;
    }

    // disconnect from device
    public void disconnect() {
        sendConsoleMessage("disconnecting");
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            sendConsoleMessage("disconnect: bluetooth_adapter|bluetooth_gatt null");
            return;
        }
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Log.d(Constants.TAG, "connect: No Permission");

            } else {
                bluetoothGatt.disconnect();
            }
        }
    }

    /**
     * Discover Services
     **/
    public void discoverServices() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            return;
        }
        Log.d(Constants.TAG, "Discovering GATT services");

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Log.d(Constants.TAG, "connect: No Permission");

            } else {
                bluetoothGatt.discoverServices();
            }
        }
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null)
            return null;
        return bluetoothGatt.getServices();
    }

    /**
     * Read Characteristics
     **/
    public boolean readCharacteristic(String serviceUuid, String characteristicUuid) {
        Log.d(Constants.TAG, "readCharacteristic:" + characteristicUuid + " of service " + serviceUuid);

        if (bluetoothAdapter == null || bluetoothGatt == null) {
            sendConsoleMessage("readCharacteristic: bluetooth_adapter|bluetooth_gatt null");
            return false;
        }

        BluetoothGattService gattService = bluetoothGatt.getService(java.util.UUID.fromString(serviceUuid));
        if (gattService == null) {
            sendConsoleMessage("readCharacteristic: gattService null");
            return false;
        }
        BluetoothGattCharacteristic gattChar = gattService
                .getCharacteristic(java.util.UUID.fromString(characteristicUuid));
        if (gattChar == null) {
            sendConsoleMessage("readCharacteristic: gattChar null");
            return false;
        }

        boolean c = false;
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Log.d(Constants.TAG, "connect: No Permission");

            } else {
                c = bluetoothGatt.readCharacteristic(gattChar);
            }
        }
        return c;
    }

    /**
     * Write Characteristics
     **/
    public boolean writeCharacteristic(String serviceUuid, String characteristicUuid, byte[] value) {
        Log.d(Constants.TAG, "writeCharacteristic:" + characteristicUuid + " of service " + serviceUuid);

        if (bluetoothAdapter == null || bluetoothGatt == null) {
            sendConsoleMessage("writeCharacteristic: bluetooth_adapter|bluetooth_gatt null");
            return false;
        }

        BluetoothGattService gattService = bluetoothGatt.getService(java.util.UUID.fromString(serviceUuid));
        if (gattService == null) {
            sendConsoleMessage("writeCharacteristic: gattService null");
            return false;
        }
        BluetoothGattCharacteristic gattChar = gattService
                .getCharacteristic(java.util.UUID.fromString(characteristicUuid));

        if (gattChar == null) {
            sendConsoleMessage("writeCharacteristic: gattChar null");
            return false;
        }
        gattChar.setValue(value);

        boolean c = false;
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Log.d(Constants.TAG, "connect: No Permission");

            } else {
                c = bluetoothGatt.writeCharacteristic(gattChar);
            }
        }
        return c;
    }

    /**
     * Set Indication State
     **/

    public boolean setIndicationsState(String serviceUuid, String characteristicUuid, boolean enabled) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            sendConsoleMessage("setIndicationsState: bluetooth_adapter|bluetooth_gatt null");
            return false;
        }
        BluetoothGattService gattService = bluetoothGatt.getService(java.util.UUID.fromString(serviceUuid));
        if (gattService == null) {
            sendConsoleMessage("setIndicationsState: gattService null");
            return false;
        }
        BluetoothGattCharacteristic gattChar = gattService.getCharacteristic(java.util.UUID.fromString(characteristicUuid));
        if (gattChar == null) {
            sendConsoleMessage("setIndicationsState: gattChar null");
            return false;
        }

        List<BluetoothGattDescriptor> d = gattChar.getDescriptors();

        for (BluetoothGattDescriptor a : d) {
            Log.d(Constants.TAG, "setIndicationsState: " + a.getUuid().toString().toUpperCase(Locale.ROOT));
        }

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Log.d(Constants.TAG, "connect: No Permission");

            } else {
                bluetoothGatt.setCharacteristicNotification(gattChar, enabled);
            }
        }

        // Enable remote notifications
        descriptor = gattChar.getDescriptor(UUID.fromString(BLUETOOTH_LE_CCCD));

        if (descriptor == null) {
            sendConsoleMessage("setIndicationsState: descriptor null");
            return false;
        }

        if (enabled) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }

        Log.d(TAG, "request max MTU");
        bluetoothGatt.requestMtu(512);  //Without this the esp32 is only able to receive maximum 20 bytes

        return bluetoothGatt.writeDescriptor(descriptor);
    }


    /************************ GATT CALLBACK **************************/
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        /**
         * On Connection State Change
         **/
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(Constants.TAG, "onConnectionStateChange: status=" + status + ", New State = " + newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(Constants.TAG, "onConnectionStateChange: CONNECTED");
                connected = true;
                Message msg = Message.obtain(activityHandler, GATT_CONNECTED);
                msg.sendToTarget();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(Constants.TAG, "onConnectionStateChange: DISCONNECTED");
                connected = false;
                Message msg = Message.obtain(activityHandler, GATT_DISCONNECT);
                msg.sendToTarget();

                if (bluetoothGatt != null) {
                    Log.d(Constants.TAG, "Closing and destroying BluetoothGatt object");

                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Log.d(Constants.TAG, "connect: No Permission");

                        } else {
                            bluetoothGatt.close();
                        }
                    }

                    bluetoothGatt = null;
                }
            }
        }

        /**
         * On Services Discovered
         **/
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            sendConsoleMessage("Services Discovered");
            Message msg = Message.obtain(activityHandler, GATT_SERVICES_DISCOVERED);
            msg.sendToTarget();
        }

        /**
         * On Characteristics Read
         **/
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Bundle bundle = new Bundle();
                bundle.putString(PARCEL_CHARACTERISTIC_UUID, characteristic.getUuid().toString());
                bundle.putString(PARCEL_SERVICE_UUID, characteristic.getService().getUuid().toString());
                bundle.putByteArray(PARCEL_VALUE, characteristic.getValue());
                Message msg = Message.obtain(activityHandler, GATT_CHARACTERISTIC_READ);
                msg.setData(bundle);
                msg.sendToTarget();

            } else {
                Log.d(Constants.TAG, "failed to read characteristic:" + characteristic.getUuid().toString() + " of service " + characteristic.getService().getUuid().toString() + " : status=" + status);
                sendConsoleMessage("characteristic read err:" + status);
            }
        }

        /**
         * On Characteristics Write
         **/
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(Constants.TAG, "onCharacteristicWrite");

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Bundle bundle = new Bundle();
                bundle.putString(PARCEL_CHARACTERISTIC_UUID, characteristic.getUuid().toString());
                bundle.putString(PARCEL_SERVICE_UUID, characteristic.getService().getUuid().toString());
                bundle.putByteArray(PARCEL_VALUE, characteristic.getValue());
                Message msg = Message.obtain(activityHandler, GATT_CHARACTERISTIC_WRITTEN);
                msg.setData(bundle);
                msg.sendToTarget();

            } else {
                sendConsoleMessage("characteristic write err:" + status);
            }
        }

        /**
         * On Characteristic Changed
         **/
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.d(Constants.TAG, "onCharacteristicChanged");

            Bundle bundle = new Bundle();
            bundle.putString(PARCEL_CHARACTERISTIC_UUID, characteristic.getUuid().toString());
            bundle.putString(PARCEL_SERVICE_UUID, characteristic.getService().getUuid().toString());
            bundle.putByteArray(PARCEL_VALUE, characteristic.getValue());

            // notifications and indications are both communicated from here in this way
            Message msg = Message.obtain(activityHandler, NOTIFICATION_OR_INDICATION_RECEIVED);
            msg.setData(bundle);
            msg.sendToTarget();

            Log.d(Constants.TAG, "onCharacteristicChanged: " + Arrays.toString(characteristic.getValue()));

        }
    };
}
