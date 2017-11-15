package com.workshop.iot.linescaner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

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
    Box box=new Box();
    Handler handler=new Handler();

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
                blueToothClient.start();
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
    private boolean sendPathCordinates(){
        if(blueToothClient==null){
            return false;
        }
       return blueToothClient.setLineCordinates((byte)box.centre,(byte)(box.inclination+50),(byte)1);
    }
    private void checkBaseBitmap(CheckPreviewTask checkPreviewTask) throws IOException {
        if(baseBitmap!=null){
            baseBitmap.recycle();
            baseBitmap=null;
        }
        byte[] frameData = cameraController.getFrame();
        checkPreviewTask.updateProgress(10);
        if (frameData == null) {
            return;
        }
        baseBitmap = BitmapFactory.decodeByteArray(frameData, 0, frameData.length);
        checkPreviewTask.updateProgress(20);
        //box = ScanUtil.getWhiteLineCordinate(baseBitmap);
        //box = ScanUtil.getRedLineCordinate(baseBitmap);
        //box = ScanUtil.getYellowLineCordinate(baseBitmap);
        box = ScanUtil.getBlueLineCordinate(baseBitmap);
    }

    @Override
    public void onMessage(Object object) {

    }

    @Override
    public boolean onConnect() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "BLE connected",
                        Toast.LENGTH_LONG).show();
            }
        });
        return false;
    }

    @Override
    public boolean onDisConnect() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "BLE disconnected",
                        Toast.LENGTH_LONG).show();
            }
        });
        return false;
    }

    @Override
    public void onError(Exception e) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "BLE error",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    public class CheckPreviewTask extends AsyncTask<Void, Integer, Void> {
        long scanDelay=300;
        @Override
        protected void onPostExecute(Void result) {
            new DrawOverlayOutline().execute();
            sendPathCordinates();
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
                Thread.sleep(scanDelay);
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
            canvas.drawText("C:" + box.centre+" I:"+box.inclination, canvas.getWidth() / 2, canvas.getHeight() / 2, myPaint);
            int squareOutLineLength1 = (cameraPreviewDisplayWidth * box.l1.length) / 100;
            int squareOutLineCenter1 = (cameraPreviewDisplayWidth * box.l1.centre) / 100;
            int squareOutLineLength2 = (cameraPreviewDisplayWidth * box.l2.length) / 100;
            int squareOutLineCenter2 = (cameraPreviewDisplayWidth * box.l2.centre) / 100;
            int squareOutLineCenter = (cameraPreviewDisplayWidth * box.centre) / 100;
            canvas.drawLine(widthPreviewgap,heightPreviewgap+(canvasHeight-heightPreviewgap*2)/8,canvasWidth-widthPreviewgap,heightPreviewgap+(canvasHeight-heightPreviewgap*2)/8,myPaint);
            canvas.drawLine(widthPreviewgap,heightPreviewgap+(canvasHeight-heightPreviewgap*2)/2,canvasWidth-widthPreviewgap,heightPreviewgap+(canvasHeight-heightPreviewgap*2)/2,myPaint);
            Paint boxPaint = new Paint();
            boxPaint.setColor(Color.argb(255, 0, 0, 255));
            boxPaint.setStrokeWidth(5);
            boxPaint.setStyle(Paint.Style.STROKE);
            Point p1=new Point(widthPreviewgap+squareOutLineCenter1-squareOutLineLength1/2,heightPreviewgap+(canvasHeight-heightPreviewgap*2)/8);
            Point p2=new Point(widthPreviewgap+squareOutLineCenter1+squareOutLineLength1/2,heightPreviewgap+(canvasHeight-heightPreviewgap*2)/8);
            Point p3=new Point(widthPreviewgap+squareOutLineCenter2+squareOutLineLength2/2,heightPreviewgap+(canvasHeight-heightPreviewgap*2)/2);
            Point p4=new Point(widthPreviewgap+squareOutLineCenter2-squareOutLineLength2/2,heightPreviewgap+(canvasHeight-heightPreviewgap*2)/2);
            canvas.drawLine(p1.x,p1.y,p2.x,p2.y,boxPaint);
            canvas.drawLine(p2.x,p2.y,p3.x,p3.y,boxPaint);
            canvas.drawLine(p3.x,p3.y,p4.x,p4.y,boxPaint);
            canvas.drawLine(p4.x,p4.y,p1.x,p1.y,boxPaint);

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
