package com.hoho.android.usbserial.examples;

import android.os.Handler;
import android.widget.TextView;

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
    TextView syncState;
    TextView timer;
    TextView rightArrow;
    TextView leftArrow;
    TextView batteryVoltage;
    TextView RPMBackground;
    TextView lambdaBackground;
    TextView engineEnable;
    TextView limpMode;
    public Handler handler;
    //public MqttAndroidClient MQTTClient;


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
