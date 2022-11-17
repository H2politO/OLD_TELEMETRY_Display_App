package com.hoho.android.usbserial.examples;

import static com.hoho.android.usbserial.examples.DevicesFragment.IDRA;

import android.os.Handler;
import android.os.Looper;

public class JunoThread extends Thread{

    public Handler handler = new myHandlerJuno();
    public Looper looper;

    @Override
    public void run() {
        Looper.prepare();

        looper=Looper.myLooper();

        Looper.loop();
    }
}
