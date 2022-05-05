package com.hoho.android.usbserial.examples;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Date;

public class TerminalFragment extends Fragment implements SerialInputOutputManager.Listener {

    private static final int WRITE_WAIT_MILLIS = 2000;

    private enum UsbPermission { Unknown, Requested, Granted, Denied }

    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";

    private int deviceId, portNum, baudRate;
    private boolean withIoManager;

    private final BroadcastReceiver broadcastReceiver;
    private final Handler mainLooper;
    private TextView purge;
    private TextView _short;
    private TextView emergences;
    private TextView motorOn;
    private TextView actuationOn;
    private TextView temperature;
    private TextView strategy;
    private TextView FCVoltage;
    private TextView FCCurrent;
    private TextView SCVoltage;
    private TextView speed;

    int id=10;

    private SerialInputOutputManager usbIoManager;
    private UsbSerialPort usbSerialPort;
    private UsbPermission usbPermission = UsbPermission.Unknown;
    private boolean connected = false;

    public TerminalFragment() {
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
            status("disconnected");
            disconnect();
        }
        getActivity().unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    public void onStop() {
        super.onStop();
        ((AppCompatActivity)getActivity()).getSupportActionBar().show();
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout, container, false);

        purge = view.findViewById(R.id.Purge);
        _short = view.findViewById(R.id.Short);
        emergences = view.findViewById(R.id.Emergences);
        motorOn = view.findViewById(R.id.MotorOn);
        actuationOn = view.findViewById(R.id.ActuationOn);
        temperature = view.findViewById(R.id.Temperature);
        strategy = view.findViewById(R.id.Strategy);
        FCVoltage = view.findViewById(R.id.FCVoltage);
        FCCurrent = view.findViewById(R.id.CurrentFC);
        SCVoltage = view.findViewById(R.id.VoltageSC);
        speed = view.findViewById(R.id.Speed);
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
        mainLooper.post(() -> {
            status("connection lost: " + e.getMessage());
            disconnect();
        });
    }

    private void connect() {
        UsbDevice device = null;
        UsbManager usbManager = (UsbManager) getActivity().getSystemService(Context.USB_SERVICE);
        for(UsbDevice v : usbManager.getDeviceList().values())
            if(v.getDeviceId() == deviceId)
                device = v;
        if(device == null) {
            status("connection failed: device not found");
            return;
        }
        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        if(driver == null) {
            driver = CustomProber.getCustomProber().probeDevice(device);
        }
        if(driver == null) {
            status("connection failed: no driver for device");
            return;
        }
        if(driver.getPorts().size() < portNum) {
            status("connection failed: not enough ports at device");
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
        if(usbConnection == null) {
            if (!usbManager.hasPermission(driver.getDevice()))
                status("connection failed: permission denied");
            else
                status("connection failed: open failed");
            return;
        }

        try {
            usbSerialPort.open(usbConnection);
            usbSerialPort.setParameters(baudRate, 8, 1, UsbSerialPort.PARITY_NONE);
            if(withIoManager) {
                usbIoManager = new SerialInputOutputManager(usbSerialPort, this);
                usbIoManager.start();
            }
            status("connected");
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
        if(data.length>0){
           id = byteToInt(data[0]);
        switch(id){
            case 32://wheel :
                int strategy=byteToInt(data[1]);
                this.strategy.setText(String.format("%d",strategy));
                if(data[2]!=0)  //motor on
                    this.motorOn.setBackgroundColor(Color.parseColor("#FF0000"));
                else            //motor off
                    this.motorOn.setBackgroundColor(Color.TRANSPARENT);
                if(data[3]!=0)  //purge on
                    this.purge.setBackgroundColor(Color.parseColor("#00FF00"));
                else            //purge off
                    this.purge.setBackgroundColor(Color.TRANSPARENT);
                if(data[4]!=0)  //powermode on
                    this.SCVoltage.setBackgroundColor(Color.parseColor("#00FF00"));
                else            //powermode off
                    this.SCVoltage.setBackgroundColor(Color.TRANSPARENT);
                if(data[5]!=0)  //short on
                    this._short.setBackgroundColor(Color.parseColor("#FFFF00"));
                else            //short off
                    this._short.setBackgroundColor(Color.TRANSPARENT);
                break;
            case 16://service board: emergences
                for(int i=1;i<5;i++)
                    if(data[i]!=0)
                        emergences.setBackgroundColor(Color.parseColor("#FF0000"));
                break;
            case 17://service board: speed
                float speed=byteToFloat(data[4],data[3],data[2],data[1]);
                this.speed.setText(String.format("%f",speed));
                break;
            case 18://service board: temperature
                float temperature=byteToFloat(data[4],data[3],data[2],data[1]);
                this.temperature.setText(String.format("%f",temperature));
                break;
            case 19://service board: FCVoltage
                float FCVoltage=byteToFloat(data[4],data[3],data[2],data[1]);
                this.FCVoltage.setText(String.format("%f",FCVoltage));
                break;
            case 20://service board: SCVoltage
                float SCVoltage=byteToFloat(data[4],data[3],data[2],data[1]);
                this.SCVoltage.setText(String.format("%f",SCVoltage));
                break;
            case 48://actuation board: FCCurrent
                float FCCurrent=byteToFloat(data[4],data[3],data[2],data[1]);
                this.FCCurrent.setText(String.format("%f",FCCurrent));
                break;
            case 49://actuation board: Motor Duty
                break;
            case 50://actuation board: Fan Duty
                break;
            default:
                break;
        }
        }
    }


    public int byteToInt(byte... data) {
        int val = 0;
        int length = data.length;
        for (int i = 0; i < length; i++) {
            val=val<<8;
            val=val|(data[i] & 0xFF);
        }
        return val;
    }
    public float byteToFloat(byte... data) {
        return ByteBuffer.wrap(data).getFloat();
    }

    void status(String str) {

    }
}
