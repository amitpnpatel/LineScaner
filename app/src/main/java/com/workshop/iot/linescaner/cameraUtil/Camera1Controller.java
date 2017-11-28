package com.workshop.iot.linescaner.cameraUtil;

import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Environment;
import android.view.TextureView;

import com.workshop.iot.linescaner.AutofitPreview;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

/**
 * Created by amit on 10-May-17.
 */

public class Camera1Controller implements CameraController {

    private SharedPreferences sharedPreferences;
    Camera phoneRearCamera;
    private AutofitPreview autofitPreview;
    private SurfaceTexture previewSurfaceTexture;
    private boolean toPreviewFrame = false;
    Boolean frameDataLock = true;
    byte[] uploadFrameData;
    byte[] capturedImage;
    private boolean isImageCaptured=false;
    private Camera.Size previewsize=null;

    CameraControllerListener cameraControllerListener;

    public Camera1Controller(AutofitPreview autofitPreview,
                             CameraControllerListener cameraControllerListener) {
        this.sharedPreferences = sharedPreferences;
        this.autofitPreview = autofitPreview;
        this.autofitPreview.setSurfaceTextureListener(surfaceTextureListener);
        this.cameraControllerListener = cameraControllerListener;
    }

    @Override
    public byte[] getFrame() {

        // phoneRearCamera.startPreview();
        toPreviewFrame = true;
        long startTime = System.currentTimeMillis();
        while ((System.currentTimeMillis() - startTime) < 1000) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!toPreviewFrame) {
                return uploadFrameData;
            }
        }
        return null;
    }

    @Override
    public byte[] getImage() {
        isImageCaptured=false;
        captureImage();
        long startTime = System.currentTimeMillis();
        while ((System.currentTimeMillis() - startTime) < 3000) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (isImageCaptured) {
                return capturedImage;
            }
        }
       return null;
    }

    @Override
    public boolean setFrameSize(int width, int height) {
        Camera.Parameters param = phoneRearCamera.getParameters();
        param.setPreviewSize(width, height);
        param.setRotation(90);
        phoneRearCamera.setParameters(param);
        return false;
    }

    @Override
    public boolean setImageSize(int width, int height) {
        Camera.Parameters param = phoneRearCamera.getParameters();
        param.setPictureSize(width, height);
        param.setRotation(90);
        phoneRearCamera.setParameters(param);
        return false;
    }

    @Override
    public void closeCamera() {
        if (null != phoneRearCamera) {
            phoneRearCamera.stopPreview();
            phoneRearCamera.setPreviewCallback(null);
            phoneRearCamera.release();
            phoneRearCamera = null;
        }
    }

    @Override
    public void autoFocusCamera() {
        if (null != phoneRearCamera) {
            phoneRearCamera.autoFocus(null);
        }
    }

    private final Camera.PreviewCallback cameraPreviewCallback =
            new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    if (toPreviewFrame) {
                        byte[] bdata = new byte[data.length];
                        for (int i = 0; i < bdata.length; i++) {
                            bdata[i] = data[i];
                        }
                        uploadFrameData = getJpegFromyuv(camera.getParameters().getPreviewSize().width,
                                camera.getParameters().getPreviewSize().height, bdata);
                        toPreviewFrame = false;
                    }
                }

            };

    private byte[] getJpegFromyuv(int width, int height, byte[] yuvimage) {
        YuvImage yuvImage = new YuvImage(yuvimage, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    private final TextureView.SurfaceTextureListener surfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            try {
                // open the phoneRearCamera
                phoneRearCamera = Camera.open();
                phoneRearCamera.setPreviewCallback(cameraPreviewCallback);
            } catch (RuntimeException e) {
                // check for exceptions
                System.err.println(e);
                return;
            }
            Camera.Parameters param = phoneRearCamera.getParameters();
            previewsize = getBestPreviewSize_4_3(param);
            Camera.Size picturesize = getBestPictureSize(param);
            param.setPreviewSize(previewsize.width, previewsize.height);
            param.setPictureSize(picturesize.width, picturesize.height);
            param.setRotation(90);
            phoneRearCamera.setParameters(param);
            phoneRearCamera.setDisplayOrientation(90);
            try {
                // The Surface has been created, now tell the mCamera1 where to draw
                // the preview.
                autofitPreview.setAspectRatio(previewsize.height, previewsize.width);
                phoneRearCamera.setPreviewTexture(texture);
                phoneRearCamera.startPreview();
                phoneRearCamera.autoFocus(null);
                if(cameraControllerListener!=null){
                    cameraControllerListener.onCameraReady(previewsize);
                }
            } catch (Exception e) {
                // check for exceptions
                System.err.println(e);
                return;
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };



    PictureCallback jpegPictureCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
//            phoneRearCamera.startPreview();
//            Bitmap bitmap = BitmapFactory.decodeByteArray(data,0,data.length);
//            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 30, byteArrayOutputStream);
//            capturedImage=byteArrayOutputStream.toByteArray();
//            isImageCaptured=true;
        }
    };

    public void captureImage() {
        phoneRearCamera.takePicture(null, null, jpegPictureCallback);
    }

    public void refreshCamera() {

        if (autofitPreview == null) {
            // preview surface does not exist
            return;
        }

        // close preview before making changes
        try {
            phoneRearCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to close a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        // start preview with new settings
        try {
            phoneRearCamera.setPreviewTexture(previewSurfaceTexture);
            phoneRearCamera.startPreview();
        } catch (Exception e) {

        }
    }

    private File getDir() {
        File sdDir = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        //  File sdDir = Environment.getExternalStorageDirectory();
        return new File(sdDir, "OMREvalutor");
    }

    private Camera.Size getSmallestPreviewSize(Camera.Parameters param) {
        List psizes = param.getSupportedPreviewSizes();
        Camera.Size smallestsize = (Camera.Size) psizes.get(0);
        for (int ii = 1; ii < psizes.size(); ii++) {
            Camera.Size temp = (Camera.Size) psizes.get(ii);
            int temparea = temp.width * temp.height;
            if (temparea < smallestsize.height * smallestsize.width) {
                smallestsize = temp;
            }
        }
        return smallestsize;

    }

    private Camera.Size getBestPreviewSize(Camera.Parameters param) {
        List psizes = param.getSupportedPreviewSizes();
        Camera.Size nearAspect = null;
        Camera.Size nearArea = null;
        int previewDesiredarea = 480 * 640;
        for (int ii = 0; ii < psizes.size(); ii++) {
            Camera.Size temp = (Camera.Size) psizes.get(ii);
            int temparea = temp.width * temp.height;
            if ((temp.width == 640) && (temp.height == 480)) {
                return temp;
            }
            if (((double) temp.width / 640.0) == ((double) temp.height / 480.0)) {
                if (nearAspect == null) {
                    nearAspect = temp;
                } else {
                    if ((temparea < previewDesiredarea * 2 + 1) && (temparea > previewDesiredarea)) {
                        nearAspect = temp;
                    }
                }
            }
            if ((temparea < previewDesiredarea * 2 + 1) && (temparea > previewDesiredarea)) {
                nearArea = temp;
            }
        }
        if (nearAspect != null) {
            return nearAspect;
        }
        if (nearArea != null) {
            return nearArea;
        } else {
            return (Camera.Size) psizes.get(0);
        }
    }

    private Camera.Size getBestPictureSize(Camera.Parameters param) {
        List psizes = param.getSupportedPictureSizes();
        Camera.Size nearAspect = null;
        Camera.Size nearArea = null;
        int previewDesiredarea = 960 * 1280;
        for (int ii = 0; ii < psizes.size(); ii++) {
            Camera.Size temp = (Camera.Size) psizes.get(ii);
            int temparea = temp.width * temp.height;
            if ((temp.width == 1280) && (temp.height == 960)) {
                return temp;
            }
            if (((double) temp.width / 640.0) == ((double) temp.height / 480.0)) {
                if (nearAspect == null) {
                    nearAspect = temp;
                } else {
                    if ((temparea < previewDesiredarea * 2) && (temparea > previewDesiredarea / 2 - 1)) {
                        nearAspect = temp;
                    }
                }
            }
            if ((temparea < previewDesiredarea * 2 + 1) && (temparea > previewDesiredarea)) {
                nearArea = temp;
            }
        }
        if (nearAspect != null) {
            return nearAspect;
        }
        if (nearArea != null) {
            return nearArea;
        } else {
            return (Camera.Size) psizes.get(0);
        }
    }
    private Camera.Size getBestPreviewSize_4_3(Camera.Parameters param) {
        List psizes = param.getSupportedPreviewSizes();
        Camera.Size maxAreaSize = (Camera.Size) psizes.get(psizes.size()-1);
        double maxWidthRatio=0;
        for (int ii = 0; ii < psizes.size(); ii++) {
            Camera.Size temp = (Camera.Size) psizes.get(ii);
            double widthRatio=0;
            if(temp.width>680){
                widthRatio=680.0/temp.width;
            }else{
                widthRatio=((double)temp.width)/680.0;
            }
            if (widthRatio > maxWidthRatio) {
                if (isPreviewAspectRatioAcceptable(temp)) {
                    maxWidthRatio = widthRatio;
                    maxAreaSize = temp;
                }
            }
        }
        return maxAreaSize;
    }

    private boolean isPreviewAspectRatioAcceptable(Camera.Size size) {
        double lAspectRatio = 1.0;
        double highaspectRatio = 4.5 / 3.0;
        double aspectratio;
        if (size.height > size.width) {
            aspectratio = (double) size.height / (double) size.width;
        } else {
            aspectratio = (double) size.width / (double) size.height;
        }
        return ((aspectratio < highaspectRatio) && (aspectratio > lAspectRatio));
    }
}
