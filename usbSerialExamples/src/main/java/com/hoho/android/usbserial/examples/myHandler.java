package com.hoho.android.usbserial.examples;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

public class myHandler extends Handler {

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    @Override
    public void handleMessage(@NonNull Message msg) {
        Passer passer= (Passer) msg.obj;
        byte[] data= passer.data;
        int id = byteToInt(data[0]);
        switch(id){
            case 17://emergences fromm the safe board
                switch (data[3]){
                    case 1:
                        passer.SOS.post(()-> passer.SOS.setText("Deadman"));
                        break;
                    case 2:
                        passer.SOS.post(()-> passer.SOS.setText("External"));
                        break;
                    case 3:
                        passer.SOS.post(()-> passer.SOS.setText("Internal"));
                        break;
                    default:
                        break;
                }
                break;
            case 61://first batch of messages from ECU
                //first 2 bytes->RMP
                passer.RPM.post(()->passer.RPM.setText(String.format("%d RMP",byteToInt(data[1],data[2]))));
                //second 2 bytes->tps
                passer.TPS.post(()-> passer.TPS.setText(String.format("TPS: %d",byteToInt(data[3],data[4]))));
                //third 2 bytes->ext1

                //forth 2 bytes->lambda
                passer.lambda.post(()->passer.lambda.setText(String.format("L:%.2f",byteToInt(data[7],data[8])/81.7)));
                break;
            case 62://second batch of messages from ECU

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
