package com.hoho.android.usbserial.examples;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import androidx.annotation.ContentView;
import androidx.annotation.NonNull;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class myHandlerIdra extends Handler {

    private static final String EMERGENCY = "H2polito/Emergency";
    private static final String SPEED = "H2polito/Speed";
    private static final String TEMPERATURE = "H2polito/Temperature";
    private static final String FC_VOLTAGE = "H2polito/FCVoltage";
    private static final String SC_VOLTAGE = "H2polito/SCVoltage";
    private static final String STRATEGY = "H2polito/Strategy";
    private static final String MOTOR_ON = "H2polito/MotorOn";
    private static final String ACTUATION_ON = "H2polito/ActuationOn";
    private static final String PURGE = "H2polito/Purge";
    private static final String POWER_MODE = "H2polito/PowerMode";
    private static final String SHORT = "H2polito/Short";
    private static final String FC_CURRENT = "H2polito/FCCurrent";

    public static final String SERVERURI = "tcp://broker.hivemq.com:1883";
    public static final String CLIENTID = MqttClient.generateClientId();
    private static final String PASSWORD = "H2display";
    private static final String USERNAME = "DisplayIdra";


    myBoolean connected = new myBoolean(false);

    private MqttAndroidClient client;

    @SuppressLint("DefaultLocale")
    @Override
    public void handleMessage(@NonNull Message msg) {
        int id;
        byte[] data;
        Passer passer;
        if (!connected.isState()) {
            client = new MqttAndroidClient((Context) msg.obj, SERVERURI, CLIENTID);
            try {
                IMqttToken token = client.connect();
                token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    //we are connected
                    connected.setState(true);
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    //we are not connected
                }
            });
           } catch (Exception e) {
                e.printStackTrace();
           }
           return;
        }
        else{
            passer = (Passer) msg.obj;
            data = passer.data;
            id = byteToInt(data[0]);
        }
        switch (id) {
            case 16://service board: emergences
                boolean emActive = false;
                StringBuilder emString = new StringBuilder();
                boolean h2Emergency = false;
                for (int i = 1; i < 5; i++) {
                    if (data[i] != 0) {
                        switch (i) {
                            case 1:
                                emString.append("H2 ");
                                h2Emergency = true;
                                break;
                            case 2:
                                emString.append("Deadman ");
                                break;
                            case 3:
                                emString.append("External ");
                                break;
                            case 4:
                                emString.append("Internal");
                                break;
                        }
                        emActive = true;
                    }
                }
                if (emActive) {
                    String finalEm_string = emString.toString();
                    passer.handler.post(() -> passer.emergences.setText(finalEm_string));
                    if (h2Emergency)
                        passer.handler.post(() -> passer.emergences.setBackgroundColor(Color.parseColor("#0000FF")));
                    else
                        passer.handler.post(() -> passer.emergences.setBackgroundColor(Color.parseColor("#FF0000")));
                    publish(EMERGENCY, emString.toString());
                } else {
                    passer.handler.post(() -> passer.emergences.setText("E"));
                    passer.handler.post(() -> passer.emergences.setBackgroundColor(Color.TRANSPARENT));
                    publish(EMERGENCY, "none");
                }
                break;
            case 17://service board: speed
                float speed = byteToFloat(data[4], data[3], data[2], data[1]);
                passer.handler.post(() -> passer.speed.setText(String.format("%.2f Km/h", speed)));
                publish(SPEED, speed);
                break;
            case 18://service board: temperature
                float temperature = byteToFloat(data[4], data[3], data[2], data[1]);
                passer.handler.post(() -> passer.temperature.setText(String.format("%.2f C°", temperature)));
                publish(TEMPERATURE, temperature);
                break;
            case 19://service board: FCVoltage
                float FCVoltage = byteToFloat(data[4], data[3], data[2], data[1]);
                passer.handler.post(() -> passer.FCVoltage.setText(String.format("FC: %.2f V", FCVoltage)));
                publish(FC_VOLTAGE, FCVoltage);
                break;
            case 20://service board: SCVoltage
                float SCVoltage = byteToFloat(data[4], data[3], data[2], data[1]);
                passer.handler.post(() -> passer.SCVoltage.setText(String.format("SC: %.2f V", SCVoltage)));
                publish(SC_VOLTAGE, SCVoltage);
                break;
            case 32://wheel :
                int strategy = byteToInt(data[1]);
                int motorOn = 0;
                int purge = 0;
                int powerMode = 0;
                int _short = 0;

                passer.handler.post(() -> passer.strategy.setText(String.format("ST: %d", strategy)));
                if (data[2] != 0) {  //motor on
                    passer.handler.post(() -> passer.motorOn.setBackgroundColor(Color.parseColor("#FF0000")));
                    motorOn = 1;
                } else {            //motor off
                    passer.handler.post(() -> passer.motorOn.setBackgroundColor(Color.TRANSPARENT));
                }
                if (data[3] != 0) {  //purge on
                    passer.handler.post(() -> passer.purge.setBackgroundColor(Color.parseColor("#00FF00")));
                    purge = 1;
                } else {            //purge off
                    passer.handler.post(() -> passer.purge.setBackgroundColor(Color.TRANSPARENT));
                }
                if (data[4] != 0) { //powerMode on
                    passer.handler.post(() -> passer.SCVoltage.setBackgroundColor(Color.parseColor("#FF00FF")));
                    powerMode = 1;
                } else {            //powerMode off
                    passer.handler.post(() -> passer.SCVoltage.setBackgroundColor(Color.TRANSPARENT));
                }
                if (data[5] != 0) {  //short on
                    passer.handler.post(() -> passer._short.setBackgroundColor(Color.parseColor("#FFFF00")));
                    _short = 1;
                } else {            //short off
                    passer.handler.post(() -> passer._short.setBackgroundColor(Color.TRANSPARENT));
                }
                publish(STRATEGY, strategy);
                publish(MOTOR_ON, motorOn);
                publish(PURGE, purge);
                publish(POWER_MODE, powerMode);
                publish(SHORT, _short);
                break;
            case 48://actuation board: FCCurrent
                float FCCurrent = byteToFloat(data[4], data[3], data[2], data[1]);
                passer.handler.post(() -> passer.FCCurrent.setText(String.format("%.2f A", FCCurrent)));
                publish(FC_CURRENT, FCCurrent);
                break;
            case 63://actuation board heartbeat
                int actuationOn = 0;
                if (data[1] == 1) {
                    passer.handler.post(() -> passer.actuationOn.setBackgroundColor(Color.parseColor("#0000FF")));
                    actuationOn = 1;
                } else {
                    passer.handler.post(() -> passer.actuationOn.setBackgroundColor(Color.TRANSPARENT));
                }
                publish(ACTUATION_ON, actuationOn);
                break;
            default:
                break;
        }
    }

    private void publish(String topic, byte[] payload) {
        try {
            MqttMessage message = new MqttMessage(payload);
            message.setQos(0);
            message.setRetained(true);
            client.publish(topic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void publish(String topic, int payload) {
        byte[] data = String.valueOf(payload).getBytes(StandardCharsets.UTF_8);
        publish(topic, data);
    }

    private void publish(String topic, float payload) {
        byte[] data =String.valueOf(payload).getBytes(StandardCharsets.UTF_8);
        publish(topic, data);
    }

    private void publish(String topic, String payload) {
        byte[] data = payload.getBytes(StandardCharsets.UTF_8);
        publish(topic, data);
    }

    public int byteToInt(byte... data) {
        int val = 0;
        for (byte datum : data) {
            val = val << 8;
            val = val | (datum & 0xFF);
        }
        return val;
    }

    public float byteToFloat(byte... data) {
        return ByteBuffer.wrap(data).getFloat();
    }
}