package com.workshop.iot.linescaner.cameraUtil;

/**
 * Created by amit on 10-May-17.
 */

public interface CameraController {

     byte[] getFrame();
     byte[] getImage();
     boolean setFrameSize(int width, int height);
     boolean setImageSize(int width, int height);
     void closeCamera();
     void autoFocusCamera();
}
