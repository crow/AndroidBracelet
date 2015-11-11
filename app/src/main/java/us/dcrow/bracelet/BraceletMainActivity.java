package us.dcrow.bracelet;

import android.app.Activity;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;


/**
 *
 *
 */
public class BraceletMainActivity extends Activity implements BraceletService.BraceletServiceInterface {

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


    private static final int COLOR_SETTING_DISABLED = 0;

    private static final int COLOR_SETTING_BRIGHTNESS_ONLY = 1;

    private static final int COLOR_SETTING_ENABLED = 2;




    BraceletService braceletService;

    Intent serviceIntent;

    // Bracelet Service Interface
    @Override
    public void braceletConnectionStateChanged(int state) {
        switch (state) {
            case BluetoothProfile.STATE_DISCONNECTED:
                BraceletMainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mantraConnected) {
                            colorSettingEnabledState(COLOR_SETTING_BRIGHTNESS_ONLY);
                        } else {
                            colorSettingEnabledState(COLOR_SETTING_DISABLED);
                        }
                        connectBraceletButton.setText(R.string.connectBracelet);
                        connectBraceletButton.setEnabled(true);
                        braceletConnected = false;
                    }
                });
                break;
            case BluetoothProfile.STATE_CONNECTING:
                BraceletMainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mantraConnected) {
                            colorSettingEnabledState(COLOR_SETTING_BRIGHTNESS_ONLY);
                        } else {
                            colorSettingEnabledState(COLOR_SETTING_DISABLED);
                        }
                        connectBraceletButton.setText(R.string.connecting);
                        connectBraceletButton.setEnabled(false);
                        braceletConnected = false;
                    }
                });
                break;
            case BluetoothProfile.STATE_CONNECTED:
                BraceletMainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mantraConnected) {
                            colorSettingEnabledState(COLOR_SETTING_BRIGHTNESS_ONLY);
                        } else {
                            colorSettingEnabledState(COLOR_SETTING_ENABLED);
                        }
                        connectBraceletButton.setText(R.string.disconnectBracelet);
                        connectBraceletButton.setEnabled(true);
                        braceletConnected = true;
                    }
                });
                break;
            case BluetoothProfile.STATE_DISCONNECTING:
                BraceletMainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mantraConnected) {
                            colorSettingEnabledState(COLOR_SETTING_BRIGHTNESS_ONLY);
                        } else {
                            colorSettingEnabledState(COLOR_SETTING_DISABLED);
                        }
                        connectBraceletButton.setText(R.string.disconnecting);
                        connectBraceletButton.setEnabled(false);
                        braceletConnected = false;
                    }
                });
                break;
            default:
                break;

        }
    }

    private void colorSettingEnabledState (int state) {

        switch (state) {
            case COLOR_SETTING_ENABLED:
                redBar.setEnabled(true);
                greenBar.setEnabled(true);
                blueBar.setEnabled(true);
                brightnessBar.setEnabled(true);
                break;
            case COLOR_SETTING_BRIGHTNESS_ONLY:
                redBar.setEnabled(false);
                greenBar.setEnabled(false);
                blueBar.setEnabled(false);
                brightnessBar.setEnabled(true);
                break;
            case COLOR_SETTING_DISABLED:
                redBar.setEnabled(false);
                greenBar.setEnabled(false);
                blueBar.setEnabled(false);
                brightnessBar.setEnabled(false);
                break;
            default:
                break;
        }
    }

    @Override
    public void mantraConnectionStateChanged(int state) {
        switch (state) {
            case BluetoothProfile.STATE_DISCONNECTED:
                BraceletMainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connectMantraButton.setText(R.string.connectMantra);
                        connectMantraButton.setEnabled(true);
                        mantraConnected = false;
                        if (braceletConnected) {
                            colorSettingEnabledState(COLOR_SETTING_ENABLED);
                        } else {
                            colorSettingEnabledState(COLOR_SETTING_DISABLED);
                        }
                    }
                });
                break;
            case BluetoothProfile.STATE_CONNECTING:
                BraceletMainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connectMantraButton.setText(R.string.connecting);
                        connectMantraButton.setEnabled(false);
                        mantraConnected = false;
                        if (braceletConnected) {
                            colorSettingEnabledState(COLOR_SETTING_ENABLED);
                        } else {
                            colorSettingEnabledState(COLOR_SETTING_DISABLED);
                        }
                    }
                });
                break;
            case BluetoothProfile.STATE_CONNECTED:
                BraceletMainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connectMantraButton.setText(R.string.disconnectMantra);
                        connectMantraButton.setEnabled(true);
                        mantraConnected = true;
                        colorSettingEnabledState(COLOR_SETTING_BRIGHTNESS_ONLY);
                    }
                });

                break;
            case BluetoothProfile.STATE_DISCONNECTING:
                BraceletMainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connectMantraButton.setText(R.string.disconnecting);
                        connectMantraButton.setEnabled(false);
                        mantraConnected = false;
                        if (braceletConnected) {
                            colorSettingEnabledState(COLOR_SETTING_ENABLED);
                        } else {
                            colorSettingEnabledState(COLOR_SETTING_DISABLED);
                        }
                    }
                });
                break;
            default:
                break;

        }

    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Toast.makeText(BraceletMainActivity.this, "onServiceConnected called", Toast.LENGTH_SHORT).show();
            // We've binded to LocalService, cast the IBinder and get LocalService instance
            BraceletService.LocalBinder binder = (BraceletService.LocalBinder) service;
            braceletService = binder.getServiceInstance(); //Get instance of your service!
            braceletService.registerClient(BraceletMainActivity.this); //Activity register in the service as client for callabcks!
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Toast.makeText(BraceletMainActivity.this, "onServiceDisconnected called", Toast.LENGTH_SHORT).show();
        }
    };

    public BraceletMainActivity(){
        super();
        mantraConnected = false;
        braceletConnected = false;
        handler = new Handler();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_bracelet_main);

        // Start and bind to braceletService
        serviceIntent = new Intent(this, BraceletService.class);
        startService(serviceIntent); //Starting the service
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE); //Binding to the service!

        connectBraceletButton = (Button) findViewById(R.id.connectBraceletButton);
        connectMantraButton = (Button) findViewById(R.id.connectMantraButton);


        connectBraceletButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                braceletService.connectBracelet();
            }
        });

        connectMantraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                braceletService.connectMantra();
            }
        });

        redBar = (SeekBar) findViewById(R.id.redBar);
        redBar.setMax(255);//RGB are sent as 1 byte each, values from 0 to 255
        redBar.setOnSeekBarChangeListener(barchangeListener);

        greenBar = (SeekBar) findViewById(R.id.greenBar);
        greenBar.setMax(255);
        greenBar.setOnSeekBarChangeListener(barchangeListener);

        blueBar = (SeekBar) findViewById(R.id.blueBar);
        blueBar.setMax(255);
        blueBar.setOnSeekBarChangeListener(barchangeListener);

        brightnessBar = (SeekBar) findViewById(R.id.brightnessBar);
        brightnessBar.setMax(100);
        brightnessBar.setOnSeekBarChangeListener(barchangeListener);

        if (braceletConnected) {
            colorSettingEnabledState(COLOR_SETTING_ENABLED);
        } else if (mantraConnected) {
            colorSettingEnabledState(COLOR_SETTING_BRIGHTNESS_ONLY);
        } else {
            colorSettingEnabledState(COLOR_SETTING_DISABLED);
        }
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

            if (braceletConnected) {
                braceletService.updateColor(redBar.getProgress(), greenBar.getProgress(), blueBar.getProgress(), brightnessBar.getProgress());
            }
        }
    };

    //TODO make stop service button that makes this call: stopService(serviceIntent);

    // TODO move this to service onStop
    @Override
    protected void onStop() {
        super.onStop();

        unbindService(mConnection);
    }

}
