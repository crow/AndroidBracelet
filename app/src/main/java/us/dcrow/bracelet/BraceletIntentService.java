package us.dcrow.bracelet;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class BraceletIntentService extends IntentService {

    /**
     * The local bluetooth adapter, basic bluetooth interface
     */
    private BluetoothAdapter bluetoothAdapter;

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
    private static final long SCAN_TIMEOUT = 20000;

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
    private static final String TAG = "Android Bracelet";

    public BraceletIntentService() {


        super("BraceletIntentService");
    }

    // Connect mantra button listener should call this
    public void connectMantra {

        if (!mantraConnected) {
            //If not connected, let's look for Mantra and connect to it

            // TODO disable mantra button
            //connectMantraButton.setEnabled(false);

            bluetoothAdapter.startLeScan(handleMantraScan);

            //Program a to stop the scanning after some seconds
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!mantraConnected) {
                        //Cancel scan only if not connected
                        Log.d(TAG, "Mantra scanning timed out, stopping the scan");
                        bluetoothAdapter.stopLeScan(handleMantraScan);
                        //All graphical things must be run on the UI thread:
                        BraceletMainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // TODO enable mantra button
                                //connectMantraButton.setEnabled(true);
                                mantraConnected = false;
                                // TODO update mantra button text
                                //connectMantraButton.setText(R.string.connectMantra);
                            }
                        });
                    }
                }
            }, SCAN_TIMEOUT);

        } else {
            //If connected, the same button is used for disconnecting
            mantraConnected = false;
            // TODO update mantra button text
            //connectMantraButton.setText(R.string.connectMantra);

            // TODO disable all bars
            //redBar.setEnabled(false);
            //greenBar.setEnabled(false);
            //blueBar.setEnabled(false);
            //brightnessBar.setEnabled(false);


            if (mantraBluetoothGatt != null)
                mantraBluetoothGatt.disconnect();
        }
    }

    public void connectBracelet {

        if(!braceletConnected){
            //If not connected, let's look for RFduino and connect to it
            // TODO disable connectBracelet button
            //connectBraceletButton.setEnabled(false);
            bluetoothAdapter.startLeScan(handleBraceletScan);

            //Program a to stop the scanning after some seconds
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(!braceletConnected){
                        //Cancel scan only if not connected
                        Log.d(TAG, "Bracelet scanning timed out, stopping the scan");
                        bluetoothAdapter.stopLeScan(handleBraceletScan);
                        //All graphical things must be run on the UI thread:
                        BraceletMainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // TODO enable connectBraceletButton
                                //connectBraceletButton.setEnabled(true);
                                braceletConnected = false;
                                // TODO set connectBraceletButtonText
                                //connectBraceletButton.setText(R.string.connectBracelet);
                            }
                        });
                    }
                }
            }, SCAN_TIMEOUT);

        } else {
            //If connected, the same button is used for disconnecting
            braceletConnected = false;
            // TODO set connectBraceletButtonText
            //connectBraceletButton.setText(R.string.connectBracelet);

            // TODO disable all bars
            //redBar.setEnabled(false);
            //greenBar.setEnabled(false);
            //blueBar.setEnabled(false);
            //brightnessBar.setEnabled(false);

            if(braceletBluetoothGatt != null)
                braceletBluetoothGatt.disconnect();
        }

    }


    @Override
    public void onCreate() {

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        // Initialize 400 sample array
        sampleWindow = new ArrayList<>(400);

        // Initialize 15 sample filter window
        filterWindow = new ArrayList<>(15);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Bracelet service starting", Toast.LENGTH_SHORT).show();
        return super.onStartCommand(intent,flags,startId);
    }

    /**
     * The IntentService calls this method from the default worker thread with
     * the intent that started the service. When this method returns, IntentService
     * stops the service, as appropriate.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        // Bracelet BLE work goes here
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }
}
