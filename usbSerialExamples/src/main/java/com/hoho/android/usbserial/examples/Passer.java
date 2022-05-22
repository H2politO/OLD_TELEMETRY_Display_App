package com.hoho.android.usbserial.examples;

import android.content.Context;
import android.os.Handler;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;

public class Passer {
    public byte[] data;
    public TextView purge;
    public TextView _short;
    public TextView emergences;
    public TextView motorOn;
    public TextView actuationOn;
    public TextView temperature;
    public TextView strategy;
    public TextView FCVoltage;
    public TextView FCCurrent;
    public TextView SCVoltage;
    public TextView speed;
    public Handler handler;
    public MqttAndroidClient MQTTClient;

    public Passer(TextView purge,
                  TextView _short,
                  TextView emergences,
                  TextView motorOn,
                  TextView actuationOn,
                  TextView temperature,
                  TextView strategy,
                  TextView FCVoltage,
                  TextView FCCurrent,
                  TextView SCVoltage,
                  TextView speed,
                  Handler handler,
                  MqttAndroidClient MQTTClient)
    {

        this.purge = purge;
        this._short = _short;
        this.emergences = emergences;
        this.motorOn = motorOn;
        this.actuationOn = actuationOn;
        this.temperature = temperature;
        this.strategy = strategy;
        this.FCVoltage = FCVoltage;
        this.FCCurrent = FCCurrent;
        this.SCVoltage = SCVoltage;
        this.speed = speed;
        this.handler= handler;
        this.MQTTClient=MQTTClient;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
