package com.hoho.android.usbserial.examples;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

public class myHandlerIdra extends Handler {

    @SuppressLint("DefaultLocale")
    @Override
    public void handleMessage(@NonNull Message msg) {
        Passer passer= (Passer) msg.obj;
        byte[] data= passer.data;
        int id = byteToInt(data[0]);
        switch(id){
            case 16://service board: emergences
                boolean emActive=false;
                String emString="";
                boolean h2Emergency=false;
                for(int i=1;i<5;i++) {
                    if (data[i] != 0){
                        switch (i){
                            case 1:
                                emString+="H2 ";
                                h2Emergency=true;
                                break;
                            case 2:
                                emString+="Deadman ";
                                break;
                            case 3:
                                emString+="External ";
                                break;
                            case 4:
                                emString+="Internal";
                                break;
                        }
                    }
                    emActive = true;
                }
                if(emActive) {
                    String finalEm_string = emString;
                    passer.handler.post(() -> passer.emergences.setText(finalEm_string));
                    if(h2Emergency)
                        passer.handler.post(() -> passer.emergences.setBackgroundColor(Color.parseColor("#0000FF")));
                    else
                        passer.handler.post(() -> passer.emergences.setBackgroundColor(Color.parseColor("#FF0000")));
                }
                else {
                    passer.handler.post(() -> passer.emergences.setText("E"));
                    passer.handler.post(() -> passer.emergences.setBackgroundColor(Color.TRANSPARENT));
                }
                break;
            case 17://service board: speed
                float speed=byteToFloat(data[4],data[3],data[2],data[1]);
                passer.handler.post(() -> passer.speed.setText(String.format("%.2f Km/h",speed)));
                break;
            case 18://service board: temperature
                float temperature=byteToFloat(data[4],data[3],data[2],data[1]);
                passer.handler.post(() -> passer.temperature.setText(String.format("%.2f C°",temperature)));
                break;
            case 19://service board: FCVoltage
                float FCVoltage=byteToFloat(data[4],data[3],data[2],data[1]);
                passer.handler.post(() -> passer.FCVoltage.setText(String.format("%.2f V",FCVoltage)));
                break;
            case 20://service board: SCVoltage
                float SCVoltage=byteToFloat(data[4],data[3],data[2],data[1]);
                passer.handler.post(() -> passer.SCVoltage.setText(String.format("%.2f V",SCVoltage)));
                break;
            case 32://wheel :
                int strategy=byteToInt(data[1]);
                passer.handler.post(() -> passer.strategy.setText(String.format("ST: %d",strategy)));
                if(data[2]!=0)  //motor on
                    passer.handler.post(() -> passer.motorOn.setBackgroundColor(Color.parseColor("#FF0000")));
                else            //motor off
                    passer.handler.post(() -> passer.motorOn.setBackgroundColor(Color.TRANSPARENT));
                if(data[3]!=0)  //purge on
                    passer.handler.post(() -> passer.purge.setBackgroundColor(Color.parseColor("#00FF00")));
                else            //purge off
                    passer.handler.post(() -> passer.purge.setBackgroundColor(Color.TRANSPARENT));
                if(data[4]!=0)  //powerMode on
                    passer.handler.post(() -> passer.SCVoltage.setBackgroundColor(Color.parseColor("#FF00FF")));
                else            //powerMode off
                    passer.handler.post(() -> passer.SCVoltage.setBackgroundColor(Color.TRANSPARENT));
                if(data[5]!=0)  //short on
                    passer.handler.post(() -> passer._short.setBackgroundColor(Color.parseColor("#FFFF00")));
                else            //short off
                    passer.handler.post(() -> passer._short.setBackgroundColor(Color.TRANSPARENT));
                break;
            case 48://actuation board: FCCurrent
                float FCCurrent=byteToFloat(data[4],data[3],data[2],data[1]);
                passer.handler.post(() -> passer.FCCurrent.setText(String.format("%.2f A",FCCurrent)));
                break;
            case 63://actuation board heartbeat
                passer.handler.post(() -> passer.actuationOn.setBackgroundColor(Color.parseColor("#0000FF")));
                if(data[1]<1)
                    passer.handler.post(() -> passer.actuationOn.setBackgroundColor(Color.TRANSPARENT));
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