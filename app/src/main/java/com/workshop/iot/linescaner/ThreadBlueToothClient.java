package com.workshop.iot.linescaner;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Amit on 1/25/2015.
 */
public class ThreadBlueToothClient extends Thread {
    public BluetoothSocket bluetoothSocket;
    private final BluetoothDevice bluetoothDevice;
    private OutputStream outputStream;
    private InputStream inputStream;
    public DataOutputStream dataOutputStream = null;
    private DataInputStream dataInputStream = null;
    public boolean torun = true;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private MessengerListener messengerListener;
    private String TAG = "ThreadBlueToothClient";

    public ThreadBlueToothClient(BluetoothDevice device, MessengerListener bluetoothListner) {
        messengerListener=bluetoothListner;
        bluetoothDevice = device;
    }

    public void run() {
        try {
            initializeConnection();
            if (messengerListener != null) {
                messengerListener.onConnect();
            }
        } catch (IOException e) {
            processException(e);
        }
        while (torun) {
            try {
                char inputChar = (char) dataInputStream.readByte();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        if (messengerListener != null) {
            messengerListener.onDisConnect();
        }
        close();
    }

    private void processOnMessage(Object object) {
        if (messengerListener != null) {
            messengerListener.onMessage(object);
        }
    }

    private void processException(Exception e) {
        if (messengerListener != null) {
            messengerListener.onError(e);
        }
        close();
    }

    private void initializeConnection() throws IOException {
        bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
        bluetoothSocket.connect();
        outputStream = bluetoothSocket.getOutputStream();
        inputStream = bluetoothSocket.getInputStream();
        dataOutputStream = new DataOutputStream(new BufferedOutputStream(outputStream));
        dataInputStream = new DataInputStream(new BufferedInputStream(inputStream));
    }

    public void close() {
        torun = false;
        try {
            if (dataOutputStream != null) {
                dataOutputStream.close();
            }
            if (dataInputStream != null) {
                dataInputStream.close();
            }
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException ioe) {

        }
    }

    public boolean writeStringToBTModule(String str) {
        if (dataOutputStream != null) {
            try {
                dataOutputStream.write(str.getBytes());
                dataOutputStream.write('\n');
                dataOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
                close();
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    public boolean setChargingState(boolean state) {
        if (dataOutputStream != null) {
            try {
                if (state) {
                    dataOutputStream.write(("SBT").getBytes());
                } else {
                    dataOutputStream.write(("SBF").getBytes());
                }
                dataOutputStream.write('\n');
                dataOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
                close();
                return false;
            }
        } else {
            return false;
        }
        return true;
    }
    public boolean setLineCordinates(byte centre, byte inclination,byte state,byte onMarker) {
        if (dataOutputStream != null) {
            try {
                dataOutputStream.write(("SLC").getBytes());
                dataOutputStream.write(state);
                dataOutputStream.write(centre);
                dataOutputStream.write(inclination);
                dataOutputStream.write(onMarker);
                dataOutputStream.write('E');
                dataOutputStream.write('\n');
                dataOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
                close();
                return false;
            }
        } else {
            return false;
        }
        return true;
    }
}