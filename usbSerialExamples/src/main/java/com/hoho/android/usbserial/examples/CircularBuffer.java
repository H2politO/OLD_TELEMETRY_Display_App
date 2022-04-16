package com.hoho.android.usbserial.examples;


import java.nio.ByteBuffer;

public class CircularBuffer {

    private static int FLOAT=4;
    private static final int BOOLEAN=1;
    private int[] id;
    private int idHead;
    private int idTail;
    private byte[] buffer;
    private int size;
    private int bufferHead;
    private int bufferTail;
    private int idSize;

    //create the object
    public CircularBuffer(int size) {
        this.buffer = new byte[size];
        idSize = size / 4;
        this.id = new int[idSize];
        this.size = size;
        this.bufferHead = 0;
        this.bufferTail = 0;
        this.idHead = 0;
        this.idTail = 0;
    }

    //return true if the buffer is empty
    public boolean isEmpty() {
        return bufferHead == bufferTail;
    }

    //return true if the buffer is full
    public boolean isFull() {
        return bufferHead == (bufferTail - 1) % size;
    }

    //insert data into data buffer
    public void insertData(byte[] data) {
        int length = data.length;
        for (int i = 0; i < length; i++)
            buffer[(bufferHead + i) % size] = data[i];
        bufferHead = (bufferHead + length) % size;
        return;
    }

    //insert id in buffer
    public void insertId(int id) {
        this.id[idHead] = id;
        idHead = (idHead + 1) % idSize;
    }

    //return the next type of data
    public int getId(){
        int nextType=id[idTail];
        idTail=(idTail+1)%idSize;
        return nextType;
    }

    //get a floating point value
    public float getFloat(){
        byte[] currentData=new byte[4];

        for(int i=0;i<4;i++)
            currentData[i]=buffer[(bufferTail+i)%size];
        return ByteBuffer.wrap(currentData).getFloat();
    }

    //get boolean value
    public boolean getBoolean(){
        byte b= buffer[bufferTail];
        bufferTail=(bufferTail+1)%size;
        return b==0;
    }
}
