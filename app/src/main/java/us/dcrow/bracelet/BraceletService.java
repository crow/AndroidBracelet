package us.dcrow.bracelet;

import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.UUID;

public class BraceletService extends IntentService {

    /**
     * The local bluetooth adapter, basic (deprecated) bluetooth interface
     */
    private BluetoothAdapter bluetoothAdapter;

    /**
     * The local bluetooth scanner, basic bluetooth interface
     */
    private BluetoothLeScanner bleScanner;

    /**
     * The bluetooth generic attribute profile
     */
    private BluetoothGatt braceletBluetoothGatt;

    /**
     * The bluetooth generic attribute profile
     */
    private BluetoothGatt mantraBluetoothGatt;

    /**
     * The bluetooth send characteristic
     */
    private BluetoothGattCharacteristic braceletSendCharacteristic;

    /**
     * Service where to send data.
     * Discovered empirically,
     * it would be possible to find it programmatically by parsing the GATT messages.
     */
    private final String serviceUUID = "00002220-0000-1000-8000-00805f9b34fb";

    /**
     * Send Characteristic.
     * Discovered empirically,
     * it would be possible to find it programmatically by parsing the GATT messages.
     */
    private final String sendCharacteristicUUID = "00002222-0000-1000-8000-00805f9b34fb";

    /**
     * Receive Characteristic
     * Discovered empirically,
     * it would be possible to find it programmatically by parsing the GATT messages.
     */
    private final String receiveCharacteristicUUID = "00002221-0000-1000-8000-00805f9b34fb";

    /**
     * Client Config Characteristic
     * Discovered empirically,
     * it would be possible to find it programmatically by parsing the GATT messages.
     */
    private final String clientConfigCharacteristicUUID = "00002902-0000-1000-8000-00805f9b34fb";

    /**
     * Timeout for searching for an RFduino.
     */
    private static final long SCAN_TIMEOUT = 5000;

    /**
     * Tells if we are connected or not.
     */
    private boolean braceletConnected;

    /**
     * Tells if we are connected or not.
     */
    private boolean mantraConnected;

    /**
     * Used to call functions asynchronously.
     */
    private Handler handler;

    /**
     * Max calibrated value for the breathing sensor
     */
    private int maxSensorValue;

    /**
     * Max calibrated value for the breathing sensor
     */
    private int minSensorValue;

    /**
     * Filtered sensor value
     */
    private int filteredSensorValue;

    /**
     * Latest color and brightness values
     */
    private int r = 0;
    private int g = 0;
    private int b = 0;
    private int a = 0;


    /**
     * Sample window
     */
    private List<Integer> sampleWindow;

    /**
     * Filter window
     */
    private List<Integer> filterWindow;

    /**
     * Used for logging.
     */
    private static final String TAG = "Bracelet Service";

    private NotificationManager mNM;

    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = R.string.braceletServiceStarted;

    private final IBinder mBinder = new LocalBinder();

    BraceletServiceInterface activity;

    /**
     * The last service start ID
     */
    int lastStartId;

    public BraceletService() {
        super("BraceletService");
        handler = new Handler();
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }

    public class LocalBinder extends Binder {
        public BraceletService getServiceInstance(){
            return BraceletService.this;
        }
    }

    // Here Bracelet Main Activity register to the service as Callbacks client
    public void registerClient(Activity activity){
        this.activity = (BraceletServiceInterface)activity;
    }

    // Interface for Bracelet Activity
    public interface BraceletServiceInterface {
        void braceletConnectionStateChanged(int state);

        void mantraConnectionStateChanged(int state);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // Display a notification about starting.  We put an icon in the status bar.
        showNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        lastStartId = startId;

        return START_STICKY;
    }

    public void connectBracelet () {

        if (!braceletConnected) {
            //If not connected, let's look for RFduino and connect to it
            // TODO disable connectBracelet button
            //connectBraceletButton.setEnabled(false);

            bluetoothAdapter.startLeScan(handleBraceletScan);
            activity.braceletConnectionStateChanged(BluetoothProfile.STATE_CONNECTING);

            //Program a to stop the scanning after some seconds
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!braceletConnected) {
                        //Cancel scan only if not connected
                        Log.d(TAG, "Bracelet scanning timed out, stopping the scan");
                        bluetoothAdapter.stopLeScan(handleBraceletScan);
                        //All graphical things must be run on the UI thread:

                        braceletConnected = false;
                        // Make braceletConnectionStateChanged callback connected to Main Activity to update views
                        activity.braceletConnectionStateChanged(BluetoothProfile.STATE_DISCONNECTED);

                    }
                }
            }, SCAN_TIMEOUT);

        } else {
            //If connected, the same button is used for disconnecting
            braceletConnected = true;
            activity.braceletConnectionStateChanged(BluetoothProfile.STATE_CONNECTED);

            if (braceletBluetoothGatt != null) {
                braceletBluetoothGatt.disconnect();
                braceletConnected = false;
            }
        }

    }

    public void connectMantra () {

        if (!mantraConnected) {
            //If not connected, let's look for Mantra and connect to it

            // TODO disable mantra button
            //connectMantraButton.setEnabled(false);

            bluetoothAdapter.startLeScan(handleMantraScan);
            activity.mantraConnectionStateChanged(BluetoothProfile.STATE_CONNECTING);

            //Program a to stop the scanning after some seconds
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!mantraConnected) {
                        //Cancel scan only if not connected
                        Log.d(TAG, "Mantra scanning timed out, stopping the scan");
                        bluetoothAdapter.stopLeScan(handleMantraScan);

                        mantraConnected = false;
                        activity.mantraConnectionStateChanged(BluetoothProfile.STATE_DISCONNECTED);
                    }
                }
            }, SCAN_TIMEOUT);

        } else {
            //If connected, the same button is used for disconnecting
            mantraConnected = true;
            activity.mantraConnectionStateChanged(BluetoothProfile.STATE_CONNECTED);

            if (mantraBluetoothGatt != null) {
                mantraBluetoothGatt.disconnect();
                mantraConnected = false;
            }
        }
    }


    // TODO: make a button to call stop bracelet service
    public void stopBraceletService () {
        //When the application stops we disconnect
        bluetoothAdapter.stopLeScan(handleMantraScan);
        bluetoothAdapter.stopLeScan(handleBraceletScan);
        if(braceletBluetoothGatt != null) {
            braceletBluetoothGatt.disconnect();
        }
        if(mantraBluetoothGatt != null) {
            mantraBluetoothGatt.disconnect();
        }

    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.braceletServiceStarted);

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, BraceletMainActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.braceletServiceStarted))  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .build();

        // Send the notification.
        mNM.notify(NOTIFICATION, notification);
    }

    /**
     * Sends the breathing color to Bracelet.
     * Gets color value from the BLE characteristic change and brightness from the seekbar and sends them as 4 bytes.
     */
    private void sendBreathingColor(int value){

        int colorToSend = mapSensorValueToColor(value);

        byte[] data = new byte[4];
        data[0] = (byte) Color.red(colorToSend);
        data[1] = (byte) Color.green(colorToSend);
        data[2] = (byte) Color.blue(colorToSend);
        data[3] = (byte) a; // use last stored brightness level

        if(braceletSendCharacteristic != null){
            Log.d(TAG, "Sending Breathing Color to send characteristic:" + braceletSendCharacteristic);
            braceletSendCharacteristic.setValue(data);
            Log.d(TAG, "Sending Breathing Color R:" + Color.red(colorToSend) + " G:" + Color.green(colorToSend) + " B:" + Color.blue(colorToSend));

            braceletSendCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            braceletBluetoothGatt.writeCharacteristic(braceletSendCharacteristic);
        }
    }

    /**
     * Sends the color (red, green, blue, brightness) to Bracelet from seek bars.
     */
    public void updateColor(int r, int g, int b, int a) {
        // Store last manually set colors
        this.r = r;
        this.b = b;
        this.g = g;
        this.a = a;

        byte[] data = new byte[4];
        data[0] = (byte) r;
        data[1] = (byte) g;
        data[2] = (byte) b;
        data[3] = (byte) a;

        if(braceletSendCharacteristic != null){
            Log.d(TAG, "Sending Color to send characteristic:" + braceletSendCharacteristic);
            braceletSendCharacteristic.setValue(data);
            braceletSendCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            braceletBluetoothGatt.writeCharacteristic(braceletSendCharacteristic);
        }
    }

    /**
     * Ranges the sensor data (get the maximum and minimum values)
     */
    private void rangeSensorValue (int sensorValue) {

        int sensorValueBuffer = sensorValue;
        List<Integer> sampleWindowBuffer;

        try {
            // If sampleWindow is at capacity remove zeroth entry before adding
            if (sampleWindow.size() >= 400) {
                sampleWindow.remove(0);
            }
            sampleWindow.add(sensorValue);

            // If sampleWindow is at capacity remove zeroth entry before adding
            if (filterWindow.size() >= 15) {
                //calculate average
                int sum = 0;
                for (int vals : filterWindow) {
                    sum += vals;
                }

                // Set filtered window value to average
                filteredSensorValue =  sum / filterWindow.size();

                filterWindow.remove(0);
            }

            filterWindow.add(sensorValue);

            // Use buffer to avoid concurrent modification
            sampleWindowBuffer = sampleWindow;

            if (sampleWindowBuffer != null && sampleWindowBuffer.size() == 400) {
                maxSensorValue = Collections.max(sampleWindowBuffer);
                minSensorValue = Collections.min(sampleWindowBuffer);
            }

            Log.d(TAG, "Ranged max: " + maxSensorValue + " Ranged min: " + minSensorValue);
        }
        catch (ConcurrentModificationException e) {
            System.err.println("Caught exception: " + e.getMessage());
        }
    }

    /**
     * Color mapping helper, outputs a value between 0 and 360 used for converting to hue
     */
    private int mapSensorValueToColor(int value) {

        // populate filterSensorValue
        rangeSensorValue(value);

        int inputRange = maxSensorValue - minSensorValue;

        int outputRange = 360;
        int hueValue = 0;

        if (value == 0) {
            Log.d(TAG, "Attempted to map null sensor value to color.");
            return 0;
        }

        // Avoid dividing by 0
        if (inputRange != 0) hueValue = (filteredSensorValue - minSensorValue) * outputRange / inputRange + 0;

        Log.d(TAG, "hue value:" +  hueValue);
        Log.d(TAG, "hue value float:" +  (float)hueValue);

        return  Color.HSVToColor(new float[] { (float)hueValue, 1.0f, 1.0f });
    }

    /**
     * Generic mapping helper.
     */
    private int mapValueToRange(int value, int inputMin, int inputMax, int outputMin, int outputMax) {

        int inputRange = inputMax - inputMin;
        int outputRange = outputMax - outputMin;

        return (value - inputMin)*outputRange / inputRange + outputMin;
    }


    // Might be able to just implement a difference callback for each peripheral
    /**
     * Handles the scanning of Mantra devices.
     */
    private BluetoothAdapter.LeScanCallback handleMantraScan = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            if(device.getName() == null) {
                // return early if device is null
                return;
            }

            if(device.getName().compareToIgnoreCase("Mantra") == 0){
                Log.d(TAG, "Found Mantra device: " + device.getName());
                //Got a Mantra
                mantraConnected = true;

                bluetoothAdapter.stopLeScan(handleMantraScan);

                Log.d(TAG, "Found Mantra trying to connect to address: " + device.getAddress());
                mantraBluetoothGatt = device.connectGatt(BraceletService.this, true, new BluetoothGattCallback() {

                    @Override
                    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            Log.d(TAG, "Connected to Mantra, attempting to start service discovery:" +
                                    mantraBluetoothGatt.discoverServices());
                            activity.braceletConnectionStateChanged(BluetoothProfile.STATE_CONNECTED);

                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            Log.d(TAG, "Disconnected from Mantra.");
                            activity.braceletConnectionStateChanged(BluetoothProfile.STATE_DISCONNECTED);
                        }
                    }

                    @Override
                    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                        // 0x11 = unint8 format
                        Log.d(TAG, "Characteristic changed: " + characteristic.getIntValue(0x11, 0));

                        sendBreathingColor(characteristic.getIntValue(0x11, 0));
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {

                        // Set the get notifications set up if device is Mantra
                        if (device.getName().compareToIgnoreCase("Mantra") == 0) {
                            BluetoothGattCharacteristic receiveCharacteristic =
                                    gatt.getService(UUID.fromString(serviceUUID)).getCharacteristic(UUID.fromString(receiveCharacteristicUUID));
                            if (receiveCharacteristic != null) {
                                BluetoothGattDescriptor receiveConfigDescriptor =
                                        receiveCharacteristic.getDescriptor(UUID.fromString(clientConfigCharacteristicUUID));
                                if (receiveConfigDescriptor != null) {
                                    gatt.setCharacteristicNotification(receiveCharacteristic, true);

                                    receiveConfigDescriptor.setValue(
                                            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                    gatt.writeDescriptor(receiveConfigDescriptor);
                                } else {
                                    Log.e(TAG, "Mantra receive config descriptor not found!");

                                }

                            } else {
                                Log.e(TAG, "Mantra receive characteristic not found!");
                            }
                        }
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            //Once the services have been discovered we can connect to the one
                            //we know is used for receiving the RGB values
                            BluetoothGattService serv = mantraBluetoothGatt.getService(UUID.fromString(serviceUUID));

                            //Don't set the send characteristic when Mantra connects, we have nothing to send it, for now
                            //sendCharacteristic = serv.getCharacteristic(UUID.fromString(sendCharacteristicUUID));

                            //Now we assume that the device is fully connected
                            activity.mantraConnectionStateChanged(BluetoothProfile.STATE_CONNECTED);

                        }
                    }
                });
            }
        }
    };

    /**
     * Handles the scanning of Bracelet devices.
     */
    private BluetoothAdapter.LeScanCallback handleBraceletScan = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            if(device.getName() == null) {
                // return early if device is null
                return;
            }
            // Do I need to copy this whole block per peripheral? Or is there a more elegant way of doing what I want
            if(device.getName().compareToIgnoreCase("Bracelet") == 0){

                Log.d(TAG, "Found Bracelet device: " + device.getName());

                //Got a Bracelet
                braceletConnected = true;

                bluetoothAdapter.stopLeScan(handleBraceletScan);

                Log.d(TAG, "Found Bracelet trying to connect to " + device.getAddress());
                braceletBluetoothGatt = device.connectGatt(BraceletService.this, true, new BluetoothGattCallback() {

                    @Override
                    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            Log.d(TAG, "Connected to Bracelet, attempting to start service discovery:" +
                                    braceletBluetoothGatt.discoverServices());
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            Log.d(TAG, "Disconnected from Bracelet.");
                        }
                    }

                    @Override
                    public void onReadRemoteRssi (BluetoothGatt gatt, int rssi, int status) {
                        Log.d(TAG, "Bracelet reported RSSI:" + rssi + " and status: " + status);

                    }

                    @Override
                    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "Characteristic Changed on GATT:" + gatt + " Characteristic: " + characteristic);
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {

                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            //Once the services have been discovered we can connect to the one
                            //we know is used for receiving the RGB values
                            BluetoothGattService serv = braceletBluetoothGatt.getService(UUID.fromString(serviceUUID));
                            braceletSendCharacteristic = serv.getCharacteristic(UUID.fromString(sendCharacteristicUUID));

                            //Now we assume that the device is fully connected
                            activity.braceletConnectionStateChanged(BluetoothProfile.STATE_CONNECTED);
                        }
                    }
                });
            }
        }
    };
}
