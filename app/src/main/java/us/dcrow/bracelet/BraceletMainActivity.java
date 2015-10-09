package us.dcrow.bracelet;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.UUID;



/**
 *
 *
 */
public class BraceletMainActivity extends Activity {

    /**
     * Scan/connect button for the Bracelet
     */
    private Button connectBraceletButton;

    /**
     * Scan/connect button for the Breathing Sensor
     */
    private Button connectMantraButton;

    /**
     * Bar for adjusting color values sent to rfduino
     */
    private SeekBar redBar, greenBar, blueBar, brightnessBar;

    /**
     * Used to call functions asynchronously.
     */
    private Handler handler;

    /**
     * Tells if we are connected or not.
     */
    private boolean braceletConnected;

    /**
     * Tells if we are connected or not.
     */
    private boolean mantraConnected;


    public BraceletMainActivity(){
        super();
        mantraConnected = false;
        braceletConnected = false;
        handler = new Handler();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bracelet_main);

        connectBraceletButton = (Button) findViewById(R.id.connectBraceletButton);
        connectMantraButton = (Button) findViewById(R.id.connectMantraButton);

        connectMantraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               //TODO call connectMantra on service
            }
        });

        connectBraceletButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO call to connectBracelet on service
            }
        });

        redBar = (SeekBar) findViewById(R.id.redBar);
        redBar.setEnabled(false);
        redBar.setMax(255);//RGB are sent as 1 byte each, values from 0 to 255
        redBar.setOnSeekBarChangeListener(barchangeListener);

        greenBar = (SeekBar) findViewById(R.id.greenBar);
        greenBar.setEnabled(false);
        greenBar.setMax(255);
        greenBar.setOnSeekBarChangeListener(barchangeListener);

        blueBar = (SeekBar) findViewById(R.id.blueBar);
        blueBar.setEnabled(false);
        blueBar.setMax(255);
        blueBar.setOnSeekBarChangeListener(barchangeListener);

        brightnessBar = (SeekBar) findViewById(R.id.brightnessBar);
        brightnessBar.setEnabled(false);
        brightnessBar.setMax(100);
        brightnessBar.setOnSeekBarChangeListener(barchangeListener);
    }

    OnSeekBarChangeListener barchangeListener = new OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            //When the value of a bar changes, send the values to RFduino
            sendColor();
        }
    };

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
        data[3] = (byte) brightnessBar.getProgress();

        if(braceletSendCharacteristic != null){
            Log.d(TAG, "Sending Breathing Color to send characteristic:" + braceletSendCharacteristic);
            braceletSendCharacteristic.setValue(data);
            Log.d(TAG, "Sending Breathing Color R:" + Color.red(colorToSend) + " G:" + Color.green(colorToSend) + " B:" + Color.blue(colorToSend));

            braceletSendCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            braceletBluetoothGatt.writeCharacteristic(braceletSendCharacteristic);
        }
    }

    /**
     * Sends the color to Bracelet.
     * Reads the values in the seekbars and sends them as 4 bytes.
     */
    private void sendColor() {

        byte[] data = new byte[4];
        data[0] = (byte) redBar.getProgress();
        data[1] = (byte) greenBar.getProgress();
        data[2] = (byte) blueBar.getProgress();
        data[3] = (byte) brightnessBar.getProgress();

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
    LeScanCallback handleMantraScan = new LeScanCallback() {
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
                mantraBluetoothGatt = device.connectGatt(BraceletMainActivity.this, true, new BluetoothGattCallback() {

                    @Override
                    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            Log.d(TAG, "Connected to Mantra, attempting to start service discovery:" +
                                    mantraBluetoothGatt.discoverServices());
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            Log.d(TAG, "Disconnected from Mantra.");

                            if (braceletConnected) {
                                BraceletMainActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //On the UI we shall enable the bars and the disconnect button
                                        // If bracelet is connected and mantra disconnects, re-display color bars
                                        redBar.setEnabled(true);
                                        greenBar.setEnabled(true);
                                        blueBar.setEnabled(true);
                                    }
                                });
                            }

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
                            BraceletMainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //On the UI we shall enable the bars and the disconnect button
                                    connectMantraButton.setEnabled(true);
                                    connectMantraButton.setText(R.string.disconnectMantra);

                                    // When Mantra is connected, sliders go away except brightness
                                    redBar.setEnabled(false);
                                    greenBar.setEnabled(false);
                                    blueBar.setEnabled(false);

                                    brightnessBar.setEnabled(true);
                                }
                            });
                        }
                    }
                });
            }
        }
    };

    /**
     * Handles the scanning of Bracelet devices.
     */
    LeScanCallback handleBraceletScan = new LeScanCallback() {
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
                braceletBluetoothGatt = device.connectGatt(BraceletMainActivity.this, true, new BluetoothGattCallback() {

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
                            BraceletMainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //On the UI we shall enable the bars and the disconnect button
                                    connectBraceletButton.setEnabled(true);
                                    connectBraceletButton.setText(R.string.disconnectBracelet);

                                    //Only enable color sliders if Mantra is connected
                                    if (!mantraConnected) {
                                        redBar.setEnabled(true);
                                        greenBar.setEnabled(true);
                                        blueBar.setEnabled(true);
                                        brightnessBar.setEnabled(true);
                                    }
                                }
                            });
                        }
                    }
                });
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        //When the application stops we disconnect
        bluetoothAdapter.stopLeScan(handleMantraScan);
        bluetoothAdapter.stopLeScan(handleBraceletScan);
        if(braceletBluetoothGatt != null)
            braceletBluetoothGatt.disconnect();
        if(mantraBluetoothGatt != null)
            mantraBluetoothGatt.disconnect();
    }

}
