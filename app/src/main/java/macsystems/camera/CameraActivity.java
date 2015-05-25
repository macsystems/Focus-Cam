package macsystems.camera;

import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import de.macsystems.camera.R;


public class CameraActivity extends Activity implements SurfaceHolder.Callback, SensorEventListener {

    private SurfaceView surfaceView;

    private volatile Camera camera;

    private SensorManager sensorManager;

    private AutoFocus focus = new AutoFocus();

    private volatile boolean needsFocus = false;

    private TextView statusText;
    private TextView focusMode;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        statusText = (TextView) findViewById(R.id.status);
        focusMode = (TextView) findViewById(R.id.focus_mode);
        this.surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        this.surfaceView.getHolder().setKeepScreenOn(true);
        this.surfaceView.getHolder().addCallback(this);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        sensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    public void surfaceCreated(final SurfaceHolder surfaceHolder) {

        final int nrOfCams = Camera.getNumberOfCameras();
        final Camera.CameraInfo[] infos = new Camera.CameraInfo[nrOfCams];
        for (int cam = 0; cam < nrOfCams; cam++) {
            final Camera.CameraInfo info = new Camera.CameraInfo();
            infos[cam] = info;
            Camera.getCameraInfo(cam, infos[cam]);
        }

        final int bestIndex = findBestCamera(infos);
        camera = Camera.open(bestIndex);
    }


    private int findBestCamera(final Camera.CameraInfo[] infos) {
        // find back first
        for (int i = 0; i < infos.length; i++) {
            final Camera.CameraInfo info = infos[i];
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return i;
            }
        }
        // find front
        for (int i = 0; i < infos.length; i++) {
            final Camera.CameraInfo info = infos[i];
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return i;
            }
        }

        throw new RuntimeException("No cam found:"+ Arrays.toString(infos));

    }



    @Override
    public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
        try {
            camera.setPreviewDisplay(holder);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        //
        final Camera.Parameters params = camera.getParameters();
        final String bestFocusMode = findFocusMode(params.getSupportedFocusModes());
        focusMode.setText("Mode :"+bestFocusMode);

        params.setFocusMode(bestFocusMode);
        camera.setParameters(params);
        camera.startPreview();
    }

    private String findFocusMode(List<String> focusMode) {
        if(focusMode.size() == 1)
        {
            return focusMode.get(0);
        }
        if(focusMode.contains(Camera.Parameters.FOCUS_MODE_AUTO))
        {
            return Camera.Parameters.FOCUS_MODE_AUTO;
        }
        else if(focusMode.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
        {
            return Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
        }
        else if(focusMode.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
        {
            return Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;
        }
        throw new RuntimeException("No focus modes found that are good: "+Arrays.toString(focusMode.toArray()));
    }

    @Override
    public void surfaceDestroyed(final SurfaceHolder surfaceHolder) {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override
    public void onSensorChanged(final SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            getAccelerometer(sensorEvent);
        }
    }

    private void getAccelerometer(final SensorEvent sensorEvent) {
        if (camera == null) {
            return;
        }

        final float[] values = sensorEvent.values;
        final float x = values[0];
        final float y = values[1];
        final float z = values[2];

        final float movement = (x * x + y * y + z * z)
                / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
        if (movement >= 1.08 && !needsFocus) {
            needsFocus = true;
            camera.autoFocus(focus);
            statusText.setText("Autofocus : Focusing started");

            //Log.d(getClass().getSimpleName(), "accelationSquareRoot: " + movement);
        }
    }

    @Override
    public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
    }

    private class AutoFocus implements Camera.AutoFocusCallback {
        @Override
        public void onAutoFocus(final boolean success, final Camera camera) {
            if (success) {
                statusText.setText("Autofocus : Success");
                needsFocus = false;
                return;
            }
            camera.autoFocus(this);
        }
    }
}
