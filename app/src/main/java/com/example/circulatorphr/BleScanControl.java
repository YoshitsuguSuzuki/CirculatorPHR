package com.example.circulatorphr;

import android.annotation.SuppressLint;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;

@SuppressLint("NewApi")
public class BleScanControl extends ScanCallback {
    interface BleScanControlCallback {
        public void BleScanControlCallback(ScanResult result);
    }
    BleScanControlCallback mBleScanControlCallback;

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        if (callbackType == ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
            mBleScanControlCallback.BleScanControlCallback(result);
        }
    }

    public void setCallBack(BleScanControlCallback arg) {
        mBleScanControlCallback = arg;
    }
}
