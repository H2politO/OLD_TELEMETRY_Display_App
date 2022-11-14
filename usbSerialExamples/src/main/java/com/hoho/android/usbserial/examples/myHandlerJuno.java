package com.hoho.android.usbserial.examples;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;


public class myHandlerJuno extends Handler {

    //sensor
    private static final String EMERGENCES = "";
    private static final String HMI = "";
    private static final String ECU1 = ""; //RPM & TPS
    private static final String ECU2 = ""; //Oil Temp & Lambda
    private static final String ECU3 = ""; //Engine & Limp-->modalita centralina
    private static final String ECU4 = ""; //Battery & Voltage
    private static final String ECU5 = ""; //RunMode & SyncState
    private static final String ECU6 = ""; //Speed

    //Mqtt
    public static final String SERVERURI = "tcp://broker.hivemq.com:1883";
    public static final String CLIENTID= MqttClient.generateClientId();
    private static final String PASSWORD = "H2display";
    private static final String USERNAME = "DisplayJuno";

    myBoolean connected = new myBoolean(false);
    myBoolean network = new myBoolean(false);
    private MqttAndroidClient client;

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    @Override
    public void handleMessage(@NonNull Message msg) {
        final Passer passer;
        byte[] data;
        int id ;
        if(msg.what == 0){
            //if msg 0, try to connect MqttServer
            Context context = (Context) msg.obj;
            client = new MqttAndroidClient(context, SERVERURI, CLIENTID);

            try{
                IMqttToken token = client.connect();
                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        connected.setState(true);
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                        Toast.makeText(context, "device wasn't able to connect, i am working only as a display", Toast.LENGTH_SHORT).show();
                        connected.setState(false);
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
            return;
        }else{
            //copy msg for work on it
            passer = (Passer) msg.obj;
            data = passer.data;
            id = byteToInt(data[0]);
            network.setState(passer.connected);
        }
        switch(id) {
            case 17://emergences fromm the safe board
                if(data[4]!=0) {
                    if (data[3] == 2)
                        passer.SOS.post(() -> passer.SOS.setText("Deadman"));
                    else if (data[3] == 4)
                        passer.SOS.post(() -> passer.SOS.setText("External"));
                    else if (data[3] == 8)
                        passer.SOS.post(() -> passer.SOS.setText("Internal"));
                }
                else
                    passer.SOS.post(() -> passer.SOS.setText("SOS"));
                break;

            case 49://HMI
                //first bit->high beam
                if (getBit(0, data[2]) == 1) {
                    passer.highBeam.post(() -> passer.highBeam.setBackgroundColor(Color.parseColor("#858282")));
                    passer.highBeam.post(()->passer.highBeam.setTextColor(Color.parseColor("#000000")));
                } else {
                    passer.highBeam.post(() -> passer.highBeam.setBackgroundColor(Color.TRANSPARENT));
                    passer.highBeam.post(() -> passer.highBeam.setTextColor(Color.parseColor("#FFFFFF")));
                }
                //second bit ->low beam
                if (getBit(1, data[2]) == 1) {
                    passer.lowBeam.post(() -> passer.lowBeam.setBackgroundColor(Color.parseColor("#858282")));
                    passer.lowBeam.post(()->passer.lowBeam.setTextColor(Color.parseColor("#000000")));
                } else {
                    passer.lowBeam.post(() -> passer.lowBeam.setBackgroundColor(Color.TRANSPARENT));
                    passer.lowBeam.post(()->passer.lowBeam.setTextColor(Color.parseColor("#FFFFFF")));
                }
                //third bit -> left arrow
                if (getBit(2, data[2]) == 1){
                    passer.leftArrow.post(() -> passer.leftArrow.setBackgroundColor(Color.parseColor("#f79148")));
                    passer.leftArrow.post(()->passer.leftArrow.setTextColor(Color.parseColor("#000000")));
                }else {
                    passer.leftArrow.post(() -> passer.leftArrow.setBackgroundColor(Color.TRANSPARENT));
                    passer.leftArrow.post(()->passer.leftArrow.setTextColor(Color.parseColor("#FFFFFF")));
                }
                //forth bit-> right arrow
                if(getBit(3,data[2])==1) {
                    passer.rightArrow.post(() -> passer.rightArrow.setBackgroundColor(Color.parseColor("#f79148")));
                    passer.rightArrow.post(()->passer.rightArrow.setTextColor(Color.parseColor("#000000")));
                }else {
                    passer.rightArrow.post(() -> passer.rightArrow.setBackgroundColor(Color.TRANSPARENT));
                    passer.rightArrow.post(()->passer.rightArrow.setTextColor(Color.parseColor("#FFFFFF")));
                }
                break;

            case 60://first batch of messages from ECU(1/2)
                //first 2 bytes->RPM
                int RPM = byteToInt(data[1], data[2]);
                passer.RPM.post(() -> passer.RPM.setText(String.format("%d RMP", RPM)));
                if (RPM > 0 && RPM < 3500) {
                    passer.RPMBackground.post(() -> passer.RPMBackground.setBackgroundColor(Color.parseColor("#0000FF")));
                } else if (RPM>= 3500 && RPM < 5000) {
                    passer.RPMBackground.post(() -> passer.RPMBackground.setBackgroundColor(Color.parseColor("#00FF00")));
                } else if (RPM > 5000) {
                    passer.RPMBackground.post(() -> passer.RPMBackground.setBackgroundColor(Color.parseColor("#FF0000")));
                }
                //second 2 bytes->tps
                float TPS = (float) (byteToInt(data[3], data[4]) / 81.92);
                passer.TPS.post(() -> passer.TPS.setText(String.format("TPS: %.2f", TPS)));
                break;

            case 61://first batch of messages from ECU(2/2)
                //third 2 bytes->engine coolant temperature
                float oilTemp = (float)(byteToInt(data[1], data[2]) / 10.0);
                passer.oilTemp.post(() -> passer.oilTemp.setText(String.format("OIL: %.2f", oilTemp)));
                //forth 2 bytes->lambda
                float lambda = (float) (byteToInt(data[3], data[4]) / 1000.0);
                passer.lambda.post(() -> passer.lambda.setText(String.format("L: %.2f", lambda)));
                if (lambda >=0 && lambda < 0.9) {
                    passer.lambdaBackground.post(() -> passer.lambdaBackground.setBackgroundColor(Color.parseColor("#c5db32")));
                    passer.lambda.post(()->passer.lambda.setTextColor(Color.parseColor("#000000")));
                } else if (lambda >= 0.9 && lambda <= 1.1){
                    passer.lambdaBackground.post(() -> passer.lambdaBackground.setBackgroundColor(Color.TRANSPARENT));
                    passer.lambda.post(()->passer.lambda.setTextColor(Color.parseColor("#FFFFFF")));
                }else if(lambda>1.1 && lambda<=1.4) {
                    passer.lambdaBackground.post(() -> passer.lambdaBackground.setBackgroundColor(Color.parseColor("#344be0")));
                    passer.lambda.post(() -> passer.lambda.setTextColor(Color.parseColor("#000000")));
                }
                break;
            case 62://second batch of messages from ECU(1/2)
                //first 2 bytes -> limp mode
                int limpMode =byteToInt(data[1],data[2]);
                if(limpMode==0)//
                    passer.limpMode.post(()->passer.limpMode.setBackgroundColor(Color.parseColor("#344be0")));
                else//
                    passer.limpMode.post(()->passer.limpMode.setBackgroundColor(Color.parseColor("#ad0c14")));
                //second 2 bytes ->engine Enable
                int engineEnable = byteToInt(data[3],data[4]);
                if(engineEnable==0)
                    passer.engineEnable.post(()->passer.engineEnable.setBackgroundColor(Color.parseColor("#af34e0")));
                else
                    passer.engineEnable.post(()->passer.engineEnable.setBackgroundColor(Color.parseColor("#ad0c14")));
                break;

            case 63://second batch of messages from ECU(2/2)
                //forth 2 bytes-> battery Voltage
                float VBattery = (float)(byteToInt(data[3],data[4])/1000.0);
                passer.batteryVoltage.post(()->passer.batteryVoltage.setText(String.format("%.2f V",VBattery)));
                if(VBattery<11) {
                    passer.batteryVoltage.post(() -> passer.batteryVoltage.setBackgroundColor(Color.parseColor("ed0909")));
                    passer.batteryVoltage.post(() -> passer.batteryVoltage.setTextColor(Color.parseColor("#000000")));
                }else {
                    passer.batteryVoltage.post(() -> passer.batteryVoltage.setBackgroundColor(Color.TRANSPARENT));
                    passer.batteryVoltage.post(()->passer.batteryVoltage.setTextColor(Color.parseColor("#FFFFFF")));
                }
                break;
            case 64://third batch of messages from ECU(1/2)
                //first 2 bytes -> runMode
                int runMode = byteToInt(data[1],data[2]);
                if(runMode==3)
                    passer.engineEnable.post(()->passer.engineEnable.setBackgroundColor(Color.parseColor("#ad0c14")));
                //second 2 bytes -> sync state
                int syncState=byteToInt(data[3],data[4]);
                if(syncState==0)
                    passer.syncState.post(()->passer.syncState.setText("0"));
                else if(syncState==2)
                    passer.syncState.post(()->passer.syncState.setText("360"));
                else if(syncState==3)
                    passer.syncState.post(()->passer.syncState.setText("720"));
                break;

            case 65://third batch of messages from ECU(2/2)
                //third 2 bytes -> vehicle speed
                double speed= byteToInt(data[5],data[6])*0.036;
                passer.speed.post(()->passer.speed.setText(String.format("%.2f Km/h ",speed)));
                break;
            default:
                break;
        }
    }

    public void publish(String topic, byte[] payload){
        if(!connected.isState() && !network.isState())
            return;
        try{
            MqttMessage message = new MqttMessage(payload);
            message.setQos(0);
            message.setRetained(true);
            client.publish(topic, message);
        }catch (MqttException e){
            e.printStackTrace();
        }
    }

    public void publish(String topic, int payload){
        byte[] data = String.valueOf(payload).getBytes(StandardCharsets.UTF_8);
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

    public int  getBit(int position, byte val)
    {
        return (val >> position) & 1;
    }
}
