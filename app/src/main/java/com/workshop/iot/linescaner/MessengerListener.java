package com.workshop.iot.linescaner;

/**
 * Created by amit on 11-May-17.
 */

public interface MessengerListener {
    void onMessage(Object object);
    boolean onConnect();
    boolean onDisConnect();
    void onError(Exception e);
   }
