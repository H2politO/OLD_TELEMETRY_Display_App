package com.hoho.android.usbserial.examples;

import android.os.Handler;
import android.os.Looper;

public class ReadingThread extends Thread{

    public Handler handler;
    public Looper looper;

    @Override
    public void run() {
        Looper.prepare();
        looper=Looper.myLooper();

        handler=new myHandler();

        Looper.loop();
    }
}
