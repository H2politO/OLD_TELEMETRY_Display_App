package com.hoho.android.usbserial.examples;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class myHandler extends Handler {

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @SuppressLint("DefaultLocale")
    @Override
    public void handleMessage(@NonNull Message msg) {
        Passer passer= (Passer) msg.obj;
        byte[] data= passer.data;
        int id = byteToInt(data[0]);
        MqttMessage MQTTmessage=new MqttMessage();
        MQTTmessage.setRetained(true);
        MQTTmessage.setQos(0);
        switch(id){
            case 16://service board: emergences
                boolean emActive=false;
                String emString="";
                boolean h2Emergency=false;
                for(int i=1;i<5;i++) {
                    if (data[i] != 0) {
                        switch (i) {
                            case 1:
                                emString += "H2 ";
                                h2Emergency = true;
                                break;
                            case 2:
                                emString += "Deadman ";
                                break;
                            case 3:
                                emString += "External ";
                                break;
                            case 4:
                                emString += "Internal";
                                break;
                        }
                        emActive = true;
                    }
                }
                if(emActive) {
                    String finalEm_string = emString;
                    passer.handler.post(() -> passer.emergences.setText(finalEm_string));
                    if(h2Emergency)
                        passer.handler.post(() -> passer.emergences.setBackgroundColor(Color.parseColor("#0000FF")));
                    else
                        passer.handler.post(() -> passer.emergences.setBackgroundColor(Color.parseColor("#FF0000")));
                    try {
                        MQTTmessage.setPayload(emString.getBytes(StandardCharsets.UTF_8));
                        passer.MQTTClient.publish("Emergency",MQTTmessage);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    passer.handler.post(() -> passer.emergences.setText("E"));
                    passer.handler.post(() -> passer.emergences.setBackgroundColor(Color.TRANSPARENT));
                }
                    break;
            case 17://service board: speed
                float speed=byteToFloat(data[4],data[3],data[2],data[1]);
                passer.handler.post(() -> passer.speed.setText(String.format("%.2f Km/h",speed)));
                try {
                    MQTTmessage.setPayload(String.valueOf(speed).getBytes(StandardCharsets.UTF_8));
                    passer.MQTTClient.publish("Speed",MQTTmessage);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                break;
            case 18://service board: temperature
                float temperature=byteToFloat(data[4],data[3],data[2],data[1]);
                passer.handler.post(() -> passer.temperature.setText(String.format("%.2f C°",temperature)));
                try {
                    MQTTmessage.setPayload(String.valueOf(temperature).getBytes(StandardCharsets.UTF_8));
                    passer.MQTTClient.publish("Temperature",MQTTmessage);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                break;
            case 19://service board: FCVoltage
                float FCVoltage=byteToFloat(data[4],data[3],data[2],data[1]);
                passer.handler.post(() -> passer.FCVoltage.setText(String.format("%.2f V",FCVoltage)));
                try {
                    MQTTmessage.setPayload(String.valueOf(FCVoltage).getBytes(StandardCharsets.UTF_8));
                    passer.MQTTClient.publish("FCVoltage",MQTTmessage);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                break;
            case 20://service board: SCVoltage
                float SCVoltage=byteToFloat(data[4],data[3],data[2],data[1]);
                passer.handler.post(() -> passer.SCVoltage.setText(String.format("%.2f V",SCVoltage)));
                try {
                    MQTTmessage.setPayload(String.valueOf(SCVoltage).getBytes(StandardCharsets.UTF_8));
                    passer.MQTTClient.publish("SCVoltage",MQTTmessage);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                break;
            case 32://wheel :
                int strategy=byteToInt(data[1]);
                passer.handler.post(() -> passer.strategy.setText(String.format("ST: %d",strategy)));
                try {
                    MQTTmessage.setPayload(String.valueOf(strategy).getBytes(StandardCharsets.UTF_8));
                    passer.MQTTClient.publish("Strategy",MQTTmessage);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                if(data[2]!=0) { //motor on
                    passer.handler.post(() -> passer.motorOn.setBackgroundColor(Color.parseColor("#FF0000")));
                    try {
                        MQTTmessage.setPayload("1".getBytes(StandardCharsets.UTF_8));
                        passer.MQTTClient.publish("MotorOn", MQTTmessage);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
                else {           //motor off
                    passer.handler.post(() -> passer.motorOn.setBackgroundColor(Color.TRANSPARENT));
                    try {
                        MQTTmessage.setPayload("0".getBytes(StandardCharsets.UTF_8));
                        passer.MQTTClient.publish("MotorOn", MQTTmessage);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
                if(data[3]!=0) {  //purge on
                    passer.handler.post(() -> passer.purge.setBackgroundColor(Color.parseColor("#00FF00")));
                    try {
                        MQTTmessage.setPayload("1".getBytes(StandardCharsets.UTF_8));
                        passer.MQTTClient.publish("Purge", MQTTmessage);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
                else {            //purge off
                    passer.handler.post(() -> passer.purge.setBackgroundColor(Color.TRANSPARENT));
                    try {
                        MQTTmessage.setPayload("0".getBytes(StandardCharsets.UTF_8));
                        passer.MQTTClient.publish("Purge", MQTTmessage);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
                if(data[4]!=0) {  //powerMode on
                    passer.handler.post(() -> passer.SCVoltage.setBackgroundColor(Color.parseColor("#FF00FF")));
                    try {
                        MQTTmessage.setPayload("1".getBytes(StandardCharsets.UTF_8));
                        passer.MQTTClient.publish("PowerMode", MQTTmessage);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
                else {            //powerMode off
                    passer.handler.post(() -> passer.SCVoltage.setBackgroundColor(Color.TRANSPARENT));
                    try {
                        MQTTmessage.setPayload("0".getBytes(StandardCharsets.UTF_8));
                        passer.MQTTClient.publish("PowerMode", MQTTmessage);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
                if(data[5]!=0) {  //short on
                    passer.handler.post(() -> passer._short.setBackgroundColor(Color.parseColor("#FFFF00")));
                    try {
                        MQTTmessage.setPayload("1".getBytes(StandardCharsets.UTF_8));
                        passer.MQTTClient.publish("Short", MQTTmessage);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
                else {          //short off
                    passer.handler.post(() -> passer._short.setBackgroundColor(Color.TRANSPARENT));
                    try {
                        MQTTmessage.setPayload("1".getBytes(StandardCharsets.UTF_8));
                        passer.MQTTClient.publish("Short", MQTTmessage);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case 48://actuation board: FCCurrent
                float FCCurrent=byteToFloat(data[4],data[3],data[2],data[1]);
                passer.handler.post(() -> passer.FCCurrent.setText(String.format("%.2f A",FCCurrent)));
                try {
                    MQTTmessage.setPayload(String.valueOf(FCCurrent).getBytes(StandardCharsets.UTF_8));
                    passer.MQTTClient.publish("FCCurrent",MQTTmessage);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                break;
            case 63://actuation board heartbeat
                passer.handler.post(() -> passer.actuationOn.setBackgroundColor(Color.parseColor("#0000FF")));
                try {
                    MQTTmessage.setPayload("1".getBytes(StandardCharsets.UTF_8));
                    passer.MQTTClient.publish("ActuationOn", MQTTmessage);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                if(data[1]<1)
                    passer.handler.post(() -> passer.actuationOn.setBackgroundColor(Color.TRANSPARENT));
                try {
                    MQTTmessage.setPayload("0".getBytes(StandardCharsets.UTF_8));
                    passer.MQTTClient.publish("ActuationOn", MQTTmessage);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
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
