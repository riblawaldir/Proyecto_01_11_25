package com.tuempresa.proyecto_01_11_25.sensors;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.tuempresa.proyecto_01_11_25.model.HabitEvent;
import com.tuempresa.proyecto_01_11_25.model.HabitEventStore;

public class StepSensorManager {

    private static final float TARGET_METERS = 150f;

    private final Context ctx;
    private final FusedLocationProviderClient fused;
    private Location last;
    private float accMeters = 0f;
    private boolean done = false;

    private LocationCallback callback;
    private Runnable onWalkCompletedCallback;

    public StepSensorManager(Context ctx) {
        this(ctx, null);
    }

    public StepSensorManager(Context ctx, Runnable onWalkCompletedCallback) {
        this.ctx = ctx;
        this.onWalkCompletedCallback = onWalkCompletedCallback;
        this.fused = LocationServices.getFusedLocationProviderClient(ctx);
    }

    @SuppressLint("MissingPermission")
    public void start() {
        done = false;
        accMeters = 0f;
        if (!hasPermission()) {
            Toast.makeText(ctx, "Sin permiso de ubicación", Toast.LENGTH_SHORT).show();
            return;
        }

        LocationRequest req = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setMinUpdateIntervalMillis(1500)
                .setMinUpdateDistanceMeters(2)
                .build();

        callback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                for (Location loc : result.getLocations()) {
                    if (last != null) {
                        accMeters += last.distanceTo(loc);
                    }
                    last = loc;

                    if (!done && accMeters >= TARGET_METERS) {
                        done = true;
                        HabitEventStore.add(new HabitEvent(
                                loc.getLatitude(),
                                loc.getLongitude(),
                                "Caminar completado (" + (int)accMeters + " m) 🚶",
                                HabitEvent.HabitType.WALK
                        ));
                        Toast.makeText(ctx, "Meta de caminar alcanzada", Toast.LENGTH_LONG).show();
                        
                        // Notificar callback si existe
                        if (onWalkCompletedCallback != null) {
                            onWalkCompletedCallback.run();
                        }
                        
                        stop();
                        break;
                    }
                }
            }
        };

        fused.requestLocationUpdates(req, callback, ctx.getMainLooper());
    }

    public void stop() {
        if (callback != null) fused.removeLocationUpdates(callback);
        callback = null;
    }

    private boolean hasPermission() {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }
}
