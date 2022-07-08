package com.example.bleserial.ui;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.bleserial.R;
import com.example.bleserial.bluetooth.BluetoothAdapterService;
import com.example.bleserial.bluetooth.Constants;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class PeripheralControlActivity extends AppCompatActivity {
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_ID = "id";

    private BluetoothAdapterService bluetoothLeAdapter;

    private String deviceAddress;
    private boolean backRequested = false;

    private TextView nameTextView, receivedTextView;
    private Button sendButton, connectButton;
    private EditText inputEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peripheral_control);

        initViews();
        setOnClickListeners();

        // Read intent data
        final Intent intent = getIntent();
        String deviceName = intent.getStringExtra(EXTRA_NAME);
        deviceAddress = intent.getStringExtra(EXTRA_ID);

        // Show the device name
        nameTextView.setText(String.format("Device : %s [%s]", deviceName, deviceAddress));

        // Connect to the Bluetooth adapter service
        Intent gattServiceIntent = new Intent(this, BluetoothAdapterService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);
        showMessage("READY");
    }

    private void initViews() {
        Toolbar mainActivityToolbar = findViewById(R.id.peripheralActivityToolbar);
        setSupportActionBar(mainActivityToolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        nameTextView = findViewById(R.id.nameTextView);
        connectButton = findViewById(R.id.connectButton);
        sendButton = findViewById(R.id.sendButton);
        receivedTextView = findViewById(R.id.receivedTextView);
        inputEditText = findViewById(R.id.inputEditText);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public void onBackPressed() {
        Log.d(Constants.TAG, "onBackPressed");
        backRequested = true;

        if (bluetoothLeAdapter.isConnected()) {
            try {
                bluetoothLeAdapter.disconnect();

            } catch (Exception ignored) {
            }

        } else {
            finish();
        }
    }

    private void setOnClickListeners() {
        connectButton.setOnClickListener(v -> {
            if (bluetoothLeAdapter != null) {
                if (!bluetoothLeAdapter.isConnected()) {
                    showMessage("onConnect");
                    bluetoothLeAdapter.connect(deviceAddress);

                } else {
                    showMessage("onDisconnect");
                    bluetoothLeAdapter.disconnect();
                }
            }
        });

        sendButton.setOnClickListener(v -> {
            if (bluetoothLeAdapter != null) {
                String str = inputEditText.getText().toString();

                if (!bluetoothLeAdapter.isConnected()) {
                    showMessage(getResources().getString(R.string.connect_to_device_first));
                } else if (str.isEmpty()) {
                    showMessage(getResources().getString(R.string.enter_text_to_send));

                } else {
                    byte[] data = (str).getBytes();
                    bluetoothLeAdapter.writeCharacteristic(BluetoothAdapterService.UART_SERVICE_UUID, BluetoothAdapterService.CHARACTERISTIC_UUID_RX, data);
                }
            }
        });
    }

    /**
     * Service Connection
     **/
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetoothLeAdapter = ((BluetoothAdapterService.LocalBinder) service).getService();
            bluetoothLeAdapter.setActivityHandler(messageHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothLeAdapter = null;
        }
    };

    /**
     * Message Handler from the service
     **/
    @SuppressLint("HandlerLeak")
    private final Handler messageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle;
            String service_uuid;
            String characteristic_uuid;
            byte[] b;

            switch (msg.what) {
                case BluetoothAdapterService.MESSAGE:
                    bundle = msg.getData();
                    String text = bundle.getString(BluetoothAdapterService.PARCEL_TEXT);
                    showMessage(text);
                    break;

                case BluetoothAdapterService.GATT_CONNECTED:
                    showMessage("Connected to Device!");

                    //UI
                    connectButton.setText(getResources().getString(R.string.disconnect_from_device));
                    connectButton.setBackgroundColor(getResources().getColor(R.color.red));

                    bluetoothLeAdapter.discoverServices();

                    break;

                case BluetoothAdapterService.GATT_DISCONNECT:
                    showMessage("Not Connected!");

                    //UI
                    connectButton.setText(getResources().getString(R.string.connect_to_device));
                    connectButton.setBackgroundColor(getResources().getColor(R.color.green));

                    if (backRequested) {
                        PeripheralControlActivity.this.finish();
                    }
                    break;

                case BluetoothAdapterService.GATT_SERVICES_DISCOVERED:
                    // validate services and if ok....
                    List<BluetoothGattService> servicesList = bluetoothLeAdapter.getSupportedGattServices();
                    boolean uart_present = false;

                    for (BluetoothGattService service : servicesList) {
                        Log.d(Constants.TAG, "UUID=" + service.getUuid().toString().toUpperCase() + " INSTANCE=" + service.getInstanceId());

                        if (service.getUuid().toString().equalsIgnoreCase(BluetoothAdapterService.UART_SERVICE_UUID)) {
                            uart_present = true;
                        }
                    }

                    if (uart_present) {
                        showMessage("Device has expected services");

                        /** Enable notifications when confirmed that the required services are present **/
                        if (!bluetoothLeAdapter.setIndicationsState(BluetoothAdapterService.UART_SERVICE_UUID, BluetoothAdapterService.CHARACTERISTIC_UUID_TX, true)) {
                            showMessage("Failed to set notification");
                        }
                    } else {
                        showMessage("Device does not have expected GATT services");
                    }
                    break;

                case BluetoothAdapterService.GATT_CHARACTERISTIC_READ:
                    bundle = msg.getData();
                    Log.d(Constants.TAG, "Service=" + bundle.get(BluetoothAdapterService.PARCEL_SERVICE_UUID).toString().toUpperCase()
                            + " Characteristic=" + bundle.get(BluetoothAdapterService.PARCEL_CHARACTERISTIC_UUID).toString().toUpperCase());

                    characteristic_uuid = bundle.get(BluetoothAdapterService.PARCEL_CHARACTERISTIC_UUID).toString()
                            .toUpperCase();
                    service_uuid = bundle.get(BluetoothAdapterService.PARCEL_SERVICE_UUID).toString()
                            .toUpperCase();

                    if (characteristic_uuid.equals(BluetoothAdapterService.CHARACTERISTIC_UUID_TX)
                            && service_uuid.equals(BluetoothAdapterService.UART_SERVICE_UUID)) {

                        b = bundle.getByteArray(BluetoothAdapterService.PARCEL_VALUE);
                        if (b.length > 0) {
                            String s = new String(b, StandardCharsets.US_ASCII);
                            receivedTextView.setText(s);
                        }
                    }
                    break;

                case BluetoothAdapterService.GATT_CHARACTERISTIC_WRITTEN:
                    bundle = msg.getData();

                    characteristic_uuid = bundle.get(BluetoothAdapterService.PARCEL_CHARACTERISTIC_UUID).toString()
                            .toUpperCase();
                    service_uuid = bundle.get(BluetoothAdapterService.PARCEL_SERVICE_UUID).toString()
                            .toUpperCase();

                    if (characteristic_uuid.equals(BluetoothAdapterService.CHARACTERISTIC_UUID_RX)
                            && service_uuid.equals(BluetoothAdapterService.UART_SERVICE_UUID)) {

                        b = bundle.getByteArray(BluetoothAdapterService.PARCEL_VALUE);
                        if (b.length > 0) {
                            String s = new String(b, StandardCharsets.US_ASCII);
                            showMessage("Value: " + s + " sent");
                        }
                    }
                    break;

                case BluetoothAdapterService.NOTIFICATION_OR_INDICATION_RECEIVED:
                    bundle = msg.getData();
                    service_uuid = bundle.getString(BluetoothAdapterService.PARCEL_SERVICE_UUID);
                    characteristic_uuid = bundle.getString(BluetoothAdapterService.PARCEL_CHARACTERISTIC_UUID);

                    if (characteristic_uuid.equalsIgnoreCase((BluetoothAdapterService.CHARACTERISTIC_UUID_TX))) {
                        b = bundle.getByteArray(BluetoothAdapterService.PARCEL_VALUE);
                        if (b.length > 0) {
                            String s = new String(b, StandardCharsets.US_ASCII);
                            receivedTextView.setText(s);
                            Log.d(Constants.TAG, "handleMessage: " + s);
                        }
                    }
            }
        }
    };

    private void showMessage(final String msg) {
        Log.d(Constants.TAG, msg);
        runOnUiThread(() -> ((TextView) findViewById(R.id.msgTextView)).setText(msg));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        bluetoothLeAdapter = null;
    }
}