package com.example.circulatorphr;

public class BleUtil {

    public static final String BLE_TARGET_DEVICE_NAME             = "PHR-Circulator";
    public static final String SERVICE_CONTROL_UUID               = "00000000-000f-11e1-9ab4-0002a5d5c51b";
    public static final String CHARACTERISTIC_CONTROL_WRITE_UUID  = "00000002-000f-11e1-9ab4-0002a5d5c51b";
    public static final String CHARACTERISTIC_CONTROL_READ_UUID   = "00000004-000f-11e1-9ab4-0002a5d5c51b";
    public static final String CHARACTERISTIC_CONTROL_NOTIFY_UUID = "00000006-000f-11e1-9ab4-0002a5d5c51b";
    public static final String CHARACTERISTIC_NOTIFY_COMMON       = "00002902-0000-1000-8000-00805f9b34fb";

    public static final int CONTROL_WRITE_LENGTH = 16;
    public static final int CONTROL_READ_LENGTH = 2;
}
