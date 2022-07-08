package com.example.bleserial.bluetooth;

import android.bluetooth.BluetoothDevice;

public interface ScanResultsConsumer {
    public void candidateDevice(BluetoothDevice device, byte[] scan_record, int rssi);

    public void scanningStarted();

    public void scanningStopped();
}