package com.tuempresa.proyecto_01_11_25.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Gestor del sensor acelerómetro para detectar ejercicio/movimiento
 * Detecta movimiento brusco o repetitivo (indicador de ejercicio)
 */
public class AccelerometerSensorManager implements SensorEventListener {

    private static final float MOVEMENT_THRESHOLD = 12.0f; // m/s² - umbral para detectar movimiento significativo
    private static final long MIN_EXERCISE_DURATION_MS = 3000; // 3 segundos de movimiento continuo
    private static final long DEBOUNCE_MS = 5000; // 5 segundos entre detecciones

    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private final OnExerciseDetectedListener listener;

    private boolean isListening = false;
    private boolean exerciseDetected = false; // Flag para evitar múltiples detecciones
    private long movementStartTime = 0;
    private long lastExerciseDetection = 0;
    private boolean isMoving = false;

    public interface OnExerciseDetectedListener {
        void onExerciseDetected();
    }

    public AccelerometerSensorManager(Context context, OnExerciseDetectedListener listener) {
        this.listener = listener;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void start() {
        if (accelerometer != null && !isListening) {
            isListening = true;
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            android.util.Log.d("AccelerometerSensor", "Sensor de acelerómetro iniciado");
        } else if (accelerometer == null) {
            android.util.Log.w("AccelerometerSensor", "Sensor de acelerómetro no disponible");
        }
    }

    public void stop() {
        if (isListening) {
            isListening = false;
            sensorManager.unregisterListener(this);
            isMoving = false;
            movementStartTime = 0;
            android.util.Log.d("AccelerometerSensor", "Sensor de acelerómetro detenido");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No necesitamos manejar cambios de precisión
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isListening || exerciseDetected) return;

        // Valores del acelerómetro (m/s²)
        float accX = event.values[0];
        float accY = event.values[1];
        float accZ = event.values[2];

        // Calcular magnitud total (excluyendo gravedad)
        float magnitude = (float) Math.sqrt(accX * accX + accY * accY + accZ * accZ);
        // Restar gravedad aproximada (9.8 m/s²)
        float movementMagnitude = Math.abs(magnitude - SensorManager.GRAVITY_EARTH);

        long now = System.currentTimeMillis();

        // Verificar si hay movimiento significativo
        if (movementMagnitude > MOVEMENT_THRESHOLD) {
            if (!isMoving) {
                // Iniciar contador de movimiento
                isMoving = true;
                movementStartTime = now;
                android.util.Log.d("AccelerometerSensor", "Movimiento detectado: " + String.format("%.2f", movementMagnitude) + " m/s²");
            } else {
                // Verificar si el movimiento ha durado lo suficiente
                long movementDuration = now - movementStartTime;
                if (movementDuration >= MIN_EXERCISE_DURATION_MS) {
                    // Verificar que haya pasado suficiente tiempo desde la última detección
                    if (now - lastExerciseDetection >= DEBOUNCE_MS) {
                        exerciseDetected = true;
                        lastExerciseDetection = now;
                        android.util.Log.d("AccelerometerSensor", "Ejercicio detectado después de " + movementDuration + "ms de movimiento");
                        
                        if (listener != null) {
                            listener.onExerciseDetected();
                        }
                        
                        // Resetear para permitir futuras detecciones después del debounce
                        resetAfterDelay();
                    }
                }
            }
        } else {
            // Si el movimiento se detiene, resetear contador
            if (isMoving) {
                android.util.Log.d("AccelerometerSensor", "Movimiento detenido");
                isMoving = false;
                movementStartTime = 0;
            }
        }
    }

    /**
     * Resetea el flag de ejercicio después del período de debounce
     */
    private void resetAfterDelay() {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            exerciseDetected = false;
            isMoving = false;
            movementStartTime = 0;
            android.util.Log.d("AccelerometerSensor", "Flag de ejercicio reseteado, listo para nueva detección");
        }, DEBOUNCE_MS);
    }
}

