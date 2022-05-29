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
    public TextView TPS;
    public TextView oilTemp;
    public TextView lambda;
    public TextView RPM;
    public TextView SOS;
    public TextView lowBeam;
    public TextView highBeam;
    public TextView syncState;
    public TextView timer;
    public TextView rightArrow;
    public TextView leftArrow;
    public TextView batteryVoltage;
    public TextView RPMBackground;
    public TextView lambdaBackground;
    public TextView engineEnable;
    public TextView limpMode;
    public Handler handler;
    //public MqttAndroidClient MQTTClient;

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
                  Handler handler)
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

    }

    public Passer(TextView TPS,
                  TextView oilTemp,
                  TextView lambda,
                  TextView lambdaBackground,
                  TextView speed,
                  TextView RPM,
                  TextView RPMBackground,
                  TextView SOS,
                  TextView lowBeam,
                  TextView highBeam,
                  TextView syncState,
                  TextView timer,
                  TextView rightArrow,
                  TextView leftArrow,
                  TextView batteryVoltage,
                  TextView engineEnable,
                  TextView limpMode,
                  Handler handler) {

        this.TPS = TPS;
        this.oilTemp = oilTemp;
        this.lambda = lambda;
        this.speed = speed;
        this.RPM = RPM;
        this.SOS = SOS;
        this.lowBeam = lowBeam;
        this.highBeam = highBeam;
        this.syncState = syncState;
        this.timer = timer;
        this.rightArrow = rightArrow;
        this.leftArrow = leftArrow;
        this.batteryVoltage = batteryVoltage;
        this.handler = handler;
        this.engineEnable= engineEnable;
        this.lambdaBackground= lambdaBackground;
        this.RPMBackground= RPMBackground;
        this.limpMode= limpMode;
    }
    public void setData(byte[] data) {
        this.data = data;
    }
}
