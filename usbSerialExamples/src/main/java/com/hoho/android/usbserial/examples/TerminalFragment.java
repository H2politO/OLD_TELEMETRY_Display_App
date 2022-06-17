package com.hoho.android.usbserial.examples;

import static com.hoho.android.usbserial.examples.DevicesFragment.IDRA;
import static com.hoho.android.usbserial.examples.DevicesFragment.JUNO;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import org.eclipse.paho.android.service.BuildConfig;
import org.eclipse.paho.android.service.MqttAndroidClient;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public class TerminalFragment extends Fragment implements SerialInputOutputManager.Listener {

    private static final int WRITE_WAIT_MILLIS = 2000;
    public static final int THREAD_NUMBER= 18;

    private int car;


    private enum UsbPermission { Unknown, Requested, Granted, Denied }

    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";

    private int deviceId, portNum, baudRate;
    private boolean withIoManager;

    private final BroadcastReceiver broadcastReceiver;
    private final Handler mainLooper;


    Passer[] passers= new Passer[THREAD_NUMBER];

    JunoThread[] junoThreads= new JunoThread[THREAD_NUMBER];
    IdraThread[] idraThreads= new IdraThread[THREAD_NUMBER];

    int threadCounter=0;
    Handler handler=new Handler(Looper.getMainLooper());

    private MqttAndroidClient MQTTClient;

    private SerialInputOutputManager usbIoManager;
    private UsbSerialPort usbSerialPort;
    private UsbPermission usbPermission = UsbPermission.Unknown;
    private boolean connected = false;

    public TerminalFragment(int car) {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(INTENT_ACTION_GRANT_USB.equals(intent.getAction())) {
                    usbPermission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                            ? UsbPermission.Granted : UsbPermission.Denied;
                    connect();
                }
            }
        };
        this.car=car;
        mainLooper = new Handler(Looper.getMainLooper());
    }

    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        assert getArguments() != null;
        deviceId = getArguments().getInt("device");
        portNum = getArguments().getInt("port");
        baudRate = getArguments().getInt("baud");
        withIoManager = getArguments().getBoolean("withIoManager");
        for(int i=0;i<THREAD_NUMBER;i++) {
            Message msg = Message.obtain();
            msg.what=0;
            msg.obj=getContext().getApplicationContext();
            if(car==JUNO) {
                junoThreads[i] = new JunoThread();
                junoThreads[i].start();
            }else {
                idraThreads[i] = new IdraThread();
                idraThreads[i].start();
                idraThreads[i].handler.sendMessage(msg);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(INTENT_ACTION_GRANT_USB));
        ((AppCompatActivity)getActivity()).getSupportActionBar().hide();
        if(usbPermission == UsbPermission.Unknown || usbPermission == UsbPermission.Granted)
            mainLooper.post(this::connect);
        Date currentTime = Calendar.getInstance().getTime();
        send(currentTime.toString());
    }

    @Override
    public void onPause() {
        if(!connected) {
            Toast.makeText(getActivity(), "not able to connect", Toast.LENGTH_SHORT).show();
            disconnect();
        }
        getActivity().unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    public void onStop() {
        super.onStop();
        ((AppCompatActivity)getActivity()).getSupportActionBar().show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for(int i=0;i<THREAD_NUMBER;i++) {
            try {
                if(car==IDRA) {
                    idraThreads[i].looper.quit();
                    idraThreads[i].join();
                }
                else{
                    junoThreads[i].looper.quit();
                    junoThreads[i].join();
                }
            } catch (InterruptedException ignored){}
        }
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view ;

        if(car==IDRA) {
            view = inflater.inflate(R.layout.layout_idra, container, false);

            TextView purge;
            TextView _short;
            TextView emergences;
            TextView motorOn;
            TextView actuationOn;
            TextView temperature;
            TextView strategy;
            TextView FCVoltage;
            TextView FCCurrent;
            TextView SCVoltage;
            TextView powerModeBackground;
            TextView speed;

            purge = view.findViewById(R.id.Purge);
            _short = view.findViewById(R.id.Short);
            emergences = view.findViewById(R.id.Emergences);
            motorOn = view.findViewById(R.id.MotorOn);
            actuationOn = view.findViewById(R.id.ActuationOn);
            temperature = view.findViewById(R.id.Temperature);
            strategy = view.findViewById(R.id.Strategy);
            FCVoltage = view.findViewById(R.id.FCVoltage);
            FCCurrent = view.findViewById(R.id.FCCurrent);
            SCVoltage = view.findViewById(R.id.VoltageSC);
            speed = view.findViewById(R.id.Speed);
            powerModeBackground= view.findViewById(R.id.PowerModeBackground);

            for (int i = 0; i < THREAD_NUMBER; i++) {
                passers[i] = new Passer(
                        purge,
                        _short,
                        emergences,
                        motorOn,
                        actuationOn,
                        temperature,
                        strategy,
                        FCVoltage,
                        FCCurrent,
                        SCVoltage,
                        speed,
                        powerModeBackground,
                        handler
                );
            }
        }
        else{
            view = inflater.inflate(R.layout.layout_juno, container, false);

            TextView TPS = view.findViewById(R.id.TPS);
            TextView OilTemp = view.findViewById(R.id.OilTemp);
            TextView Lambda = view.findViewById(R.id.Lambda);
            TextView lambdaBackground= view.findViewById(R.id.LambdaBackground);
            TextView Speed = view.findViewById(R.id.Speed);
            TextView RPM = view.findViewById(R.id.RPM);
            TextView RPMBackground= view.findViewById(R.id.RPMBackground);
            TextView SOS = view.findViewById(R.id.SOS);
            TextView lowBeam = view.findViewById(R.id.LowBeam);
            TextView highBeam = view.findViewById(R.id.HighBeam);
            TextView syncState = view.findViewById(R.id.syncState);
            TextView timer = view.findViewById(R.id.Timer);
            TextView rightArrow = view.findViewById(R.id.RightArrow);
            TextView leftArrow = view.findViewById(R.id.LeftArrow);
            TextView batteryVoltage= view.findViewById(R.id.BatteryVoltage);
            TextView engineEnable = view.findViewById(R.id.engineEnable);
            TextView limpMode = view.findViewById(R.id.limpMode);

            for(int i=0;i<THREAD_NUMBER;i++) {
                passers[i] = new Passer(
                        TPS,
                        OilTemp,
                        Lambda,
                        lambdaBackground,
                        Speed,
                        RPM,
                        RPMBackground,
                        SOS,
                        lowBeam,
                        highBeam,
                        syncState,
                        timer,
                        rightArrow,
                        leftArrow,
                        batteryVoltage,
                        engineEnable,
                        limpMode,
                        handler
                );
            }
        }
        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear) {
            return true;
        } else if( id == R.id.send_break) {
            if(!connected) {
                Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    usbSerialPort.setBreak(true);
                    Thread.sleep(100); // should show progress bar instead of blocking UI thread
                    usbSerialPort.setBreak(false);
                } catch(UnsupportedOperationException ignored) {
                    Toast.makeText(getActivity(), "BREAK not supported", Toast.LENGTH_SHORT).show();
                } catch(Exception e) {
                    Toast.makeText(getActivity(), "BREAK failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onNewData(byte[] data) {
        mainLooper.post(() -> receive(data));
    }

    @Override
    public void onRunError(Exception e) {
        mainLooper.post(this::disconnect);
    }

    private void connect() {
        UsbDevice device = null;
        UsbManager usbManager = (UsbManager) getActivity().getSystemService(Context.USB_SERVICE);
        for(UsbDevice v : usbManager.getDeviceList().values())
            if(v.getDeviceId() == deviceId)
                device = v;
        if(device == null) {
            return;
        }
        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        if(driver == null) {
            driver = CustomProber.getCustomProber().probeDevice(device);
        }
        if(driver == null) {
            return;
        }
        if(driver.getPorts().size() < portNum) {
            return;
        }
        usbSerialPort = driver.getPorts().get(portNum);
        UsbDeviceConnection usbConnection = usbManager.openDevice(driver.getDevice());
        if(usbConnection == null && usbPermission == UsbPermission.Unknown && !usbManager.hasPermission(driver.getDevice())) {
            usbPermission = UsbPermission.Requested;
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(getActivity(), 0, new Intent(INTENT_ACTION_GRANT_USB), 0);
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
            return;
        }
        if(usbConnection == null)
            return;
        try {
            usbSerialPort.open(usbConnection);
            usbSerialPort.setParameters(baudRate, 8, 1, UsbSerialPort.PARITY_NONE);
            if(withIoManager) {
                usbIoManager = new SerialInputOutputManager(usbSerialPort, this);
                usbIoManager.start();
            }
            connected = true;
        } catch (Exception e) {
            Toast.makeText(getActivity(), "not able to connect", Toast.LENGTH_SHORT).show();
            disconnect();
        }
    }

    private void disconnect() {
        connected = false;
        if(usbIoManager != null) {
            usbIoManager.setListener(null);
            usbIoManager.stop();
        }
        usbIoManager = null;
        try {
            usbSerialPort.close();
        } catch (IOException ignored) {}
        usbSerialPort = null;
    }

    private void send(String str) {
        if(!connected) {
            return;
        }
        try {
            byte[] data =  str.getBytes();
            usbSerialPort.write(data, WRITE_WAIT_MILLIS);
        } catch (Exception e) {
            onRunError(e);
        }
    }

    //spurghi->verde #00FF00
    //corti->giallo #FFFF00
    //motorOn->rosso #FF0000
    //supercap->viola #FF00FF
    //Attuazione->blu #0000FF
    @SuppressLint("DefaultLocale")
    private void receive(byte[] data) {
        if (/*connected &&*/ data.length>0 ) {
            Message msg = Message.obtain();
            passers[threadCounter].setData(data);
            msg.obj = passers[threadCounter];
            msg.what=1;
            if(car==IDRA)
                idraThreads[threadCounter].handler.sendMessage(msg);
            else
                junoThreads[threadCounter].handler.sendMessage(msg);
            threadCounter = (threadCounter + 1) % THREAD_NUMBER;
        }
    }
}
