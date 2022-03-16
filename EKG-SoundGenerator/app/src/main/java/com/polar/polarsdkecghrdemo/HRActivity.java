package com.polar.polarsdkecghrdemo;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.androidplot.xy.XYPlot;
import com.polar.sdk.api.PolarBleApi;
import com.polar.sdk.api.PolarBleApiCallback;
import com.polar.sdk.api.PolarBleApiDefaultImpl;
import com.polar.sdk.api.errors.PolarInvalidArgument;
import com.polar.sdk.api.model.PolarDeviceInfo;
import com.polar.sdk.api.model.PolarHrData;

import java.util.List;
import java.util.Set;
import java.util.UUID;




public class HRActivity extends AppCompatActivity implements PlotterListener {
    private static final String TAG = "HRActivity";
    private XYPlot plot;
    private TimePlotter plotter;
    private TextView textViewHR;
    private TextView textViewFW;
    private ImageView heartView;
    private PolarBleApi api;
    private UIUpdater mUIUpdater;
    private UIUpdater heartUpdater;

    private MediaPlayer mp;
    private int HR=0;
    private int currentResID=1;
    private boolean musicIsPlaying = false;
    private boolean performanceCanBegin = false;
    private boolean performanceShouldEnd = false;
    private boolean drumsHavePlayed = false;

    private AudioManager mAudioManager;
    private int originalVolume;



    private final int widthPixels = Resources.getSystem().getDisplayMetrics().widthPixels;
    private final int heightPixels = Resources.getSystem().getDisplayMetrics().heightPixels;




    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hr);
        String deviceId = getIntent().getStringExtra("id");
        textViewHR = findViewById(R.id.info2);
        textViewFW = findViewById(R.id.fw2);
        heartView = findViewById(R.id.heart);

        ObjectAnimator scaleDown = ObjectAnimator.ofPropertyValuesHolder(heartView,
                PropertyValuesHolder.ofFloat("scaleX", 1.1f),
                PropertyValuesHolder.ofFloat("scaleY", 1.1f));
        scaleDown.setDuration(300);

        scaleDown.setRepeatCount(ObjectAnimator.INFINITE);
        scaleDown.setRepeatMode(ObjectAnimator.REVERSE);
        scaleDown.start();

        mp=new MediaPlayer();
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);

        originalVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        //checks every 5 seconds for music changes
        mUIUpdater = new UIUpdater(new Runnable() {
            @Override
            public void run() {
                System.out.println("Handler "+HR + " " + getAudio());
                //end and begin of
                if(HR>110 && !performanceShouldEnd){
                    musicIsPlaying=true;
                    updateMusic();
                }
                //if looped music (the performance) has already started but needs to end now
                if(drumsHavePlayed){
                    performanceShouldEnd=true;
                    if(HR<118){
                        musicIsPlaying=false;
                        mp.stop();
                        mp.release();
                    }
                    else updateMusic();
                }

            }
        }, 5000);
        //update heart images after every 2 seconds
        heartUpdater = new UIUpdater(new Runnable() {
            @Override
            public void run() {
                if(performanceShouldEnd){
                    updateHeartForEnd();
                }
                   else  updateHeart();
            }
        }, 2000);

        // Start updates
        heartUpdater.startUpdates();

        Button playMusicButton = findViewById(R.id.playmusic);
        playMusicButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                System.out.println("Button clicked");
                performanceCanBegin=true;
                mUIUpdater.startUpdates();

            }
        });


        api = PolarBleApiDefaultImpl.defaultImplementation(getApplicationContext(),
                PolarBleApi.FEATURE_BATTERY_INFO |
                        PolarBleApi.FEATURE_DEVICE_INFO |
                        PolarBleApi.FEATURE_HR);

        api.setApiLogger(str -> Log.d("SDK", str));


        api.setApiCallback(new PolarBleApiCallback() {
            @Override
            public void blePowerStateChanged(boolean b) {
                Log.d(TAG, "BluetoothStateChanged " + b);
            }

            @Override
            public void deviceConnected(@NonNull PolarDeviceInfo s) {
                Log.d(TAG, "Device connected " + s.deviceId +"adress: "+ s.address);
                Toast.makeText(getApplicationContext(), R.string.connected, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void deviceConnecting(@NonNull PolarDeviceInfo polarDeviceInfo) {

            }

            @Override
            public void deviceDisconnected(@NonNull PolarDeviceInfo s) {
                Log.d(TAG, "Device disconnected " + s);

            }

            @Override
            public void streamingFeaturesReady(@NonNull final String identifier,
                                               @NonNull final Set<PolarBleApi.DeviceStreamingFeature> features) {

                for (PolarBleApi.DeviceStreamingFeature feature : features) {
                    Log.d(TAG, "Streaming feature is ready: " + feature);
                    switch (feature) {
                        case ECG:
                        case ACC:
                        case MAGNETOMETER:
                        case GYRO:
                        case PPI:
                        case PPG:
                            break;
                    }
                }
            }

            @Override
            public void hrFeatureReady(@NonNull String s) {
                Log.d(TAG, "HR Feature ready " + s);
            }

            @Override
            public void disInformationReceived(@NonNull String s, @NonNull UUID u, @NonNull String s1) {
                if (u.equals(UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb"))) {
                    String msg = "Firmware: " + s1.trim();
                    Log.d(TAG, "Firmware: " + s + " " + s1.trim());
                    //textViewFW.append(msg + "\n");
                }
            }

            @Override
            public void batteryLevelReceived(@NonNull String s, int i) {
                String msg = "ID: " + s + "\nBattery level: " + i;
                Log.d(TAG, "Battery level " + s + " " + i);
//                Toast.makeText(classContext, msg, Toast.LENGTH_LONG).show();
               // textViewFW.append(msg + "\n");
            }

            @Override
            public void hrNotificationReceived(@NonNull String s,
                                               @NonNull PolarHrData polarHrData) {
                Log.d(TAG, "HR " + polarHrData.hr);
                List<Integer> rrsMs = polarHrData.rrsMs;
                StringBuilder msg = new StringBuilder(polarHrData.hr + "\n");
                for (int i : rrsMs) {
                    msg.append(i).append(",");
                }
                if (msg.toString().endsWith(",")) {
                    msg = new StringBuilder(msg.substring(0, msg.length() - 1));
                }

                HR=polarHrData.hr;
                //String st = msg.toString();
                if(!textViewHR.getText().equals("Good night!")){
                textViewHR.setText(Integer.toString(HR));}


            }

            @Override
            public void polarFtpFeatureReady(@NonNull String s) {
                Log.d(TAG, "Polar FTP ready " + s);
            }
        });
        try {
            api.connectToDevice(deviceId);
        } catch (PolarInvalidArgument a) {
            a.printStackTrace();
        }


    }


    private void updateHeartForEnd(){
        ViewGroup.LayoutParams layoutParams = heartView.getLayoutParams();
        if(HR>=115){
            heartView.setVisibility(View.VISIBLE);
            textViewHR.setTextColor(Color.WHITE);
            layoutParams.width = (int)(((HR-80)*(widthPixels*1.5-60))/100)+60;
            layoutParams.height = (int)(((HR-80)*(heightPixels*1.5-60))/100)+60;
            heartView.setLayoutParams(layoutParams);
        }else{
            heartView.setVisibility(View.INVISIBLE);
            textViewHR.setTextColor(Color.parseColor("#C00000"));
            if(performanceShouldEnd){
                textViewHR.setText("Good night!");
                ObjectAnimator scaleDown = ObjectAnimator.ofPropertyValuesHolder(textViewHR,
                        PropertyValuesHolder.ofFloat("scaleX", 1.2f),
                        PropertyValuesHolder.ofFloat("scaleY", 1.2f));
                scaleDown.setDuration(300);

                scaleDown.setRepeatCount(ObjectAnimator.INFINITE);
                scaleDown.setRepeatMode(ObjectAnimator.REVERSE);
                scaleDown.start();

                mUIUpdater.stopUpdates();
                heartUpdater.stopUpdates();
            }

            //changing width and height programmatically will require the view to be redrawn
            textViewHR.requestLayout();
        }
        //changing width and height programmatically will require the view to be redrawn
        heartView.requestLayout();

    }
    private void updateHeart() {
        ViewGroup.LayoutParams layoutParams = heartView.getLayoutParams();
        if(HR>=90){
            heartView.setVisibility(View.VISIBLE);
            textViewHR.setTextColor(Color.WHITE);
            layoutParams.width = (int)(((HR-80)*(widthPixels*1.5-60))/95)+60;
            layoutParams.height = (int)(((HR-80)*(heightPixels*1.5-60))/95)+60;
            heartView.setLayoutParams(layoutParams);
        }
        else{
            heartView.setVisibility(View.INVISIBLE);
            textViewHR.setTextColor(Color.parseColor("#C00000"));
        }
        //changing width and height programmatically will require the view to be redrawn
        heartView.requestLayout();
    }
    private void updateMusicForEnd() {


    }

    private void updateMusic() {
        if(musicIsPlaying) {
            if (currentResID != getAudio()) {
                currentResID = getAudio();
                //stop the music
                mp.setLooping(false);
                mp.stop();
                mp.release();


                try {
                    mp = MediaPlayer.create(getApplicationContext(), currentResID);
                    mp.setLooping(true);
                    mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mp.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }



    private int getAudio(){
        //towards the end of the performance
        if(performanceShouldEnd){
            if(this.HR>186) return R.raw.darbuka176bpm;
            if(this.HR>177) return R.raw.tempo_300;
            if(this.HR>167) return R.raw.tempo_230;
            if(this.HR>159) return R.raw.tempo_180;
            if(this.HR>151) return R.raw.tempo_120;
            if(this.HR>146) return R.raw.tempo_100;
            if(this.HR>139) return R.raw.tempo_90;
            if(this.HR>129) return R.raw.tempo_70;
            else return R.raw.tempo_50;
        }

        //before the drums
        if(this.HR>182) {
            drumsHavePlayed=true;
            return R.raw.darbuka176bpm;
        }
        if(this.HR>176) return R.raw.tempo_300;
        if(this.HR>168) return R.raw.tempo_230;
        if(this.HR>154) return R.raw.tempo_180;
        if(this.HR>148) return R.raw.tempo_120;
        if(this.HR>138) return R.raw.tempo_100;
        if(this.HR>128) return R.raw.tempo_90;
        if(this.HR>118) return R.raw.tempo_70;
        else return R.raw.tempo_50;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        api.shutDown();
        // Stop updates
        musicIsPlaying =false;
        mUIUpdater.stopUpdates();
        heartUpdater.stopUpdates();
        mp.setVolume(0,0);
        mp.stop();
    }

    public void update() {
        runOnUiThread(() -> System.out.println("update"));
    }
}
