package com.example.circulatorphr;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class BleDeviceAdapter extends ArrayAdapter<BleScannedDevice> {
    private static final String PREFIX_RSSI = "RSSI:";
    private List<BleScannedDevice> mList;
    private LayoutInflater mInflater;
    private int mResId;

    public BleDeviceAdapter(Context context, int resId, List<BleScannedDevice> objects) {
        super(context, resId, objects);
        mResId = resId;
        mList = objects;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        BleScannedDevice item = (BleScannedDevice) getItem(position);

        if (convertView == null) {
            convertView = mInflater.inflate(mResId, null);
        }
        TextView name = (TextView) convertView.findViewById(R.id.device_name);
        name.setText(item.getDisplayName());
        TextView address = (TextView) convertView.findViewById(R.id.device_address);
        address.setText(item.getDevice().getAddress());

        return convertView;
    }

    /** add or update BluetoothDevice */
    public void update(BluetoothDevice newDevice) {
        if ((newDevice == null) || (newDevice.getAddress() == null)) {
            return;
        }

        boolean contains = false;
        for (BleScannedDevice device : mList) {
            if (newDevice.getAddress().equals(device.getDevice().getAddress())) {
                contains = true;
                break;
            }
        }
        if (!contains) {
            // add new BluetoothDevice
            mList.add(new BleScannedDevice(newDevice));
        }
        notifyDataSetChanged();
    }
}
