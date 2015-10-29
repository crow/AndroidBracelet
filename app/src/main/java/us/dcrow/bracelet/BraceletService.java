package us.dcrow.bracelet;

import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

import com.example.android.apis.app.RemoteService.Controller;

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
    private static final long SCAN_TIMEOUT = 20000;

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

    public static final String PARAM_IN_MSG = "imsg";
    public static final String PARAM_OUT_MSG = "omsg";

    public static final String STOP_BRACELET_SERVICE = "stop_bracelet_service";


    public static final String CONNECT_BRACELET = "connect_bracelet";
    public static final String CONNECT_MANTRA = "connect_mantra";

    public static final int CONNECTION_STATE_DISCONNECTED = 0;
    public static final int CONNECTION_STATE_CONNECTING = 1;
    public static final int CONNECTION_STATE_CONNECTED = 2;


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

    public BraceletService(Context context) {
        super("BraceletService");
        handler = new Handler();
    }

    public class LocalBinder extends Binder {
        public BraceletService getServiceInstance(){
            return BraceletService.this;
        }
    }

    public void updateBraceletColor(int r, int g, int b){
        // update color
    }

    // Here Bracelet Main Activity register to the service as Callbacks client
    public void registerClient(Activity activity){
        this.activity = (BraceletServiceInterface)activity;
    }

    // Interface for Bracelet Activity
    public interface BraceletServiceInterface {
        public void braceletConnectionStateChanged(int state);

        public void mantraConnectionStateChanged(int state);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // Display a notification about starting.  We put an icon in the status bar.
        showNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        lastStartId = startId;

        return START_STICKY;
    }
    @Override
    public int onSto

    public void connectBracelet () {

        if (!braceletConnected) {
            //If not connected, let's look for RFduino and connect to it
            // TODO disable connectBracelet button
            //connectBraceletButton.setEnabled(false);
            bluetoothAdapter.startLeScan(handleBraceletScan);

            //Program a to stop the scanning after some seconds
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!braceletConnected) {
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
                                activity.braceletConnectionStateChanged(false);
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

            // TODO send braceletConnected interface callback

            // TODO set connectBraceletButtonText
            //connectBraceletButton.setText(R.string.connectBracelet);

            // TODO disable all bars
            //redBar.setEnabled(false);
            //greenBar.setEnabled(false);
            //blueBar.setEnabled(false);
            //brightnessBar.setEnabled(false);

            if (braceletBluetoothGatt != null)
                braceletBluetoothGatt.disconnect();
        }

    }

   // TODO onStop or disconnect
//    //When the application stops we disconnect
//    bluetoothAdapter.stopLeScan(handleMantraScan);
//    bluetoothAdapter.stopLeScan(handleBraceletScan);
//    if(braceletBluetoothGatt != null)
//            braceletBluetoothGatt.disconnect();
//    if(mantraBluetoothGatt != null)
//            mantraBluetoothGatt.disconnect();

    public void connectMantra () {

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
}
