package com.tuempresa.proyecto_01_11_25.sensors;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.Toast;

import com.tuempresa.proyecto_01_11_25.model.Habit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class StepSensorManager implements SensorEventListener {

    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private final Activity activity;
    private long lastUpdate = 0;
    private float lastX, lastY, lastZ;

    public StepSensorManager(Activity activity) {
        this.activity = activity;
        sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void start() {
        if (accelerometer != null)
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        else
            Toast.makeText(activity, "No hay acelerómetro disponible", Toast.LENGTH_SHORT).show();
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastUpdate) > 500) {
            long diff = currentTime - lastUpdate;
            lastUpdate = currentTime;

            float speed = Math.abs(x + y + z - lastX - lastY - lastZ) / diff * 10000;

            if (speed > 800) {
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, "Movimiento detectado 🏃‍♂️", Toast.LENGTH_SHORT).show();
                    marcarEjercicioComoCompletado();
                });
            }

            lastX = x;
            lastY = y;
            lastZ = z;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void marcarEjercicioComoCompletado() {
        SharedPreferences prefs = activity.getSharedPreferences("HabitusPrefs", Context.MODE_PRIVATE);
        String json = prefs.getString("habits", null);
        List<Habit> habits = new ArrayList<>();

        if (json != null) {
            try {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    Habit h = new Habit(
                            obj.getString("name"),
                            obj.getString("goal"),
                            obj.getString("period"),
                            obj.getString("type"),
                            obj.getBoolean("done")
                    );

                    if (h.getName().toLowerCase().contains("ejercicio")) {
                        h.setDone(true);
                    }
                    habits.add(h);
                }

                JSONArray newArray = new JSONArray();
                for (Habit h : habits) {
                    JSONObject o = new JSONObject();
                    o.put("name", h.getName());
                    o.put("goal", h.getGoal());
                    o.put("period", h.getPeriod());
                    o.put("type", h.getType());
                    o.put("done", h.isDone());
                    newArray.put(o);
                }

                prefs.edit().putString("habits", newArray.toString()).apply();
                Toast.makeText(activity, "Hábito 'Ejercicio' completado 💪", Toast.LENGTH_SHORT).show();

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
