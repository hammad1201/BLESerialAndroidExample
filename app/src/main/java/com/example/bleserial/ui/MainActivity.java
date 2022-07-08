package com.example.bleserial.ui;

import static androidx.recyclerview.widget.DividerItemDecoration.VERTICAL;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bleserial.R;
import com.example.bleserial.bluetooth.Constants;
import com.example.bleserial.bluetooth.ScanResultsConsumer;
import com.example.bleserial.bluetooth.Scanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements ScanResultsConsumer {
    private boolean bleScanning = false;
    private static final long SCAN_TIMEOUT = 10000;
    private int deviceCount = 0;
    private Button scanButton;
    public static RecyclerView availableDevicesRecyclerView;

    /*****************************************************************************************/
    DeviceAdapter availableDeviceAdapter;
    private Scanner bleScanner;

    List<BluetoothDevice> deviceList;
    private BluetoothAdapter mBtAdapter = null;
    private LocationManager locationManager;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_ACCESS_FINE_LOCATION = 3;
    private final int REQUEST_BLUETOOTH_SCAN = 4;

    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setButtonText();
        initViews();
        setOnClickListener();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                int REQUEST_BLUETOOTH_CONNECT = 0;
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_CONNECT);
            }
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            showMessage(getResources().getString(R.string.ble_not_supported));
            finish();
        }

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            showMessage("Bluetooth is not available!");
            return;
        }

        bleScanner = new Scanner(this.getApplicationContext());

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        availableDevicesRecyclerView.setLayoutManager(layoutManager);

        availableDeviceAdapter = new DeviceAdapter(deviceList, position -> {
            setScanState(false);
            if (bleScanning) {
                bleScanner.stopScanning();
            }
            BluetoothDevice device = availableDeviceAdapter.getDevice(position);

            Intent intent = new Intent(MainActivity.this, PeripheralControlActivity.class);
            intent.putExtra(PeripheralControlActivity.EXTRA_NAME, device.getName());
            intent.putExtra(PeripheralControlActivity.EXTRA_ID, device.getAddress());
            startActivity(intent);

        }, this);

        availableDevicesRecyclerView.setAdapter(availableDeviceAdapter);
        DividerItemDecoration decoration = new DividerItemDecoration(getApplicationContext(), VERTICAL);
        decoration.setDrawable(Objects.requireNonNull(getResources().getDrawable(R.drawable.divider)));
        availableDevicesRecyclerView.addItemDecoration(decoration);
    }

    private void initViews() {
        Toolbar mainActivityToolbar = findViewById(R.id.mainActivityToolbar);
        setSupportActionBar(mainActivityToolbar);

        scanButton = findViewById(R.id.scanButton);
        availableDevicesRecyclerView = findViewById(R.id.availableDevicesRecyclerView);
        deviceList = new ArrayList<>();
    }

    private void setOnClickListener() {
        scanButton.setOnClickListener(v -> {
            if (!bleScanner.isScanning()) {
                if (!mBtAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);

                } else if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ACCESS_FINE_LOCATION);

                } else if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    MainActivity.this.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));

                } else {
                    deviceCount = 0;
                    startScanning();
                }
            } else {
                bleScanner.stopScanning();
            }
        });
    }

    private void startScanning() {
        runOnUiThread(() -> {
            availableDeviceAdapter.clear();
            availableDeviceAdapter.notifyDataSetChanged();
        });
        showMessage(Constants.SCANNING);
        bleScanner.startScanning(this, SCAN_TIMEOUT);
    }


    @Override
    public void candidateDevice(BluetoothDevice device, byte[] scan_record, int rssi) {
        runOnUiThread(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, REQUEST_BLUETOOTH_SCAN);
                }
            }
            if (!availableDeviceAdapter.contains(device)) {
                availableDeviceAdapter.addDevice(device);
                availableDeviceAdapter.notifyItemInserted(availableDeviceAdapter.getItemCount() - 1);
                deviceCount++;
            }
        });
    }

    @Override
    public void scanningStarted() {
        setScanState(true);
    }

    @Override
    public void scanningStopped() {
        setScanState(false);
    }

    private void showMessage(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void setButtonText() {
        String text = "";
        text = Constants.FIND;
        final String button_text = text;
        runOnUiThread(() -> ((TextView) MainActivity.this.findViewById(R.id.scanButton)).setText(button_text));
    }

    private void setScanState(boolean value) {
        bleScanning = value;
        ((Button) this.findViewById(R.id.scanButton)).setText(value ? Constants.STOP_SCANNING : Constants.FIND);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    showMessage("Bluetooth has turned on!");

                } else {
                    Log.d("MainActivity", "BT not enabled");
                    showMessage("Bluetooth can not be enabled!!!");
                }
                break;

            default:
                Log.e("MainActivity", "Wrong Request code");
                break;
        }
    }
}