package com.example.circulatorphr;

import static java.lang.Thread.sleep;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements BleScanControl.BleScanControlCallback, BleGattInterface.BleGattInterfaceCallback {

    private static final int CONTROL_ID_DEVICE_CONTROL = 1;
    private static final int STATUS_ID_DEVICE_STATUS = 1;
    private static final int STATUS_ID_OFF_TIMER = 2;
    private final String TAG = "PHR";

    private final String HHUAWEI_MEDIAPADT5 = "AGS2-W09";
    private final String LENOVO_TAB10 = "Lenovo TB-X606F";

    private BleScanControl mBleScanControl;
    private BleGattInterface mBleGattInterface;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BleDeviceAdapter mBleDeviceAdapter;
    private String mBluetoothAddress;
    private String mBleSelectDeviceAddress;
    private Boolean isBleConnect;
    private Boolean isPowerOn;
    private Boolean isAirFlow;
    private Boolean isHorizontalSwing;
    private Boolean isVerticalSwing;
    private int mAirFlowLv;
    private int mAirFlowRotation;
    private int mSetTimerValue;

    private Timer mBleConnectingTimer;
    private BleConnectionTask mBleConnectionTask;
    private Timer mAirFlowBarRotationTimer;
    private AirFlowBarRotationTask mAirFlowBarRotationTask;
    private Timer mPowerOffTimer;
    private PowerOffTask mPowerOffTask;

    private ImageView mImageViewBluetoothDisconnected;
    private ImageView mImageViewBluetoothConnected;
    private ImageView mImageViewBluetoothCancel;
    private ImageView mImageViewPowerOff;
    private ImageView mImageViewPowerOn;
    private ImageView mImageViewAirFlowOff;
    private ImageView mImageViewAirFlowOn;
    private ImageView mImageViewAirFlowLow;
    private ImageView mImageViewAirFlowMiddle;
    private ImageView mImageViewAirFlowHigh;
    private ImageView mImageViewHorizontalSwingOff;
    private ImageView mImageViewHorizontalSwingOn;
    private ImageView mImageViewVerticalSwingOff;
    private ImageView mImageViewVerticalSwingOn;
    private ImageView mImageViewTimerOff;
    private ImageView mImageViewTimerOn;
    private TextView mTextViewTimerMin;
    private TextView mTextViewTimerVal;

    /**
     * onCreate Method
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        isBleConnect = false;
        isPowerOn = false;
        isAirFlow = false;
        isHorizontalSwing = false;
        isVerticalSwing = false;
        mAirFlowLv = 0;
        mAirFlowRotation = 0;
        mSetTimerValue = 0;

        Log.i(TAG, "Model: " + Build.MODEL);

        setContentView(R.layout.activity_main);
        initialContents();
        initializeBleService();
    }

    /**
     * onStart Method
     */
    @Override
    public void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();
    }

    /**
     * onResume Method
     */
    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
    }

    /**
     * onRestart Method
     */
    @Override
    public void onRestart() {
        Log.i(TAG, "onRestart");
        super.onRestart();
    }

    /**
     * onPause
     */
    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
    }

    /**
     * onStop Method
     */
    @Override
    protected void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
    }

    /**
     * onDestroy Method
     */
    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    /**
     * onBackPressed Method
     */
    @Override
    public void onBackPressed() {
        Log.i(TAG, "onBackPressed");
    }

    /**
     * mOnClickListener
     */
    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.equals(mImageViewBluetoothDisconnected)) {
                setContentView(R.layout.ble_scan_device);
                startScanBleDevice();
            } else if(view.equals(mImageViewBluetoothCancel)) {
                if (null != mBleConnectingTimer) {
                    mBleConnectingTimer.cancel();
                    mBleConnectingTimer = null;
                }
                mImageViewBluetoothCancel.setVisibility(View.INVISIBLE);
                isBleConnect = false;
                mImageViewBluetoothDisconnected.setVisibility(View.VISIBLE);
                mImageViewBluetoothConnected.setVisibility(View.INVISIBLE);
            } else if (view.equals(mImageViewPowerOff)) {
                if(isBleConnect)
                {
                    isPowerOn = true;
                    sendControlWrite(CONTROL_ID_DEVICE_CONTROL);
                    sendControlRead(STATUS_ID_DEVICE_STATUS);
                }
            } else if (view.equals(mImageViewPowerOn)) {
                setPowerOffContents();
                sendControlWrite(CONTROL_ID_DEVICE_CONTROL);
            } else if (view.equals(mImageViewAirFlowOn)) {
                if (isPowerOn) {
                    mAirFlowLv = (mAirFlowLv + 1) % 3;
                    sendControlWrite(CONTROL_ID_DEVICE_CONTROL);
                    switch (mAirFlowLv) {
                        case 0:
                            mImageViewAirFlowLow.setVisibility(View.VISIBLE);
                            mImageViewAirFlowMiddle.setVisibility(View.INVISIBLE);
                            mImageViewAirFlowHigh.setVisibility(View.INVISIBLE);
                            break;
                        case 1:
                            mImageViewAirFlowLow.setVisibility(View.INVISIBLE);
                            mImageViewAirFlowMiddle.setVisibility(View.VISIBLE);
                            mImageViewAirFlowHigh.setVisibility(View.INVISIBLE);
                            break;
                        case 2:
                            mImageViewAirFlowLow.setVisibility(View.INVISIBLE);
                            mImageViewAirFlowMiddle.setVisibility(View.INVISIBLE);
                            mImageViewAirFlowHigh.setVisibility(View.VISIBLE);
                            break;
                        default:
                            break;
                    }
                    mAirFlowBarRotationTimer.cancel();
                    mAirFlowBarRotationTimer = new Timer();
                    mAirFlowBarRotationTask = new AirFlowBarRotationTask();
                    mAirFlowBarRotationTimer.schedule(mAirFlowBarRotationTask, 10, (100 - (mAirFlowLv * 30)));
                }
            } else if (view.equals(mImageViewHorizontalSwingOff)) {
                if (isPowerOn) {
                    isHorizontalSwing = true;
                    sendControlWrite(CONTROL_ID_DEVICE_CONTROL);
                    mImageViewHorizontalSwingOn.setVisibility(View.VISIBLE);
                    mImageViewHorizontalSwingOff.setVisibility(View.INVISIBLE);
                }
            } else if (view.equals(mImageViewHorizontalSwingOn)) {
                isHorizontalSwing = false;
                sendControlWrite(CONTROL_ID_DEVICE_CONTROL);
                mImageViewHorizontalSwingOff.setVisibility(View.VISIBLE);
                mImageViewHorizontalSwingOn.setVisibility(View.INVISIBLE);
            } else if (view.equals(mImageViewVerticalSwingOff)) {
                if (isPowerOn) {
                    isVerticalSwing = true;
                    sendControlWrite(CONTROL_ID_DEVICE_CONTROL);
                    mImageViewVerticalSwingOn.setVisibility(View.VISIBLE);
                    mImageViewVerticalSwingOff.setVisibility(View.INVISIBLE);
                }
            } else if (view.equals(mImageViewVerticalSwingOn)) {
                isVerticalSwing = false;
                sendControlWrite(CONTROL_ID_DEVICE_CONTROL);
                mImageViewVerticalSwingOff.setVisibility(View.VISIBLE);
                mImageViewVerticalSwingOn.setVisibility(View.INVISIBLE);
            }
            else if(view.equals(mImageViewTimerOff))
            {
                if (isPowerOn) {
                    mSetTimerValue = 1;
                    sendControlWrite(STATUS_ID_OFF_TIMER);
                    mImageViewTimerOn.setVisibility(View.VISIBLE);
                    mImageViewTimerOff.setVisibility(View.INVISIBLE);
                    mTextViewTimerMin.setVisibility(View.VISIBLE);
                    mTextViewTimerMin.setText(String.format("%d", mSetTimerValue));
                    if (null != mPowerOffTimer) {
                        mPowerOffTimer.cancel();
                    }
                    mPowerOffTimer = new Timer();
                    mPowerOffTask = new PowerOffTask();
                    mPowerOffTimer.schedule(mPowerOffTask, 10, 1000);
                }
            }
            else if(view.equals(mImageViewTimerOn))
            {
                switch(mSetTimerValue)
                {
                    case 1:
                        mSetTimerValue = 2;
                        break;
                    case 2:
                        mSetTimerValue = 4;
                        break;
                    case 4:
                        mSetTimerValue = 0;
                        break;
                    default:
                        break;
                }
                sendControlWrite(STATUS_ID_OFF_TIMER);
                if(0 < mSetTimerValue)
                {
                    mTextViewTimerMin.setText(String.format("%d", mSetTimerValue));
                }
                else {
                    if (null != mPowerOffTimer) {
                        mPowerOffTimer.cancel();
                        mPowerOffTimer = null;
                    }
                    mImageViewTimerOn.setVisibility(View.INVISIBLE);
                    mImageViewTimerOff.setVisibility(View.VISIBLE);
                    mTextViewTimerMin.setVisibility(View.INVISIBLE);
                    mTextViewTimerMin.setText("-");
                    mTextViewTimerVal.setVisibility(View.INVISIBLE);
                }
            }
        }
    };

    /**
     * initialContents Method
     */
    private void initialContents() {
        mImageViewBluetoothDisconnected = findViewById(R.id.imageViewBluetoothDisconnected);
        mImageViewBluetoothDisconnected.setOnClickListener(mOnClickListener);
        mImageViewBluetoothConnected = findViewById(R.id.imageViewBluetoothConnected);
        mImageViewPowerOff = findViewById(R.id.imageViewPowerOff);
        mImageViewPowerOff.setOnClickListener(mOnClickListener);
        mImageViewPowerOn = findViewById(R.id.imageViewPowerOn);
        mImageViewPowerOn.setOnClickListener(mOnClickListener);

        mImageViewBluetoothCancel = findViewById(R.id.imageViewBluetoothCancel);
        mImageViewBluetoothCancel.setOnClickListener(mOnClickListener);
        mImageViewBluetoothCancel.setVisibility(View.INVISIBLE);

        mImageViewAirFlowOff = findViewById(R.id.imageViewAirFlowOff);
        mImageViewAirFlowOn = findViewById(R.id.imageViewAirFlowOn);
        mImageViewAirFlowOn.setOnClickListener(mOnClickListener);
        mImageViewAirFlowLow = findViewById(R.id.imageViewAirFlowLow);
        mImageViewAirFlowMiddle = findViewById(R.id.imageViewAirFlowMiddle);
        mImageViewAirFlowHigh = findViewById(R.id.imageViewAirFlowHigh);

        mImageViewTimerOff = findViewById(R.id.imageViewTimerOff);
        mImageViewTimerOff.setOnClickListener(mOnClickListener);
        mImageViewTimerOn = findViewById(R.id.imageViewTimerOn);
        mImageViewTimerOn.setOnClickListener(mOnClickListener);
        mTextViewTimerMin = findViewById(R.id.textViewTimerMin);
        mTextViewTimerVal = findViewById(R.id.textViewTimerVal);

        mImageViewHorizontalSwingOff = findViewById(R.id.imageViewHorizontalSwingOff);
        mImageViewHorizontalSwingOff.setOnClickListener(mOnClickListener);
        mImageViewHorizontalSwingOn = findViewById(R.id.imageViewHorizontalSwingOn);
        mImageViewHorizontalSwingOn.setOnClickListener(mOnClickListener);
        mImageViewVerticalSwingOff = findViewById(R.id.imageViewVerticalSwingOff);
        mImageViewVerticalSwingOff.setOnClickListener(mOnClickListener);
        mImageViewVerticalSwingOn = findViewById(R.id.imageViewVerticalSwingOn);
        mImageViewVerticalSwingOn.setOnClickListener(mOnClickListener);

        // Power
        if (isPowerOn) {
            setPowerOnContents();
        } else {
            setPowerOffContents();
        }

        if(Build.MODEL.equals(LENOVO_TAB10))
        {
            android.view.ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams)findViewById(R.id.horizontalSwingLayout).getLayoutParams();
            marginParams.setMargins(90, 600, 0, 0);
            findViewById(R.id.horizontalSwingLayout).setLayoutParams(marginParams);

            marginParams = (ViewGroup.MarginLayoutParams)findViewById(R.id.airFlowLayout).getLayoutParams();
            marginParams.setMargins(315, 600, 0, 0);
            findViewById(R.id.airFlowLayout).setLayoutParams(marginParams);

            marginParams = (ViewGroup.MarginLayoutParams)findViewById(R.id.powerLayout).getLayoutParams();
            marginParams.setMargins(540, 600, 0, 0);
            findViewById(R.id.powerLayout).setLayoutParams(marginParams);

            marginParams = (ViewGroup.MarginLayoutParams)findViewById(R.id.timerLayout).getLayoutParams();
            marginParams.setMargins(765, 600, 0, 0);
            findViewById(R.id.timerLayout).setLayoutParams(marginParams);

            marginParams = (ViewGroup.MarginLayoutParams)findViewById(R.id.verticalSwingLayout).getLayoutParams();
            marginParams.setMargins(990, 600, 0, 0);
            findViewById(R.id.verticalSwingLayout).setLayoutParams(marginParams);
        }
    }

    /**
     * initializeBleService Method
     * @return Is Success initialize BLE Server
     */
    private boolean initializeBleService() {
        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        mBleScanControl = new BleScanControl();
        mBleScanControl.setCallBack((BleScanControl.BleScanControlCallback) this);
        mBleGattInterface = new BleGattInterface();
        mBleGattInterface.setmContext(this);
        mBleGattInterface.setCallBack((BleGattInterface.BleGattInterfaceCallback) this);
        mBleGattInterface.startInputReportThread();

        if (null != manager) {
            mBluetoothAdapter = manager.getAdapter();
        }
        if (null == mBluetoothAdapter) {
            return false;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            return false;
        }
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        return true;
    }

    /**
     * setPowerOffContents Method
     */
    private void setPowerOffContents() {
        // Timer stop
        if (null != mAirFlowBarRotationTimer) {
            mAirFlowBarRotationTimer.cancel();
            mAirFlowBarRotationTimer = null;
        }
        if (null != mPowerOffTimer) {
            mPowerOffTimer.cancel();
            mPowerOffTimer = null;
        }

        // Flags reset
        isPowerOn = false;

        // Set contents
        mImageViewPowerOff.setVisibility(View.VISIBLE);
        mImageViewPowerOn.setVisibility(View.INVISIBLE);
        mImageViewAirFlowOff.setVisibility(View.VISIBLE);
        mImageViewAirFlowOn.setVisibility(View.INVISIBLE);
        mImageViewAirFlowLow.setVisibility(View.INVISIBLE);
        mImageViewAirFlowMiddle.setVisibility(View.INVISIBLE);
        mImageViewAirFlowHigh.setVisibility(View.INVISIBLE);
        mImageViewHorizontalSwingOff.setVisibility(View.VISIBLE);
        mImageViewHorizontalSwingOn.setVisibility(View.INVISIBLE);
        mImageViewVerticalSwingOff.setVisibility(View.VISIBLE);
        mImageViewVerticalSwingOn.setVisibility(View.INVISIBLE);
        mImageViewTimerOff.setVisibility(View.VISIBLE);
        mImageViewTimerOn.setVisibility(View.INVISIBLE);
        mTextViewTimerMin.setVisibility(View.INVISIBLE);
        mTextViewTimerVal.setVisibility(View.INVISIBLE);
    }

    /**
     * setPowerOnContents Method
     */
    private void setPowerOnContents() {
        // Flags reset
        isPowerOn = true;

        // Set contents
        mImageViewPowerOn.setVisibility(View.VISIBLE);
        mImageViewPowerOff.setVisibility(View.INVISIBLE);
        if(isAirFlow)
        {
            mImageViewAirFlowOn.setVisibility(View.VISIBLE);
            mImageViewAirFlowOff.setVisibility(View.INVISIBLE);
            switch (mAirFlowLv) {
                case 0:
                    mImageViewAirFlowLow.setVisibility(View.VISIBLE);
                    mImageViewAirFlowMiddle.setVisibility(View.INVISIBLE);
                    mImageViewAirFlowHigh.setVisibility(View.INVISIBLE);
                    break;
                case 1:
                    mImageViewAirFlowLow.setVisibility(View.INVISIBLE);
                    mImageViewAirFlowMiddle.setVisibility(View.VISIBLE);
                    mImageViewAirFlowHigh.setVisibility(View.INVISIBLE);
                    break;
                case 2:
                    mImageViewAirFlowLow.setVisibility(View.INVISIBLE);
                    mImageViewAirFlowMiddle.setVisibility(View.INVISIBLE);
                    mImageViewAirFlowHigh.setVisibility(View.VISIBLE);
                    break;
                default:
                    break;
            }
        }
        else
        {
            mImageViewAirFlowOn.setVisibility(View.INVISIBLE);
            mImageViewAirFlowOff.setVisibility(View.VISIBLE);
        }
        if (isHorizontalSwing) {
            mImageViewHorizontalSwingOn.setVisibility(View.VISIBLE);
            mImageViewHorizontalSwingOff.setVisibility(View.INVISIBLE);
        } else {
            mImageViewHorizontalSwingOn.setVisibility(View.INVISIBLE);
            mImageViewHorizontalSwingOff.setVisibility(View.VISIBLE);
        }
        if (isVerticalSwing) {
            mImageViewVerticalSwingOn.setVisibility(View.VISIBLE);
            mImageViewVerticalSwingOff.setVisibility(View.INVISIBLE);
        } else {
            mImageViewVerticalSwingOn.setVisibility(View.INVISIBLE);
            mImageViewVerticalSwingOff.setVisibility(View.VISIBLE);
        }

        // Timer start
        if (null == mAirFlowBarRotationTimer) {
            mAirFlowBarRotationTimer = new Timer();
            mAirFlowBarRotationTask = new AirFlowBarRotationTask();
            mAirFlowBarRotationTimer.schedule(mAirFlowBarRotationTask, 10, (100 - (mAirFlowLv * 30)));
        }
    }

    /**
     * startScanBleDevice Method
     */
    @SuppressLint("MissingPermission")
    private void startScanBleDevice() {
        Log.i(TAG, "Start BLE Scan");
        // Cancel
        Button canselButton = (Button) findViewById(R.id.cancel);
        canselButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Log.i(TAG, "Cancel BLE Scan");
                mBluetoothLeScanner.stopScan(mBleScanControl);
                setContentView(R.layout.activity_main);
                initialContents();
            }
        });
        // init listview
        ListView deviceListView = (ListView) findViewById(R.id.list);
        mBleDeviceAdapter = new BleDeviceAdapter(this, R.layout.listitem_device, new ArrayList<BleScannedDevice>());
        deviceListView.setAdapter(mBleDeviceAdapter);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterview, View view, int position, long id) {
                BleScannedDevice item = mBleDeviceAdapter.getItem(position);
                if (item != null) {
                    Log.i(TAG, "Stop BLE Scan");
                    mBluetoothLeScanner.stopScan(mBleScanControl);
                    setContentView(R.layout.activity_main);
                    initialContents();

                    mBleConnectingTimer = new Timer();
                    mBleConnectionTask = new BleConnectionTask();
                    mBleConnectingTimer.schedule(mBleConnectionTask, 10, 500);

                    mBluetoothAddress = item.getDevice().getAddress();
                    Log.i(TAG, "Connect to " + mBluetoothAddress);

                    mBleSelectDeviceAddress = item.getDevice().getAddress();
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mBleSelectDeviceAddress);
                    mBleGattInterface.startConnection(device, mBleGattInterface);
                    mImageViewBluetoothCancel.setVisibility(View.VISIBLE);
                }
            }
        });

        mBluetoothLeScanner.startScan(buildScanFilters(), buildScanSettings(), mBleScanControl);
    }

    /**
     * buildScanFilters Method
     * @return The list of Scan filter
     */
    private List<ScanFilter> buildScanFilters()
    {
        List<ScanFilter> scanFilters = new ArrayList<>();
        ScanFilter.Builder builder = new ScanFilter.Builder();

        builder.setDeviceName(BleUtil.BLE_TARGET_DEVICE_NAME);
        scanFilters.add(builder.build());

        return scanFilters;
    }

    /**
     * buildScanSettings Method
     * @return Scan setting builder
     */
    private ScanSettings buildScanSettings()
    {
        ScanSettings.Builder builder = new ScanSettings.Builder();

        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        return builder.build();
    }

    public void sendControlWrite(int controlId)
    {
        byte[] data = new byte[BleUtil.CONTROL_WRITE_LENGTH];
        data[0] = (byte)controlId;

        if(CONTROL_ID_DEVICE_CONTROL == controlId)
        {
            // Power Control
            if(isPowerOn)
            {
                data[2] = 1;
            }
            else
            {
                data[2] = 0;
            }
            // Air Flow Control
            data[3] = (byte)(mAirFlowLv + 1);
            // Horizontal Rotation Control
            if(isHorizontalSwing)
            {
                data[4] = 1;
            }
            else
            {
                data[4] = 0;
            }
            // Vertical Rotation Control
            if(isVerticalSwing)
            {
                data[5] = 1;
            }
            else
            {
                data[5] = 0;
            }
        }
        else if(STATUS_ID_OFF_TIMER == controlId)
        {
            data[2] = (byte)mSetTimerValue;
        }

        mBleGattInterface.sendControlWriteRequest(data);
    }

    public void sendControlRead(int controlId)
    {
        byte[] data = new byte[BleUtil.CONTROL_READ_LENGTH];
        data[0] = (byte)controlId;
        mBleGattInterface.sendStatusReadRequest(data);
    }

    /**
     * BleScanControlCallback Method
     * @param result The information of scanned device
     */
    @Override
    public void BleScanControlCallback(ScanResult result) {
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(() -> {
            // Add Scan Device
            mBleDeviceAdapter.update(result.getDevice());
        });
    }

    /**
     * BleGattInterfaceConnectCallback Method
     * @param state Connection result
     */
    @Override
    public void BleGattInterfaceConnectCallback(int state) {
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(() -> {
            if (null != mBleConnectingTimer) {
                mBleConnectingTimer.cancel();
                mBleConnectingTimer = null;
            }
            mImageViewBluetoothCancel.setVisibility(View.INVISIBLE);
            if (1 == state) {
                Log.i(TAG, "BLE Connected");
                isBleConnect = true;
                mBleGattInterface.sendCharacteristicNotification(true);
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mImageViewBluetoothDisconnected.setVisibility(View.INVISIBLE);
                mImageViewBluetoothConnected.setVisibility(View.VISIBLE);
                sendControlRead(STATUS_ID_DEVICE_STATUS);
                sendControlRead(STATUS_ID_OFF_TIMER);
            }
            else if(0 == state)
            {
                Log.i(TAG, "BLE Disconnected");
                isBleConnect = false;
                mImageViewBluetoothDisconnected.setVisibility(View.VISIBLE);
                mImageViewBluetoothConnected.setVisibility(View.INVISIBLE);
            }
        });
    }

    /**
     * BleGattInterfaceControlNotifyCallback Method
     * @param data Received data buffer
     */
    @Override
    public void BleGattInterfaceControlNotifyCallback(byte[] data)
    {
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(() -> {
            if(STATUS_ID_DEVICE_STATUS == data[0])
            {
                Log.i(TAG, "Status Notify [Control Status]");
                // Power Status
                if(0 == data[2])
                {
                    isPowerOn = false;
                    mImageViewPowerOn.setVisibility(View.INVISIBLE);
                    mImageViewPowerOff.setVisibility(View.VISIBLE);
                }
                else
                {
                    isPowerOn = true;
                    mImageViewPowerOn.setVisibility(View.VISIBLE);
                    mImageViewPowerOff.setVisibility(View.INVISIBLE);
                }
                if(isPowerOn)
                {
                    // Air Flow Status
                    switch (data[3]) {
                        case 1:
                            mAirFlowLv = 0;
                            mImageViewAirFlowOff.setVisibility(View.INVISIBLE);
                            mImageViewAirFlowOn.setVisibility(View.VISIBLE);
                            mImageViewAirFlowLow.setVisibility(View.VISIBLE);
                            mImageViewAirFlowMiddle.setVisibility(View.INVISIBLE);
                            mImageViewAirFlowHigh.setVisibility(View.INVISIBLE);
                            break;
                        case 2:
                            mAirFlowLv = 1;
                            mImageViewAirFlowOff.setVisibility(View.INVISIBLE);
                            mImageViewAirFlowOn.setVisibility(View.VISIBLE);
                            mImageViewAirFlowLow.setVisibility(View.INVISIBLE);
                            mImageViewAirFlowMiddle.setVisibility(View.VISIBLE);
                            mImageViewAirFlowHigh.setVisibility(View.INVISIBLE);
                            break;
                        case 3:
                            mAirFlowLv = 2;
                            mImageViewAirFlowOff.setVisibility(View.INVISIBLE);
                            mImageViewAirFlowOn.setVisibility(View.VISIBLE);
                            mImageViewAirFlowLow.setVisibility(View.INVISIBLE);
                            mImageViewAirFlowMiddle.setVisibility(View.INVISIBLE);
                            mImageViewAirFlowHigh.setVisibility(View.VISIBLE);
                            break;
                        default:
                            break;
                    }
                    if (null != mAirFlowBarRotationTimer) {
                        mAirFlowBarRotationTimer.cancel();
                    }
                    mAirFlowBarRotationTimer = new Timer();
                    mAirFlowBarRotationTask = new AirFlowBarRotationTask();
                    mAirFlowBarRotationTimer.schedule(mAirFlowBarRotationTask, 10, (100 - (mAirFlowLv * 30)));

                    // Horizontal Rotation
                    if(0 == data[4])
                    {
                        isHorizontalSwing = false;
                        mImageViewHorizontalSwingOff.setVisibility(View.VISIBLE);
                        mImageViewHorizontalSwingOn.setVisibility(View.INVISIBLE);
                    }
                    else
                    {
                        isHorizontalSwing = true;
                        mImageViewHorizontalSwingOff.setVisibility(View.INVISIBLE);
                        mImageViewHorizontalSwingOn.setVisibility(View.VISIBLE);
                    }
                    // Vertical Rotation
                    if(0 == data[5])
                    {
                        isVerticalSwing = false;
                        mImageViewVerticalSwingOn.setVisibility(View.INVISIBLE);
                        mImageViewVerticalSwingOff.setVisibility(View.VISIBLE);
                    }
                    else
                    {
                        isVerticalSwing = true;
                        mImageViewVerticalSwingOn.setVisibility(View.VISIBLE);
                        mImageViewVerticalSwingOff.setVisibility(View.INVISIBLE);
                    }
                }
                else
                {
                    setPowerOffContents();
                }
            }
            else if(STATUS_ID_OFF_TIMER == data[0])
            {
                if((0 < data[3]) || (0 < data[4]))
                {
                    mSetTimerValue = (int)data[2];
                    mTextViewTimerMin.setVisibility(View.VISIBLE);
                    mTextViewTimerMin.setText(String.format("%d", mSetTimerValue));
                    if (null == mPowerOffTimer) {
                        mPowerOffTimer = new Timer();
                        mPowerOffTask = new PowerOffTask();
                        mPowerOffTimer.schedule(mPowerOffTask, 10, 1000);
                    }
                    mImageViewTimerOn.setVisibility(View.VISIBLE);
                    mImageViewTimerOff.setVisibility(View.INVISIBLE);
                    mTextViewTimerVal.setVisibility(View.VISIBLE);
                    mTextViewTimerVal.setText(String.format("%02d", data[3]) + ":" + String.format("%02d", data[4]));
                }
                else
                {
                    if (null != mPowerOffTimer) {
                        mPowerOffTimer.cancel();
                        mPowerOffTimer = null;
                    }
                    mImageViewTimerOn.setVisibility(View.INVISIBLE);
                    mImageViewTimerOff.setVisibility(View.VISIBLE);
                    mTextViewTimerVal.setVisibility(View.INVISIBLE);
                    mTextViewTimerMin.setText("-");
                }
            }
        });
    }

    /**
     * BleConnectionTask
     */
    public class BleConnectionTask extends TimerTask {

        @Override
        public void run() {
            final Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> {
                if(View.INVISIBLE == mImageViewBluetoothDisconnected.getVisibility())
                {
                    mImageViewBluetoothDisconnected.setVisibility(View.VISIBLE);
                    mImageViewBluetoothConnected.setVisibility(View.INVISIBLE);
                }
                else
                {
                    mImageViewBluetoothDisconnected.setVisibility(View.INVISIBLE);
                    mImageViewBluetoothConnected.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    /**
     * AirFlowBarRotationTask
     */
    public class AirFlowBarRotationTask extends TimerTask {

        @Override
        public void run() {
            final Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> {
                mAirFlowRotation = (mAirFlowRotation + 10) % 360;
                mImageViewAirFlowOn.setRotation(mAirFlowRotation);
            });
        }
    }

    /**
     * PowerOffTask
     */
    public class PowerOffTask extends TimerTask {

        @Override
        public void run() {
            final Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> {
                if(isPowerOn) {
                    sendControlRead(STATUS_ID_OFF_TIMER);
                }
            });
        }
    }
}
