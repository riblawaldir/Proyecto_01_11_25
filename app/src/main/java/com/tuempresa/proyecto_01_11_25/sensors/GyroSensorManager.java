package com.tuempresa.proyecto_01_11_25.sensors;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.widget.Toast;

import android.view.View;
import android.view.ViewGroup;

public class GyroSensorManager implements SensorEventListener {

    private final SensorManager sensorManager;
    private final Sensor gyroscope;
    private final Activity activity;
    private boolean focusMode = false;
    private long lastTriggerTime = 0;

    public GyroSensorManager(Activity activity) {
        this.activity = activity;
        sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    public void start() {
        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(activity, "No hay giroscopio disponible", Toast.LENGTH_SHORT).show();
        }
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float rotX = event.values[0];
        float rotY = event.values[1];
        float rotZ = event.values[2];


        float velocidad = Math.abs(rotX) + Math.abs(rotY) + Math.abs(rotZ);

        if (velocidad > 5 && (System.currentTimeMillis() - lastTriggerTime > 2000)) {
            lastTriggerTime = System.currentTimeMillis();
            toggleFocusMode();
        }
    }

    private void toggleFocusMode() {
        focusMode = !focusMode;

        activity.runOnUiThread(() -> {
            View layout = ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);

            if (focusMode) {
                layout.setBackgroundColor(0xFF1A237E);
                Toast.makeText(activity, " Modo concentración activado", Toast.LENGTH_SHORT).show();
            } else {
                layout.setBackgroundColor(0xFFFFFFFF);
                Toast.makeText(activity, "Modo concentración desactivado", Toast.LENGTH_SHORT).show();
            }
        });


        if (focusMode) {
            new Handler().postDelayed(() -> {
                focusMode = false;
                activity.runOnUiThread(() -> {
                    View layout = ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
                    layout.setBackgroundColor(0xFFFFFFFF);
                    Toast.makeText(activity, "Tiempo de concentración terminado", Toast.LENGTH_SHORT).show();
                });
            }, 15000);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
