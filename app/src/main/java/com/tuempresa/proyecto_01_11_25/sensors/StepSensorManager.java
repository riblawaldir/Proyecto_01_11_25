package com.tuempresa.proyecto_01_11_25.sensors;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.Toast;
public class StepSensorManager implements SensorEventListener {
    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private final Context context;
    private long lastUpdate=0;
    private float lastX, lastY, lastZ;

    public StepSensorManager(Context context){
        this.context = context;
        sensorManager = (SensorManager) context.getSystemService(context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }
    public void start(){
        sensorManager.registerListener(this, accelerometer,SensorManager.SENSOR_DELAY_NORMAL);
    }
    public void stop (){
        sensorManager.unregisterListener(this);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];

        long currentTime = System.currentTimeMillis();
        if ((currentTime-lastUpdate)> 500 ){
            long diff = currentTime - lastUpdate;
            lastUpdate = currentTime;

            float speed = Math.abs(x + y + z - lastX- lastY- lastZ)/ diff * 10000;
            if(speed > 800){
                Toast.makeText(context,"Movimiento detectado", Toast.LENGTH_SHORT).show();
            }
            lastX = x;
            lastY = y;
            lastZ = z;
        }

    }
}
