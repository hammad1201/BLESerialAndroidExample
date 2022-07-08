package com.example.bleserial.ui;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bleserial.R;
import com.example.bleserial.bluetooth.Constants;

import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<BluetoothDevice> dataSet;
    private OnClickListenerScanDevice onClickListener;
    private Context context;

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView deviceNameTextView, macAddressTextView;
        OnClickListenerScanDevice onClickListener;

        public ViewHolder(View itemView, OnClickListenerScanDevice onClickListener) {
            super(itemView);
            this.deviceNameTextView = itemView.findViewById(R.id.deviceNameTextView);
            this.macAddressTextView = itemView.findViewById(R.id.macAddressTextView);
            this.onClickListener = onClickListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            onClickListener.onItemClick(getAdapterPosition());
        }
    }


    public DeviceAdapter(List<BluetoothDevice> data, OnClickListenerScanDevice onClickListener, Context context) {
        this.dataSet = data;
        this.onClickListener = onClickListener;
        this.context = context;
    }

    public void addDevice(BluetoothDevice device) {
        if (!dataSet.contains(device)) {
            dataSet.add(device);
        }
    }

    public boolean contains(BluetoothDevice device) {
        return dataSet.contains(device);
    }

    public BluetoothDevice getDevice(int position) {
        return dataSet.get(position);
    }

    public void clear() {
        dataSet.clear();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_adapter_layout,
                parent, false);
        return new ViewHolder(v, onClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int listPosition) {
        Log.d(Constants.TAG, "onBindViewHolder: Here");
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Activity activity = (Activity) context;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    int REQUEST_BLUETOOTH_CONNECT = 0;
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_CONNECT);
                }
            }

            String name = dataSet.get(listPosition).getName();
            if (name == null)
                name = "No name...";

            ((ViewHolder) holder).deviceNameTextView.setText(name);
            ((ViewHolder) holder).macAddressTextView.setText(dataSet.get(listPosition).getAddress());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    public interface OnClickListenerScanDevice {
        void onItemClick(int position);
    }
}
