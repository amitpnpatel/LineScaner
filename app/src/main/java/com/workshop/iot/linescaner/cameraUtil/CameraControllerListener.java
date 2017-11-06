package com.workshop.iot.linescaner.cameraUtil;

import android.hardware.Camera;

/**
 * Created by amit on 16-May-17.
 */

public interface CameraControllerListener {
    void onCameraReady(Camera.Size previewsize);
    void onCameraClosed();
}
