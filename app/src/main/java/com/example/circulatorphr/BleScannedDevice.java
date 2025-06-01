package com.example.circulatorphr;

import android.bluetooth.BluetoothDevice;

public class BleScannedDevice {
    private static final String UNKNOWN = "Unknown";
    /** BluetoothDevice */
    private BluetoothDevice mDevice;
    /** Display Name */
    private String mDisplayName;

    public BleScannedDevice(BluetoothDevice device) {
        if (device == null) {
            throw new IllegalArgumentException("BluetoothDevice is null");
        } else {
            mDevice = device;
            mDisplayName = device.getName();
            if ((mDisplayName == null) || (mDisplayName.length() == 0)) {
                mDisplayName = UNKNOWN;
            }
        }
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public void setDisplayName(String displayName) {
        mDisplayName = displayName;
    }
}
