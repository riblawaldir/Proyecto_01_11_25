package com.tuempresa.proyecto_01_11_25.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Gestor del sensor de luz para detectar modo nocturno
 * Umbral: < 15 lux = modo oscuro, >= 15 lux = modo claro
 */
public class LightSensorManager implements SensorEventListener {

    public interface OnLowLightListener {
        void onLowLight();
        void onNormalLight();
    }

    private static final float LIGHT_THRESHOLD = 15f; // lux
    private static final long DEBOUNCE_MS = 2500; // 2.5 segundos

    private final SensorManager sensorManager;
    private final Sensor lightSensor;
    private final OnLowLightListener listener;

    private boolean isListening = false;
    private boolean currentStateIsNight = false; // Estado actual detectado
    private long lastNotificationTime = 0;

    public LightSensorManager(Context context, OnLowLightListener listener) {
        this.listener = listener;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    public void start() {
        if (lightSensor != null && !isListening) {
            isListening = true;
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void stop() {
        if (isListening) {
            isListening = false;
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No necesitamos manejar cambios de precisión
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isListening) return;

        float lightValue = event.values[0];
        boolean isLowLight = lightValue < LIGHT_THRESHOLD;
        long now = System.currentTimeMillis();

        // Log para debugging (comentar en producción)
        android.util.Log.d("LightSensor", String.format("Luz: %.2f lux, isLowLight: %s, currentState: %s", 
            lightValue, isLowLight, currentStateIsNight));

        // Inicializar estado en la primera lectura
        if (lastNotificationTime == 0) {
            currentStateIsNight = isLowLight;
            lastNotificationTime = now;
            android.util.Log.d("LightSensor", "Estado inicial: " + (isLowLight ? "BAJA" : "NORMAL"));
            return;
        }

        // Solo notificar si cambió el estado Y pasó el debounce
        if (isLowLight != currentStateIsNight) {
            if (now - lastNotificationTime >= DEBOUNCE_MS) {
                android.util.Log.d("LightSensor", "Cambio detectado: " + (isLowLight ? "BAJA" : "NORMAL") + " después de " + (now - lastNotificationTime) + "ms");
                currentStateIsNight = isLowLight;
                lastNotificationTime = now;

                // Notificar cambio
                if (isLowLight) {
                    listener.onLowLight();
                } else {
                    listener.onNormalLight();
                }
            } else {
                android.util.Log.d("LightSensor", "Esperando debounce: " + (DEBOUNCE_MS - (now - lastNotificationTime)) + "ms restantes");
            }
        }
    }
}
