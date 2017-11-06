package com.workshop.iot.linescaner;

import android.app.ActionBar;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ProgressBar;

import com.workshop.iot.linescaner.cameraUtil.Camera1Controller;
import com.workshop.iot.linescaner.cameraUtil.CameraController;
import com.workshop.iot.linescaner.cameraUtil.CameraControllerListener;

import java.io.IOException;
import java.util.Set;

public class ScanerAcivity extends AppCompatActivity implements CameraControllerListener, MessengerListener {

    ScanerAcivity activity = this;
    Camera.Size previewSize;
    Bitmap baseBitmap;
    private boolean isActivityVisible;
    private SurfaceView overlayView;
    private SurfaceHolder overlaySurfaceHolder;
    private CameraController cameraController = null;
    private AutofitPreview mTextureView;
    private ProgressBar scanProgress;
    ThreadBlueToothClient blueToothClient;
    Square square = new Square(0, 0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scaner_acivity);
        mTextureView = (AutofitPreview) findViewById(R.id.surfaceView);
        scanProgress = (ProgressBar) findViewById(R.id.scan_progress);
        cameraController = new Camera1Controller(mTextureView, this);
        overlayView = (SurfaceView) findViewById(R.id.overlayView);
        overlayView.setZOrderOnTop(true);
        overlayView.setBackgroundColor(Color.argb(0, 255, 255, 255));
        overlaySurfaceHolder = overlayView.getHolder();
        overlayView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraController.autoFocusCamera();
            }
        });
    }

    private BluetoothDevice getBlueToothDevice() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice bd : pairedDevices) {
                return bd;
                //  bluetoothDevices.add(bd);
                //   deviceNameList.add(bd.getName());
                //  deviceMacAddress.add(bd.getAddress());
            }
        }
        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scan, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent currentActivityReference in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_cble) {
            BluetoothDevice bluetoothDevice = getBlueToothDevice();
            if (getBlueToothDevice() != null) {
                blueToothClient = new ThreadBlueToothClient(bluetoothDevice, this);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCameraReady(Camera.Size previewsize) {
        previewSize = previewsize;
        processPreiewFrame();
    }

    @Override
    public void onCameraClosed() {

    }

    @Override
    public void onResume() {
        super.onResume();
        isActivityVisible = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityVisible = false;
        if (cameraController != null) {
            cameraController.closeCamera();
        }
    }

    private void checkBaseBitmap(CheckPreviewTask checkPreviewTask) throws IOException {
        byte[] frameData = cameraController.getFrame();
        checkPreviewTask.updateProgress(10);
        if (frameData == null) {
            return;
        }
        baseBitmap = BitmapFactory.decodeByteArray(frameData, 0, frameData.length);
        checkPreviewTask.updateProgress(20);
        square = ScanUtil.getLineCordinate(baseBitmap);
    }

    @Override
    public void onMessage(Object object) {

    }

    @Override
    public boolean onConnect() {
        return false;
    }

    @Override
    public boolean onDisConnect() {
        return false;
    }

    @Override
    public void onError(Exception e) {

    }

    public class CheckPreviewTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPostExecute(Void result) {
            new DrawOverlayOutline().execute();
            processPreiewFrame();
        }

        public void updateProgress(int value) {
            publishProgress(value);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            scanProgress.setProgress(values[0]);
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Thread.sleep(1000);
                updateProgress(5);
                checkBaseBitmap(this);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private void processPreiewFrame() {
        new CheckPreviewTask().execute();
    }

    private class DrawOverlayOutline extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPostExecute(Void result) {
            processPreiewFrame();
        }

        @Override
        protected Void doInBackground(Void... params) {
            overlaySurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
            Canvas canvas = overlaySurfaceHolder.lockCanvas();
            if (canvas != null) {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                drawoutLineForPreviewaspectRatio(canvas, previewSize.width, previewSize.height);
                overlaySurfaceHolder.unlockCanvasAndPost(canvas);
            }
            return null;
        }


        private void drawoutLineForPreviewaspectRatio(Canvas canvas, int previewHeight, int previewWidth) {
            int canvasHeight = canvas.getHeight();
            int canvasWidth = canvas.getWidth();
            int cameraPreviewDisplayHeight = 0;
            int cameraPreviewDisplayWidth = 0;
            if (((float) (canvasHeight) / (float) (canvasWidth)) > ((float) previewHeight / (float) previewWidth)) {
                cameraPreviewDisplayHeight = (previewHeight * canvasWidth) / previewWidth;
                cameraPreviewDisplayWidth = canvasWidth;
            } else {
                cameraPreviewDisplayHeight = canvasHeight;
                cameraPreviewDisplayWidth = (previewWidth * canvasHeight) / previewHeight;
            }
            Paint myPaint = new Paint();
            myPaint.setColor(Color.argb(255, 255, 0, 0));
            myPaint.setStrokeWidth(3);
            myPaint.setTextSize(100);
            myPaint.setStyle(Paint.Style.STROKE);
            int widthPreviewgap = (canvas.getWidth() - cameraPreviewDisplayWidth) / 2;
            int heightPreviewgap = (canvas.getHeight() - cameraPreviewDisplayHeight) / 2;
            canvas.drawRect(widthPreviewgap, heightPreviewgap, canvas.getWidth() - widthPreviewgap, canvas.getHeight() - heightPreviewgap, myPaint);
            canvas.drawText("" + square.center, canvas.getWidth() / 2, canvas.getHeight() / 2, myPaint);
            int squareOutLineLength = (cameraPreviewDisplayWidth * square.length) / 100;
            int squareOutLineCenter = (cameraPreviewDisplayWidth * square.center) / 100;
            // drawSquare(canvas, widthPreviewgap+squareOutLineCenter, heightPreviewgap+cameraPreviewDisplayHeight/4, squareOutLineLength/2);
        }

        private void drawSquare(Canvas canvas, int pixelCordinatesx, int pixelCordinatey, int lengthinpixel) {
            Paint myPaint = new Paint();
            myPaint.setColor(Color.argb(255, 255, 0, 0));
            myPaint.setStrokeWidth(3);
            myPaint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(pixelCordinatesx - lengthinpixel, pixelCordinatey - lengthinpixel, pixelCordinatesx + lengthinpixel, pixelCordinatey + lengthinpixel, myPaint);
        }
    }
}
