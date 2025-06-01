package com.example.circulatorphr;

import static java.lang.Thread.sleep;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BleGattInterface extends BluetoothGattCallback {

    interface BleGattInterfaceCallback {
        public void BleGattInterfaceConnectCallback(int state);
        public void BleGattInterfaceControlNotifyCallback(byte[] data);
    }

    private final String TAG = "PHR";
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";

    private Context mContext;
    private Timer mTimer;
    private Boolean isDeviceConnected = false;
    BleGattInterfaceCallback mBleGattInterfaceCallback;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;

    private BluetoothGattService mBluetoothGattServiceControl;
    private BluetoothGattCharacteristic mBluetoothGattCharacteristicControlWrite;
    private BluetoothGattCharacteristic mBluetoothGattCharacteristicControlRead;
    private BluetoothGattCharacteristic mBluetoothGattCharacteristicControlNotify;

    private final Queue<ReportHolder> mInputReportQueue = new ConcurrentLinkedQueue<>();
    private boolean mEnableReportInput;

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

        if(BluetoothProfile.STATE_CONNECTED == newState)
        {
            broadcastUpdate(ACTION_GATT_CONNECTED);
            mBluetoothGatt.discoverServices();
        }
        else if(BluetoothProfile.STATE_DISCONNECTED == newState)
        {
            broadcastUpdate(ACTION_GATT_DISCONNECTED);
            isDeviceConnected = false;
            mBleGattInterfaceCallback.BleGattInterfaceConnectCallback(0);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
        for (BluetoothGattService service : gatt.getServices()) {
            if ((service == null) || (service.getUuid() == null)) {
                continue;
            }

            int charaProp;

            if (BleUtil.SERVICE_CONTROL_UUID.equals(service.getUuid().toString())) {
                mBluetoothGattServiceControl =service;
                int state = 1;

                // Control Write
                mBluetoothGattCharacteristicControlWrite = service.getCharacteristic(UUID.fromString(BleUtil.CHARACTERISTIC_CONTROL_WRITE_UUID));
                charaProp = mBluetoothGattCharacteristicControlWrite.getProperties();
                Log.i(TAG, "Control Write: " + charaProp);

                // Control Read
                mBluetoothGattCharacteristicControlRead = service.getCharacteristic(UUID.fromString(BleUtil.CHARACTERISTIC_CONTROL_READ_UUID));
                charaProp = mBluetoothGattCharacteristicControlRead.getProperties();
                Log.i(TAG, "Control Read: " + charaProp);

                // Control Notify
                mBluetoothGattCharacteristicControlNotify = service.getCharacteristic(UUID.fromString(BleUtil.CHARACTERISTIC_CONTROL_NOTIFY_UUID));
                charaProp = mBluetoothGattCharacteristicControlNotify.getProperties();
                Log.i(TAG, "Control Notify: " + charaProp);

                isDeviceConnected = true;
                mEnableReportInput = true;
                mBleGattInterfaceCallback.BleGattInterfaceConnectCallback(state);
            }
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        mEnableReportInput = true;
        if(BluetoothGatt.GATT_SUCCESS != status) {
            Log.i(TAG, "CharacteristicWrite error: " + status);
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if(BluetoothGatt.GATT_SUCCESS != status) {
            Log.i(TAG, "CharacteristicRead error: " + status);
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        if(BluetoothGatt.GATT_SUCCESS != status) {
            Log.i(TAG, "DescriptorWrite error: " + status);
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if(mBluetoothGattCharacteristicControlNotify.equals(characteristic))
        {
            byte[] data = characteristic.getValue();
            mBleGattInterfaceCallback.BleGattInterfaceControlNotifyCallback(data);
        }
    }

    public void initParameters() {
        mEnableReportInput = true;
    }

    public void setCallBack(BleGattInterfaceCallback arg) {
        mBleGattInterfaceCallback = arg;
    }

    public void setmContext(Context arg) {
        mContext = arg;
    }

    public void startConnection(BluetoothDevice arg, BleGattInterface callback) {
        mBluetoothDevice = arg;
        if(null == mBluetoothDevice)
        {
            Log.i(TAG, "null");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false, (BluetoothGattCallback) callback, BluetoothDevice.TRANSPORT_LE);
        }
    }

    public void startDisconnection()
    {
        if(null != mBluetoothGatt)
        {
            isDeviceConnected = false;
            if(!mEnableReportInput)
            {
                try {
                    sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mInputReportQueue.clear();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    public void sendControlWriteRequest(byte[] data)
    {
        if(isDeviceConnected)
        {
            mInputReportQueue.offer(new ReportHolder(mBluetoothGatt, mBluetoothGattCharacteristicControlWrite, data));
        }
    }

    public void sendStatusReadRequest(byte[] data)
    {
        if(isDeviceConnected)
        {
            mInputReportQueue.offer(new ReportHolder(mBluetoothGatt, mBluetoothGattCharacteristicControlRead, data));
        }
    }

    public void sendCharacteristicNotification(boolean notify) {
        mBluetoothGatt.setCharacteristicNotification(mBluetoothGattCharacteristicControlNotify, notify);
        BluetoothGattDescriptor descriptor = mBluetoothGattCharacteristicControlNotify.getDescriptor(UUID.fromString(BleUtil.CHARACTERISTIC_NOTIFY_COMMON));
        if(notify) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        if(!mBluetoothGatt.writeDescriptor(descriptor)) {
            Log.i(TAG, "Error");
        }
    }

    public void startInputReportThread() {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                final ReportHolder holder = mInputReportQueue.poll();
                if(null != holder) {
                    while(true) {
                        if(mEnableReportInput) {
                            if(isDeviceConnected)
                            {
                                mEnableReportInput = false;
                                BluetoothGattCharacteristic characteristic = holder.getGattCharacteristic();
                                characteristic.setValue(holder.getReport());
                                BluetoothGatt gatt = holder.getGatt();
                                if(false == gatt.writeCharacteristic(characteristic)) {
                                    Log.i(TAG, "writeCharacteristic error");
                                }
                            }
                            break;
                        } else {
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        },0, 1);
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        mContext.sendBroadcast(intent);
    }

    private class ReportHolder {
        private BluetoothGatt mGatt;
        private BluetoothGattCharacteristic mGattCharacteristic;
        private byte [] mReport;

        ReportHolder(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final byte[] report) {
            mGatt = gatt;
            mGattCharacteristic = characteristic;
            mReport = report;
        }

        BluetoothGatt getGatt() {
            return mGatt;
        }

        BluetoothGattCharacteristic getGattCharacteristic() {
            return mGattCharacteristic;
        }

        byte[] getReport() {
            return mReport;
        }
    }
}
