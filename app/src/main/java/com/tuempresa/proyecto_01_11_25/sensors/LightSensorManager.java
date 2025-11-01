package com.tuempresa.proyecto_01_11_25.sensors;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tuempresa.proyecto_01_11_25.R;

public class LightSensorManager implements SensorEventListener {

    private final SensorManager sensorManager;
    private final Sensor lightSensor;
    private final Activity activity;

    public LightSensorManager(Activity activity) {
        this.activity = activity;
        sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    public void start() {
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(activity, "No hay sensor de luz en este dispositivo", Toast.LENGTH_SHORT).show();
        }
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float luz = event.values[0];

        activity.runOnUiThread(() -> {
            View layout = ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);

            if (layout != null) {
                if (luz < 10) {

                    layout.setBackgroundColor(0xFF222222);
                    Toast.makeText(activity, "Modo nocturno activado ", Toast.LENGTH_SHORT).show();
                } else if (luz > 500) {

                    layout.setBackgroundColor(0xFFFFFFFF);
                    Toast.makeText(activity, "Modo diurno ☀", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
