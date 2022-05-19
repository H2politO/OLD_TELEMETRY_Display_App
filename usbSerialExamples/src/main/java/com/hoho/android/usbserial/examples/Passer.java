package com.hoho.android.usbserial.examples;

import android.content.Context;
import android.os.Handler;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;

public class Passer {
    public byte[] data;
    TextView TPS;
    TextView oilTemp;
    TextView lambda;
    TextView speed;
    TextView RPM;
    TextView SOS;
    TextView lowBeam;
    TextView highBeam;
    TextView injection;
    TextView timer;
    TextView rightArrow;
    TextView leftArrow;
    TextView batteryVoltage;
    public Handler handler;
    //public MqttAndroidClient MQTTClient;


    public Passer(TextView TPS,
                  TextView oilTemp,
                  TextView lambda,
                  TextView speed,
                  TextView RPM,
                  TextView SOS,
                  TextView lowBeam,
                  TextView highBeam,
                  TextView injection,
                  TextView timer,
                  TextView rightArrow,
                  TextView leftArrow,
                  TextView batteryVoltage,
                  Handler handler) {


        this.TPS = TPS;
        this.oilTemp = oilTemp;
        this.lambda = lambda;
        this.speed = speed;
        this.RPM = RPM;
        this.SOS = SOS;
        this.lowBeam = lowBeam;
        this.highBeam = highBeam;
        this.injection = injection;
        this.timer = timer;
        this.rightArrow = rightArrow;
        this.leftArrow = leftArrow;
        this.batteryVoltage = batteryVoltage;
        this.handler = handler;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
