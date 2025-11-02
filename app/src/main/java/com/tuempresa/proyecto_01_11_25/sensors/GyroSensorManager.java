package com.tuempresa.proyecto_01_11_25.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Gestor del sensor giroscopio para activar modo foco
 * Requiere 3 giros seguidos (dentro de 1.2 segundos cada uno)
 */
public class GyroSensorManager implements SensorEventListener {

    private static final double ROTATION_THRESHOLD = 4.5; // rad/s
    private static final long MAX_TIME_BETWEEN_ROTATIONS = 1200; // ms
    private static final int REQUIRED_ROTATIONS = 3;

    private final SensorManager sensorManager;
    private final Sensor gyroscope;
    private final OnFocusModeListener listener;

    private boolean isListening = false;
    private boolean isFocusModeActive = false; // Flag para evitar múltiples activaciones
    private int rotationCount = 0;
    private long lastRotationTime = 0;

    public interface OnFocusModeListener {
        void onFocusModeActivated();
    }

    public GyroSensorManager(Context context, OnFocusModeListener listener) {
        this.listener = listener;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    public void start() {
        if (gyroscope != null && !isListening) {
            isListening = true;
            rotationCount = 0; // Resetear contador al iniciar
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void stop() {
        if (isListening) {
            isListening = false;
            sensorManager.unregisterListener(this);
            rotationCount = 0; // Resetear al detener
        }
    }

    /**
     * Permite resetear el modo foco (útil cuando se desactiva manualmente)
     */
    public void resetFocusMode() {
        isFocusModeActive = false;
        rotationCount = 0;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No necesitamos manejar cambios de precisión
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isListening || isFocusModeActive) return;

        float rotX = Math.abs(event.values[0]);
        float rotY = Math.abs(event.values[1]);
        float rotZ = Math.abs(event.values[2]);

        // Calcular magnitud de rotación
        double rotationMagnitude = Math.sqrt(rotX * rotX + rotY * rotY + rotZ * rotZ);

        if (rotationMagnitude > ROTATION_THRESHOLD) {
            long now = System.currentTimeMillis();

            // Si pasó mucho tiempo desde la última rotación, resetear contador
            if (now - lastRotationTime > MAX_TIME_BETWEEN_ROTATIONS) {
                rotationCount = 1;
                android.util.Log.d("GyroSensor", "Rotación detectada (resetear contador): " + String.format("%.2f", rotationMagnitude));
            } else {
                rotationCount++;
                android.util.Log.d("GyroSensor", "Rotación " + rotationCount + "/" + REQUIRED_ROTATIONS + ": " + String.format("%.2f", rotationMagnitude));
            }

            lastRotationTime = now;

            // Si se alcanzaron 3 rotaciones seguidas, activar modo foco
            if (rotationCount >= REQUIRED_ROTATIONS) {
                android.util.Log.d("GyroSensor", "¡Modo foco activado por " + REQUIRED_ROTATIONS + " giros!");
                isFocusModeActive = true;
                rotationCount = 0;
                listener.onFocusModeActivated();
            }
        }
    }
}
